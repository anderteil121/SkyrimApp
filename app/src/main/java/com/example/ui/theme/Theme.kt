package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    primaryContainer = SkyrimSurfaceVariant,
    onPrimaryContainer = ParchmentLight,
    secondary = DovahkiinCyan,
    onSecondary = Color.Black,
    secondaryContainer = SkyrimBorder,
    onSecondaryContainer = Color.White,
    tertiary = ImperialRed,
    background = SkyrimDarkBackground,
    onBackground = ParchmentLight,
    surface = SkyrimSurface,
    onSurface = ParchmentLight,
    surfaceVariant = SkyrimSurfaceVariant,
    onSurfaceVariant = ParchmentBase,
    outline = SkyrimBorderLight,
    error = AlertFire
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep immersive Skyrim fantasy theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
