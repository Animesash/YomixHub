package com.yomixhub.android.data.persistence

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.yomixhub.android.data.Title
import com.yomixhub.android.data.TitleFormat
import com.yomixhub.android.data.source.SourceChapter
import org.json.JSONObject

/**
 * JSON (de)serialization of the domain models, shared by the DataStore
 * persistence layer ([DownloadsStore], [BookmarksStore]) and the Firestore
 * bookmark sync ([com.yomixhub.android.data.BookmarkRepository]).
 *
 * Compose [Color]s are stored as ARGB ints; nullable fields are omitted so
 * JSON objects only ever hold non-null values (Firestore-friendly).
 */
internal fun titleToJson(title: Title): JSONObject = JSONObject().apply {
    put("id", title.id)
    put("name", title.name)
    put("author", title.author)
    put("format", title.format.name)
    put("volumes", title.volumes)
    put("chapters", title.chapters)
    put("rating", title.rating)
    put("unread", title.unreadChapters)
    title.status?.let { put("releaseStatus", it) }
    title.coverUrl?.let { put("coverUrl", it) }
    title.sourceId?.let { put("sourceId", it) }
    title.detailsSlug?.let { put("slug", it) }
    put("from", title.coverFrom.toArgb())
    put("to", title.coverTo.toArgb())
}

internal fun titleFromJson(obj: JSONObject): Title = Title(
    id = obj.optInt("id"),
    name = obj.optString("name"),
    author = obj.optString("author"),
    format = runCatching {
        TitleFormat.valueOf(obj.optString("format"))
    }.getOrDefault(TitleFormat.MANGA),
    volumes = obj.optInt("volumes"),
    chapters = obj.optInt("chapters"),
    rating = obj.optDouble("rating", 0.0),
    unreadChapters = obj.optInt("unread", 0),
    status = obj.optString("releaseStatus").takeIf { it.isNotBlank() }
        // Backwards compatibility with the old DownloadsStore format.
        ?: obj.optString("status").takeIf { it.isNotBlank() },
    coverUrl = obj.optString("coverUrl").takeIf { it.isNotBlank() },
    sourceId = obj.optString("sourceId").takeIf { it.isNotBlank() },
    detailsSlug = obj.optString("slug").takeIf { it.isNotBlank() },
    coverFrom = Color(obj.optInt("from")),
    coverTo = Color(obj.optInt("to")),
)

internal fun chapterToJson(chapter: SourceChapter): JSONObject = JSONObject().apply {
    put("id", chapter.id)
    put("volume", chapter.volume)
    put("number", chapter.number)
    chapter.name?.let { put("name", it) }
    chapter.branchId?.let { put("branchId", it) }
    chapter.teamName?.let { put("team", it) }
    chapter.createdAt?.let { put("createdAt", it) }
    put("slug", chapter.slugUrl)
    put("web", chapter.webUrl)
    put("sourceId", chapter.sourceId)
}

internal fun chapterFromJson(obj: JSONObject): SourceChapter = SourceChapter(
    id = obj.optLong("id"),
    volume = obj.optString("volume"),
    number = obj.optString("number"),
    name = obj.optString("name").takeIf { it.isNotBlank() },
    branchId = if (obj.has("branchId") && !obj.isNull("branchId")) {
        obj.optLong("branchId")
    } else {
        null
    },
    teamName = obj.optString("team").takeIf { it.isNotBlank() },
    createdAt = obj.optString("createdAt").takeIf { it.isNotBlank() },
    slugUrl = obj.optString("slug"),
    webUrl = obj.optString("web"),
    sourceId = obj.optString("sourceId"),
)
