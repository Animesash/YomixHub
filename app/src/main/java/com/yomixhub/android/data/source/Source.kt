package com.yomixhub.android.data.source

/**
 * Contract for an external catalogue the reader can pull titles from
 * (MangaLib, RanobeLib, HentaiLib, …).
 */
interface Source {

    /** Stable machine id, e.g. `"mangalib"`. */
    val id: String

    /** Human-readable source name shown in the UI. */
    val name: String

    /** Public website of the source. */
    val baseUrl: String

    /** Headers (Referer / User-Agent) that must accompany image requests. */
    val imageHeaders: Map<String, String>

    /** Most popular titles page by page (1-based). */
    suspend fun popularTitles(page: Int): SourcePage

    /** Recently updated titles page by page (1-based). */
    suspend fun latestTitles(page: Int): SourcePage

    /** Full-text search. */
    suspend fun searchTitles(query: String, page: Int = 1): SourcePage

    /** Extended information for a title (description, authors, genres, status). */
    suspend fun titleDetails(slugUrl: String): SourceDetails

    /** All available chapters of a title. */
    suspend fun chapters(slugUrl: String): List<SourceChapter>

    /** Reader content of a single chapter: novel text or manga pages. */
    suspend fun chapterText(chapter: SourceChapter): ChapterContent
}

/** One page of catalogue results. */
data class SourcePage(
    val titles: List<SourceTitle>,
    val page: Int,
    val hasNextPage: Boolean,
)

/** A title as returned by a catalogue listing. */
data class SourceTitle(
    val id: Long,
    val name: String,
    val originalName: String,
    val coverUrl: String?,
    val slugUrl: String,
    val webUrl: String,
    val rating: Double,
    val status: String?,
    val country: String?,
    val ageRating: String?,
    val releaseYear: String?,
)

/** Extended title information for the details screen. */
data class SourceDetails(
    val title: SourceTitle,
    val description: String,
    val authors: List<String>,
    val genres: List<String>,
    val tags: List<String>,
    val status: String?,
    val translationStatus: String?,
    val publisher: String?,
)

/** A single chapter reference. */
data class SourceChapter(
    val id: Long,
    val volume: String,
    val number: String,
    val name: String?,
    val branchId: Long?,
    val teamName: String?,
    val createdAt: String?,
    val slugUrl: String,
    val webUrl: String,
    val sourceId: String,
) {
    val displayName: String
        get() = "Том $volume · Глава $number" + (name?.let { " — $it" } ?: "")
}

/**
 * Reader content of a chapter. Light novels return [paragraphs] of text;
 * manga/manhwa return [pages] – absolute image URLs.
 */
data class ChapterContent(
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val pages: List<String> = emptyList(),
) {
    val isText: Boolean get() = paragraphs.isNotEmpty()
}

/** Source-related failures surfaced to the UI as readable messages. */
sealed class SourceException(message: String) : Exception(message) {

    class Http(val code: Int) : SourceException("HTTP $code")

    class Network(cause: Throwable) : SourceException("Нет соединения (${cause.message ?: "network"})")

    class Parse(cause: Throwable? = null, what: String = "Не удалось разобрать ответ") :
        SourceException("$what (${cause?.message ?: "parse"})")

    class NotFound(what: String) : SourceException("$what не найдено")

    object EmptyResponse : SourceException("Пустой ответ источника")
}
