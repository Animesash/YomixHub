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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.ChapterUpdate
import com.yomixhub.android.ui.components.CoverImage
import com.yomixhub.android.ui.components.SearchField
import com.yomixhub.android.ui.components.mergedWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 24 * 60 * 60 * 1000L

/**
 * "Уведомления" – feed of new chapter releases for bookmarked titles,
 * populated live from [AppState]. Tapping a row marks it as read.
 *
 * While the shared search is active, the feed is filtered by title name in
 * real time.
 */
@Composable
fun NotificationsScreen(
    innerPadding: PaddingValues,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val updates = AppState.notifications
    val filtered = if (searchQuery.isBlank()) {
        updates
    } else {
        updates.filter { it.title.name.contains(searchQuery, ignoreCase = true) }
    }

    val now = System.currentTimeMillis()
    val today = filtered.filter { now - it.createdAt < DAY_MS }
    val earlier = filtered.filter { now - it.createdAt >= DAY_MS }

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
                    hint = stringResource(R.string.search_hint_notifications),
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }

        if (updates.isEmpty()) {
            item(key = "empty") { EmptyNotifications() }
        } else if (filtered.isEmpty()) {
            item(key = "empty-search") { EmptyResultsRow() }
        }

        if (today.isNotEmpty()) {
            item(key = "header-today") {
                SectionLabel(text = stringResource(R.string.section_today))
            }
            items(today, key = { "today-${it.title.id}-${it.createdAt}" }) { update ->
                UpdateRow(update = update, onClick = { AppState.markNotificationRead(update) })
                NotificationDivider()
            }
        }

        if (earlier.isNotEmpty()) {
            item(key = "header-week") {
                SectionLabel(text = stringResource(R.string.section_this_week))
            }
            items(earlier, key = { "week-${it.title.id}-${it.createdAt}" }) { update ->
                UpdateRow(update = update, onClick = { AppState.markNotificationRead(update) })
                NotificationDivider()
            }
        }
    }
}

@Composable
private fun NotificationDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 92.dp),
    )
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
private fun EmptyNotifications() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_notifications),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.hint_notifications),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun UpdateRow(update: ChapterUpdate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = update.title.coverUrl,
            sourceId = update.title.sourceId,
            letter = update.title.name.take(1),
            from = update.title.coverFrom,
            to = update.title.coverTo,
            modifier = Modifier.size(width = 44.dp, height = 60.dp),
            shape = RoundedCornerShape(8.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.title.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = stringResource(R.string.new_chapter, update.chapter) + " · " +
                    formatTimeAgo(update.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (update.isUnread) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

/** "Только что" / "5 мин" / "3 ч" / date for anything older. */
@Composable
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> stringResource(R.string.time_just_now)
        diff < 60 * 60_000L -> stringResource(
            R.string.time_minutes_ago,
            (diff / 60_000L).toInt(),
        )

        diff < DAY_MS -> stringResource(R.string.time_hours_ago, (diff / 3_600_000L).toInt())
        else -> SimpleDateFormat("d MMMM", Locale("ru")).format(Date(timestamp))
    }
}
