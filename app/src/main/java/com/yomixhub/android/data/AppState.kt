package com.yomixhub.android.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.yomixhub.android.data.persistence.BookmarksStore
import com.yomixhub.android.data.persistence.DownloadsStore
import com.yomixhub.android.data.source.ChapterContent
import com.yomixhub.android.data.source.Source
import com.yomixhub.android.data.source.SourceChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Reading status of a bookmarked title (mirrors the library filter chips). */
enum class ReadingStatus { READING, PLANNED, COMPLETED }

/** A title saved by the user. */
data class Bookmark(
    val title: Title,
    val status: ReadingStatus = ReadingStatus.READING,
    val lastSeenChapterId: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Session state shared by the "Закладки", "Уведомления" and "Загрузки"
 * screens. Backed by Compose snapshot state, so every screen updates live.
 *
 *  * bookmarks are added from the details screen (bookmark icon);
 *  * notifications appear when a bookmarked title gets chapters newer than
 *    the last seen one (checked whenever its chapter list loads);
 *  * downloads are started from the chapter list and really fetch the
 *    chapter content through its source.
 */
object AppState {

    val bookmarks = mutableStateListOf<Bookmark>()
    val notifications = mutableStateListOf<ChapterUpdate>()
    val downloads = mutableStateListOf<DownloadEntry>()

    /** Chapter content fetched by the downloader, kept for offline reading. */
    val downloadedChapters = mutableStateMapOf<Long, ChapterContent>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var appContext: Context? = null
    private var localLoadJob: Job? = null

    /**
     * Wires disk persistence; call once from `YomixApplication.onCreate()`.
     * Restores the bookmark collection and previously downloaded chapters.
     */
    fun attach(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        localLoadJob = scope.launch {
            val storedDownloads = runCatching {
                DownloadsStore.load(context.applicationContext)
            }.getOrNull()
            if (storedDownloads != null) {
                downloads.clear()
                // Downloads that were mid-flight when the app died cannot
                // resume: surface them as failed so they can be retried.
                downloads.addAll(storedDownloads.entries.map { entry ->
                    when (entry.status) {
                        DownloadStatus.ACTIVE,
                        DownloadStatus.QUEUED,
                        DownloadStatus.PAUSED,
                        -> entry.copy(status = DownloadStatus.FAILED)

                        else -> entry
                    }
                })
                downloadedChapters.clear()
                storedDownloads.contents.forEach { (chapterId, content) ->
                    downloadedChapters[chapterId] = content
                }
            }

            val storedBookmarks = runCatching {
                BookmarksStore.load(context.applicationContext)
            }.getOrNull().orEmpty()
            if (storedBookmarks.isNotEmpty()) {
                bookmarks.clear()
                bookmarks.addAll(storedBookmarks)
            }
        }
    }

    /**
     * Resolves once the local DataStore restore has finished; cloud sync
     * ([BookmarkRepository]) awaits this before merging remote bookmarks.
     */
    suspend fun awaitLocalBookmarks() {
        localLoadJob?.join()
    }

    /** Snapshots the bookmark collection to disk (fire-and-forget). */
    private fun persistBookmarks() {
        val context = appContext ?: return
        val snapshot = bookmarks.toList()
        scope.launch(Dispatchers.IO) {
            runCatching { BookmarksStore.save(context, snapshot) }
        }
    }

    /** Snapshots the current downloads to disk (fire-and-forget). */
    private fun persist() {
        val context = appContext ?: return
        val entries = downloads.toList()
        val contents = downloadedChapters.toMap()
        scope.launch(Dispatchers.IO) {
            runCatching { DownloadsStore.save(context, entries, contents) }
        }
    }

    fun key(title: Title): String = "${title.sourceId ?: "demo"}:${title.detailsSlug ?: title.id}"

    fun isBookmarked(title: Title): Boolean = bookmarks.any { key(it.title) == key(title) }

    fun bookmarkOf(title: Title): Bookmark? = bookmarks.firstOrNull { key(it.title) == key(title) }

    /** Unread notifications for a title (drives the unread badge in the library). */
    fun unreadCount(title: Title): Int = notifications.count { key(it.title) == key(title) && it.isUnread }

    /** Adds or removes a bookmark; returns the new bookmarked state. */
    fun toggleBookmark(title: Title, chapters: List<SourceChapter>?): Boolean {
        val existing = bookmarkOf(title)
        if (existing != null) {
            bookmarks.remove(existing)
            persistBookmarks()
            BookmarkRepository.deleteBookmark(title)
            return false
        }
        val bookmark = Bookmark(
            title = title,
            lastSeenChapterId = chapters?.firstOrNull()?.id ?: 0L,
        )
        bookmarks.add(0, bookmark)
        persistBookmarks()
        BookmarkRepository.pushBookmark(bookmark)
        return true
    }

    fun setBookmarkStatus(title: Title, status: ReadingStatus) {
        val index = bookmarks.indexOfFirst { key(it.title) == key(title) }
        if (index < 0) return
        val updated = bookmarks[index].copy(status = status)
        bookmarks[index] = updated
        persistBookmarks()
        BookmarkRepository.pushBookmark(updated)
    }

    /**
     * Called when a chapter list (re)loads: for bookmarked titles every
     * chapter newer than the last seen one becomes a notification.
     */
    fun onChaptersLoaded(title: Title, chapters: List<SourceChapter>) {
        val index = bookmarks.indexOfFirst { key(it.title) == key(title) }
        if (index < 0) return
        val bookmark = bookmarks[index]
        val newest = chapters.firstOrNull()?.id ?: return
        if (bookmark.lastSeenChapterId >= newest) return

        if (bookmark.lastSeenChapterId == 0L) {
            // Never seen chapters before – mark as seen silently.
            bookmarks[index] = bookmark.copy(lastSeenChapterId = newest)
            persistBookmarks()
            return
        }

        val fresh = chapters.filter { it.id > bookmark.lastSeenChapterId }
        fresh.asReversed().take(5).forEach { chapter ->
            notifications.add(
                0,
                ChapterUpdate(
                    title = title,
                    chapter = chapter.displayName,
                    createdAt = System.currentTimeMillis(),
                    isUnread = true,
                ),
            )
        }
        val updated = bookmark.copy(lastSeenChapterId = newest)
        bookmarks[index] = updated
        persistBookmarks()
        BookmarkRepository.pushBookmark(updated)
    }

    /**
     * Merges remote (Firestore) bookmarks into the local collection and
     * returns the merged snapshot (the caller pushes it back to the cloud).
     *
     * Union by key; on conflict the newer record (greater `addedAt`) wins the
     * reading status, `lastSeenChapterId` takes the maximum of both sides.
     */
    fun mergeRemoteBookmarks(remote: List<Bookmark>): List<Bookmark> {
        val byKey = LinkedHashMap<String, Bookmark>(bookmarks.size + remote.size)
        bookmarks.forEach { local -> byKey[key(local.title)] = local }
        remote.forEach { remoteBookmark ->
            val key = key(remoteBookmark.title)
            val local = byKey[key]
            byKey[key] = if (local == null) {
                remoteBookmark
            } else {
                local.copy(
                    status = if (remoteBookmark.addedAt > local.addedAt) {
                        remoteBookmark.status
                    } else {
                        local.status
                    },
                    lastSeenChapterId = maxOf(
                        local.lastSeenChapterId,
                        remoteBookmark.lastSeenChapterId,
                    ),
                )
            }
        }
        val merged = byKey.values.sortedByDescending { it.addedAt }
        bookmarks.clear()
        bookmarks.addAll(merged)
        persistBookmarks()
        return merged
    }

    fun isDownloaded(chapterId: Long): Boolean = downloadedChapters.containsKey(chapterId)

    /** An entry exists and is still waiting or transferring. */
    fun isQueuedOrActive(chapterId: Long): Boolean =
        downloads.any {
            it.chapterRef.id == chapterId &&
                (it.status == DownloadStatus.ACTIVE || it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.PAUSED)
        }

    /** The latest download attempt of the chapter has failed. */
    fun isFailed(chapterId: Long): Boolean =
        downloads.any { it.chapterRef.id == chapterId && it.status == DownloadStatus.FAILED }

    /**
     * Starts a download: an entry appears in [downloads] with live progress.
     * A failed entry is retried, finished downloads are not restarted.
     */
    fun download(source: Source?, title: Title, chapter: SourceChapter) {
        if (isDownloaded(chapter.id)) return
        val existing = downloads.indexOfFirst { it.chapterRef.id == chapter.id }
        if (existing >= 0) {
            when (downloads[existing].status) {
                DownloadStatus.ACTIVE, DownloadStatus.QUEUED, DownloadStatus.PAUSED -> return
                DownloadStatus.DONE -> return
                DownloadStatus.FAILED -> downloads.removeAt(existing)
            }
        }
        downloads.add(
            0,
            DownloadEntry(
                title = title,
                chapterRef = chapter,
                progress = 0.05f,
                status = DownloadStatus.ACTIVE,
            ),
        )
        scope.launch {
            val ticker = launch {
                while (true) {
                    delay(220)
                    updateEntry(chapter.id) { entry ->
                        if (entry.status == DownloadStatus.ACTIVE && entry.progress < 0.9f) {
                            entry.copy(progress = minOf(0.9f, entry.progress + 0.07f))
                        } else {
                            entry
                        }
                    }
                }
            }
            val result = runCatching {
                source?.chapterText(chapter) ?: error("Источник недоступен")
            }
            ticker.cancel()
            updateEntry(chapter.id) { entry ->
                result.fold(
                    onSuccess = { content ->
                        downloadedChapters[chapter.id] = content
                        entry.copy(progress = 1f, status = DownloadStatus.DONE)
                    },
                    onFailure = { entry.copy(status = DownloadStatus.FAILED) },
                )
            }
            persist()
        }
    }

    fun removeDownload(chapterId: Long) {
        downloads.removeAll { it.chapterRef.id == chapterId }
        persist()
    }

    fun markAllNotificationsRead() {
        for (i in notifications.indices) {
            if (notifications[i].isUnread) {
                notifications[i] = notifications[i].copy(isUnread = false)
            }
        }
    }

    fun markNotificationRead(update: ChapterUpdate) {
        val index = notifications.indexOfFirst { it === update || (it.title.id == update.title.id && it.createdAt == update.createdAt) }
        if (index >= 0 && notifications[index].isUnread) {
            notifications[index] = notifications[index].copy(isUnread = false)
        }
    }

    private fun updateEntry(chapterId: Long, transform: (DownloadEntry) -> DownloadEntry) {
        val index = downloads.indexOfFirst { it.chapterRef.id == chapterId }
        if (index >= 0) downloads[index] = transform(downloads[index])
    }
}
