package com.example.guessthebeat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonBlue,
    background = DarkBackground,
    surface = DarkSurface,
    error = ErrorRed,
    onPrimary = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun GuessTheBeatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // Jeśli masz własną typografię, zostaw; jeśli błąd, usuń tę linijkę
        content = content
    )
}