package com.example.tujelly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill
import com.example.tujelly.ui.theme.TvRatingBadge

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CompactFocusedHeader(
    item: MediaItem?,
    onPlayClick: (MediaItem) -> Unit,
    onDetailClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: String = ACCENT_CYAN,
    buttonStyle: String = BUTTON_STYLE_ICONS_ONLY,
    isMonochrome: Boolean = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current
) {
    if (item == null) return

    val focusColor = if (isMonochrome) Color.White else TvAccent.getColor(accentColor)
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else TvAccent.getFocusedContentColor(accentColor)
    val showIcons = buttonStyle != BUTTON_STYLE_TEXT_ONLY
    val showText = buttonStyle != BUTTON_STYLE_ICONS_ONLY

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD90D0F18))
            .border(0.75.dp, if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Title / ClearLogo + Badges
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                var isLogoLoaded by remember(item.id, item.logoUrl) { mutableStateOf(false) }

                if (!item.logoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 38.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        AsyncImage(
                            model = item.logoUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .heightIn(max = 38.dp)
                                .widthIn(max = 260.dp),
                            onSuccess = { isLogoLoaded = true },
                            onError = { isLogoLoaded = false }
                        )

                        if (!isLogoLoaded) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    brush = if (!isMonochrome) {
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color.White, Color(0xFFE2E8F0), focusColor.copy(alpha = 0.85f))
                                        )
                                    } else null
                                ),
                                color = if (isMonochrome) Color.White else Color.Unspecified,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = if (!isMonochrome) {
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color.White, Color(0xFFE2E8F0), focusColor.copy(alpha = 0.85f))
                                )
                            } else null
                        ),
                        color = if (isMonochrome) Color.White else Color.Unspecified,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.rating != null && item.rating > 0f) {
                        TvRatingBadge(rating = item.rating, compact = true)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (item.year != null) {
                        TvPill(text = "${item.year}")
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val isTv = item.type.equals("Series", ignoreCase = true) ||
                            item.type.equals("Episode", ignoreCase = true)
                    TvPill(text = if (isTv) "SERIE" else "PELÍCULA")

                    if (!item.overview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Quick Action Buttons
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showText) {
                            if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reproducir",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

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
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (showText) {
                            if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detalles",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
