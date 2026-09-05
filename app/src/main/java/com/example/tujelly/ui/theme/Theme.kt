package com.example.tujelly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.darkColorScheme as m3DarkColorScheme

private val DeepBackground = Color(0xFF0C0C12)
private val SurfaceDark = Color(0xFF161622)
private val AccentPrimary = Color(0xFFA775F8)
private val AccentSecondary = Color(0xFF8054C8)
private val TextWhite = Color(0xFFF5F5FA)
private val TextMuted = Color(0xFFB0B0C4)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TujellyTheme(
    content: @Composable () -> Unit,
) {
    val tvColorScheme = tvDarkColorScheme(
        primary = AccentPrimary,
        secondary = AccentSecondary,
        background = DeepBackground,
        surface = SurfaceDark,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = TextWhite,
        onSurface = TextWhite,
        onSurfaceVariant = TextMuted
    )

    val m3ColorScheme = m3DarkColorScheme(
        primary = AccentPrimary,
        secondary = AccentSecondary,
        background = DeepBackground,
        surface = SurfaceDark,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = TextWhite,
        onSurface = TextWhite,
        onSurfaceVariant = TextMuted
    )

    TvMaterialTheme(
        colorScheme = tvColorScheme,
        typography = Typography
    ) {
        M3MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}