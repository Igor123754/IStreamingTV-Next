package com.igor.istreamingtv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IStreamingDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Background,

    background = Background,
    onBackground = TextPrimary,

    surface = BackgroundElevated,
    onSurface = TextPrimary,

    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary
)

@Composable
fun IStreamingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IStreamingDarkColors,
        typography = IStreamingTypography,
        content = content
    )
}
