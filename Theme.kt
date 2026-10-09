package com.resona.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ResonaDark = darkColorScheme(
    primary = Color(0xFF7FE3C3),
    secondary = Color(0xFFB39DFF),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF17171D),
    onPrimary = Color(0xFF00201A),
    onBackground = Color(0xFFEDEDF2),
    onSurface = Color(0xFFEDEDF2)
)

@Composable
fun ResonaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ResonaDark,
        content = content
    )
}
