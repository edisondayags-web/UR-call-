package com.urcall.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val UrDarkScheme = darkColorScheme(
    primary = UrNeon,
    secondary = UrPink,
    background = UrBlack,
    surface = UrBlack,
    onPrimary = UrBlack,
    onBackground = UrTextWhite,
    onSurface = UrTextWhite
)

@Composable
fun URCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UrDarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
