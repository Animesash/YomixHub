package com.yomixhub.android.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Client for the shared LibGroup JSON API (`https://api.cdnlibs.org/api`)
 * that powers the whole family of sites: MangaLib (site 1), HentaiLib
 * (site 4) and RanobeLib (site 3) – all selected through the `Site-Id`
 * header / `site_id[]` query parameter.
 *
 * Endpoints:
 *  * list / search – `GET /api/manga?site_id[]=…[&q=…]&page=…&sort=rate`
 *  * latest       – `GET /api/latest-updates?page=…`
 *  * details      – `GET /api/manga/{slug_url}?fields[]=summary&…`
 *  * chapters     – `GET /api/manga/{slug_url}/chapters`
 *  * reader       – `GET /api/manga/{slug_url}/chapter?volume=…&number=…[&branch_id=…]`
 *
 * Reader content comes in two shapes: light novels return an HTML-ish string
 * (`"абзац</p>абзац</p>…"`) or a tiptap JSON document (both handled by
 * [extractParagraphs]), manga/manhwa return `pages[]` with *relative* image
 * paths that must be prefixed with an image server URL from
 * `GET /api/constants?fields[]=imageServers`.
 */
open class LibGroupSource(
    override val id: String,
    override val name: String,
    override val baseUrl: String,
    private val siteId: Int,
) : Source {

    override val imageHeaders: Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Referer" to baseUrl,
    )

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** The API serves some routes only to "browser" requests with proper origin headers. */
    private val apiHeaders: Headers = Headers.headersOf(
        "User-Agent", USER_AGENT,
        "Accept", "application/json, text/plain, */*",
        "Origin", baseUrl,
        "Referer", "$baseUrl/",
        "Site-Id", siteId.toString(),
    )

    // Small in-memory caches so that details -> chapters -> reader -> back
    // navigation does not re-hit the API.
    private val detailsCache = ConcurrentHashMap<String, SourceDetails>()
    private val chaptersCache = ConcurrentHashMap<String, List<SourceChapter>>()
    private val textCache = ConcurrentHashMap<Long, ChapterContent>()

    @Volatile
    private var cachedImageServer: String? = null

    override suspend fun popularTitles(page: Int): SourcePage =
        list("$API/manga?site_id[]=$siteId&sort=rate&sort_type=desc&page=$page", page)

    override suspend fun latestTitles(page: Int): SourcePage =
        list("$API/latest-updates?page=$page", page)

    override suspend fun searchTitles(query: String, page: Int): SourcePage {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return list("$API/manga?site_id[]=$siteId&q=$q&page=$page", page)
    }

    override suspend fun titleDetails(slugUrl: String): SourceDetails = withContext(Dispatchers.IO) {
        detailsCache[slugUrl]?.let { return@withContext it }

        val data = getJson(
            "$API/manga/$slugUrl" +
                "?fields[]=eng_name&fields[]=otherNames&fields[]=summary&fields[]=genres" +
                "&fields[]=tags&fields[]=authors&fields[]=artists&fields[]=status_id" +
                "&fields[]=publisher&fields[]=format",
        ).getJSONObject("data")

        val details = SourceDetails(
            title = parseTitle(data, slugUrl),
            description = extractText(data.opt("summary")),
            authors = nameList(data.optJSONArray("authors")),
            genres = nameList(data.optJSONArray("genres")),
            tags = nameList(data.optJSONArray("tags")),
            status = data.optJSONObject("status")?.optString("label")?.takeIf { it.isNotBlank() },
            translationStatus = data.optJSONObject("scanlateStatus")?.optString("label")
                ?.takeIf { it.isNotBlank() },
            publisher = data.optJSONObject("publisher")?.optString("name")?.takeIf { it.isNotBlank() },
        )
        detailsCache[slugUrl] = details
        details
    }

    override suspend fun chapters(slugUrl: String): List<SourceChapter> = withContext(Dispatchers.IO) {
        chaptersCache[slugUrl]?.let { return@withContext it }

        val data = getJson("$API/manga/$slugUrl/chapters").optJSONArray("data") ?: JSONArray()
        val chapters = ArrayList<SourceChapter>(data.length())
        for (i in 0 until data.length()) {
            val chapter = data.optJSONObject(i) ?: continue
            parseChapter(chapter, slugUrl)?.let(chapters::add)
        }

        // API returns oldest-first; readers expect the newest chapter on top.
        val result = chapters.asReversed()
        chaptersCache[slugUrl] = result
        result
    }

    override suspend fun chapterText(chapter: SourceChapter): ChapterContent = withContext(Dispatchers.IO) {
        textCache[chapter.id]?.let { return@withContext it }

        val url = buildString {
            append(API).append("/manga/").append(chapter.slugUrl)
                .append("/chapter?volume=").append(chapter.volume)
                .append("&number=").append(chapter.number)
            chapter.branchId?.let { append("&branch_id=").append(it) }
        }

        val data = getJson(url).getJSONObject("data")

        // Novels: "content" (HTML-ish string or tiptap JSON).
        val content = if (data.isNull("content")) null else data.opt("content")

        // Manga: "pages" with relative image paths -> absolute server URLs.
        val pages = data.optJSONArray("pages")
        val pageUrls = if (pages != null && pages.length() > 0) {
            val server = imageServerUrl()
            val out = ArrayList<String>(pages.length())
            for (i in 0 until pages.length()) {
                val path = pages.optJSONObject(i)?.optString("url")?.takeIf { it.isNotBlank() }
                if (path != null) {
                    out.add(server.trimEnd('/') + "/" + path.trimStart('/'))
                }
            }
            out
        } else {
            emptyList()
        }

        val text = ChapterContent(
            title = data.optString("name").takeIf { it.isNotBlank() }
                ?: "Том ${chapter.volume} · Глава ${chapter.number}",
            paragraphs = extractParagraphs(content),
            pages = pageUrls,
        )
        textCache[chapter.id] = text
        text
    }

    // ------------------------------------------------------------------ //
    // Transport
    // ------------------------------------------------------------------ //

    private suspend fun list(url: String, page: Int): SourcePage = withContext(Dispatchers.IO) {
        val root = getJson(url)
        val items = root.optJSONArray("data") ?: JSONArray()
        val titles = ArrayList<SourceTitle>(items.length())
        for (i in 0 until items.length()) {
            items.optJSONObject(i)?.let { titles.add(parseTitle(it)) }
        }
        SourcePage(
            titles = titles,
            page = page,
            hasNextPage = root.optJSONObject("meta")?.optBoolean("has_next_page", false) == true,
        )
    }

    private fun getJson(url: String): JSONObject = try {
        val request = Request.Builder().url(url).headers(apiHeaders).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw SourceException.Http(response.code)
            val body = response.body?.string()
            if (body.isNullOrBlank()) throw SourceException.EmptyResponse
            val root = JSONObject(body)
            // The API reports missing titles as HTTP 200 + {"data":{"toast":"Not Found"}}.
            root.optJSONObject("data")?.optJSONObject("toast")?.let {
                throw SourceException.NotFound("Тайтл")
            }
            root
        }
    } catch (e: IOException) {
        throw SourceException.Network(e)
    } catch (e: JSONException) {
        throw SourceException.Parse(e)
    }

    /**
     * Image server base url for this site, picked the same way the official
     * clients do: the "compress" server when available, otherwise the first
     * server serving this site.
     */
    @Synchronized
    private fun imageServerUrl(): String {
        cachedImageServer?.let { return it }

        val servers = getJson("$API/constants?fields[]=imageServers")
            .optJSONObject("data")
            ?.optJSONArray("imageServers")

        val candidates = ArrayList<JSONObject>()
        if (servers != null) {
            for (i in 0 until servers.length()) {
                val server = servers.optJSONObject(i) ?: continue
                if (serverServesSite(server) && server.optString("url").isNotBlank()) {
                    candidates.add(server)
                }
            }
        }

        val chosen = candidates.firstOrNull { it.optString("id") == "compress" }
            ?: candidates.firstOrNull()
        val url = chosen?.optString("url")?.takeIf { it.isNotBlank() }
            ?: throw SourceException.Parse(what = "Не удалось получить сервер изображений")

        cachedImageServer = url
        return url
    }

    private fun serverServesSite(server: JSONObject): Boolean {
        val ids = server.optJSONArray("site_ids") ?: return false
        for (i in 0 until ids.length()) {
            if (ids.optInt(i) == siteId) return true
        }
        return false
    }

    // ------------------------------------------------------------------ //
    // JSON mapping
    // ------------------------------------------------------------------ //

    private fun parseTitle(item: JSONObject, slugUrlOverride: String? = null): SourceTitle {
        val cover = item.optJSONObject("cover")
        val slugUrl = slugUrlOverride ?: item.optString("slug_url")
        return SourceTitle(
            id = item.optLong("id"),
            name = listOf("rus_name", "eng_name", "name").firstNotNullOfOrNull { key ->
                item.optString(key).takeIf { it.isNotBlank() }
            } ?: slugUrl,
            originalName = item.optString("name"),
            coverUrl = cover?.optString("md")?.takeIf { it.isNotBlank() }
                ?: cover?.optString("default")?.takeIf { it.isNotBlank() }
                ?: cover?.optString("thumbnail")?.takeIf { it.isNotBlank() },
            slugUrl = slugUrl,
            webUrl = "$baseUrl/ru/manga/$slugUrl",
            rating = item.optJSONObject("rating")?.optString("average")?.toDoubleOrNull() ?: 0.0,
            status = item.optJSONObject("status")?.optString("label")?.takeIf { it.isNotBlank() },
            country = item.optJSONObject("type")?.optString("label")?.takeIf { it.isNotBlank() },
            ageRating = item.optJSONObject("ageRestriction")?.optString("label")?.takeIf { it.isNotBlank() },
            releaseYear = item.optString("releaseDateString").takeIf { it.isNotBlank() },
        )
    }

    private fun parseChapter(chapter: JSONObject, slugUrl: String): SourceChapter? {
        val branches = chapter.optJSONArray("branches") ?: return null

        // Pick the first branch that is not on moderation and not restricted
        // (paid / closed).
        var chosen: JSONObject? = null
        for (i in 0 until branches.length()) {
            val branch = branches.optJSONObject(i) ?: continue
            if (branch.optJSONObject("moderation")?.optInt("id", 1) == 0) continue
            val restricted = branch.optJSONObject("restricted_view")
            if (restricted != null && !restricted.optBoolean("is_open", true)) continue
            chosen = branch
            break
        }
        val branch = chosen ?: return null

        val volume = chapter.optString("volume")
        val number = chapter.optString("number")
        return SourceChapter(
            id = chapter.optLong("id"),
            volume = volume,
            number = number,
            name = chapter.optString("name").takeIf { it.isNotBlank() },
            branchId = if (branch.has("branch_id") && !branch.isNull("branch_id")) {
                branch.optLong("branch_id")
            } else {
                null
            },
            teamName = branch.optJSONArray("teams")?.optJSONObject(0)?.optString("name")
                ?.takeIf { it.isNotBlank() },
            createdAt = branch.optString("created_at").takeIf { it.isNotBlank() },
            slugUrl = slugUrl,
            webUrl = "$baseUrl/ru/manga/$slugUrl/read/v$volume/c$number",
            sourceId = id,
        )
    }

    private fun nameList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let(out::add)
        }
        return out
    }

    // ------------------------------------------------------------------ //
    // Content extraction
    // ------------------------------------------------------------------ //

    /**
     * Chapter / summary content arrives in two shapes:
     *  * a string with `</p>` paragraph separators (sometimes proper `<p>` HTML),
     *  * a tiptap JSON document: `{ "content": [ { "type": "paragraph",
     *    "content": [ { "type": "text", "text": "…" } ] } ] }`.
     */
    internal fun extractParagraphs(content: Any?): List<String> = when (content) {
        null -> emptyList()
        is String -> htmlParagraphs(content)
        is JSONObject -> tiptapParagraphs(content)
        is JSONArray -> {
            val out = ArrayList<String>()
            for (i in 0 until content.length()) {
                val node = content.opt(i) ?: continue
                when (node) {
                    is JSONObject -> out += tiptapParagraphs(node)
                    is String -> out += htmlParagraphs(node)
                }
            }
            out
        }

        else -> emptyList()
    }

    internal fun extractText(content: Any?): String = extractParagraphs(content).joinToString("\n")

    private fun htmlParagraphs(html: String): List<String> =
        html.split("</p>", "</P>")
            .map { paragraph -> Jsoup.parse(paragraph).text().trim() }
            .filter { it.isNotEmpty() }

    private fun tiptapParagraphs(doc: JSONObject): List<String> {
        val out = ArrayList<String>()
        collectTiptapParagraphs(doc, out)
        return out.filter { it.isNotBlank() }
    }

    private fun collectTiptapParagraphs(node: JSONObject, out: MutableList<String>) {
        val content = node.optJSONArray("content") ?: return
        for (i in 0 until content.length()) {
            val child = content.optJSONObject(i) ?: continue
            when (child.optString("type")) {
                "paragraph", "heading" -> out.add(tiptapInlineText(child))
                else -> collectTiptapParagraphs(child, out)
            }
        }
    }

    private fun tiptapInlineText(node: JSONObject): String {
        val builder = StringBuilder()
        appendTiptapInline(node, builder)
        return builder.toString().trim()
    }

    private fun appendTiptapInline(node: JSONObject, builder: StringBuilder) {
        when (node.optString("type")) {
            "text" -> builder.append(node.optString("text"))
            "hardBreak" -> builder.append('\n')
        }
        val content = node.optJSONArray("content") ?: return
        for (i in 0 until content.length()) {
            content.optJSONObject(i)?.let { appendTiptapInline(it, builder) }
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        private const val API = "https://api.cdnlibs.org/api"
    }
}
