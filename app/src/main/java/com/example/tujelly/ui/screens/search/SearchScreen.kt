package com.example.tujelly.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.tujelly.ui.components.MediaCard
import com.example.tujelly.ui.components.TvNavTab
import com.example.tujelly.ui.components.TvTopBar
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

    // Enfoque inicial automático en la primera tecla del teclado (A)
    val initialKeyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(180)
        try {
            initialKeyFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // El botón Atrás del mando borra letras si hay texto; si está vacío, sale de la pantalla
    BackHandler(enabled = true) {
        if (uiState.query.isNotEmpty()) {
            viewModel.onBackspace()
        } else {
            onBack()
        }
    }

    // Cursor animado parpadeante para la barra de búsqueda
    val infiniteTransition = rememberInfiniteTransition(label = "SearchCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0E))
    ) {
        // Universal Persistent TV TopBar
        TvTopBar(
            selectedTab = TvNavTab.SEARCH,
            onNavigateHome = onNavigateHome,
            onOpenMedusa = onOpenMedusa,
            onOpenFavorites = onOpenFavorites,
            onOpenSearch = { /* Ya en Buscar */ },
            onOpenSettings = onOpenSettings,
            accentColorKey = accentColorKey,
            buttonStyleKey = buttonStyleKey,
            isMonochrome = isMonochrome
        )

        // Contenedor principal de búsqueda dividida (Panel Izquierdo: Teclado | Panel Derecho: Resultados)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 6.dp)
        ) {
            // ==========================================================
            // PANEL IZQUIERDO: Display de Texto + Teclado Integrado Fijo
            // ==========================================================
            Column(
                modifier = Modifier.width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de visualización de lo que se escribe (Compacta, sin lupa para no confundir con un input)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color(0xFF131622), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (uiState.query.isNotEmpty()) focusColor.copy(alpha = 0.5f) else Color(0x18FFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "Buscar...",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = uiState.query,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Cursor parpadeante
                            Text(
                                text = "|",
                                color = (if (isMonochrome) Color.White else focusColor).copy(alpha = cursorAlpha),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Selector de Modo Predictivo sobrio y limpio
                    Button(
                        onClick = { viewModel.togglePredictive() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                        colors = ButtonDefaults.colors(
                            containerColor = if (uiState.isPredictiveActive) {
                                if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.18f)
                            } else Color(0x0AFFFFFF),
                            contentColor = if (uiState.isPredictiveActive) {
                                if (isMonochrome) Color.White else focusColor
                            } else Color(0xFF64748B),
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        border = ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    0.8.dp,
                                    if (uiState.isPredictiveActive) focusColor.copy(alpha = 0.4f) else Color(0x18FFFFFF)
                                )
                            ),
                            focusedBorder = Border(border = BorderStroke(1.5.dp, focusColor))
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (uiState.isPredictiveActive) "Auto" else "Libre",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Teclado integrado en pantalla D-pad (Posición 100% fija, sin saltos verticales)
                TvIntegratedKeyboard(
                    validNextChars = uiState.validNextChars,
                    isPredictiveActive = uiState.isPredictiveActive,
                    query = uiState.query,
                    focusColor = focusColor,
                    focusContentColor = focusContent,
                    onKeyClick = { viewModel.appendChar(it) },
                    onBackspace = { viewModel.onBackspace() },
                    onClear = { viewModel.onClear() },
                    initialFocusRequester = initialKeyFocusRequester
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // ==========================================
            // PANEL DERECHO: Resultados / Sugerencias
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (uiState.query.isBlank()) {
                    // Estado inicial limpio y minimalista (sin duplicar novedades del Home)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                (if (isMonochrome) Color.White else focusColor).copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        (if (isMonochrome) Color.White else focusColor).copy(alpha = 0.25f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = if (isMonochrome) Color.White else focusColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Búsqueda en catálogo",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Estado con consulta activa: Muestra Sugerencias superiores, contador y Cuadrícula
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Fila de sugerencias de autocompletado directa
                        if (uiState.suggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.suggestions.take(4).forEach { suggestion ->
                                    Button(
                                        onClick = { viewModel.selectSuggestion(suggestion) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                        colors = ButtonDefaults.colors(
                                            containerColor = Color(0xFF191D2C),
                                            contentColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = focusColor,
                                            focusedContentColor = focusContent
                                        ),
                                        border = ButtonDefaults.border(
                                            border = Border(border = BorderStroke(0.7.dp, Color(0x22FFFFFF))),
                                            focusedBorder = Border(border = BorderStroke(1.5.dp, focusColor))
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Cabecera de resultados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val counterText = when {
                                uiState.isLoading -> "Buscando en tu servidor Jellyfin..."
                                uiState.totalHits == 1 -> "1 título encontrado"
                                uiState.totalHits > 1 -> "${uiState.totalHits} títulos encontrados"
                                else -> "Sin coincidencias"
                            }

                            Text(
                                text = counterText,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (uiState.totalHits > 0) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (uiState.results.isEmpty() && !uiState.isLoading) {
                            // Sin resultados para el término
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No se encontraron títulos para \"${uiState.query}\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Cuadrícula TV de Resultados (Pósters interactivos)
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.results,
                                    key = { it.id }
                                ) { item ->
                                    MediaCard(
                                        item = item,
                                        onClick = { onDetailMedia(item.id) },
                                        onFocus = { viewModel.setFocusedItem(item) }
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
