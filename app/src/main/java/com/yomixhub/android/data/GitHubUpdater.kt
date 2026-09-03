package com.yomixhub.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub for app updates:
 * `GET https://api.github.com/repos/Animesash/YomixHub/releases/latest`.
 *
 * The latest release tag is compared with [com.yomixhub.android.BuildConfig.VERSION_NAME];
 * when newer, the user is offered the release's APK asset, opened in the
 * browser for download.
 */
object GitHubUpdater {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/Animesash/YomixHub/releases/latest"
    private const val RELEASES_PAGE =
        "https://github.com/Animesash/YomixHub/releases"

    /** The important bits of a GitHub release. */
    data class Release(
        val tag: String,
        val name: String?,
        /** Direct browser URL of the first `.apk` asset, if any. */
        val apkUrl: String?,
        /** Fallback: the release page on GitHub. */
        val pageUrl: String,
        /** Release notes (markdown body), may be long. */
        val notes: String?,
    )

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Fetches the latest release; throws [IOException] on network/HTTP errors. */
    suspend fun fetchLatestRelease(): Release = withContext(Dispatchers.IO) {
        // The GitHub API rejects requests without a User-Agent.
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("User-Agent", "YomixHub-Android")
            .header("Accept", "application/vnd.github+json")
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // 404 – the repository has no published releases yet.
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body?.string()
            if (body.isNullOrBlank()) throw IOException("Пустой ответ GitHub")
            parse(JSONObject(body))
        }
    }

    /**
     * Numeric version comparison of a release tag against the app version.
     * Tolerates a leading `v` and separators like `.`/`-`/`_` (e.g. `v1.2.3`
     * vs `1.2`); falls back to plain inequality when either side cannot be
     * parsed as numbers.
     */
    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = numericParts(remoteTag)
        val current = numericParts(currentVersion)
        if (remote != null && current != null) {
            val depth = maxOf(remote.size, current.size)
            for (i in 0 until depth) {
                val r = remote.getOrElse(i) { 0 }
                val c = current.getOrElse(i) { 0 }
                if (r != c) return r > c
            }
            return false
        }
        return remoteTag.isNotBlank() && normalize(remoteTag) != normalize(currentVersion)
    }

    private fun parse(root: JSONObject): Release {
        val assets: JSONArray = root.optJSONArray("assets") ?: JSONArray()
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                apkUrl = url
                break
            }
        }

        return Release(
            tag = root.optString("tag_name"),
            name = root.optString("name").takeIf { it.isNotBlank() },
            apkUrl = apkUrl,
            pageUrl = root.optString("html_url").takeIf { it.isNotBlank() } ?: RELEASES_PAGE,
            notes = root.optString("body").takeIf { it.isNotBlank() },
        )
    }

    private fun normalize(version: String): String =
        version.trim().trimStart('v', 'V')

    private fun numericParts(version: String): List<Int>? {
        val parts = normalize(version)
            .split('.', '-', '_', '+', ' ')
            .mapNotNull { it.toIntOrNull() }
        return parts.takeIf { it.isNotEmpty() }
    }
}
