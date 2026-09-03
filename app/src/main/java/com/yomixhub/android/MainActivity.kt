package com.yomixhub.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yomixhub.android.data.persistence.SettingsStore
import com.yomixhub.android.ui.YomixHubApp
import com.yomixhub.android.ui.components.AlphaWelcomeDialog
import com.yomixhub.android.ui.theme.YomixHubTheme

/**
 * Single-activity entry point. The whole reader UI is Jetpack Compose with
 * Material 3, drawn edge-to-edge under the system bars.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            YomixHubTheme {
                YomixHubApp()
                AlphaWelcomeDialogHost()
            }
        }
    }
}

/**
 * Shows the one-time alpha welcome dialog on launch: the DataStore flag is
 * read asynchronously, and the dialog only appears while it is still true.
 */
@Composable
private fun AlphaWelcomeDialogHost() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showDialog = SettingsStore.shouldShowAlphaWelcome(context)
    }

    if (showDialog) {
        AlphaWelcomeDialog(onDismiss = { showDialog = false })
    }
}
