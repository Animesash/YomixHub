package com.yomixhub.android.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.ReadingStatus
import com.yomixhub.android.data.Title
import com.yomixhub.android.ui.components.SearchField
import com.yomixhub.android.ui.components.SectionHeader
import com.yomixhub.android.ui.components.TitleCard
import com.yomixhub.android.ui.components.mergedWith

private enum class ShelfFilter { ALL, READING, PLANNED, COMPLETED }

/**
 * "Закладки" – the user's saved collection, driven live by [AppState].
 * Cards show an unread badge for every unseen chapter notification and open
 * the title's details screen.
 *
 * While the shared search is active (top-bar icon) a search field is shown
 * and the collection is filtered by name in real time.
 *
 * The grid consumes [innerPadding] (window insets from the Scaffold) as its
 * `contentPadding` so cards scroll under the top bar and above the nav bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onOpenTitle: (Title) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shelf by remember { mutableStateOf(ShelfFilter.ALL) }

    // Computed directly (not remembered) so unread badges update the moment
    // a notification arrives or is read.
    val titles = when (shelf) {
        ShelfFilter.ALL -> AppState.bookmarks.map { it.title }
        ShelfFilter.READING ->
            AppState.bookmarks.filter { it.status == ReadingStatus.READING }.map { it.title }
        ShelfFilter.PLANNED ->
            AppState.bookmarks.filter { it.status == ReadingStatus.PLANNED }.map { it.title }
        ShelfFilter.COMPLETED ->
            AppState.bookmarks.filter { it.status == ReadingStatus.COMPLETED }.map { it.title }
    }.map { title -> title.copy(unreadChapters = AppState.unreadCount(title)) }

    val filtered = if (searchQuery.isBlank()) {
        titles
    } else {
        titles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
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
                    hint = stringResource(R.string.search_hint_library),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        item(key = "shelf-row", span = { GridItemSpan(maxLineSpan) }) {
            ShelfRow(
                selected = shelf,
                onSelect = { shelf = it },
            )
        }
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(text = stringResource(R.string.section_library))
        }
        when {
            filtered.isEmpty() && searchQuery.isNotBlank() ->
                item(key = "empty-search", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyResultsRow()
                }

            titles.isEmpty() ->
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyLibrary()
                }

            else ->
                items(filtered, key = { "${it.sourceId}-${it.id}" }) { title ->
                    TitleCard(
                        title = title,
                        showUnread = true,
                        onClick = { onOpenTitle(title) },
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
private fun EmptyLibrary() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_bookmarks),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.hint_bookmarks),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfRow(
    selected: ShelfFilter,
    onSelect: (ShelfFilter) -> Unit,
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

        ShelfFilter.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes())) },
                colors = chipColors,
            )
        }
    }
}

private fun ShelfFilter.labelRes(): Int = when (this) {
    ShelfFilter.ALL -> R.string.chip_all
    ShelfFilter.READING -> R.string.chip_reading
    ShelfFilter.PLANNED -> R.string.chip_planned
    ShelfFilter.COMPLETED -> R.string.chip_completed
}
