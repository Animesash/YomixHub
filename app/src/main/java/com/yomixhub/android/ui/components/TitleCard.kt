package com.yomixhub.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yomixhub.android.R
import com.yomixhub.android.data.Title
import com.yomixhub.android.data.TitleFormat

/**
 * Grid card for a manga / novel title: elevated MD3 surface, 2:3 cover (remote
 * artwork or a tonal gradient placeholder) with a small rounded format chip
 * ("MANGA" / "NOVEL") in the corner and an optional unread-chapters badge.
 *
 * Titles loaded from a source show `★ rating · status` instead of the
 * volumes / chapters counters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleCard(
    title: Title,
    modifier: Modifier = Modifier,
    showUnread: Boolean = false,
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            CoverImage(
                coverUrl = title.coverUrl,
                sourceId = title.sourceId,
                letter = title.name.take(1),
                from = title.coverFrom,
                to = title.coverTo,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            )

            FormatChip(
                format = title.format,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )

            if (showUnread && title.unreadChapters > 0) {
                UnreadBadge(
                    count = title.unreadChapters,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
        ) {
            Text(
                text = title.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title.metaLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Title.metaLine(): String {
    if (volumes == 0 && chapters == 0) {
        // Remote title without library metadata: show rating and status.
        return buildString {
            if (rating > 0.0) append("★ ").append(rating)
            status?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
            if (isEmpty()) append("—")
        }
    }
    return stringResource(R.string.title_meta, volumes, chapters, rating.toString())
}

/**
 * Small rounded tag in the corner of a cover indicating the format:
 * "MANGA" on `secondaryContainer`, "NOVEL" on `tertiaryContainer`.
 */
@Composable
fun FormatChip(
    format: TitleFormat,
    modifier: Modifier = Modifier,
) {
    val containerColor: Color
    val contentColor: Color
    when (format) {
        TitleFormat.MANGA -> {
            containerColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        }

        TitleFormat.NOVEL -> {
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = format.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Number of unread chapters, bottom-anchored to the cover's top-end corner. */
@Composable
private fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = stringResource(R.string.unread_chapters, count),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Section header used across the screens (horizontal padding comes from the grid). */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 12.dp, bottom = 10.dp),
    )
}
