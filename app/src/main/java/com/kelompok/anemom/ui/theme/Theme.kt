package com.kelompok.anemom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnemomColorScheme = lightColorScheme(
    primary = Pink500,
    onPrimary = Color.White,
    primaryContainer = Pink100,
    onPrimaryContainer = Maroon,
    secondary = GradientStart,
    onSecondary = Color.White,
    secondaryContainer = Pink100,
    onSecondaryContainer = Pink500,
    background = Pink50,
    onBackground = Maroon,
    surface = Color.White,
    onSurface = Maroon,
    surfaceVariant = Pink100,
    onSurfaceVariant = MutedText,
    outline = CardBorder,
    error = ErrorRed
)

@Composable
fun AnemomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AnemomColorScheme,
        typography = AnemomTypography,
        content = content
    )
}
