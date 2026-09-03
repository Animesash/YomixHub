package com.yomixhub.android.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore(name = "yomixhub_settings")

/**
 * Small key-value settings (DataStore Preferences): UI flags that are not
 * part of the app content state, e.g. one-time welcome dialogs.
 */
object SettingsStore {

    /** Whether the alpha welcome dialog should be shown on launch. */
    private val KEY_SHOW_ALPHA_WELCOME = booleanPreferencesKey("show_alpha_welcome_dialog")

    /**
     * Reads the alpha welcome dialog flag; defaults to `true` so the dialog
     * appears on first launch until the user dismisses it for good.
     */
    suspend fun shouldShowAlphaWelcome(context: Context): Boolean = withContext(Dispatchers.IO) {
        context.settingsDataStore.data.first()[KEY_SHOW_ALPHA_WELCOME] ?: true
    }

    /** Hides the alpha welcome dialog on all future launches. */
    suspend fun setAlphaWelcomeDismissed(context: Context) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SHOW_ALPHA_WELCOME] = false
        }
    }
}
