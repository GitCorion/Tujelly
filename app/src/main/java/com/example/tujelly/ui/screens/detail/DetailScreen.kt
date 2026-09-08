package com.example.tujelly.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.domain.model.SeriesStatusType
import com.example.tujelly.ui.components.EpisodeCard
import com.example.tujelly.ui.components.InAppTrailerPlayer
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill
import com.example.tujelly.ui.theme.TvRatingBadge

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    var isShowingTrailer by remember { mutableStateOf(false) }
    var isLaunchingPlayer by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isLaunchingPlayer = false
        }
    }

    LaunchedEffect(itemId) {
        viewModel.loadDetail(itemId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isMonochromeFlow by viewModel.isMonochrome.collectAsState()
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current || isMonochromeFlow

    val playButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Success) {
            delay(120)
            runCatching {
                playButtonFocusRequester.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070B))
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.tujelly.ui.components.JellyLoadingIndicator(
                        size = 84.dp,
                        message = "Cargando detalles...",
                        isMonochrome = isMonochrome
                    )
                }
            }

            is DetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text("Volver", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is DetailUiState.Success -> {
                val entity = state.entity
                val focusColor = TvAccent.getColor(state.accentColor)
                val focusContent = TvAccent.getFocusedContentColor(state.accentColor)
                val isTv = entity.type.equals("Series", ignoreCase = true) || entity.type.equals("Episode", ignoreCase = true)
                val isTvSeries = entity.type.equals("Series", ignoreCase = true)

                // Backdrop Image Background
                if (state.backdropUrl != null) {
                    AsyncImage(
                        model = state.backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xF507070B),
                                    Color(0xD807070B),
                                    Color(0x8807070B),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 1400f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x5507070B),
                                    Color(0xFF07070B)
                                )
                            )
                        )
                )

                // Main Content Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Poster Card
                    if (state.posterUrl != null) {
                        Box(
                            modifier = Modifier
                                .width(210.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF222438), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = state.posterUrl,
                                contentDescription = entity.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(36.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // isMonochrome is already computed in outer scope (line 98)

                        // ClearLogo transparent vector with high-contrast typography fallback
                        var isLogoLoaded by remember(entity.id) { mutableStateOf(false) }

                        if (!state.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.logoUrl,
                                contentDescription = entity.title,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .heightIn(max = 75.dp)
                                    .widthIn(max = 380.dp)
                                    .graphicsLayer { alpha = if (isLogoLoaded) 1f else 0f },
                                onSuccess = { isLogoLoaded = true },
                                onError = { isLogoLoaded = false }
                            )
                        }

                        if (state.logoUrl.isNullOrBlank() || !isLogoLoaded) {
                            Text(
                                text = entity.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (entity.communityRating != null && entity.communityRating > 0f) {
                                TvRatingBadge(rating = entity.communityRating, compact = false)
                            }

                            if (entity.productionYear != null) {
                                TvPill(
                                    text = "${entity.productionYear}",
                                    containerColor = Color(0x18FFFFFF),
                                    textColor = Color(0xFFE2E8F0),
                                    borderColor = Color(0x22FFFFFF)
                                )
                            }

                            TvPill(
                                text = when {
                                    state.isCollection -> "COLECCIÓN"
                                    isTv -> "SERIE"
                                    else -> "PELÍCULA"
                                },
                                containerColor = if (state.isCollection) Color(0x3338BDF8) else Color(0x18FFFFFF),
                                textColor = if (state.isCollection && !isMonochrome) Color(0xFF7DD3FC) else Color(0xFFE2E8F0),
                                borderColor = if (state.isCollection) Color(0x6638BDF8) else Color(0x22FFFFFF)
                            )

                            // Series Status Badge (Terminada, Continúa, Suspendida / Sin final)
                            if (state.seriesStatus != null && state.seriesStatus.type != SeriesStatusType.UNKNOWN) {
                                when (state.seriesStatus.type) {
                                    SeriesStatusType.CANCELED -> {
                                        TvPill(
                                            text = if (isMonochrome) state.seriesStatus.label else "🔴 ${state.seriesStatus.label}",
                                            containerColor = if (isMonochrome) Color(0x18FFFFFF) else Color(0x33EF4444),
                                            textColor = if (isMonochrome) Color.White else Color(0xFFFCA5A5),
                                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x66EF4444)
                                        )
                                    }
                                    SeriesStatusType.ENDED -> {
                                        TvPill(
                                            text = if (isMonochrome) state.seriesStatus.label else "⚪ ${state.seriesStatus.label}",
                                            containerColor = Color(0x18FFFFFF),
                                            textColor = Color(0xFFE2E8F0),
                                            borderColor = Color(0x33FFFFFF)
                                        )
                                    }
                                    SeriesStatusType.CONTINUING -> {
                                        TvPill(
                                            text = if (isMonochrome) state.seriesStatus.label else "🟢 ${state.seriesStatus.label}",
                                            containerColor = if (isMonochrome) Color(0x18FFFFFF) else Color(0x2E10B981),
                                            textColor = if (isMonochrome) Color.White else Color(0xFF6EE7B7),
                                            borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x5510B981)
                                        )
                                    }
                                    SeriesStatusType.UNKNOWN -> {}
                                }
                            }

                            if (!entity.genres.isNullOrBlank()) {
                                TvPill(
                                    text = entity.genres,
                                    containerColor = Color(0x12FFFFFF),
                                    textColor = Color(0xFF94A3B8),
                                    borderColor = Color(0x18FFFFFF)
                                )
                            }

                            val totalEps = entity.totalItemCount
                            val unplayedEps = entity.unplayedItemCount
                            val playedEps = if (totalEps != null && unplayedEps != null) (totalEps - unplayedEps).coerceAtLeast(0) else null
                            val isSeriesFinished = if (isTvSeries && unplayedEps != null) unplayedEps == 0 && (totalEps ?: 0) > 0 else entity.isPlayed
                            val hasSeriesProgress = !isSeriesFinished && isTvSeries && playedEps != null && totalEps != null && playedEps > 0

                            if (hasSeriesProgress) {
                                TvPill(
                                    text = "PROGRESO: $playedEps/$totalEps",
                                    containerColor = Color(0x22FFFFFF),
                                    textColor = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                                    borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x5500A4DC)
                                )
                            }

                            if (state.isFavorite) {
                                TvPill(
                                    text = "♥ FAVORITO",
                                    containerColor = Color(0x22FFFFFF),
                                    textColor = if (isMonochrome) Color.White else Color(0xFFEF4444),
                                    borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x66EF4444)
                                )
                            }

                            if (isSeriesFinished && !hasSeriesProgress) {
                                TvPill(
                                    text = "✓ VISTO",
                                    containerColor = Color(0x22FFFFFF),
                                    textColor = if (isMonochrome) Color.White else Color(0xFF6EE7B7),
                                    borderColor = if (isMonochrome) Color(0x33FFFFFF) else Color(0x3310B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val showIcons = state.buttonStyle != BUTTON_STYLE_TEXT_ONLY
                        val showText = state.buttonStyle != BUTTON_STYLE_ICONS_ONLY

                        // Action Buttons Row (Adapts dynamically to buttonStyle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val playTargetId = when {
                                state.isCollection && state.collectionItems.isNotEmpty() -> state.collectionItems.first().id
                                isTvSeries && state.nextUpEpisode != null -> state.nextUpEpisode.id
                                else -> entity.id
                            }
                            val isResume = state.nextUpEpisode != null && (state.nextUpEpisode.isPlayed || state.nextUpEpisode.playbackPositionTicks > 0)
                            val playLabel = when {
                                state.isCollection -> if (state.collectionItems.isNotEmpty()) "Reproducir Colección" else "Reproducir"
                                isTvSeries && state.nextUpEpisode != null && isResume -> "Continuar ${state.nextUpEpisode.displayCode}"
                                isTvSeries && state.nextUpEpisode != null -> "Reproducir ${state.nextUpEpisode.displayCode}"
                                else -> "Reproducir"
                            }

                            // 1. Play Button
                            Button(
                                onClick = {
                                    isLaunchingPlayer = true
                                    onPlay(playTargetId)
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0x28FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = focusColor,
                                    focusedContentColor = focusContent
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                                modifier = Modifier
                                    .focusRequester(playButtonFocusRequester)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = playLabel,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (showText) {
                                        if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                        Text(playLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            // 2. Favorite Toggle Button
                            Button(
                                onClick = { viewModel.toggleFavorite() },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (state.isFavorite) Color(0x28FFFFFF) else Color(0x14FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = focusColor,
                                    focusedContentColor = focusContent
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = if (state.isFavorite) "En Favoritos" else "Añadir a Favoritos",
                                            tint = if (state.isFavorite && !isMonochrome) Color(0xFFEF4444) else Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    if (showText) {
                                        if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (state.isFavorite) "En Favoritos" else "Añadir a Favoritos",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // 3. Trailer Button
                            if (state.trailerUrl != null) {
                                Button(
                                    onClick = { isShowingTrailer = true },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0x14FFFFFF),
                                        contentColor = Color.White,
                                        focusedContainerColor = focusColor,
                                        focusedContentColor = focusContent
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (showIcons) {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = "Ver Tráiler",
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        if (showText) {
                                            if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                            Text("Ver Tráiler", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // 4. Official App Button
                            Button(
                                onClick = {
                                    val packages = listOf(
                                        "org.jellyfin.androidtv",
                                        "org.jellyfin.mobile",
                                        "org.jellyfin.androidtv.debug",
                                        "org.jellyfin.mobile.debug",
                                        "com.mb.android"
                                    )
                                    val pm = context.packageManager
                                    var launched = false

                                    for (pkg in packages) {
                                        val appIntent = pm.getLaunchIntentForPackage(pkg)
                                        if (appIntent != null) {
                                            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(appIntent)
                                            launched = true
                                            break
                                        }
                                    }

                                    if (!launched && !state.baseUrl.isNullOrBlank()) {
                                        val itemWebUrl = Uri.parse("${state.baseUrl}/web/index.html#!/details?id=${entity.id}")
                                        val webIntent = Intent(Intent.ACTION_VIEW, itemWebUrl).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching {
                                            context.startActivity(webIntent)
                                            launched = true
                                        }
                                    }

                                    if (!launched) {
                                        Toast.makeText(context, "App oficial de Jellyfin no encontrada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0x14FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = focusColor,
                                    focusedContentColor = focusContent
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tv,
                                            contentDescription = "App Jellyfin",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    if (showText) {
                                        if (showIcons) Spacer(modifier = Modifier.width(6.dp))
                                        Text("App Jellyfin", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Overview Synopsis Box (ALWAYS present right under buttons)
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF111220), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF222438), RoundedCornerShape(12.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Sinopsis",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entity.overview?.takeIf { it.isNotBlank() } ?: "Sin descripción disponible para este título.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        // Seasons and Episodes Section for Series
                        if (isTvSeries && state.seasons.isNotEmpty()) {
                            if (state.seasons.size > 1) {
                                Spacer(modifier = Modifier.height(22.dp))
                                Text(
                                    text = "TEMPORADAS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    state.seasons.forEach { season ->
                                        val isSelected = season.id == state.selectedSeasonId
                                        Button(
                                            onClick = { viewModel.selectSeason(season.id) },
                                            colors = ButtonDefaults.colors(
                                                containerColor = if (isSelected) Color(0x33FFFFFF) else Color(0x10FFFFFF),
                                                contentColor = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                focusedContainerColor = focusColor,
                                                focusedContentColor = focusContent
                                            ),
                                            border = ButtonDefaults.border(
                                                border = Border(
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) (if (isMonochrome) Color.White.copy(alpha = 0.6f) else focusColor.copy(alpha = 0.6f)) else Color(0x18FFFFFF)
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                ),
                                                focusedBorder = Border(
                                                    border = BorderStroke(2.dp, focusColor),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                            ),
                                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp))
                                        ) {
                                            Text(
                                                text = season.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "CAPÍTULOS (${state.episodes.size})",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (state.isLoadingEpisodes) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cargando capítulos...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (state.episodes.isEmpty()) {
                                Text(
                                    text = "No se encontraron capítulos en el servidor para esta temporada.",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(state.episodes) { episode ->
                                        EpisodeCard(
                                            episode = episode,
                                            onClick = {
                                                isLaunchingPlayer = true
                                                onPlay(episode.id)
                                            },
                                            accentColor = state.accentColor
                                        )
                                    }
                                }
                            }
                        }

                        // Collection Titles Section for Collections / BoxSets
                        if (state.collectionItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(28.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TvPill(
                                    text = "COLECCIÓN",
                                    containerColor = Color(0x3338BDF8),
                                    textColor = Color(0xFF7DD3FC),
                                    borderColor = Color(0x6638BDF8)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TÍTULOS DE ESTA COLECCIÓN (${state.collectionItems.size})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.collectionItems, key = { it.id }) { item ->
                                    MediaCard(
                                        item = item,
                                        onClick = {
                                            viewModel.loadDetail(item.id)
                                        }
                                    )
                                }
                            }
                        }

                        // Discrete Recommendations Section at the Very Bottom
                        if (state.similarItems.isNotEmpty() || (state.genreItems.isNotEmpty() && !state.genreName.isNullOrBlank())) {
                            Spacer(modifier = Modifier.height(44.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x18FFFFFF))
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            if (state.similarItems.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TvPill(
                                        text = "RECOMENDADO",
                                        containerColor = Color(0x18FFFFFF),
                                        textColor = Color(0xFFCBD5E1),
                                        borderColor = Color(0x22FFFFFF)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "TÍTULOS SIMILARES",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(state.similarItems, key = { it.id }) { item ->
                                        MediaCard(
                                            item = item,
                                            onClick = {
                                                viewModel.loadDetail(item.id)
                                            }
                                        )
                                    }
                                }
                            }

                            if (state.genreItems.isNotEmpty() && !state.genreName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TvPill(
                                        text = "GÉNERO",
                                        containerColor = Color(0x18FFFFFF),
                                        textColor = Color(0xFFCBD5E1),
                                        borderColor = Color(0x22FFFFFF)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "MÁS DE ${state.genreName!!.uppercase()}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(state.genreItems, key = { it.id }) { item ->
                                        MediaCard(
                                            item = item,
                                            onClick = {
                                                viewModel.loadDetail(item.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isShowingTrailer && state.trailerUrl != null) {
                    InAppTrailerPlayer(
                        trailerUrl = state.trailerUrl,
                        onClose = { isShowingTrailer = false }
                    )
                }
            }
        }

        // Overlay when launching player
        if (isLaunchingPlayer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(50.dp)
                    )
                    Text(
                        text = "Iniciando reproductor...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
