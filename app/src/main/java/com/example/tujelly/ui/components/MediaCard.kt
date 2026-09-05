package com.example.tujelly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
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
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.ui.theme.TvRatingBadge

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E222D) else Color(0xFF111319),
            focusedContainerColor = Color(0xFF1E222D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        modifier = modifier
            .width(150.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocus()
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.75.dp,
                color = if (isFocused) Color.White else Color(0x18FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isFocused) Color(0xFF1B1F2A) else Color(0xFF0F1117))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Right Rating Badge
                if (item.rating != null && item.rating > 0f) {
                    TvRatingBadge(
                        rating = item.rating,
                        compact = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }

                // Top-Left Badges: TOP 10 rank, Favorito, Visto
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.rank != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xCC000000),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TOP 10",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
                    val isMonochrome = indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME

                    val hasEpisodeProgress = !item.isPlayed &&
                            item.type.equals("Series", ignoreCase = true) &&
                            item.playedEpisodes != null && item.totalEpisodes != null &&
                            item.playedEpisodes > 0 && item.totalEpisodes > 0

                    if (hasEpisodeProgress) {
                        val progressBorder = if (isMonochrome) Color(0x66FFFFFF) else Color(0x9900A4DC)
                        val progressTextColor = if (isMonochrome) Color.White else Color(0xFF38BDF8)

                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xE60F172A),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, progressBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.playedEpisodes}/${item.totalEpisodes}",
                                color = progressTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (item.isFavorite) {
                        val favColor = if (isMonochrome) Color.White else Color(0xFFEF4444)
                        val favBorder = if (isMonochrome) Color(0x66FFFFFF) else Color(0x88EF4444)

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = Color(0xE60F172A),
                                    shape = CircleShape
                                )
                                .border(1.dp, favBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favorito",
                                tint = favColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (item.isPlayed) {
                        val checkColor = if (isMonochrome) Color.White else Color(0xFF10B981)
                        val checkBorder = if (isMonochrome) Color(0x66FFFFFF) else Color(0x9910B981)

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = Color(0xE60F172A),
                                    shape = CircleShape
                                )
                                .border(1.dp, checkBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Visto",
                                tint = checkColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (item.rank != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE000000)),
                                    startY = 80f
                                )
                            )
                    )

                    Text(
                        text = "${item.rank}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 42.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 6.dp, bottom = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.year != null) {
                        Text(
                            text = item.year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    if (item.year != null) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                    val isTv = item.type.equals("Series", ignoreCase = true) ||
                            item.type.equals("TvProgram", ignoreCase = true) ||
                            item.type.equals("Episode", ignoreCase = true)
                    Text(
                        text = if (isTv) "SERIE" else "PELÍCULA",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
