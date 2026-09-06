package com.example.tujelly.ui.screens.medusa

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    // Altura dinámica animada de la constelación:
    // En reposo (menos de 3 estrellas) toma 370dp para explorar cómodamente.
    // Al iluminarse (3 o más estrellas) se compacta con suavidad a 200dp para que las recomendaciones
    // aparezcan directamente en pantalla en la mitad inferior sin necesidad de scroll.
    val constellationHeight by animateDpAsState(
        targetValue = if (uiState.isFormed) 200.dp else 370.dp,
        animationSpec = tween(durationMillis = 500),
        label = "constellationHeight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060709)) // Negro estelar puro
    ) {
        // =========================================================================
        // CABECERA IDÉNTICA Y HOMOGÉNEA CON HOME (Protegida contra wrapping)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo Tujelly en la izquierda
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerLogo = if (isMonochrome) {
                    R.drawable.ic_tujelly_header_mono
                } else {
                    R.drawable.ic_tujelly_header
                }
                Image(
                    painter = painterResource(id = headerLogo),
                    contentDescription = "TuJelly",
                    modifier = Modifier.height(38.dp)
                )
            }

            // Menú superior homogéneo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                // Botón Inicio
                Button(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x0AFFFFFF),
                        contentColor = Color(0xFF64748B),
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTopIcons) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Inicio",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showTopText) {
                            if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inicio",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Botón Medusa (ACTIVO)
                var isMedusaFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { /* Ya en Medusa */ },
                    modifier = Modifier.onFocusChanged { isMedusaFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x24FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    border = ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTopIcons) {
                            val medusaSymbol = if (isMonochrome) {
                                if (isMedusaFocused) R.drawable.ic_jelly_symbol_mono_dark else R.drawable.ic_jelly_symbol_mono
                            } else {
                                R.drawable.ic_jelly_symbol
                            }
                            Image(
                                painter = painterResource(id = medusaSymbol),
                                contentDescription = "Medusa",
                                modifier = Modifier.size(16.dp),
                                colorFilter = if (isMonochrome) {
                                    if (isMedusaFocused) androidx.compose.ui.graphics.ColorFilter.tint(focusContent) else androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                                } else null
                            )
                        }
                        if (showTopText) {
                            if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Medusa",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Botón Favoritos
                Button(
                    onClick = onOpenFavorites,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x0AFFFFFF),
                        contentColor = Color(0xFF64748B),
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTopIcons) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favoritos",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showTopText) {
                            if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Favoritos",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Botón Buscar
                Button(
                    onClick = onOpenSearch,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x0AFFFFFF),
                        contentColor = Color(0xFF64748B),
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTopIcons) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Buscar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showTopText) {
                            if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buscar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Botón Ajustes
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x0AFFFFFF),
                        contentColor = Color(0xFF64748B),
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTopIcons) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Ajustes",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showTopText) {
                            if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajustes",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CUERPO: CONSTELACIÓN ESTELAR + RECOMENDACIONES EN PANTALLA
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
                        .pointerInput(uiState.visibleStarIds) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val h = size.height
                                val radiusPx = 55.dp.toPx()
                                val tapped = MedusaViewModel.constellationStars.firstOrNull { star ->
                                    if (!uiState.visibleStarIds.contains(star.id)) return@firstOrNull false
                                    val sx = w * star.normX
                                    val sy = h * star.normY
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

                    // 1. Trazos celestes que enlazan dinámicamente las estrellas visibles
                    MedusaConstellationView(
                        modifier = Modifier.fillMaxSize(),
                        visibleStarIds = uiState.visibleStarIds,
                        selectedStarIds = uiState.selectedStarIds,
                        accentColor = medusaColor
                    )

                    // 2. Estrellas visibles (surgen dinámicamente al pulsar sobre sugerencias)
                    MedusaViewModel.constellationStars.forEach { star ->
                        val isVisible = uiState.visibleStarIds.contains(star.id)
                        val isSelected = uiState.selectedStarIds.contains(star.id)
                        val posX = w * star.normX
                        val posY = h * star.normY

                        if (isVisible) {
                            val starWidth = 150.dp
                            val starHeight = 38.dp
                            val offsetX = if (star.textOnLeft) posX - 128.dp else posX - 22.dp
                            val offsetY = posY - 19.dp

                            ConstellationAstroStar(
                                star = star,
                                isSelected = isSelected,
                                accentColor = medusaColor,
                                onClick = {
                                    Log.d("MedusaScreen", "Direct click on star: ${star.id}")
                                    viewModel.toggleStar(star.id)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = offsetX, y = offsetY)
                                    .size(width = starWidth, height = starHeight)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // SECCIÓN RECOMENDACIONES (DIRECTAMENTE VISIBLE EN LA MITAD INFERIOR)
            // =========================================================================
            if (uiState.isFormed) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    ) {
                        // Fila de estado celestial
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
                                val activeWords = MedusaViewModel.constellationStars
                                    .filter { uiState.selectedStarIds.contains(it.id) }
                                    .joinToString(" • ") { it.label }
                                Text(
                                    text = "Constelación Formada: $activeWords",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Botón sutil de reiniciar constelación
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
                                        contentDescription = "Reiniciar",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Reiniciar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
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
                                        text = "Alineando constelación con tu catálogo...",
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
                                    text = "No se encontraron títulos alineados con esta combinación en tu biblioteca.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            // Carrusel visible directamente en pantalla
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
                // Menos de 3 estrellas: indicación progresiva de cuántas faltan
                item {
                    val message = when (uiState.selectedCount) {
                        0 -> "✦ Toca una estrella para iniciar la constelación"
                        1 -> "✦ 1 estrella iluminada • Elige 2 más para revelar recomendaciones"
                        2 -> "✦ 2 estrellas iluminadas • Falta 1 para formar la medusa"
                        else -> ""
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message,
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
 * Estrella astronómica interactiva con palabra (estilo mapa estelar puro).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConstellationAstroStar(
    star: MedusaStar,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0x15FFFFFF),
            pressedContainerColor = Color(0x30FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(1.dp, Color(0x60FFFFFF)))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        modifier = modifier
            .zIndex(2f)
            .onFocusChanged { isFocused = it.isFocused }
            .pointerInput(star.id) {
                detectTapGestures {
                    Log.d("MedusaScreen", "Pointer tap gesture on star: ${star.id}")
                    onClick()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (star.textOnLeft) Arrangement.End else Arrangement.Start
        ) {
            if (star.textOnLeft) {
                Text(
                    text = star.label,
                    color = when {
                        isFocused -> Color.White
                        isSelected -> Color(0xFFF1F5F9)
                        else -> Color(0x99FFFFFF)
                    },
                    fontSize = if (isFocused) 14.sp else 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                StarPoint(isFocused = isFocused, isSelected = isSelected, accentColor = accentColor)
            } else {
                StarPoint(isFocused = isFocused, isSelected = isSelected, accentColor = accentColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = star.label,
                    color = when {
                        isFocused -> Color.White
                        isSelected -> Color(0xFFF1F5F9)
                        else -> Color(0x99FFFFFF)
                    },
                    fontSize = if (isFocused) 14.sp else 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.3.sp,
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
            // Halo celestial amplio al estar en foco con el mando
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
            // Micro-cruz estelar de destello
            drawLine(Color.White.copy(alpha = 0.85f), Offset(center.x - 6.5f, center.y), Offset(center.x + 6.5f, center.y), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(center.x, center.y - 6.5f), Offset(center.x, center.y + 6.5f), strokeWidth = 1f)
        } else if (isSelected) {
            // Estrella activa encendida en la constelación
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
            // Pequeño fulgor estelar
            drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x - 5f, center.y), Offset(center.x + 5f, center.y), strokeWidth = 0.8f)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x, center.y - 5f), Offset(center.x, center.y + 5f), strokeWidth = 0.8f)
        } else {
            // Estrella en reposo en el firmamento
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2.6f,
                center = center
            )
        }
    }
}
