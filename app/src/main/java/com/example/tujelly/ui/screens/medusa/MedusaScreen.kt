package com.example.tujelly.ui.screens.medusa

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.tujelly.R
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.components.TvNavTab
import com.example.tujelly.ui.components.TvTopBar
import com.example.tujelly.ui.theme.TvAccent

@OptIn(ExperimentalTvMaterial3Api::class)
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
    val accentColorKey by viewModel.accentColor.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()

    val focusColor = TvAccent.getColor(accentColorKey)
    val focusContent = TvAccent.getFocusedContentColor(accentColorKey)
    val showTopIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showTopText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY

    val medusaColor = if (isMonochrome) Color.White else focusColor

    val lazyListState = rememberLazyListState()

    // Scroll automático al principio si se forma la constelación
    LaunchedEffect(uiState.isFormed) {
        if (uiState.isFormed) {
            lazyListState.animateScrollToItem(0)
        }
    }

    // Altura dinámica animada de la constelación (espacio amplio para los filamentos de la criatura marina)
    val constellationHeight by animateDpAsState(
        targetValue = if (uiState.isFormed) 285.dp else 370.dp,
        animationSpec = tween(durationMillis = 500),
        label = "constellationHeight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060709)) // Negro estelar puro
    ) {
        // =========================================================================
        // CABECERA UNIVERSAL HOMOGÉNEA CON TODAS LAS PANTALLAS
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
            isMonochrome = isMonochrome
        )

        // =========================================================================
        // CUERPO: CONSTELACIÓN ESTELAR DE IDEAS ("DE MÁS A MENOS")
        // =========================================================================
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(constellationHeight)
                        .pointerInput(uiState.visibleStarIds, uiState.starPositions) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val h = size.height
                                val radiusPx = 55.dp.toPx()
                                val tapped = uiState.allStars.firstOrNull { star ->
                                    if (!uiState.visibleStarIds.contains(star.id)) return@firstOrNull false
                                    val pos = uiState.starPositions[star.id] ?: Pair(star.baseNormX, star.baseNormY)
                                    val sx = w * pos.first
                                    val sy = h * pos.second
                                    val dx = offset.x - sx
                                    val dy = offset.y - sy
                                    (dx * dx + dy * dy) <= (radiusPx * radiusPx)
                                }
                                if (tapped != null) {
                                    Log.d("MedusaScreen", "Global canvas tap toggling: ${tapped.id}")
                                    viewModel.toggleStar(tapped.id)
                                }
                            }
                        }
                ) {
                    val w = maxWidth
                    val h = maxHeight

                    // 1. Trazos celestes conectores y tentáculos fluidos
                    MedusaConstellationView(
                        modifier = Modifier.fillMaxSize(),
                        allStars = uiState.allStars,
                        selectedPath = uiState.selectedPath,
                        activeBranchIds = uiState.activeBranchIds,
                        starPositions = uiState.starPositions,
                        accentColor = medusaColor
                    )

                    // 2. Renderizado de estrellas puras
                    uiState.allStars.forEach { star ->
                        val isInPath = uiState.selectedPath.contains(star.id)
                        val isActiveNode = uiState.selectedPath.lastOrNull() == star.id
                        val isAncestor = isInPath && !isActiveNode
                        val isBranch = uiState.activeBranchIds.contains(star.id)
                        val isCosmicBackground = uiState.isFormed && !isInPath && !isBranch

                        val targetPos = uiState.starPositions[star.id] ?: Pair(star.baseNormX, star.baseNormY)

                        val animatedNormX by animateFloatAsState(
                            targetValue = targetPos.first,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "starX_${star.id}"
                        )
                        val animatedNormY by animateFloatAsState(
                            targetValue = targetPos.second,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "starY_${star.id}"
                        )
                        val targetAlpha = if (!uiState.isFormed) {
                            1f
                        } else if (isInPath || isBranch) {
                            1f
                        } else {
                            0.28f // Estrellas cósmicas titilantes de fondo
                        }
                        val animatedAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(durationMillis = 400),
                            label = "starAlpha_${star.id}"
                        )

                        if (animatedAlpha > 0.02f) {
                            val posX = w * animatedNormX
                            val posY = h * animatedNormY

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = posX, y = posY)
                                    .graphicsLayer {
                                        translationX = -size.width / 2f
                                        translationY = -size.height / 2f
                                        alpha = animatedAlpha
                                    }
                            ) {
                                ConstellationAstroStar(
                                    star = star,
                                    isSelected = isInPath,
                                    isAncestor = isAncestor,
                                    isCosmicBackground = isCosmicBackground,
                                    accentColor = medusaColor,
                                    onClick = {
                                        Log.d("MedusaScreen", "Direct click on star: ${star.id}")
                                        viewModel.toggleStar(star.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECCIÓN RECOMENDACIONES (AL SELECCIONAR CUALQUIER IDEA)
            // =========================================================================
            if (uiState.isFormed) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(medusaColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val activeWords = uiState.selectedPath
                                    .mapNotNull { id -> uiState.starMap[id]?.label }
                                    .joinToString(" → ")
                                val levelNames = listOf("Origen", "Subgénero", "Matiz")
                                val currentLevelIdx = (uiState.selectedPath.size - 1).coerceIn(0, 2)
                                val currentLevelName = levelNames[currentLevelIdx]
                                val countSuffix = if (uiState.recommendations.isNotEmpty()) " (${uiState.recommendations.size} títulos)" else ""
                                Text(
                                    text = "Tentáculo [$currentLevelName]: $activeWords$countSuffix",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.selectedPath.size > 1) {
                                    Button(
                                        onClick = { viewModel.popLastStar() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = Color(0x18FFFFFF),
                                            contentColor = Color(0xFFCBD5E1),
                                            focusedContainerColor = medusaColor,
                                            focusedContentColor = focusContent
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = "Retroceder Nivel",
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Retroceder Nivel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.resetConstellation() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0x18FFFFFF),
                                        contentColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = medusaColor,
                                        focusedContentColor = focusContent
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Reiniciar Tentáculo",
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Reiniciar Tentáculo",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Format Filter Chips (Todos, Películas, Series)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        ) {
                            MedusaFormatFilter.entries.forEach { filter ->
                                val isSelected = uiState.selectedFormat == filter
                                Surface(
                                    onClick = { viewModel.setFormatFilter(filter) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = if (isSelected) medusaColor.copy(alpha = 0.25f) else Color(0x18FFFFFF),
                                        focusedContainerColor = medusaColor,
                                        pressedContainerColor = medusaColor.copy(alpha = 0.85f)
                                    ),
                                    border = ClickableSurfaceDefaults.border(
                                        border = Border(
                                            BorderStroke(
                                                1.dp,
                                                if (isSelected) medusaColor.copy(alpha = 0.75f) else Color(0x28FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                        focusedBorder = Border(
                                            BorderStroke(1.5.dp, Color.White),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    ),
                                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
                                ) {
                                    Text(
                                        text = filter.displayName,
                                        color = when {
                                            isSelected -> Color.White
                                            else -> Color(0xFF94A3B8)
                                        },
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = medusaColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Alineando tentáculo con tu catálogo...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            }
                        } else if (uiState.recommendations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron títulos alineados con este tentáculo en tu biblioteca.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(uiState.recommendations, key = { it.id }) { mediaItem ->
                                    MediaCard(
                                        item = mediaItem,
                                        onClick = { onDetailMedia(mediaItem.id) },
                                        onFocus = { viewModel.setFocusedItem(mediaItem) },
                                        modifier = Modifier.width(135.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✦ Navega por las estrellas celestes para despertar un tentáculo y descubrir títulos afines",
                            color = Color(0x60FFFFFF),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estrella astronómica interactiva pura (estilo firmamento real).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConstellationAstroStar(
    star: MedusaStar,
    isSelected: Boolean,
    isAncestor: Boolean = false,
    isCosmicBackground: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    if (isAncestor) {
        // Perla sináptica elegante sobre el cordón del tentáculo (no satura ni se superpone)
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = accentColor.copy(alpha = 0.25f),
                focusedContainerColor = accentColor.copy(alpha = 0.60f),
                pressedContainerColor = accentColor.copy(alpha = 0.80f)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, accentColor.copy(alpha = 0.70f)), shape = CircleShape),
                focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CircleShape)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.30f),
            modifier = modifier
                .size(28.dp)
                .zIndex(if (isFocused) 10f else 4f)
                .onFocusChanged { isFocused = it.isFocused }
                .pointerInput(star.id) {
                    detectTapGestures { onClick() }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                StarPoint(isFocused = isFocused, isSelected = true, accentColor = accentColor)
            }
        }
    } else if (isCosmicBackground && !isFocused) {
        // Estrella cósmica de fondo: punto estelar luminoso puro que no obstruye los tentáculos activos
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0x18FFFFFF),
                focusedContainerColor = accentColor.copy(alpha = 0.40f),
                pressedContainerColor = accentColor.copy(alpha = 0.60f)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(0.6.dp, Color(0x28FFFFFF)), shape = CircleShape),
                focusedBorder = Border(BorderStroke(1.8.dp, Color.White), shape = CircleShape)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.20f),
            modifier = modifier
                .size(20.dp)
                .zIndex(1f)
                .onFocusChanged { isFocused = it.isFocused }
                .pointerInput(star.id) {
                    detectTapGestures { onClick() }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                StarPoint(isFocused = false, isSelected = false, accentColor = accentColor)
            }
        }
    } else {
        // Cápsula activa, rama interactiva de tentáculo o estrella de fondo al recibir foco
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = when {
                    isSelected -> accentColor.copy(alpha = 0.24f)
                    else -> Color(0x10FFFFFF)
                },
                focusedContainerColor = accentColor.copy(alpha = 0.35f),
                pressedContainerColor = accentColor.copy(alpha = 0.50f)
            ),
            border = ClickableSurfaceDefaults.border(
                border = if (isSelected) {
                    Border(BorderStroke(1.2.dp, accentColor.copy(alpha = 0.85f)), shape = RoundedCornerShape(16.dp))
                } else {
                    Border(BorderStroke(0.8.dp, Color(0x22FFFFFF)), shape = RoundedCornerShape(16.dp))
                },
                focusedBorder = Border(BorderStroke(1.8.dp, Color.White), shape = RoundedCornerShape(16.dp))
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            modifier = modifier
                .wrapContentSize()
                .zIndex(if (isFocused) 10f else if (isSelected) 5f else 2f)
                .onFocusChanged { isFocused = it.isFocused }
                .pointerInput(star.id) {
                    detectTapGestures { onClick() }
                }
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(32.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StarPoint(isFocused = isFocused, isSelected = isSelected, accentColor = accentColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = star.label,
                    color = when {
                        isFocused -> Color.White
                        isSelected -> Color(0xFFF8FAFC)
                        else -> Color(0xDDFFFFFF)
                    },
                    fontSize = if (isFocused) 12.sp else 11.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Dibuja el punto estelar de luz pura con halos según su estado de foco y selección.
 */
@Composable
private fun StarPoint(
    isFocused: Boolean,
    isSelected: Boolean,
    accentColor: Color
) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)

        if (isFocused) {
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = 9.0f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 4.2f,
                center = center
            )
            drawLine(Color.White.copy(alpha = 0.85f), Offset(center.x - 6.5f, center.y), Offset(center.x + 6.5f, center.y), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(center.x, center.y - 6.5f), Offset(center.x, center.y + 6.5f), strokeWidth = 1f)
        } else if (isSelected) {
            drawCircle(
                color = accentColor.copy(alpha = 0.45f),
                radius = 7.5f,
                center = center,
                style = Stroke(1.2f)
            )
            drawCircle(
                color = Color.White,
                radius = 3.8f,
                center = center
            )
            drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x - 5f, center.y), Offset(center.x + 5f, center.y), strokeWidth = 0.8f)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x, center.y - 5f), Offset(center.x, center.y + 5f), strokeWidth = 0.8f)
        } else {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2.6f,
                center = center
            )
        }
    }
}
