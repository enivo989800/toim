package com.toimg.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = MutedInk,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    outline = DividerGray,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF10243B),
    secondary = Color(0xFFB5C1D0),
    background = Color(0xFF111820),
    onBackground = Color(0xFFE7EDF5),
    surface = Color(0xFF1B2632),
    onSurface = Color(0xFFE7EDF5),
    outline = Color(0xFF3B4857),
)

@Composable
fun ToImgTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
