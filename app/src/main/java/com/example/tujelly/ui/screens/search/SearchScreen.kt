package com.example.tujelly.ui.screens.search

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
import androidx.compose.material.icons.rounded.Search
import androidx.tv.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.ui.components.HeroBanner
import com.example.tujelly.ui.components.MediaRow
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvPill
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    onNavigateHome: () -> Unit = {},
    onOpenMedusa: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()
    val focusColor = TvAccent.getColor(accentColorKey)
    val focusContent = TvAccent.getFocusedContentColor(accentColorKey)
    val showIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0E))
    ) {
        // Universal Persistent TV TopBar
        com.example.tujelly.ui.components.TvTopBar(
            selectedTab = com.example.tujelly.ui.components.TvNavTab.SEARCH,
            onNavigateHome = onNavigateHome,
            onOpenMedusa = onOpenMedusa,
            onOpenFavorites = onOpenFavorites,
            onOpenSearch = { /* Ya en Buscar */ },
            onOpenSettings = onOpenSettings,
            accentColorKey = accentColorKey,
            buttonStyleKey = buttonStyleKey,
            isMonochrome = isMonochrome
        )

        // Search Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvPill(
                text = "BUSCADOR",
                containerColor = Color(0x18FFFFFF),
                textColor = Color(0xFFE2E8F0),
                borderColor = Color(0x22FFFFFF),
                fontSizeSp = 11,
                horizontalPadDp = 10.dp,
                verticalPadDp = 6.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = {
                    Text("Buscar película o serie...", color = Color(0xFF64748B), fontSize = 15.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1B1E2E),
                    unfocusedContainerColor = Color(0xFF141624),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0x44FFFFFF),
                    cursorColor = Color(0xFF38BDF8)
                )
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Results Counter
            if (uiState.statusMessage != null || uiState.totalHits > 0) {
                item {
                    Box(
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp)
                    ) {
                        val msg = uiState.statusMessage ?: "Se encontraron ${uiState.totalHits} títulos en tu servidor Jellyfin"
                        Text(
                            text = msg,
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Hero Banner for focused item
            if (uiState.focusedItem != null) {
                item {
                    HeroBanner(
                        item = uiState.focusedItem,
                        onPlayClick = { item -> onPlayMedia(item.id) },
                        onDetailClick = { item -> onDetailMedia(item.id) },
                        accentColor = accentColorKey,
                        buttonStyle = buttonStyleKey
                    )
                }
            }

            // Media Results Rows
            items(uiState.sections) { section ->
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
