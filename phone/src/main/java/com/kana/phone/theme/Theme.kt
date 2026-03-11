package com.kana.phone.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KanaColors = darkColorScheme(
    primary = Color(0xFF1DB954),            // Spotify green
    onPrimary = Color.Black,
    secondary = Color(0xFF1DB954),          // Spotify green
    onSecondary = Color.Black,
    background = Color(0xFF121212),         // Spotify black
    onBackground = Color.White,
    surface = Color(0xFF282828),            // Spotify dark gray
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333),     // Slightly lighter
    onSurfaceVariant = Color(0xFFB3B3B3),  // Spotify muted text
    error = Color(0xFFE22134),             // Spotify red
    onError = Color.White,
    outline = Color(0xFF535353),
)

@Composable
fun KanaPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KanaColors,
        content = content
    )
}
