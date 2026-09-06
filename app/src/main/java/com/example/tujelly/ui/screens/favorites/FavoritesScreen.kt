package com.example.tujelly.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.ui.components.HeroBanner
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    onNavigateHome: () -> Unit = onBack,
    onOpenMedusa: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: FavoritesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current
    val focusColor = TvAccent.getColor(accentColorKey)
    val focusContent = TvAccent.getFocusedContentColor(accentColorKey)
    val showIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY

    var focusedItem by remember { mutableStateOf<MediaItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0E))
    ) {
        when (val state = uiState) {
            is FavoritesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sincronizando favoritos con Jellyfin...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            is FavoritesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadFavorites() },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text("Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is FavoritesUiState.Success -> {
                val currentHero = focusedItem ?: state.filteredItems.firstOrNull()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Universal Persistent TV TopBar
                    com.example.tujelly.ui.components.TvTopBar(
                        selectedTab = com.example.tujelly.ui.components.TvNavTab.FAVORITES,
                        onNavigateHome = onNavigateHome,
                        onOpenMedusa = onOpenMedusa,
                        onOpenFavorites = onOpenFavorites,
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                        accentColorKey = accentColorKey,
                        buttonStyleKey = buttonStyleKey,
                        isMonochrome = isMonochrome
                    )

                    // Sub-Header: Title & Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = if (isMonochrome) Color.White else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mis Favoritos",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            TvPill(
                                text = "${state.allItems.size} ITEMS",
                                containerColor = Color(0x18FFFFFF),
                                textColor = Color(0xFFCBD5E1),
                                borderColor = Color(0x22FFFFFF)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Filter chips
                            FavoriteFilter.entries.forEach { filter ->
                                val isSelected = state.activeFilter == filter
                                Button(
                                    onClick = { viewModel.setFilter(filter) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (isSelected) Color(0x33FFFFFF) else Color(0x0AFFFFFF),
                                        contentColor = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        focusedContainerColor = focusColor,
                                        focusedContentColor = focusContent
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp))
                                ) {
                                    val label = when (filter) {
                                        FavoriteFilter.ALL -> "Todos"
                                        FavoriteFilter.MOVIES -> "Películas"
                                        FavoriteFilter.SERIES -> "Series"
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = { viewModel.loadFavorites() },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0x14FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = focusColor,
                                    focusedContentColor = focusContent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Sincronizar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (state.filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (state.allItems.isEmpty()) {
                                            "No tienes elementos en favoritos"
                                        } else {
                                            "No hay elementos con este filtro"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (state.allItems.isEmpty()) {
                                            "Pulsa el botón de favorito en cualquier película o serie para sincronizarla automáticamente con Jellyfin."
                                        } else {
                                            "Prueba seleccionando otro filtro de la barra superior."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Hero Banner with preview
                        if (currentHero != null) {
                            item {
                                HeroBanner(
                                    item = currentHero,
                                    onPlayClick = { item -> onPlayMedia(item.id) },
                                    onDetailClick = { item -> onDetailMedia(item.id) },
                                    accentColor = accentColorKey,
                                    buttonStyle = buttonStyleKey
                                )
                            }
                        }

                        // Grid of favorites
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = "Colección de Favoritos (${state.filteredItems.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(150.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(480.dp),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    items(state.filteredItems, key = { it.id }) { item ->
                                        MediaCard(
                                            item = item,
                                            onClick = { onDetailMedia(item.id) },
                                            onFocus = { focusedItem = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
