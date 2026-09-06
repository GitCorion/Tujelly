package com.example.tujelly.ui.screens.brand

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
fun BrandScreen(
    brandId: String,
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit,
    onDetailMedia: (String) -> Unit,
    viewModel: BrandViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val buttonStyleKey by viewModel.buttonStyle.collectAsState()
    val focusColor = com.example.tujelly.ui.theme.TvAccent.getColor(accentColorKey)
    val focusContent = com.example.tujelly.ui.theme.TvAccent.getFocusedContentColor(accentColorKey)

    LaunchedEffect(brandId) {
        viewModel.loadBrandFeed(brandId)
    }

    when (val state = uiState) {
        is BrandUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0F)),
                contentAlignment = Alignment.Center
            ) {
                com.example.tujelly.ui.components.JellyLoadingIndicator(
                    size = 72.dp,
                    message = "Cargando catálogo de la plataforma...",
                    isMonochrome = isMonochrome
                )
            }
        }

        is BrandUiState.Error -> {
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF1E2034),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Text("Volver", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        is BrandUiState.Success -> {
            val brand = state.brand
            val gradientColors = if (isMonochrome) {
                listOf(Color(0xFF141622), Color(0xFF07070B))
            } else {
                listOf(brand.gradientStart, Color(0xFF07070B))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = gradientColors))
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF131422))
                                        .border(1.dp, Color(0xFF222438), RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val iconRes = if (isMonochrome) brand.iconMonoRes else brand.iconRes
                                    Image(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = brand.name
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = brand.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Catálogo oficial disponible en tu servidor Jellyfin",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF1E2034),
                                    contentColor = Color.White,
                                    focusedContainerColor = focusColor,
                                    focusedContentColor = focusContent
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inicio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Hero Banner for focused item
                    item {
                        HeroBanner(
                            item = state.focusedItem,
                            onPlayClick = { item -> onPlayMedia(item.id) },
                            onDetailClick = { item -> onDetailMedia(item.id) },
                            accentColor = accentColorKey,
                            buttonStyle = buttonStyleKey
                        )
                    }

                    // Platform Rows
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
