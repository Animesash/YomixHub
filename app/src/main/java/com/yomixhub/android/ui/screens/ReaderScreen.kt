package com.yomixhub.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.SourceRepository
import com.yomixhub.android.data.source.ChapterContent
import com.yomixhub.android.data.source.SourceChapter
import com.yomixhub.android.ui.YomixIcons
import com.yomixhub.android.ui.components.HideSystemNavigationBar
import kotlinx.coroutines.CancellationException

/**
 * Reader for a single chapter, opened from the details screen or from a
 * finished download.
 *
 * Two content shapes are supported:
 *  * light novels – a comfortable long-form text column;
 *  * manga / manhwa – a vertical strip of page images (fetched with the
 *    Referer/User-Agent headers of the chapter's source).
 *
 * While reading, the system navigation bar is hidden (Option A) and revealed
 * again by an edge swipe or when the reader closes.
 */
@Composable
fun ReaderScreen(
    chapter: SourceChapter,
    novelTitle: String,
    onBack: () -> Unit,
) {
    // Downloaded chapters are shown instantly, without touching the network.
    val cached = AppState.downloadedChapters[chapter.id]
    val source = remember(chapter.sourceId) { SourceRepository.byId(chapter.sourceId) }
    var content by remember { mutableStateOf<ChapterContent?>(cached) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }

    LaunchedEffect(chapter.id, retryTick) {
        if (cached != null) {
            content = cached
            return@LaunchedEffect
        }
        content = null
        error = null
        runCatching { source?.chapterText(chapter) ?: error("Источник недоступен") }
            .onSuccess { content = it }
            .onFailure { e ->
                if (e is CancellationException) throw e
                error = e.message
            }
    }

    HideSystemNavigationBar()
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novelTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chapter.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content?.takeIf { it.pages.isNotEmpty() }?.let { pages ->
                Text(
                    text = stringResource(R.string.pages_count, pages.pages.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        val loaded = content
        when {
            error != null -> ReaderError(message = error ?: "", onRetry = { retryTick++ })

            loaded == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            loaded.isText -> TextContent(
                content = loaded,
                modifier = Modifier.weight(1f),
            )

            else -> PagesContent(
                content = loaded,
                headers = source?.imageHeaders.orEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReaderError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

/** Long-form text column for novel chapters. */
@Composable
private fun TextContent(
    content: ChapterContent,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // The system navigation bar is hidden while reading, so a plain
        // bottom padding is enough.
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 32.dp,
        ),
    ) {
        item(key = "divider") {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        item(key = "chapter-title") {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
        }
        items(content.paragraphs) { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
        item(key = "end-marker") {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.end_of_chapter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Vertical strip of manga pages. */
@Composable
private fun PagesContent(
    content: ChapterContent,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp),
    ) {
        itemsIndexed(content.pages, key = { _, url -> url }) { index, url ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // width / height of the page, refined once it is decoded.
                var ratio by remember(url) { mutableStateOf(0.7f) }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
                        .build(),
                    contentDescription = stringResource(
                        R.string.page_of,
                        index + 1,
                        content.pages.size,
                    ),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio),
                    onSuccess = { result ->
                        val size = result.painter.intrinsicSize
                        if (size.width > 0f && size.height > 0f) {
                            ratio = size.width / size.height
                        }
                    },
                )
            }
        }
        item(key = "pages-end") {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.end_of_chapter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }
    }
}
