package com.example.tujelly.ui.screens.medusa

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.domain.model.MediaFormatFilter
import com.example.tujelly.ui.components.FormatFilterBar
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.components.TvNavTab
import com.example.tujelly.ui.components.TvTopBar
import com.example.tujelly.ui.theme.LocalAccentColor
import com.example.tujelly.ui.theme.LocalIsMonochromeTheme
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvRatingBadge

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MedusaScreen(
    onNavigateHome: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    viewModel: MedusaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()

    // Respetar estrictamente la temática adoptada en Ajustes (LocalIsMonochromeTheme y LocalAccentColor)
    val isMonochromeByVm by viewModel.isMonochrome.collectAsState()
    val isMonochrome = LocalIsMonochromeTheme.current || isMonochromeByVm
    val accentColorKey = LocalAccentColor.current
    val accentColor = remember(accentColorKey, isMonochrome) {
        if (isMonochrome) Color.White else TvAccent.getColor(accentColorKey)
    }

    val formatNounPlural = when (uiState.format) {
        MediaFormat.ALL -> "títulos"
        MediaFormat.MOVIES -> "películas"
        MediaFormat.SERIES -> "series"
    }

    val topBarMedusaFocusRequester = remember { FocusRequester() }
    val tagRailFocusRequester = remember { FocusRequester() }

    var isFocusedOnMoviesRow by remember { mutableStateOf(false) }
    var isFocusedOnTagRail by remember { mutableStateOf(false) }
    var isFocusedOnActiveChain by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        topBarMedusaFocusRequester.requestFocus()
    }

    // Manejo de la tecla ATRÁS (Back) para mando de TV
    BackHandler {
        if (isFocusedOnMoviesRow) {
            isFocusedOnMoviesRow = false
            isFocusedOnTagRail = true
            try {
                tagRailFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else if (isFocusedOnActiveChain) {
            isFocusedOnActiveChain = false
            isFocusedOnTagRail = true
            try {
                tagRailFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else if (!viewModel.onBackPress()) {
            onNavigateHome()
        }
    }

    // Candidatas que no están ya seleccionadas
    val candidateTags = remember(uiState.mosaicTags) {
        uiState.mosaicTags.filterNot { it.isSelected }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B))
    ) {
        // Fondo: Solo proyectar backdrop si el usuario ha seleccionado alguna temática
        if (uiState.activeChain.isNotEmpty()) {
            val backdropUrl = uiState.selectedMovie?.backdropUrl ?: uiState.matchingMovies.firstOrNull()?.backdropUrl
            if (!backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.18f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isMonochrome) Color(0x10FFFFFF) else accentColor.copy(alpha = 0.08f),
                            Color(0xEE05070B),
                            Color(0xFF05070B)
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // 1. CABECERA UNIVERSAL DE NAVEGACIÓN (TvTopBar)
            // =========================================================================
            TvTopBar(
                selectedTab = TvNavTab.MEDUSA,
                onNavigateHome = onNavigateHome,
                onOpenMedusa = { /* Ya en Medusa */ },
                onOpenFavorites = onOpenFavorites,
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                accentColorKey = accentColorKey,
                buttonStyleKey = buttonStyleKey,
                isMonochrome = isMonochrome,
                localMediaCount = uiState.totalCatalogCount,
                medusaFocusRequester = topBarMedusaFocusRequester,
                modifier = Modifier
                    .onFocusChanged {
                        if (it.hasFocus) {
                            isFocusedOnTagRail = false
                            isFocusedOnMoviesRow = false
                        }
                    }
                    .focusProperties {
                        onEnter = {
                            if (isFocusedOnTagRail && requestedFocusDirection != androidx.compose.ui.focus.FocusDirection.Up) {
                                tagRailFocusRequester
                            } else {
                                androidx.compose.ui.focus.FocusRequester.Default
                            }
                        }
                    }
            )

            // =========================================================================
            // 2. FILTRO DE FORMATO (TODOS / PELÍCULAS / SERIES)
            // =========================================================================
            FormatFilterBar(
                selected = when (uiState.format) {
                    MediaFormat.ALL -> MediaFormatFilter.ALL
                    MediaFormat.MOVIES -> MediaFormatFilter.MOVIES
                    MediaFormat.SERIES -> MediaFormatFilter.SERIES
                },
                onSelect = { filter ->
                    viewModel.setFormat(
                        when (filter) {
                            MediaFormatFilter.ALL -> MediaFormat.ALL
                            MediaFormatFilter.MOVIES -> MediaFormat.MOVIES
                            MediaFormatFilter.SERIES -> MediaFormat.SERIES
                        }
                    )
                },
                accentColorKey = accentColorKey,
                buttonStyleKey = buttonStyleKey,
                isMonochrome = isMonochrome,
                modifier = Modifier
                    .onFocusChanged {
                        if (it.hasFocus) {
                            isFocusedOnTagRail = false
                            isFocusedOnMoviesRow = false
                        }
                    }
                    .focusProperties {
                        onEnter = {
                            if (isFocusedOnTagRail && requestedFocusDirection != androidx.compose.ui.focus.FocusDirection.Up) {
                                tagRailFocusRequester
                            } else {
                                androidx.compose.ui.focus.FocusRequester.Default
                            }
                        }
                    }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))

                // =========================================================================
                // 3. ETIQUETAS ACTIVAS SELECCIONADAS (Navegables con mando TV para borrar)
                // =========================================================================
                if (uiState.activeChain.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.activeChain.forEach { tag ->
                            SelectedTagBadge(
                                tag = tag,
                                onRemove = {
                                    val remaining = uiState.activeChain.filterNot { it.id == tag.id }
                                    viewModel.removeTagFromChain(tag)
                                    if (remaining.isEmpty()) {
                                        try {
                                            tagRailFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                },
                                isMonochrome = isMonochrome,
                                accentColor = accentColor,
                                onFocusChange = { focused ->
                                    if (focused) {
                                        isFocusedOnActiveChain = true
                                        isFocusedOnTagRail = false
                                        isFocusedOnMoviesRow = false
                                    } else {
                                        isFocusedOnActiveChain = false
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // =========================================================================
                // 4. RAIL HORIZONTAL DE TEMÁTICAS
                // =========================================================================
                MedusaTagRail(
                    tags = candidateTags,
                    onTagClick = { tag ->
                        viewModel.toggleTag(tag)
                    },
                    firstPillFocusRequester = tagRailFocusRequester,
                    isMonochrome = isMonochrome,
                    accentColor = accentColor,
                    onTagFocused = { tag ->
                        isFocusedOnTagRail = true
                        isFocusedOnMoviesRow = false
                        viewModel.onTagFocused(tag)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // =========================================================================
                // 5. SECCIÓN PROTAGONISTA: PELÍCULAS DESCUBIERTAS
                // =========================================================================
                if (uiState.activeChain.isNotEmpty() && uiState.matchingMovies.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 4.dp)
                    ) {
                        val focusedMovie = uiState.selectedMovie ?: uiState.matchingMovies.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.matchingMovies.size} ${formatNounPlural.uppercase()}",
                                    color = if (isMonochrome) Color.White else accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.SansSerif
                                )

                                if (focusedMovie != null) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = focusedMovie.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (focusedMovie.year != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${focusedMovie.year})",
                                            color = Color(0x8094A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (focusedMovie.rating != null && focusedMovie.rating > 0f) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TvRatingBadge(rating = focusedMovie.rating, compact = true)
                                    }
                                }
                            }

                            Text(
                                text = if (isFocusedOnMoviesRow) "[ARRIBA] Volver a filtros  ·  [OK] Ver ficha" else "[ABAJO] Navegar películas  ·  [BACK] Deshacer",
                                color = Color(0x7794A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        isFocusedOnMoviesRow = true
                                    }
                                },
                            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(
                                items = uiState.matchingMovies,
                                key = { _, movie -> movie.id }
                            ) { _, movie ->
                                MediaCard(
                                    item = movie,
                                    onClick = {
                                        onDetailMedia(movie.id)
                                    },
                                    onFocus = {
                                        isFocusedOnTagRail = false
                                        isFocusedOnMoviesRow = true
                                        viewModel.selectMovie(movie)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // =========================================================================
                // 6. HUD INFERIOR SOBRIO
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030508))
                        .padding(horizontal = 48.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hudText = when {
                        isFocusedOnMoviesRow -> "[ARRIBA] Volver a filtros  ·  [OK] Ver ficha  ·  [BACK] Deshacer"
                        isFocusedOnActiveChain -> "[OK] Eliminar esta temática  ·  [ABAJO] Rail de temáticas  ·  [BACK] Volver"
                        uiState.activeChain.isNotEmpty() -> "[OK] Filtrar más  ·  [ARRIBA] Editar filtros  ·  [ABAJO] Películas  ·  [BACK] Deshacer"
                        else -> "[OK] Seleccionar  ·  [← →] Explorar  ·  [BACK] Salir"
                    }

                    Text(
                        text = hudText,
                        color = if (uiState.activeChain.isNotEmpty()) {
                            if (isMonochrome) Color.White else accentColor
                        } else Color(0x8094A3B8),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp
                    )

                    if (uiState.activeChain.isNotEmpty()) {
                        Text(
                            text = "${uiState.matchingMovies.size} $formatNounPlural disponibles",
                            color = if (isMonochrome) Color.White else accentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Píldora de temática seleccionada en la cadena activa.
 * Enfocable con mando TV, resalte bioluminiscente nítido y tecla [OK] para eliminar.
 */
@Composable
private fun SelectedTagBadge(
    tag: MedusaMosaicTag,
    onRemove: () -> Unit,
    isMonochrome: Boolean,
    accentColor: Color,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
    }

    val focusColor = if (isMonochrome) Color.White else accentColor
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else Color(0xFF0F172A)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "badgeScale"
    )

    // Exactamente los mismos estados de FormatFilterBar y TvTopBar:
    // Foco -> Sólido cian con texto oscuro
    // Seleccionado en reposo -> Fondo translúcido con borde cian y texto cian
    val containerColor = if (isFocused) {
        focusColor
    } else {
        if (isMonochrome) Color(0x28FFFFFF) else focusColor.copy(alpha = 0.22f)
    }

    val contentColor = if (isFocused) {
        focusContent
    } else {
        if (isMonochrome) Color.White else focusColor
    }

    val borderColor = if (isFocused) {
        Color.Transparent
    } else {
        if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.70f)
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(containerColor)
            .then(
                if (!isFocused) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(15.dp))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRemove
            )
            .padding(horizontal = 14.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "✓",
                color = contentColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tag.label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "✕",
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
