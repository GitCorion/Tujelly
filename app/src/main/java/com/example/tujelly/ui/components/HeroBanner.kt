package com.example.tujelly.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Border
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
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    item: MediaItem?,
    onPlayClick: (MediaItem) -> Unit,
    onDetailClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    featuredItems: List<MediaItem> = emptyList(),
    accentColor: String = ACCENT_CYAN,
    buttonStyle: String = BUTTON_STYLE_ICONS_ONLY
) {
    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current ||
            indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME

    // 1. Carrusel de destacados autorrotatorio cuando no hay foco directo
    val cleanFeatured = remember(featuredItems) { featuredItems.take(5) }
    var carouselIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(item, cleanFeatured) {
        if (item == null && cleanFeatured.size > 1) {
            while (true) {
                delay(6000L)
                carouselIndex = (carouselIndex + 1) % cleanFeatured.size
            }
        }
    }

    val effectiveItem = item ?: cleanFeatured.getOrNull(carouselIndex)

    // 2. Previsualización diferida del tráiler tras 2.5s
    var showTrailer by remember(effectiveItem?.id) { mutableStateOf(false) }

    LaunchedEffect(effectiveItem?.id) {
        showTrailer = false
        if (effectiveItem?.trailerUrl != null && effectiveItem.trailerUrl.isNotBlank()) {
            delay(2500L)
            showTrailer = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
    ) {
        // Crossfade cinematográfico de la imagen de fondo
        Crossfade(
            targetState = effectiveItem,
            animationSpec = tween(durationMillis = 400),
            label = "heroBackdropCrossfade"
        ) { currentItem ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentItem?.backdropUrl != null || currentItem?.posterUrl != null) {
                    AsyncImage(
                        model = currentItem.backdropUrl ?: currentItem.posterUrl,
                        contentDescription = currentItem.title,
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
            }
        }

        // Background Trailer Video Overlay (si hay tráiler disponible y tras 2.5s)
        if (showTrailer && effectiveItem?.trailerUrl != null) {
            val videoId = extractYoutubeId(effectiveItem.trailerUrl)
            if (videoId.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.45f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()

                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <style>
                                            * { margin:0; padding:0; }
                                            html, body { width:100vw; height:100vh; background:#000; overflow:hidden; }
                                            iframe { width:100vw; height:100vh; border:none; pointer-events:none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=1&controls=0&loop=1&playlist=$videoId&playsinline=1"
                                                allow="autoplay; encrypted-media">
                                        </iframe>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
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

        if (effectiveItem != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 48.dp, end = 48.dp, top = 20.dp, bottom = 14.dp)
                    .fillMaxWidth(0.68f)
            ) {
                // Title / ClearLogo
                var isLogoLoaded by remember(effectiveItem.id, effectiveItem.logoUrl) { mutableStateOf(false) }

                if (!effectiveItem.logoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 64.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        AsyncImage(
                            model = effectiveItem.logoUrl,
                            contentDescription = effectiveItem.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .heightIn(max = 64.dp)
                                .widthIn(max = 340.dp),
                            onSuccess = { isLogoLoaded = true },
                            onError = { isLogoLoaded = false }
                        )

                        if (!isLogoLoaded) {
                            Text(
                                text = effectiveItem.title,
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
                        }
                    }
                } else {
                    Text(
                        text = effectiveItem.title,
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (effectiveItem.rating != null && effectiveItem.rating > 0f) {
                        TvRatingBadge(rating = effectiveItem.rating, compact = false)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (effectiveItem.year != null) {
                        TvPill(
                            text = "${effectiveItem.year}",
                            containerColor = Color(0x18FFFFFF),
                            textColor = Color(0xFFE2E8F0),
                            borderColor = Color(0x22FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val isTv = effectiveItem.type.equals("Series", ignoreCase = true) ||
                            effectiveItem.type.equals("TvProgram", ignoreCase = true) ||
                            effectiveItem.type.equals("Episode", ignoreCase = true)
                    TvPill(
                        text = if (isTv) "SERIE" else "PELÍCULA",
                        containerColor = if (isMonochrome) Color(0x18FFFFFF) else Color(0x2400A4DC),
                        textColor = if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFF7DD3FC),
                        borderColor = if (isMonochrome) Color(0x22FFFFFF) else Color(0x5500A4DC)
                    )

                    val isTvSeries = effectiveItem.type.equals("Series", ignoreCase = true)
                    val hasEpisodeProgress = !effectiveItem.isPlayed &&
                            isTvSeries &&
                            effectiveItem.playedEpisodes != null &&
                            effectiveItem.playedEpisodes > 0
                    val hasMovieProgress = !effectiveItem.isPlayed &&
                            !isTvSeries &&
                            effectiveItem.playbackPositionTicks > 0

                    if (hasEpisodeProgress) {
                        val progressText = if (effectiveItem.totalEpisodes != null && effectiveItem.totalEpisodes > 0) {
                            "PROGRESO: ${effectiveItem.playedEpisodes}/${effectiveItem.totalEpisodes}"
                        } else {
                            "PROGRESO: ${effectiveItem.playedEpisodes} caps"
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = progressText,
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x5500A4DC)
                        )
                    }

                    if (hasMovieProgress) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = "EN PROGRESO",
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x5500A4DC)
                        )
                    }

                    if (effectiveItem.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = "♥ FAVORITO",
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFFEF4444),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x66EF4444)
                        )
                    }

                    if (effectiveItem.isPlayed && !hasEpisodeProgress && !hasMovieProgress) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TvPill(
                            text = "✓ VISTO",
                            containerColor = Color(0x22FFFFFF),
                            textColor = if (isMonochrome) Color.White else Color(0xFF6EE7B7),
                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x3310B981)
                        )
                    }

                    val sourceLabel = when (effectiveItem.source) {
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
                if (!effectiveItem.overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = effectiveItem.overview,
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
                        onClick = { onPlayClick(effectiveItem) },
                        colors = ButtonDefaults.colors(
                            containerColor = if (isMonochrome) Color(0x28FFFFFF) else focusColor.copy(alpha = 0.22f),
                            contentColor = if (isMonochrome) Color.White else focusColor,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        shape = ButtonDefaults.shape(shape = CircleShape),
                        border = ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.60f)),
                                shape = CircleShape
                            ),
                            focusedBorder = Border.None
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
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onDetailClick(effectiveItem) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x14FFFFFF),
                            contentColor = Color(0xFFE2E8F0),
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        shape = ButtonDefaults.shape(shape = CircleShape),
                        border = ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                shape = CircleShape
                            ),
                            focusedBorder = Border.None
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

        // Carousel Indicators (Bottom Right)
        if (item == null && cleanFeatured.size > 1) {
            val focusColor = TvAccent.getColor(accentColor)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cleanFeatured.indices.forEach { idx ->
                    val isActive = idx == carouselIndex
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) (if (isMonochrome) Color.White else focusColor) else Color(0x44FFFFFF))
                    )
                }
            }
        }
    }
}

private fun extractYoutubeId(url: String): String {
    return when {
        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        url.contains("/embed/") -> url.substringAfter("/embed/").substringBefore("?")
        url.length == 11 && !url.contains("/") -> url
        else -> ""
    }
}
