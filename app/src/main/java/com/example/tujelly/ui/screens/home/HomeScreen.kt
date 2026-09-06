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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val syncProgress by viewModel.syncProgress.collectAsState()
    val localMediaCount by viewModel.localMediaCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Luxury TV Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val headerLogo = if (isMonochrome) {
                    com.example.tujelly.R.drawable.ic_tujelly_header_mono
                } else {
                    com.example.tujelly.R.drawable.ic_tujelly_header
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = headerLogo),
                    contentDescription = "TuJelly",
                    modifier = Modifier.height(42.dp)
                )

                if (syncProgress.isSyncing) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(if (isMonochrome) Color(0x22FFFFFF) else Color(0x2238BDF8), RoundedCornerShape(16.dp))
                            .border(0.75.dp, if (isMonochrome) Color(0x55FFFFFF) else Color(0x5538BDF8), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val currentNum = syncProgress.current.coerceAtLeast(localMediaCount)
                        val progressText = if (syncProgress.total > 0) {
                            val displayCur = currentNum.coerceAtMost(syncProgress.total)
                            val formattedCur = String.format("%,d", displayCur).replace(',', '.')
                            val formattedTot = String.format("%,d", syncProgress.total).replace(',', '.')
                            "$formattedCur / $formattedTot"
                        } else if (localMediaCount > 0) {
                            "${String.format("%,d", localMediaCount).replace(',', '.')} cargados..."
                        } else {
                            "Conectando..."
                        }
                        Text(
                            text = "Sincronizando: $progressText",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (syncProgress.message.isNotBlank() && (syncProgress.message.contains("401") || syncProgress.message.contains("Error") || syncProgress.message.contains("caducada", ignoreCase = true))) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x30EF4444), RoundedCornerShape(16.dp))
                            .border(0.75.dp, Color(0x70EF4444), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncProgress.message,
                            color = Color(0xFFFEE2E2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (localMediaCount > 0) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                            .border(0.75.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val formattedCount = String.format("%,d", localMediaCount).replace(',', '.')
                        Text(
                            text = "$formattedCount títulos",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Discover (active tab)
                Button(
                    onClick = { /* Already on Home */ },
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Favorites Button (inactive tab: muted slate, translucent)
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
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Search Button (inactive tab: muted slate, translucent)
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
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Settings Button (inactive tab: muted slate, translucent)
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
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

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
                                Text(text = "Configurar Servidor Jellyfin", fontWeight = FontWeight.Bold)
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
                                    Text(text = "Sincronizar", fontWeight = FontWeight.Bold)
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
                                Text(text = "Ajustes", fontWeight = FontWeight.SemiBold)
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
                        HeroBanner(
                            item = state.focusedItem,
                            onPlayClick = { item -> onPlayMedia(item.id) },
                            onDetailClick = { item -> onDetailMedia(item.id) },
                            accentColor = accentColorKey,
                            buttonStyle = buttonStyleKey
                        )
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
