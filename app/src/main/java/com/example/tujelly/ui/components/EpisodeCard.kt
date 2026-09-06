package com.example.tujelly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.domain.model.EpisodeItem
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: EpisodeItem,
    onClick: () -> Unit,
    accentColor: String = com.example.tujelly.data.local.ACCENT_CYAN,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusBorderColor = TvAccent.getColor(accentColor)

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E222D) else Color(0xFF111319),
            focusedContainerColor = Color(0xFF1E222D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        modifier = modifier
            .width(220.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.75.dp,
                color = if (isFocused) focusBorderColor else Color(0x18FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF1A1C24))
            ) {
                if (episode.imageUrl != null) {
                    AsyncImage(
                        model = episode.imageUrl,
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x88000000)
                                )
                            )
                        )
                )

                // Watched badge
                if (episode.isPlayed) {
                    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
                    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current ||
                            indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        TvPill(
                            text = "✓ Visto",
                            containerColor = if (isMonochrome) Color(0xCC334155) else Color(0xB310B981),
                            textColor = Color.White,
                            borderColor = if (isMonochrome) Color(0x88FFFFFF) else Color.Transparent
                        )
                    }
                }

                // Duration badge
                if (episode.formattedDuration != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        TvPill(
                            text = episode.formattedDuration!!,
                            containerColor = Color(0xCC000000),
                            textColor = Color(0xFFE2E8F0),
                            borderColor = Color(0x33FFFFFF)
                        )
                    }
                }

                // Play icon overlay when focused
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .background(focusBorderColor, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!episode.overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
