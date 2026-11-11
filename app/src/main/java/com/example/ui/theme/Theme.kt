package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CallBoxDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = OnPrimaryCyan,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = PrimaryCyan,
    secondary = PrimaryCyanVariant,
    onSecondary = OnPrimaryCyan,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    error = RecordingRed,
    onError = TextPrimary,
    errorContainer = RecordingRedContainer,
    onErrorContainer = RecordingRed,
    outline = BorderStroke,
    outlineVariant = OutlineVariantColor
)

@Composable
fun CallBoxTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CallBoxDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
