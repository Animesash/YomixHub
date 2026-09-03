package com.yomixhub.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R

/** Catalogue shown behind the "Все" chip of the explore screen. */
enum class ActiveSource { ALL, MANGALIB, RANOBELIB, HENTAILIB }

/**
 * Bottom sheet behind the top-bar "tune" action: lets the user pick the
 * active catalogue source. Includes HentaiLib (18+) as an opt-in choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcePickerSheet(
    selected: ActiveSource,
    onSelect: (ActiveSource) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.source_picker_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )

        ActiveSource.entries.forEach { option ->
            SourceOptionRow(
                option = option,
                selected = option == selected,
                onClick = {
                    onSelect(option)
                    onDismiss()
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SourceOptionRow(
    option: ActiveSource,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = when (option) {
        ActiveSource.ALL -> stringResource(R.string.source_all)
        ActiveSource.MANGALIB -> "MangaLib"
        ActiveSource.RANOBELIB -> "RanobeLib"
        ActiveSource.HENTAILIB -> "HentaiLib"
    }
    val subtitle = when (option) {
        ActiveSource.ALL -> stringResource(R.string.source_all_subtitle)
        ActiveSource.MANGALIB -> stringResource(R.string.chip_manga)
        ActiveSource.RANOBELIB -> stringResource(R.string.chip_novels)
        ActiveSource.HENTAILIB -> stringResource(R.string.source_hentailib_subtitle)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (option == ActiveSource.HENTAILIB) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = stringResource(R.string.badge_adult),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
