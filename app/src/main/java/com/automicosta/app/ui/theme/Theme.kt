package com.automicosta.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AutoMiCostaColors = lightColorScheme(
    primary = Color(0xFF0B6B68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F1EC),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCDE8E3),
    tertiary = Color(0xFF8A5B00),
    tertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFFF5FAF9),
    surface = Color(0xFFFCFDFB),
    surfaceVariant = Color(0xFFDAE5E2),
    error = Color(0xFFBA1A1A)
)

@Composable
fun AutoMiCostaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoMiCostaColors,
        typography = Typography(),
        content = content
    )
}
