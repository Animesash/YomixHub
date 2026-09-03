package com.yomixhub.android.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yomixhub.android.data.Bookmark
import com.yomixhub.android.data.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.bookmarksDataStore by preferencesDataStore(name = "yomixhub_bookmarks")

/**
 * Disk storage for the bookmark collection (DataStore Preferences + JSON):
 * the "local database" of the bookmark feature. The same records are mirrored
 * to Cloud Firestore by [com.yomixhub.android.data.BookmarkRepository] while
 * the user is signed in.
 */
object BookmarksStore {

    private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks")

    /** Reads the persisted bookmarks; empty when nothing was stored. */
    suspend fun load(context: Context): List<Bookmark> = withContext(Dispatchers.IO) {
        val json = context.bookmarksDataStore.data.first()[KEY_BOOKMARKS]
            ?.takeIf { it.isNotBlank() }
            ?: return@withContext emptyList()
        parse(json)
    }

    /** Persists the given snapshot atomically. */
    suspend fun save(context: Context, bookmarks: List<Bookmark>) {
        context.bookmarksDataStore.edit { prefs ->
            prefs[KEY_BOOKMARKS] = serialize(bookmarks)
        }
    }

    private fun serialize(bookmarks: List<Bookmark>): String {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject()
                    .put("title", titleToJson(bookmark.title))
                    .put("bookmarkStatus", bookmark.status.name)
                    .put("lastSeenChapterId", bookmark.lastSeenChapterId)
                    .put("addedAt", bookmark.addedAt),
            )
        }
        return array.toString()
    }

    private fun parse(json: String): List<Bookmark> {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Bookmark>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optJSONObject("title")?.let(::titleFromJson) ?: continue
            out.add(
                Bookmark(
                    title = title,
                    status = runCatching {
                        ReadingStatus.valueOf(obj.optString("bookmarkStatus"))
                    }.getOrDefault(ReadingStatus.READING),
                    lastSeenChapterId = obj.optLong("lastSeenChapterId", 0L),
                    addedAt = obj.optLong("addedAt", 0L),
                ),
            )
        }
        return out
    }
}
