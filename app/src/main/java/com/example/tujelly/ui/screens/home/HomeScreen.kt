package com.example.tujelly.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.ui.components.BrandTileRow
import com.example.tujelly.ui.components.GenreRow
import com.example.tujelly.ui.components.HeroBanner
import com.example.tujelly.ui.components.MediaRow
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenBrand: (String) -> Unit = {},
    onOpenGenre: (String) -> Unit = {},
    onOpenMedusa: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()
    val selectedPlatforms by viewModel.selectedPlatforms.collectAsState()
    val showTopIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showTopText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY

    val focusColor = TvAccent.getColor(accentColorKey)
    val focusContent = TvAccent.getFocusedContentColor(accentColorKey)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val localMediaCount by viewModel.localMediaCount.collectAsState()
    val focusedBackdrop = (uiState as? HomeUiState.Success)?.focusedItem?.backdropUrl

    Box(modifier = Modifier.fillMaxSize()) {
        // Global ambient backdrop crossfade
        Crossfade(
            targetState = focusedBackdrop,
            animationSpec = tween(durationMillis = 400),
            label = "homeGlobalBackdrop"
        ) { backdropUrl ->
            if (!backdropUrl.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.32f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xD007070B),
                                        Color(0xE007070B),
                                        Color(0xFF07070B)
                                    )
                                )
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
        // Universal Persistent TV TopBar
        com.example.tujelly.ui.components.TvTopBar(
            selectedTab = com.example.tujelly.ui.components.TvNavTab.HOME,
            onNavigateHome = { /* Ya en Inicio */ },
            onOpenMedusa = onOpenMedusa,
            onOpenFavorites = onOpenFavorites,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            accentColorKey = accentColorKey,
            buttonStyleKey = buttonStyleKey,
            isMonochrome = isMonochrome,
            syncProgress = syncProgress,
            localMediaCount = localMediaCount
        )


        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.tujelly.ui.components.JellyLoadingIndicator(
                        size = 84.dp,
                        message = "Cargando descubridor de plataformas...",
                        isMonochrome = isMonochrome
                    )
                }
            }

            is HomeUiState.EmptySettings -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bienvenido a Tujelly",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configura tu servidor Jellyfin para disfrutar de tus películas, series y recomendaciones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Configurar Servidor Jellyfin", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            is HomeUiState.EmptyLibrary -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Servidor conectado",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron películas ni series en tu servidor Jellyfin.\nComprueba que tu usuario tenga acceso a las bibliotecas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Button(
                                onClick = { viewModel.loadHomeFeed() },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Sincronizar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }

                            Button(
                                onClick = onOpenSettings,
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0x18FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Text(text = "Ajustes", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadHomeFeed() },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text(text = "Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        @OptIn(ExperimentalFoundationApi::class)
                        val heroBringIntoViewResponder = remember(listState) {
                            object : BringIntoViewResponder {
                                override fun calculateRectForParent(localRect: Rect): Rect {
                                    // Retornar Rect.Zero indica a la LazyColumn que el banner ya está 100% visible
                                    // y evita que haga scroll hacia arriba cortando la cabecera al hacer foco en sus botones.
                                    return Rect.Zero
                                }

                                override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                                    listState.scrollToItem(0, 0)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewResponder(heroBringIntoViewResponder)
                                .onFocusChanged { focusState ->
                                    if (focusState.hasFocus) {
                                        coroutineScope.launch {
                                            listState.scrollToItem(0, 0)
                                        }
                                    }
                                }
                        ) {
                            HeroBanner(
                                item = state.focusedItem,
                                onPlayClick = { item -> onPlayMedia(item.id) },
                                onDetailClick = { item -> onDetailMedia(item.id) },
                                accentColor = accentColorKey,
                                buttonStyle = buttonStyleKey
                            )
                        }
                    }

                    item {
                        if (state.genres.isNotEmpty()) {
                            GenreRow(
                                genres = state.genres,
                                onSelectGenre = { genreName ->
                                    onOpenGenre(genreName)
                                }
                            )
                        }
                    }

                    item {
                        BrandTileRow(
                            onSelectBrand = { brandId ->
                                onOpenBrand(brandId)
                            },
                            visiblePlatforms = selectedPlatforms.takeIf { it.isNotEmpty() }
                        )
                    }

                    items(state.sections) { section ->
                        MediaRow(
                            section = section,
                            onItemClick = { item -> onDetailMedia(item.id) },
                            onItemFocus = { item -> viewModel.setFocusedItem(item) }
                        )
                    }
                }
            }
        }
    }
}
}


