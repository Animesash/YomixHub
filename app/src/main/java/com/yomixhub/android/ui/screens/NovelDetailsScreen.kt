package com.yomixhub.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.ReadingStatus
import com.yomixhub.android.data.SourceRepository
import com.yomixhub.android.data.Title
import com.yomixhub.android.data.source.SourceChapter
import com.yomixhub.android.data.source.SourceDetails
import com.yomixhub.android.ui.YomixIcons
import com.yomixhub.android.ui.components.CoverImage
import com.yomixhub.android.ui.components.mergedWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Title details opened from a catalogue grid: cover, metadata chips,
 * description and the chapter list of the title's source.
 *
 * The top bar carries the bookmark toggle; chapters can be read (reader
 * overlay) or downloaded for offline reading. Chapters are reported to
 * [AppState] so bookmarked titles generate notifications for new chapters.
 */
@Composable
fun NovelDetailsScreen(
    title: Title,
    onBack: () -> Unit,
    onReadChapter: (SourceChapter) -> Unit,
) {
    val source = remember(title.sourceId) { SourceRepository.byId(title.sourceId) }
    var details by remember { mutableStateOf<SourceDetails?>(null) }
    var detailsError by remember { mutableStateOf<String?>(null) }
    var chapters by remember { mutableStateOf<List<SourceChapter>?>(null) }
    var chaptersError by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }

    val slug = title.detailsSlug

    LaunchedEffect(slug, retryTick) {
        if (slug == null) return@LaunchedEffect
        details = null
        detailsError = null
        chapters = null
        chaptersError = null
        // Details and chapters are independent – load them concurrently.
        launch {
            runCatching { source?.titleDetails(slug) ?: error("Источник недоступен") }
                .onSuccess { details = it }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    detailsError = error.message
                }
        }
        launch {
            runCatching { source?.chapters(slug) ?: error("Источник недоступен") }
                .onSuccess { loaded ->
                    chapters = loaded
                    // Feed the bookmark state with the fresh chapter list so
                    // new chapters turn into notifications.
                    AppState.onChaptersLoaded(title, loaded)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    chaptersError = error.message
                }
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = YomixIcons.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                )
            }
            Text(
                text = source?.name ?: stringResource(R.string.source_offline),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            val bookmarked = AppState.isBookmarked(title)
            IconButton(onClick = {
                AppState.toggleBookmark(title, chapters)
            }) {
                Icon(
                    imageVector = if (bookmarked) YomixIcons.BookmarkFilled else YomixIcons.BookmarkOutlined,
                    contentDescription = stringResource(
                        if (bookmarked) R.string.bookmark_remove else R.string.bookmark_add,
                    ),
                    tint = if (bookmarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
                .mergedWith(PaddingValues(bottom = 24.dp)),
        ) {
            item(key = "header") {
                DetailsHeader(title = title, details = details)
            }

            item(key = "status") {
                BookmarkStatusRow(title = title)
            }

            val genres = details?.genres.orEmpty()
            val tags = details?.tags.orEmpty()
            if (genres.isNotEmpty() || tags.isNotEmpty()) {
                item(key = "tags") {
                    TagsRow(tags = genres + tags)
                }
            }

            item(key = "description") {
                DescriptionBlock(
                    details = details,
                    error = detailsError,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                )
            }

            item(key = "chapters-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.section_chapters),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    chapters?.let {
                        Text(
                            text = it.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when {
                chapters == null && chaptersError == null ->
                    item(key = "chapters-loading") { CenteredLoadingItem() }

                chaptersError != null ->
                    item(key = "chapters-error") {
                        ErrorItem(message = chaptersError ?: "", onRetry = { retryTick++ })
                    }

                chapters.orEmpty().isEmpty() ->
                    item(key = "chapters-empty") {
                        EmptyChaptersItem()
                    }

                else ->
                    items(chapters.orEmpty(), key = { it.id }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            onClick = { onReadChapter(chapter) },
                            onDownload = { AppState.download(source, title, chapter) },
                        )
                    }
            }
        }
    }
}

@Composable
private fun DetailsHeader(
    title: Title,
    details: SourceDetails?,
) {
    val remote = details?.title

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CoverImage(
            coverUrl = title.coverUrl ?: remote?.coverUrl,
            sourceId = title.sourceId,
            letter = title.name.take(1),
            from = title.coverFrom,
            to = title.coverTo,
            modifier = Modifier.size(width = 104.dp, height = 152.dp),
            shape = RoundedCornerShape(12.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details?.authors?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                    ?: title.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (title.rating > 0.0) {
                Text(
                    text = "★ ${title.rating}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            val status = details?.status ?: title.status ?: remote?.status
            val country = remote?.country ?: title.author.takeIf { title.sourceId == null }
            val ageRating = remote?.ageRating
            val year = remote?.releaseYear

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                status?.let { MetaChip(text = it, highlight = true) }
                country?.let { MetaChip(text = it) }
                ageRating?.let { MetaChip(text = it) }
                year?.let { MetaChip(text = it) }
            }
        }
    }
}

/** Reading-status chips, visible once the title is bookmarked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkStatusRow(title: Title) {
    val bookmark = AppState.bookmarkOf(title) ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        ReadingStatus.entries.forEach { status ->
            FilterChip(
                selected = bookmark.status == status,
                onClick = { AppState.setBookmarkStatus(title, status) },
                label = { Text(stringResource(status.labelRes())) },
                colors = chipColors,
            )
        }
    }
}

private fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.READING -> R.string.chip_reading
    ReadingStatus.PLANNED -> R.string.chip_planned
    ReadingStatus.COMPLETED -> R.string.chip_completed
}

@Composable
private fun MetaChip(
    text: String,
    highlight: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (highlight) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun TagsRow(tags: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            MetaChip(text = tag)
        }
    }
}

@Composable
private fun DescriptionBlock(
    details: SourceDetails?,
    error: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.section_description),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))

        val description = details?.description
        when {
            error != null && description == null -> Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            description.isNullOrBlank() -> Text(
                text = stringResource(R.string.description_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 8,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.length > 400 || description.count { it == '\n' } > 8) {
                    TextButton(onClick = onToggle) {
                        Text(
                            text = stringResource(
                                if (expanded) {
                                    R.string.collapse_description
                                } else {
                                    R.string.expand_description
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: SourceChapter,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    val downloaded = AppState.isDownloaded(chapter.id)
    val queued = AppState.isQueuedOrActive(chapter.id)
    val failed = AppState.isFailed(chapter.id)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                chapter.teamName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                chapter.createdAt?.take(10)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                downloaded -> Icon(
                    imageVector = YomixIcons.Check,
                    contentDescription = stringResource(R.string.downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp),
                )

                queued -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(18.dp),
                )

                failed -> IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = YomixIcons.Error,
                        contentDescription = stringResource(R.string.download_status_failed),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }

                else -> IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = YomixIcons.DownloadOutlined,
                        contentDescription = stringResource(R.string.download_chapter),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun EmptyChaptersItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_chapters),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorItem(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.chapters_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
