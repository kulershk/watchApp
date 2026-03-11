package com.kana.watch.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

private val KanaColors = Colors(
    primary = Color(0xFFFF6B8A),       // Sakura pink
    primaryVariant = Color(0xFFE91E63),
    secondary = Color(0xFF80CBC4),      // Soft teal
    secondaryVariant = Color(0xFF4DB6AC),
    background = Color(0xFF1A1A2E),    // Dark navy
    surface = Color(0xFF16213E),       // Slightly lighter navy
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
)

@Composable
fun KanaWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = KanaColors,
        content = content
    )
}
