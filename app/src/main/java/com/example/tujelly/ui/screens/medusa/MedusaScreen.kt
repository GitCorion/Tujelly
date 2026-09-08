package com.example.tujelly.ui.screens.medusa

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.components.TvNavTab
import com.example.tujelly.ui.components.TvTopBar

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

    val context = androidx.compose.ui.platform.LocalContext.current
    val userPrefsRepo = remember { com.example.tujelly.data.local.UserPreferencesRepository(context) }
    val userPrefs by userPrefsRepo.userPreferencesFlow.collectAsState(initial = null)
    val particleScale = remember(userPrefs) { userPrefs?.getEffectiveParticleScale(context) ?: 1.0f }

    val topBarMedusaFocusRequester = remember { FocusRequester() }
    val canvasFocusRequester = remember { FocusRequester() }
    val moviesDrawerFocusRequester = remember { FocusRequester() }

    // Al entrar a la pantalla, mantener enfocado el botón "Medusa" en el menú superior
    LaunchedEffect(Unit) {
        topBarMedusaFocusRequester.requestFocus()
    }

    // Manejo de la tecla ATRÁS (Back) compatible con mandos de 5 botones
    BackHandler {
        if (uiState.showMoviesOverlay) {
            viewModel.toggleMoviesOverlay()
            canvasFocusRequester.requestFocus()
        } else if (!viewModel.onBackPress()) {
            onNavigateHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B))
    ) {
        // =========================================================================
        // 1. CABECERA UNIVERSAL DE NAVEGACIÓN (IDÉNTICA A HOME)
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
            onDownFromMedusa = {
                canvasFocusRequester.requestFocus()
            }
        )

        // =========================================================================
        // 2. SUB-CABECERA COMPACTA: RESUMEN DE LA CONSTELACIÓN DE SENSACIONES (BREADCRUMB)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF05070B))
                .padding(horizontal = 48.dp, vertical = 6.dp)
        ) {
            Text(
                text = "NEBULOSA DE SENSACIONES",
                color = if (isMonochrome) Color(0x99FFFFFF) else Color(0x8000E5FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.activeChain.isEmpty()) {
                    Text(
                        text = "Navega con la cruceta (D-Pad) y pulsa [OK] para conectar sensaciones",
                        color = Color(0x99FFFFFF),
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                } else {
                    // Fila con scroll horizontal para soportar 4, 5 o más sensaciones sin desbordar ni recortar
                    val breadcrumbScrollState = rememberScrollState()
                    LaunchedEffect(uiState.activeChain.size) {
                        breadcrumbScrollState.animateScrollTo(breadcrumbScrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(breadcrumbScrollState),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.activeChain.forEachIndexed { index, node ->
                            if (index > 0) {
                                Text(
                                    text = "➔",
                                    color = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMonochrome) Color(0xF01E293B) else Color(0xF01A0E2E))
                                    .border(
                                        0.8.dp,
                                        if (isMonochrome) Color.White else Color(0xFFC084FC),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. ${node.label}",
                                    color = if (isMonochrome) Color.White else Color(0xFFF3E8FF),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }

                        // Píldora interactiva del nodo Portal al final de la cadena
                        if (uiState.matchingMovies.isNotEmpty()) {
                            Text(
                                text = "➔",
                                color = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val portalInteractionSource = remember { MutableInteractionSource() }
                            val isPortalPillFocused by portalInteractionSource.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isPortalPillFocused) {
                                            if (isMonochrome) Color.White else Color(0xFF00E5FF)
                                        } else {
                                            if (isMonochrome) Color(0x35FFFFFF) else Color(0x3500E5FF)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isPortalPillFocused) Color.White else (if (isMonochrome) Color.White else Color(0xFF00E5FF)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable(
                                        interactionSource = portalInteractionSource,
                                        indication = null
                                    ) { viewModel.toggleMoviesOverlay() }
                                    .focusable(interactionSource = portalInteractionSource)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                                        ) {
                                            viewModel.toggleMoviesOverlay()
                                            true
                                        } else false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✦ VER ${uiState.matchingMovies.size} PELÍCULAS",
                                    color = if (isPortalPillFocused) Color(0xFF05070B) else (if (isMonochrome) Color.White else Color(0xFF00E5FF)),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }

                if (uiState.activeChain.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.onBackPress() },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x18FFFFFF),
                                contentColor = Color(0xFFCBD5E1),
                                focusedContainerColor = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                                focusedContentColor = Color(0xFF05070B)
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(16.dp)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = "Deshacer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.resetConstellation() },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x18FFFFFF),
                                contentColor = Color(0xFFCBD5E1),
                                focusedContainerColor = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                                focusedContentColor = Color(0xFF05070B)
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(16.dp))
                        ) {
                            Text(text = "Reiniciar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // =========================================================================
            // 3. LIENZO ESPACIAL DE LA CONSTELACIÓN A PANTALLA COMPLETA
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ConstellationCanvas(
                    nodes = uiState.nodes,
                    filaments = uiState.filaments,
                    focusedNodeId = uiState.focusedNodeId,
                    activeChain = uiState.activeChain,
                    targetCameraX = uiState.targetCameraX,
                    targetCameraY = uiState.targetCameraY,
                    targetZoom = uiState.targetZoom,
                    onNavigateDirection = { direction ->
                        viewModel.onNavigate(direction)
                    },
                    onSelectFocused = {
                        viewModel.toggleConnectFocused()
                    },
                    onPlayPressed = {
                        if (uiState.matchingMovies.isNotEmpty()) {
                            viewModel.toggleMoviesOverlay()
                        }
                    },
                    onNodeClicked = { nodeId ->
                        viewModel.onNodeClicked(nodeId)
                    },
                    onZoomOut = {
                        if (!viewModel.onBackPress()) {
                            onNavigateHome()
                        }
                    },
                    onRequestFocusBottom = null,
                    onRequestFocusTop = {
                        topBarMedusaFocusRequester.requestFocus()
                    },
                    compatibleNodeIds = uiState.compatibleNodeIds,
                    focusRequester = canvasFocusRequester,
                    isMonochrome = isMonochrome,
                    particleScale = particleScale
                )

                // Barra inferior de atajos contextual según el nodo enfocado
                val isFocusedOnPortal = uiState.focusedNode?.isPortal == true || uiState.focusedNodeId == PORTAL_NODE_ID
                val isFocusedAlreadyConnected = uiState.activeChain.any { it.id == uiState.focusedNodeId }
                val isFocusedCompatible = uiState.compatibleNodeIds == null ||
                        uiState.focusedNodeId == null ||
                        isFocusedAlreadyConnected ||
                        isFocusedOnPortal ||
                        (uiState.compatibleNodeIds?.contains(uiState.focusedNodeId) == true)

                val hudGuide = when {
                    isFocusedOnPortal -> {
                        "[OK] Abrir películas (${uiState.matchingMovies.size})  ·  [D-PAD] Moverse por la constelación  ·  [BACK] Deshacer"
                    }
                    isFocusedAlreadyConnected -> {
                        "[OK] Desconectar etiqueta  ·  [D-PAD] Explorar constelación  ·  [BACK] Deshacer"
                    }
                    uiState.activeChain.isNotEmpty() -> {
                        "[OK] Conectar a la constelación  ·  [D-PAD] Explorar constelación  ·  [BACK] Deshacer"
                    }
                    else -> {
                        "[OK] Conectar sensación  ·  [D-PAD] Explorar constelación  ·  [BACK] Salir"
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD05070B), Color(0xF505070B))
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hudGuide,
                        color = when {
                            isFocusedOnPortal -> {
                                if (isMonochrome) Color.White else Color(0xFF00E5FF)
                            }
                            else -> Color(0x9085A5C5)
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.6.sp
                    )

                    if (uiState.activeChain.isNotEmpty() && uiState.matchingMovies.isNotEmpty()) {
                        val bottomPillInteractionSource = remember { MutableInteractionSource() }
                        val isBottomPillFocused by bottomPillInteractionSource.collectIsFocusedAsState()

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isBottomPillFocused) {
                                        if (isMonochrome) Color.White else Color(0xFF00E5FF)
                                    } else {
                                        if (isMonochrome) Color(0x3520242D) else Color(0x3500E5FF)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isBottomPillFocused) Color.White else (if (isMonochrome) Color(0x60FFFFFF) else Color(0x8000E5FF)),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable(
                                    interactionSource = bottomPillInteractionSource,
                                    indication = null
                                ) {
                                    viewModel.toggleMoviesOverlay()
                                }
                                .focusable(interactionSource = bottomPillInteractionSource)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                                    ) {
                                        viewModel.toggleMoviesOverlay()
                                        true
                                    } else false
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mini abanico de carátulas reales de las películas descubiertas
                            val previewPosters = uiState.matchingMovies.take(3)
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .width((18 + (previewPosters.size - 1) * 12).dp)
                            ) {
                                previewPosters.forEachIndexed { idx, media ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (idx * 12).dp)
                                            .size(width = 18.dp, height = 26.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .border(
                                                0.5.dp,
                                                if (isBottomPillFocused) Color.Black else Color(0x60FFFFFF),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .background(Color(0xFF1A2230))
                                    ) {
                                        if (media.posterUrl != null) {
                                            AsyncImage(
                                                model = media.posterUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "✦ Ver ${uiState.matchingMovies.size} películas  ➔",
                                color = if (isBottomPillFocused) Color(0xFF05070B) else (if (isMonochrome) Color.White else Color(0xFF00E5FF)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                // =========================================================================
                // 4. VISOR DE PELÍCULAS DESCUBIERTAS (SE ABRE CON [OK] EN EL NODO PORTAL)
                // =========================================================================
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showMoviesOverlay,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MovieDiscoveryDrawer(
                        chain = uiState.activeChain,
                        movies = uiState.matchingMovies,
                        onClose = {
                            viewModel.toggleMoviesOverlay()
                            canvasFocusRequester.requestFocus()
                        },
                        onSelectMovie = { movie ->
                            viewModel.selectMovie(movie)
                        },
                        onDetailMovie = { movie ->
                            onDetailMedia(movie.id)
                        },
                        focusRequester = moviesDrawerFocusRequester,
                        isMonochrome = isMonochrome
                    )
                }
            }
        }
    }
}

/**
 * Drawer inferior deslizante que muestra las películas exactas que satisfacen
 * el árbol completo de sensaciones activas en la constelación.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieDiscoveryDrawer(
    chain: List<SpatialNebulaNode>,
    movies: List<MediaItem>,
    onClose: () -> Unit,
    onSelectMovie: (MediaItem) -> Unit,
    onDetailMovie: (MediaItem) -> Unit,
    focusRequester: FocusRequester,
    isMonochrome: Boolean = false
) {
    val firstCardFocusRequester = remember { FocusRequester() }
    var canClickCard by remember { mutableStateOf(false) }

    LaunchedEffect(movies) {
        canClickCard = false
        if (movies.isNotEmpty()) {
            delay(350)
            canClickCard = true
            firstCardFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(375.dp)
            .background(
                Brush.verticalGradient(
                    colors = if (isMonochrome) {
                        listOf(Color(0xF012141A), Color(0xFC0A0B0E))
                    } else {
                        listOf(Color(0xF0080E1A), Color(0xFC05070B))
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isMonochrome) {
                        listOf(Color(0x77FFFFFF), Color(0x15FFFFFF))
                    } else {
                        listOf(Color(0x7700E5FF), Color(0x1500E5FF))
                    }
                ),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .padding(horizontal = 48.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Título de la constelación resultante
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✦ OBRAS MAESTRAS QUE RESONAN CON TU CONSTELACIÓN (${movies.size})",
                        color = if (isMonochrome) Color.White else Color(0xFFC084FC),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                    val chainTitle = if (chain.isNotEmpty()) {
                        chain.joinToString(" ➔ ") { it.label }
                    } else "Selección General"
                    Text(
                        text = chainTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "[ARRIBA / BACK] Volver a la constelación",
                    color = Color(0x8085A5C5),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (movies.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No se encontraron títulos exactos en tu biblioteca para esta combinación tan específica",
                        color = Color(0x99FFFFFF),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionUp -> {
                                        onClose()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    contentPadding = PaddingValues(end = 32.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(movies, key = { _, it -> it.id }) { index, movie ->
                        MediaCard(
                            item = movie,
                            onClick = {
                                if (canClickCard) {
                                    onDetailMovie(movie)
                                }
                            },
                            onFocus = {
                                onSelectMovie(movie)
                            },
                            modifier = if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}
