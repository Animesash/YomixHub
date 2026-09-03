package com.yomixhub.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val YomixDarkColorScheme = darkColorScheme(
    primary = md_violet_primary,
    onPrimary = md_violet_onPrimary,
    primaryContainer = md_violet_primaryContainer,
    onPrimaryContainer = md_violet_onPrimaryContainer,
    secondary = md_violet_secondary,
    onSecondary = md_violet_onSecondary,
    secondaryContainer = md_violet_secondaryContainer,
    onSecondaryContainer = md_violet_onSecondaryContainer,
    tertiary = md_violet_tertiary,
    onTertiary = md_violet_onTertiary,
    tertiaryContainer = md_violet_tertiaryContainer,
    onTertiaryContainer = md_violet_onTertiaryContainer,
    error = md_violet_error,
    onError = md_violet_onError,
    errorContainer = md_violet_errorContainer,
    onErrorContainer = md_violet_onErrorContainer,
    background = md_violet_background,
    onBackground = md_violet_onBackground,
    surface = md_violet_surface,
    onSurface = md_violet_onSurface,
    surfaceVariant = md_violet_surfaceVariant,
    onSurfaceVariant = md_violet_onSurfaceVariant,
    surfaceContainerLowest = md_violet_surfaceContainerLowest,
    surfaceContainerLow = md_violet_surfaceContainerLow,
    surfaceContainer = md_violet_surfaceContainer,
    surfaceContainerHigh = md_violet_surfaceContainerHigh,
    surfaceContainerHighest = md_violet_surfaceContainerHighest,
    outline = md_violet_outline,
    outlineVariant = md_violet_outlineVariant,
    inverseSurface = md_violet_inverseSurface,
    inverseOnSurface = md_violet_inverseOnSurface,
    inversePrimary = md_violet_inversePrimary,
)

private val YomixLightColorScheme = lightColorScheme(
    primary = md_violet_light_primary,
    onPrimary = md_violet_light_onPrimary,
    primaryContainer = md_violet_light_primaryContainer,
    onPrimaryContainer = md_violet_light_onPrimaryContainer,
    secondary = md_violet_light_secondary,
    onSecondary = md_violet_light_onSecondary,
    secondaryContainer = md_violet_light_secondaryContainer,
    onSecondaryContainer = md_violet_light_onSecondaryContainer,
    tertiary = md_violet_light_tertiary,
    onTertiary = md_violet_light_onTertiary,
    tertiaryContainer = md_violet_light_tertiaryContainer,
    onTertiaryContainer = md_violet_light_onTertiaryContainer,
    error = md_violet_light_error,
    onError = md_violet_light_onError,
    errorContainer = md_violet_light_errorContainer,
    onErrorContainer = md_violet_light_onErrorContainer,
    surface = md_violet_light_surface,
    onSurface = md_violet_light_onSurface,
    onSurfaceVariant = md_violet_light_onSurfaceVariant,
    surfaceContainerLow = md_violet_light_surfaceContainerLow,
    surfaceContainer = md_violet_light_surfaceContainer,
    surfaceContainerHigh = md_violet_light_surfaceContainerHigh,
    surfaceContainerHighest = md_violet_light_surfaceContainerHighest,
    outline = md_violet_light_outline,
    outlineVariant = md_violet_light_outlineVariant,
)

/**
 * YomixHub theme: dark-first Material You.
 *
 * On Android 12+ the wallpaper-based dynamic color scheme is used when
 * [dynamicColor] is enabled; otherwise the bundled violet tonal palette is
 * applied so the app always matches the design spec.
 */
@Composable
fun YomixHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> YomixDarkColorScheme
        else -> YomixLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YomixTypography,
        content = content,
    )
}
