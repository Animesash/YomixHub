package com.yomixhub.android.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Immersive reading (Option A for the system navigation bar): hides the
 * Android system navigation bar while this composable is shown and brings it
 * back on dispose. Swipe from the edge reveals the bar transiently
 * ([WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE]).
 *
 * The app's own bottom NavigationBar keeps its `navigationBarsPadding()`,
 * which collapses to zero while the system bar is hidden.
 */
@Composable
fun HideSystemNavigationBar() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.navigationBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
