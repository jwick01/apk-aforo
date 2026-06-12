package com.hansbarrera.aditivosaforo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    secondaryContainer = GreenContainer,
    background = BackgroundLight,
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = GreenSecondary,
    secondary = GreenContainer
)

@Composable
fun AditivosAforoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
