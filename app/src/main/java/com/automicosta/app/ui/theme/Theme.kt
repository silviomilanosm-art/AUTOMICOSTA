package com.automicosta.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF176B51),
    onPrimary = Color.White,
    secondary = Color(0xFF4F635A),
    background = Color(0xFFF7F9F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EFEA)
)

@Composable
fun AutoMiCostaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
