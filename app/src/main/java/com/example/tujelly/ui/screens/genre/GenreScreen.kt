package com.example.tujelly.ui.screens.genre

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.ui.components.MediaRow
import com.example.tujelly.ui.theme.TvAccent

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GenreScreen(
    genreName: String,
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    onNavigateHome: () -> Unit = onBack,
    onOpenMedusa: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: GenreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()

    LaunchedEffect(genreName) {
        viewModel.loadGenreFeed(genreName)
    }

    when (val state = uiState) {
        is GenreUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0F)),
                contentAlignment = Alignment.Center
            ) {
                com.example.tujelly.ui.components.JellyLoadingIndicator(
                    size = 72.dp,
                    message = "Cargando catálogo de $genreName...",
                    isMonochrome = isMonochrome
                )
            }
        }

        is GenreUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        is GenreUiState.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF111224), Color(0xFF07070B))
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Universal Persistent TV TopBar
                    com.example.tujelly.ui.components.TvTopBar(
                        selectedTab = com.example.tujelly.ui.components.TvNavTab.NONE,
                        onNavigateHome = onNavigateHome,
                        onOpenMedusa = onOpenMedusa,
                        onOpenFavorites = onOpenFavorites,
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                        accentColorKey = accentColorKey,
                        buttonStyleKey = buttonStyleKey,
                        isMonochrome = isMonochrome,
                        titleBadge = {
                            val badgeBg = if (isMonochrome) Color(0x22FFFFFF) else TvAccent.getColor(accentColorKey).copy(alpha = 0.18f)
                            val badgeBorder = if (isMonochrome) Color(0x33FFFFFF) else TvAccent.getColor(accentColorKey).copy(alpha = 0.55f)
                            val badgeText = if (isMonochrome) Color.White else TvAccent.getColor(accentColorKey)
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(16.dp))
                                    .border(0.75.dp, badgeBorder, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "GÉNERO • ${state.genreName.uppercase()}",
                                    color = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    )

                    // Filtro de formato centrado, fino y elegante
                    com.example.tujelly.ui.components.FormatFilterBar(
                        selected = state.format,
                        onSelect = { viewModel.setFormat(it) },
                        accentColorKey = accentColorKey,
                        buttonStyleKey = buttonStyleKey,
                        isMonochrome = isMonochrome
                    )

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Secciones de Género
                        items(state.sections) { section ->
                            MediaRow(
                                section = section,
                                onItemClick = { item -> onDetailMedia(item.id) },
                                onItemFocus = { item -> viewModel.setFocusedItem(item) }
                            )
                        }

                        if (state.sections.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp, vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay títulos para este filtro en ${state.genreName}.",
                                        color = Color(0x99FFFFFF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
