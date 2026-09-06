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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.tv.material3.Icon
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.ui.components.HeroBanner
import com.example.tujelly.ui.components.MediaRow

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
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current

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
                Text(
                    text = "Cargando catálogo de $genreName...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Universal Persistent TV TopBar
                    item {
                        com.example.tujelly.ui.components.TvTopBar(
                            selectedTab = com.example.tujelly.ui.components.TvNavTab.NONE,
                            onNavigateHome = onNavigateHome,
                            onOpenMedusa = onOpenMedusa,
                            onOpenFavorites = onOpenFavorites,
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            accentColorKey = com.example.tujelly.data.local.ACCENT_CYAN,
                            buttonStyleKey = com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY,
                            isMonochrome = isMonochrome,
                            titleBadge = {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                        .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "GÉNERO • ${state.genreName.uppercase()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        )
                    }

                    // Hero Banner for focused item
                    item {
                        HeroBanner(
                            item = state.focusedItem,
                            onPlayClick = { item -> onPlayMedia(item.id) },
                            onDetailClick = { item -> onDetailMedia(item.id) }
                        )
                    }

                    // Genre Rows
                    items(state.sections) { section ->
                        MediaRow(
                            section = section,
                            onItemClick = { item -> onDetailMedia(item.id) },
                            onItemFocus = { item -> viewModel.setFocusedItem(item) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
