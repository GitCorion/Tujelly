package com.example.tujelly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill
import com.example.tujelly.ui.theme.TvRatingBadge

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    item: MediaItem?,
    onPlayClick: (MediaItem) -> Unit,
    onDetailClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: String = ACCENT_CYAN,
    buttonStyle: String = BUTTON_STYLE_ICONS_ONLY
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
    ) {
        if (item?.backdropUrl != null || item?.posterUrl != null) {
            AsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0D14))
            )
        }

        // Horizontal gradient for dark left text background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF50A0B0E),
                            Color(0xDC0A0B0E),
                            Color(0x880A0B0E),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1400f
                    )
                )
        )

        // Deep obsidian gradient overlay for cinematic contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x330A0B0E),
                            Color(0xCC0A0B0E),
                            Color(0xFF0A0B0E)
                        )
                    )
                )
        )

        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 48.dp, end = 48.dp, top = 14.dp, bottom = 12.dp)
                    .fillMaxWidth(0.68f)
            ) {
                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 18f,
                            offset = Offset(2f, 3f)
                        )
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.rating != null && item.rating > 0f) {
                        TvRatingBadge(rating = item.rating, compact = false)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (item.year != null) {
                        TvPill(
                            text = "${item.year}",
                            containerColor = Color(0x18FFFFFF),
                            textColor = Color(0xFFE2E8F0),
                            borderColor = Color(0x22FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val isTv = item.type.equals("Series", ignoreCase = true) ||
                            item.type.equals("TvProgram", ignoreCase = true) ||
                            item.type.equals("Episode", ignoreCase = true)
                    TvPill(
                        text = if (isTv) "SERIE" else "PELÍCULA",
                        containerColor = Color(0x18FFFFFF),
                        textColor = Color(0xFFE2E8F0),
                        borderColor = Color(0x22FFFFFF)
                    )

                    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
                    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current ||
                            indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME

                    val isTvSeries = item.type.equals("Series", ignoreCase = true)
                    val hasEpisodeProgress = !item.isPlayed &&
                            isTvSeries &&
                            item.playedEpisodes != null &&
                            item.playedEpisodes > 0

                    if (hasEpisodeProgress) {
                        val progressText = if (item.totalEpisodes != null && item.totalEpisodes > 0) {
                            "PROGRESO: ${item.playedEpisodes}/${item.totalEpisodes}"
                        } else {
                            "PROGRESO: ${item.playedEpisodes} caps"
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = progressText,
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x5500A4DC)
                        )
                    }

                    if (item.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = "♥ FAVORITO",
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFFEF4444),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x66EF4444)
                        )
                    }

                    if (item.isPlayed && !hasEpisodeProgress) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = "✓ VISTO",
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFF6EE7B7),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x3310B981)
                        )
                    }

                    val sourceLabel = when (item.source) {
                        MediaSource.TRAKT_RECOMMENDATION -> "Trakt Recomendado"
                        MediaSource.TMDB_TRENDING -> "En Tendencia"
                        MediaSource.TMDB_RECOMMENDATION -> "Para ti"
                        MediaSource.TRAKT_WATCHLIST -> "Tu Watchlist"
                        MediaSource.JELLYFIN -> "En tu Servidor"
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TvPill(
                        text = sourceLabel,
                        containerColor = Color(0x12FFFFFF),
                        textColor = Color(0xFF94A3B8),
                        borderColor = Color(0x18FFFFFF)
                    )
                }

                // Synopsis
                if (!item.overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                val focusColor = TvAccent.getColor(accentColor)
                val focusContent = TvAccent.getFocusedContentColor(accentColor)
                val showIcons = buttonStyle != BUTTON_STYLE_TEXT_ONLY
                val showText = buttonStyle != BUTTON_STYLE_ICONS_ONLY

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onPlayClick(item) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x28FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (showIcons) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (showText) {
                                if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reproducir",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onDetailClick(item) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x14FFFFFF),
                            contentColor = Color(0xFFE2E8F0),
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (showIcons) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Detalles",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (showText) {
                                if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Detalles",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
