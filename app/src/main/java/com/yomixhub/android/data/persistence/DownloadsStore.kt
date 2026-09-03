package com.yomixhub.android.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yomixhub.android.data.DownloadEntry
import com.yomixhub.android.data.DownloadStatus
import com.yomixhub.android.data.source.ChapterContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.downloadsDataStore by preferencesDataStore(name = "yomixhub_downloads")

/** Everything persisted for the downloads feature. */
data class StoredDownloads(
    val entries: List<DownloadEntry>,
    /** Cached reader content by chapter id (text paragraphs / page URLs). */
    val contents: Map<Long, ChapterContent>,
)

/**
 * Disk storage for downloads (DataStore Preferences + JSON), so the
 * "Загрузки" tab and offline chapter reading survive app restarts.
 *
 * Two keys are kept: the download queue entries (with their title and
 * chapter references) and the downloaded chapter contents.
 */
object DownloadsStore {

    private val KEY_ENTRIES = stringPreferencesKey("download_entries")
    private val KEY_CONTENTS = stringPreferencesKey("downloaded_contents")

    /** Reads the persisted state; returns empty data when nothing was stored. */
    suspend fun load(context: Context): StoredDownloads = withContext(Dispatchers.IO) {
        val prefs = context.downloadsDataStore.data.first()
        val entries = prefs[KEY_ENTRIES]
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseEntries)
            ?: emptyList()
        val contents = prefs[KEY_CONTENTS]
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseContents)
            ?: emptyMap()
        StoredDownloads(entries = entries, contents = contents)
    }

    /** Persists the given snapshot atomically (DataStore serializes edits). */
    suspend fun save(
        context: Context,
        entries: List<DownloadEntry>,
        contents: Map<Long, ChapterContent>,
    ) {
        context.downloadsDataStore.edit { prefs ->
            prefs[KEY_ENTRIES] = serializeEntries(entries)
            prefs[KEY_CONTENTS] = serializeContents(contents)
        }
    }

    // ------------------------------------------------------------------ //
    // Serialization: entries
    // ------------------------------------------------------------------ //

    private fun serializeEntries(entries: List<DownloadEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("title", titleToJson(entry.title))
                    .put("chapter", chapterToJson(entry.chapterRef))
                    .put("progress", entry.progress.toDouble())
                    .put("status", entry.status.name),
            )
        }
        return array.toString()
    }

    private fun parseEntries(json: String): List<DownloadEntry> {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<DownloadEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optJSONObject("title")?.let(::titleFromJson) ?: continue
            val chapter = obj.optJSONObject("chapter")?.let(::chapterFromJson) ?: continue
            val status = runCatching {
                DownloadStatus.valueOf(obj.optString("status"))
            }.getOrDefault(DownloadStatus.DONE)
            out.add(
                DownloadEntry(
                    title = title,
                    chapterRef = chapter,
                    progress = obj.optDouble("progress", 1.0).toFloat(),
                    status = status,
                ),
            )
        }
        return out
    }

    // ------------------------------------------------------------------ //
    // Serialization: chapter contents
    // ------------------------------------------------------------------ //

    private fun serializeContents(contents: Map<Long, ChapterContent>): String {
        val array = JSONArray()
        contents.forEach { (chapterId, content) ->
            array.put(
                JSONObject()
                    .put("id", chapterId)
                    .put("content", contentToJson(content)),
            )
        }
        return array.toString()
    }

    private fun parseContents(json: String): Map<Long, ChapterContent> {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyMap()
        val out = HashMap<Long, ChapterContent>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val content = obj.optJSONObject("content")?.let(::parseContent) ?: continue
            out[obj.optLong("id")] = content
        }
        return out
    }

    private fun contentToJson(content: ChapterContent): JSONObject = JSONObject().apply {
        put("title", content.title)
        put("paragraphs", JSONArray(content.paragraphs))
        put("pages", JSONArray(content.pages))
    }

    private fun parseContent(obj: JSONObject): ChapterContent = ChapterContent(
        title = obj.optString("title"),
        paragraphs = obj.optJSONArray("paragraphs").toStringList(),
        pages = obj.optJSONArray("pages").toStringList(),
    )

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            val value = optString(i)
            if (value.isNotBlank()) out.add(value)
        }
        return out
    }
}
