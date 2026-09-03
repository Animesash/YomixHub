package com.yomixhub.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.SampleData
import com.yomixhub.android.data.SourceRepository
import com.yomixhub.android.data.Title
import com.yomixhub.android.data.TitleFormat
import com.yomixhub.android.data.source.Source
import com.yomixhub.android.data.source.SourceTitle
import com.yomixhub.android.ui.components.ActiveSource
import com.yomixhub.android.ui.components.SearchField
import com.yomixhub.android.ui.components.SectionHeader
import com.yomixhub.android.ui.components.TitleCard
import com.yomixhub.android.ui.components.mergedWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CatalogFilter { ALL, MANGA, NOVEL }

/** A catalogue title together with the source it came from. */
private data class RemoteCard(val source: Source, val title: SourceTitle)

/** Duotone palettes for covers of titles loaded from a source. */
private val REMOTE_PALETTES = listOf(
    0xFF2E7D8F to 0xFF0F3A47,
    0xFFF4A63B to 0xFFC24E23,
    0xFF7E57C2 to 0xFF4527A0,
    0xFF43A047 to 0xFF1B5E20,
    0xFFEC5B77 to 0xFF8E2250,
    0xFF39A0ED to 0xFF1A4E8A,
)

/**
 * "Все тайтлы" – catalogue of all titles:
 * filter chips + responsive 3-column card grid.
 *
 * The chips pick the content type: "Манга" loads the MangaLib feed,
 * "Новеллы" the RanobeLib feed, and "Все" the catalogue chosen in the source
 * picker (top-bar tune action): both MangaLib + RanobeLib interleaved by
 * default, or a single source, including HentaiLib (18+).
 *
 * Remote search is performed on every source of the current selection.
 * Tapping a card opens the details screen with the chapter list, and
 * chapters open in the reader – both rendered as full-screen overlays hoisted
 * to [com.yomixhub.android.ui.YomixHubApp].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    innerPadding: PaddingValues,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    activeSource: ActiveSource,
    onOpenTitle: (Title) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }
    var debouncedQuery by remember { mutableStateOf(searchQuery) }
    var demoFallback by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }

    var remote by remember { mutableStateOf<List<RemoteCard>>(emptyList()) }
    var remoteLoading by remember { mutableStateOf(false) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var nextPage by remember { mutableStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val sources = remember(filter, activeSource) {
        when (filter) {
            CatalogFilter.MANGA -> listOf(SourceRepository.mangaLib)
            CatalogFilter.NOVEL -> listOf(SourceRepository.ranobeLib)
            CatalogFilter.ALL -> when (activeSource) {
                ActiveSource.ALL -> listOf(SourceRepository.mangaLib, SourceRepository.ranobeLib)
                ActiveSource.MANGALIB -> listOf(SourceRepository.mangaLib)
                ActiveSource.RANOBELIB -> listOf(SourceRepository.ranobeLib)
                ActiveSource.HENTAILIB -> listOf(SourceRepository.hentaiLib)
            }
        }
    }
    val sourceNames = remember(sources) { sources.joinToString(" + ") { it.name } }
    val isRemote = !demoFallback

    // Debounce the shared input so the API is not hit on every keystroke.
    // (Closing the search clears the query in YomixHubApp.)
    LaunchedEffect(searchQuery) {
        if (searchQuery != debouncedQuery) {
            delay(if (searchQuery.isBlank()) 0L else 400L)
            debouncedQuery = searchQuery
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) gridState.scrollToItem(0)
    }

    // Reset the remote feed when the source selection changes.
    LaunchedEffect(sources) {
        remote = emptyList()
        nextPage = 1
        hasNextPage = false
    }

    suspend fun fetchPage(page: Int): Pair<List<RemoteCard>, Boolean> {
        suspend fun fetch(source: Source) = if (debouncedQuery.isBlank()) {
            source.popularTitles(page)
        } else {
            source.searchTitles(debouncedQuery, page)
        }

        if (sources.size == 1) {
            val source = sources.first()
            val result = fetch(source)
            return result.titles.map { RemoteCard(source, it) } to result.hasNextPage
        }

        // Combined feed: fetch every source concurrently and interleave the
        // results round-robin so both catalogues are represented.
        return coroutineScope {
            val pages = sources.map { source ->
                async { runCatching { fetch(source) } }
            }.awaitAll()

            val failures = pages.filter { it.isFailure }
            if (failures.size == pages.size) {
                throw failures.first().exceptionOrNull() ?: IllegalStateException()
            }

            val lists = sources.zip(pages).mapNotNull { (source, result) ->
                result.getOrNull()?.takeIf { it.titles.isNotEmpty() }
                    ?.let { page -> page.titles.map { RemoteCard(source, it) } }
            }
            val interleaved = buildList {
                val maxIndex = lists.maxOfOrNull { it.size } ?: 0
                for (index in 0 until maxIndex) {
                    lists.forEach { list -> if (index < list.size) add(list[index]) }
                }
            }
            interleaved to pages.any { it.getOrNull()?.hasNextPage == true }
        }
    }

    // Primary feed: popular titles or remote search for the current sources.
    LaunchedEffect(sources, debouncedQuery, retryTick) {
        remoteLoading = true
        remoteError = null
        runCatching { fetchPage(1) }
            .onSuccess { (titles, hasNext) ->
                remote = titles
                nextPage = 2
                hasNextPage = hasNext
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                remoteError = error.message ?: FALLBACK_ERROR
                remote = emptyList()
                hasNextPage = false
            }
        remoteLoading = false
    }

    fun loadMore() {
        if (remoteLoading || remoteError != null || !isRemote) return
        remoteLoading = true
        scope.launch {
            val page = nextPage
            runCatching { fetchPage(page) }
                .onSuccess { (titles, hasNext) ->
                    remote = remote + titles
                    nextPage = page + 1
                    hasNextPage = hasNext
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    remoteError = error.message ?: FALLBACK_ERROR
                }
            remoteLoading = false
        }
    }

    val localTitles = remember(filter, searchQuery) {
        val base = when (filter) {
            CatalogFilter.ALL -> SampleData.catalog
            CatalogFilter.MANGA -> SampleData.catalog.filter { it.format == TitleFormat.MANGA }
            CatalogFilter.NOVEL -> SampleData.catalog.filter { it.format == TitleFormat.NOVEL }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    val remoteCards = remember(remote) { remote.map { it.title.toTitle(it.source) } }
    val displayed = if (isRemote) remoteCards else localTitles

    val headerText = when {
        demoFallback -> stringResource(R.string.section_demo)
        debouncedQuery.isNotBlank() ->
            stringResource(R.string.section_search_results) + " · " + sourceNames
        filter == CatalogFilter.NOVEL ->
            stringResource(R.string.section_popular_novels) + " · " + sourceNames
        else -> stringResource(R.string.section_popular) + " · " + sourceNames
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = innerPadding.mergedWith(
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (searchActive) {
                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    SearchField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        onClose = { onSearchActiveChange(false) },
                        hint = stringResource(R.string.search_hint_remote),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                FilterRow(
                    selected = filter,
                    onSelect = {
                        filter = it
                        demoFallback = false
                    },
                )
            }
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = headerText)
            }

            when {
                isRemote && remoteLoading && remoteCards.isEmpty() ->
                    item(key = "remote-loading", span = { GridItemSpan(maxLineSpan) }) {
                        RemoteLoadingItem()
                    }

                isRemote && remoteError != null && remoteCards.isEmpty() ->
                    item(key = "remote-error", span = { GridItemSpan(maxLineSpan) }) {
                        RemoteErrorItem(
                            message = remoteError ?: "",
                            onRetry = { retryTick++ },
                            onUseDemo = { demoFallback = true },
                        )
                    }

                else ->
                    if (displayed.isEmpty()) {
                        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                            EmptyResultsItem()
                        }
                    } else {
                        items(displayed, key = { "${it.sourceId ?: "demo"}-${it.id}" }) { card ->
                            TitleCard(
                                title = card,
                                onClick = { onOpenTitle(card) },
                            )
                        }
                    }
            }

            if (isRemote && remoteCards.isNotEmpty() && remoteLoading) {
                item(key = "loading-more", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }

            if (isRemote && hasNextPage && !remoteLoading && remoteError == null) {
                item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = ::loadMore) {
                            Text(stringResource(R.string.show_more))
                        }
                    }
                }
            }
        }
    }
}

private const val FALLBACK_ERROR = "Неизвестная ошибка"

/** Map a source title onto the UI card model. */
private fun SourceTitle.toTitle(source: Source): Title {
    val (from, to) = REMOTE_PALETTES[(id % REMOTE_PALETTES.size).toInt()]
    return Title(
        id = id.toInt(),
        name = name,
        author = country ?: releaseYear ?: source.name,
        format = if (source.id == SourceRepository.ranobeLib.id) {
            TitleFormat.NOVEL
        } else {
            TitleFormat.MANGA
        },
        volumes = 0,
        chapters = 0,
        rating = rating,
        status = status,
        coverUrl = coverUrl,
        sourceId = source.id,
        detailsSlug = slugUrl,
        coverFrom = Color(from),
        coverTo = Color(to),
    )
}

@Composable
private fun EmptyResultsItem() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
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
private fun RemoteLoadingItem() {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RemoteErrorItem(
    message: String,
    onRetry: () -> Unit,
    onUseDemo: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error_source),
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
        Row {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            TextButton(onClick = onUseDemo) { Text(stringResource(R.string.show_demo)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selected: CatalogFilter,
    onSelect: (CatalogFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        FilterChip(
            selected = selected == CatalogFilter.ALL,
            onClick = { onSelect(CatalogFilter.ALL) },
            label = { Text(stringResource(R.string.chip_all)) },
            colors = chipColors,
        )
        FilterChip(
            selected = selected == CatalogFilter.MANGA,
            onClick = { onSelect(CatalogFilter.MANGA) },
            label = { Text(stringResource(R.string.chip_manga)) },
            colors = chipColors,
        )
        FilterChip(
            selected = selected == CatalogFilter.NOVEL,
            onClick = { onSelect(CatalogFilter.NOVEL) },
            label = { Text(stringResource(R.string.chip_novels)) },
            colors = chipColors,
        )
    }
}
