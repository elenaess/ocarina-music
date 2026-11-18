package com.ocarina.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF121512)
val Card = Color(0xFF1D231E)
val Raised = Color(0xFF273027)
val Lime = Color(0xFFC5EEA4)
val WarmWhite = Color(0xFFF5F5EF)
val Muted = Color(0xFFA6AFA5)

private val OcarinaColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    background = Ink,
    onBackground = WarmWhite,
    surface = Card,
    onSurface = WarmWhite,
    surfaceVariant = Raised,
    onSurfaceVariant = Muted,
    outline = Color(0xFF424B42),
)

@Composable
fun OcarinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OcarinaColors, content = content)
}
