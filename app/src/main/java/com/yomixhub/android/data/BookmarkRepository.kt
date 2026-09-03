package com.yomixhub.android.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yomixhub.android.data.persistence.titleFromJson
import com.yomixhub.android.data.persistence.titleToJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Cloud sync of the bookmark collection with Cloud Firestore:
 * documents live at `/users/{userId}/bookmarks/{mangaId}`, where `mangaId`
 * is the same stable key [AppState.key] uses locally.
 *
 *  * **On sign-in** the remote bookmarks are fetched and merged into the
 *    local collection (union by key; on conflict the newer record wins for
 *    the reading status, `lastSeenChapterId` takes the maximum), and the
 *    merged set is written back so both sides converge.
 *  * **On every local change** (add / status change / new chapters seen /
 *    delete) the affected document is pushed to Firestore fire-and-forget –
 *    only while a user is signed in; the local DataStore copy is always
 *    updated regardless.
 */
object BookmarkRepository {

    private const val USERS_COLLECTION = "users"
    private const val BOOKMARKS_COLLECTION = "bookmarks"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /** Fired by [AuthRepository] when a user signs in. */
    fun onUserSignedIn(userId: String) {
        scope.launch {
            runCatching {
                // Wait for the local (DataStore) restore before merging, so a
                // fresh remote fetch never races – or overwrites – it.
                AppState.awaitLocalBookmarks()
                syncFromRemote(userId)
            }
        }
    }

    /** Signed out: local data stays, cloud updates pause until next sign-in. */
    fun onUserSignedOut() = Unit

    /** Local bookmark added or changed → mirror it to Firestore. */
    fun pushBookmark(bookmark: Bookmark) {
        val userId = currentUserId() ?: return
        scope.launch {
            runCatching {
                bookmarksRef(userId).document(keyOf(bookmark)).set(toDocument(bookmark)).await()
            }
        }
    }

    /** Local bookmark removed → delete the Firestore document. */
    fun deleteBookmark(title: Title) {
        val userId = currentUserId() ?: return
        scope.launch {
            runCatching {
                bookmarksRef(userId).document(keyOf(title)).delete().await()
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Sync
    // ------------------------------------------------------------------ //

    private suspend fun syncFromRemote(userId: String) {
        val remote = fetchRemoteBookmarks(userId)
        val merged = AppState.mergeRemoteBookmarks(remote)
        // Push the merged collection back so the cloud catches up with any
        // local-only changes (e.g. bookmarks created while offline).
        merged.forEach { bookmark ->
            runCatching {
                bookmarksRef(userId).document(keyOf(bookmark)).set(toDocument(bookmark)).await()
            }
        }
    }

    private suspend fun fetchRemoteBookmarks(userId: String): List<Bookmark> {
        val snapshot = bookmarksRef(userId).get().await()
        return snapshot.documents.mapNotNull(::fromDocument)
    }

    private fun bookmarksRef(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(BOOKMARKS_COLLECTION)

    private fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun keyOf(bookmark: Bookmark): String = AppState.key(bookmark.title)

    private fun keyOf(title: Title): String = AppState.key(title)

    // ------------------------------------------------------------------ //
    // Document mapping (Firestore-friendly primitives only)
    // ------------------------------------------------------------------ //

    private fun toDocument(bookmark: Bookmark): Map<String, Any?> {
        val titleJson = titleToJson(bookmark.title)
        val document = HashMap<String, Any?>(titleJson.length() + 3)
        for (key in titleJson.keys()) {
            document[key] = titleJson.get(key)
        }
        document["bookmarkStatus"] = bookmark.status.name
        document["lastSeenChapterId"] = bookmark.lastSeenChapterId
        document["addedAt"] = bookmark.addedAt
        return document
    }

    private fun fromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Bookmark? =
        runCatching {
            val data = document.data ?: return@runCatching null
            val obj = JSONObject(data)
            Bookmark(
                title = titleFromJson(obj),
                status = runCatching {
                    ReadingStatus.valueOf(obj.optString("bookmarkStatus"))
                }.getOrDefault(ReadingStatus.READING),
                lastSeenChapterId = obj.optLong("lastSeenChapterId", 0L),
                addedAt = obj.optLong("addedAt", 0L),
            )
        }.getOrNull()
}
