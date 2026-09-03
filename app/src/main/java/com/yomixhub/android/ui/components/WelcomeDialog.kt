package com.yomixhub.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yomixhub.android.BuildConfig
import com.yomixhub.android.R
import com.yomixhub.android.data.persistence.SettingsStore
import kotlinx.coroutines.launch

/**
 * One-time welcome dialog for the first alpha build: greets the user, lists
 * what already works and offers "Больше не показывать". Saving that choice
 * (DataStore `show_alpha_welcome_dialog`) happens only via the confirm
 * button, exactly as specified.
 */
@Composable
fun AlphaWelcomeDialog(onDismiss: () -> Unit) {
    var dontShowAgain by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.welcome_title, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dontShowAgain = !dontShowAgain },
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { checked -> dontShowAgain = checked },
                    )
                    Text(
                        text = stringResource(R.string.welcome_dont_show_again),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (dontShowAgain) {
                        scope.launch { SettingsStore.setAlphaWelcomeDismissed(context) }
                    }
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.welcome_start))
            }
        },
    )
}
