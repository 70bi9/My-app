package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = AnimeBlue,
    onPrimary = DarkTextBlue,
    secondary = AnimeBlueAccent,
    background = MangaDarkBackground,
    surface = MangaDarkSurface,
    surfaceVariant = MangaDarkSurfaceVariant,
    onBackground = MangaTextPrimary,
    onSurface = MangaTextPrimary,
    onSurfaceVariant = MangaTextSecondary
)

// We always want high-contrast eyes-friendly dark mode for manga reading and processing layout.
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark manga theme
    dynamicColor: Boolean = false, // Use our handcrafted slate colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
