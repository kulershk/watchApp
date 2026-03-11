package com.kana.watch.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

private val KanaColors = Colors(
    primary = Color(0xFF1DB954),        // Spotify green
    primaryVariant = Color(0xFF1AA34A),
    secondary = Color(0xFF1DB954),      // Spotify green
    secondaryVariant = Color(0xFF1AA34A),
    background = Color(0xFF121212),     // Spotify black
    surface = Color(0xFF282828),        // Spotify dark gray
    error = Color(0xFFE22134),          // Spotify red
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White,
)

@Composable
fun KanaWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = KanaColors,
        content = content
    )
}
