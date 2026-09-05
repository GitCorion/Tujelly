package com.example.tujelly.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

object TvColors {
    val Background = Color(0xFF0A0B0E)
    val SurfaceDark = Color(0xFF12141A)
    val SurfaceElevated = Color(0xFF181B24)
    val SurfaceGlass = Color(0x18FFFFFF)
    val SurfaceGlassFocused = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)

    val BorderSubtle = Color(0x18FFFFFF)
    val BorderFocused = Color(0xFFFFFFFF)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRatingBadge(
    rating: Float?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (rating == null || rating <= 0f) return

    val formattedRating = String.format(java.util.Locale.US, "%.1f", rating)

    Box(
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
            .padding(
                horizontal = if (compact) 5.dp else 7.dp,
                vertical = if (compact) 2.dp else 3.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 10.dp else 12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = formattedRating,
                color = Color.White,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (compact) 10.sp else 12.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0x18FFFFFF),
    textColor: Color = Color(0xFFE2E8F0),
    borderColor: Color? = Color(0x25FFFFFF),
    fontSizeSp: Int = 10,
    horizontalPadDp: Dp = 7.dp,
    verticalPadDp: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(4.dp))
            .then(
                if (borderColor != null) {
                    Modifier.border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
                } else Modifier
            )
            .padding(horizontal = horizontalPadDp, vertical = verticalPadDp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

object TvAccent {
    fun getColor(key: String): Color {
        return when (key) {
            com.example.tujelly.data.local.ACCENT_AMBER -> Color(0xFFF59E0B) // Amber Cinema Warm
            com.example.tujelly.data.local.ACCENT_WHITE -> Color(0xFFFFFFFF) // Pure White Minimal
            else -> Color(0xFF00A4DC)                                         // Jellyfin Cyan Sky
        }
    }

    fun getFocusedContentColor(key: String): Color {
        return Color(0xFF0F172A) // Dark slate high-contrast text on bright backgrounds
    }

    fun getDisplayName(key: String): String {
        return when (key) {
            com.example.tujelly.data.local.ACCENT_AMBER -> "Ámbar Cine"
            com.example.tujelly.data.local.ACCENT_WHITE -> "Blanco Puro"
            else -> "Azul Cian (Jellyfin)"
        }
    }

    fun getDescription(key: String): String {
        return when (key) {
            com.example.tujelly.data.local.ACCENT_AMBER -> "Tono ámbar dorado cálido inspirado en salas de cine clásicas."
            com.example.tujelly.data.local.ACCENT_WHITE -> "Resalte blanco puro estilo Apple TV minimalista."
            else -> "Azul cian característico de Jellyfin, moderno y con gran contraste."
        }
    }
}

val LocalIndicatorTheme = androidx.compose.runtime.compositionLocalOf {
    com.example.tujelly.data.local.INDICATOR_THEME_COLOR
}

val LocalPlatformLogoStyle = androidx.compose.runtime.compositionLocalOf {
    com.example.tujelly.data.local.PLATFORM_LOGO_COLOR
}


