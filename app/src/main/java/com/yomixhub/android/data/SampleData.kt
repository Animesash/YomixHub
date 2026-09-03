package com.yomixhub.android.data

import androidx.compose.ui.graphics.Color
import com.yomixhub.android.data.source.SourceChapter

/** Physical format of a title. */
enum class TitleFormat(val label: String) {
    MANGA("MANGA"),
    NOVEL("NOVEL"),
}

/**
 * A manga / light-novel title.
 *
 * Cards render the remote [coverUrl] (Coil) and fall back to a tonal
 * duotone placeholder while loading / if the URL fails.
 *
 * @param sourceId id of the [com.yomixhub.android.data.source.Source] this
 * title came from, `null` for purely local entries.
 * @param detailsSlug source slug used to open the details screen.
 */
data class Title(
    val id: Int,
    val name: String,
    val author: String,
    val format: TitleFormat,
    val volumes: Int,
    val chapters: Int,
    val rating: Double,
    val unreadChapters: Int = 0,
    val status: String? = null,
    val coverUrl: String? = null,
    val sourceId: String? = null,
    val detailsSlug: String? = null,
    val coverFrom: Color,
    val coverTo: Color,
)

/** A "new chapter available" notification entry. */
data class ChapterUpdate(
    val title: Title,
    val chapter: String,
    val createdAt: Long,
    val isUnread: Boolean,
)

/** State of a single background download. */
enum class DownloadStatus { ACTIVE, PAUSED, QUEUED, DONE, FAILED }

/** A background download entry; [chapterRef] lets a finished entry be reopened. */
data class DownloadEntry(
    val title: Title,
    val chapterRef: SourceChapter,
    val progress: Float,
    val status: DownloadStatus,
) {
    val chapter: String get() = chapterRef.displayName
}

/**
 * Offline catalogue shown when the network is unavailable: real, currently
 * popular titles with their live cover URLs, so cards always render artwork.
 */
object SampleData {

    private const val MANGALIB = "mangalib"
    private const val RANOBELIB = "ranobelib"

    val catalog: List<Title> = listOf(
        Title(
            id = 3595,
            name = "Клинок, рассекающий демонов",
            author = "Манга",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.18,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/kimetsu-no-yaiba/cover/64b6c590-53b6-453e-9612-f2b48085e296.jpg",
            sourceId = MANGALIB,
            detailsSlug = "3595--kimetsu-no-yaiba",
            coverFrom = Color(0xFF2E7D8F),
            coverTo = Color(0xFF0F3A47),
        ),
        Title(
            id = 206,
            name = "Ван Пис",
            author = "Манга",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.72,
            status = "Онгоинг",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/one-piece/cover/89a48c0c-4c5d-4636-8143-5933ae1da6bb.jpg",
            sourceId = MANGALIB,
            detailsSlug = "206--one-piece",
            coverFrom = Color(0xFFF4A63B),
            coverTo = Color(0xFFC24E23),
        ),
        Title(
            id = 7580,
            name = "Поднятие уровня в одиночку",
            author = "Манхва",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.48,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/i-alone-level-up/cover/5c955cc1-1a25-4e9b-b9c2-e75677bfb15a.jpg",
            sourceId = MANGALIB,
            detailsSlug = "7580--i-alone-level-up",
            coverFrom = Color(0xFF39A0ED),
            coverTo = Color(0xFF1A4E8A),
        ),
        Title(
            id = 34466,
            name = "Всеведущий читатель",
            author = "Манхва",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.79,
            status = "Приостановлен",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/jeonjijeog-dogja-sijeom_/cover/c8403880-e3b9-4c36-a4de-0c7209a6589d.jpg",
            sourceId = MANGALIB,
            detailsSlug = "34466--jeonjijeog-dogja-sijeom_",
            coverFrom = Color(0xFF7E57C2),
            coverTo = Color(0xFF4527A0),
        ),
        Title(
            id = 3754,
            name = "Милый дом",
            author = "Манхва",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.68,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/sweet-home-kim-carnby-/cover/bd809b25-2e0b-4422-911a-414998f2e693.jpg",
            sourceId = MANGALIB,
            detailsSlug = "3754--sweet-home-kim-carnby-",
            coverFrom = Color(0xFF43A047),
            coverTo = Color(0xFF1B5E20),
        ),
        Title(
            id = 1773,
            name = "Ветролом",
            author = "Манхва",
            format = TitleFormat.MANGA,
            volumes = 0,
            chapters = 0,
            rating = 9.76,
            status = "Выпуск прекращён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/wind-breaker/cover/1879304e-ac22-4ab9-a41c-69a4dbcecfd8.jpg",
            sourceId = MANGALIB,
            detailsSlug = "1773--wind-breaker",
            coverFrom = Color(0xFFEC5B77),
            coverTo = Color(0xFF8E2250),
        ),
        Title(
            id = 20818,
            name = "Повелитель тайн",
            author = "Китай",
            format = TitleFormat.NOVEL,
            volumes = 0,
            chapters = 0,
            rating = 9.73,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/lord-of-the-mysteries/cover/1a442a03-e6f9-48af-9b74-21540dc20857.jpg",
            sourceId = RANOBELIB,
            detailsSlug = "20818--lord-of-the-mysteries",
            coverFrom = Color(0xFF7E57C2),
            coverTo = Color(0xFF311B92),
        ),
        Title(
            id = 48611,
            name = "Re:Zero. Повторение в Альтернативном Мире с Нуля (WN)",
            author = "Япония",
            format = TitleFormat.NOVEL,
            volumes = 0,
            chapters = 0,
            rating = 9.29,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/re-zero-kara-kasaneru-isekai-seikatsu/cover/h00rhnPKUBep_250x350.jpg",
            sourceId = RANOBELIB,
            detailsSlug = "48611--re-zero-kara-kasaneru-isekai-seikatsu",
            coverFrom = Color(0xFF2E7D8F),
            coverTo = Color(0xFF143D5C),
        ),
        Title(
            id = 31524,
            name = "Проза бродячих псов: Осаму Дадзай и чёрные дни",
            author = "Япония",
            format = TitleFormat.NOVEL,
            volumes = 0,
            chapters = 0,
            rating = 9.8,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/bungou-stray-dogs-dazai-osamu-and-the-dark-era/cover/SKtkBAdWLEfj_250x350.jpg",
            sourceId = RANOBELIB,
            detailsSlug = "31524--bungou-stray-dogs-dazai-osamu-and-the-dark-era",
            coverFrom = Color(0xFF43A047),
            coverTo = Color(0xFF1B5E20),
        ),
        Title(
            id = 26863,
            name = "Смерть — единственный конец для злодейки",
            author = "Корея",
            format = TitleFormat.NOVEL,
            volumes = 0,
            chapters = 0,
            rating = 9.57,
            status = "Завершён",
            coverUrl = "https://cover.cdnlibs.org/uploads/cover/death-is-the-only-ending-for-the-villain-novel-/cover/4a4681fa-7f17-46af-acd5-41712f092606.jpg",
            sourceId = RANOBELIB,
            detailsSlug = "26863--death-is-the-only-ending-for-the-villain-novel-",
            coverFrom = Color(0xFFEC5B77),
            coverTo = Color(0xFF8E2250),
        ),
    )
}
