package com.yomixhub.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.DownloadEntry
import com.yomixhub.android.data.DownloadStatus
import com.yomixhub.android.ui.YomixIcons
import com.yomixhub.android.ui.components.CoverImage
import com.yomixhub.android.ui.components.SearchField
import com.yomixhub.android.ui.components.mergedWith

/**
 * "Загрузки" – the live download queue from [AppState]. Finished chapters
 * open straight in the reader (their content is already cached offline);
 * every entry can be removed with the trailing close button.
 */
@Composable
fun DownloadsScreen(
    innerPadding: PaddingValues,
    onOpenDownload: (DownloadEntry) -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloads = AppState.downloads
    val filtered = if (searchQuery.isBlank()) {
        downloads
    } else {
        downloads.filter {
            it.title.name.contains(searchQuery, ignoreCase = true) ||
                it.chapter.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = innerPadding.mergedWith(PaddingValues(bottom = 24.dp)),
    ) {
        if (searchActive) {
            item(key = "search") {
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    onClose = { onSearchActiveChange(false) },
                    hint = stringResource(R.string.search_hint_downloads),
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }

        if (downloads.isEmpty()) {
            item(key = "empty") { EmptyDownloads() }
        } else if (filtered.isEmpty()) {
            item(key = "empty-search") { EmptyResultsRow() }
        } else {
            item(key = "header") {
                Text(
                    text = stringResource(R.string.section_downloading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 20.dp,
                        bottom = 4.dp,
                    ),
                )
            }
            items(filtered, key = { "${it.title.id}-${it.chapterRef.id}" }) { entry ->
                DownloadRow(
                    entry = entry,
                    onOpen = { if (entry.status == DownloadStatus.DONE) onOpenDownload(entry) },
                    onRemove = { AppState.removeDownload(entry.chapterRef.id) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 92.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyResultsRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_search),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyDownloads() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_downloads),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.hint_downloads),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DownloadRow(
    entry: DownloadEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.status == DownloadStatus.DONE, onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = entry.title.coverUrl,
            sourceId = entry.title.sourceId,
            letter = entry.title.name.take(1),
            from = entry.title.coverFrom,
            to = entry.title.coverTo,
            modifier = Modifier.size(width = 44.dp, height = 60.dp),
            shape = RoundedCornerShape(8.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            when (entry.status) {
                DownloadStatus.ACTIVE,
                DownloadStatus.PAUSED,
                -> LinearProgressIndicator(
                    progress = { entry.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (entry.status == DownloadStatus.ACTIVE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                DownloadStatus.QUEUED -> LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                DownloadStatus.DONE -> LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                DownloadStatus.FAILED -> LinearProgressIndicator(
                    progress = { entry.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.chapter + " · " + statusLabel(entry.status),
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.status == DownloadStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        StatusIcon(status = entry.status, progress = entry.progress)

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = YomixIcons.Close,
                contentDescription = stringResource(R.string.remove_download),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.ACTIVE -> stringResource(R.string.download_status_active)
    DownloadStatus.PAUSED -> stringResource(R.string.download_status_paused)
    DownloadStatus.QUEUED -> stringResource(R.string.download_status_queued)
    DownloadStatus.DONE -> stringResource(R.string.download_status_done)
    DownloadStatus.FAILED -> stringResource(R.string.download_status_failed)
}

@Composable
private fun StatusIcon(status: DownloadStatus, progress: Float) {
    when (status) {
        DownloadStatus.ACTIVE -> Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            // Tiny ring mirroring the navigation bar badge.
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
            )
        }

        DownloadStatus.PAUSED -> Icon(
            imageVector = YomixIcons.Pause,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )

        DownloadStatus.QUEUED -> Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )

        DownloadStatus.DONE -> Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = YomixIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }

        DownloadStatus.FAILED -> Icon(
            imageVector = YomixIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
    }
}
