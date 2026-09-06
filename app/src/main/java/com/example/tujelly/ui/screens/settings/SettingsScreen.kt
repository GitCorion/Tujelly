package com.example.tujelly.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.data.local.ACCENT_AMBER
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.ACCENT_WHITE
import com.example.tujelly.data.local.INDICATOR_THEME_COLOR
import com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME
import com.example.tujelly.data.local.PLATFORM_LOGO_COLOR
import com.example.tujelly.data.local.APP_THEME_ORIGINAL
import com.example.tujelly.data.local.APP_THEME_MONOCHROME
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_AND_TEXT
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.ui.theme.TvPill
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LuxurySettingTile(
    title: String,
    value: String,
    placeholder: String = "No configurado",
    description: String? = null,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x10FFFFFF),
            focusedContainerColor = Color(0x28FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(0.75.dp, Color(0x18FFFFFF))),
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF1F5F9),
                        fontWeight = FontWeight.Bold
                    )
                    if (!badge.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x18FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) Color(0xFF64748B) else Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarCategoryButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x35FFFFFF) else Color(0x10FFFFFF),
            focusedContainerColor = Color(0x28FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF))),
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isSelected) Color(0xFFE2E8F0) else Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate back after successful Jellyfin connection
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            delay(1500)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // =========================================================================
            // SIDEBAR NAVEGACIÓN IZQUIERDA (250dp)
            // =========================================================================
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0F101B), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1C1E30), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                // Branding Header
                val sidebarLogo = if (uiState.isMonochrome) {
                    com.example.tujelly.R.drawable.ic_tujelly_header_mono
                } else {
                    com.example.tujelly.R.drawable.ic_tujelly_header
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = sidebarLogo),
                    contentDescription = "TuJelly",
                    modifier = Modifier.height(34.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ajustes de Sistema",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SidebarCategoryButton(
                    title = "SERVIDOR JELLYFIN",
                    subtitle = if (uiState.isJellyfinConnected) "Conectado" else "Sin conectar",
                    isSelected = uiState.activeCategory == 0,
                    onClick = { viewModel.selectCategory(0) }
                )

                SidebarCategoryButton(
                    title = "TRAKT.TV",
                    subtitle = if (uiState.isTraktConnected) "Vinculado" else "No vinculado",
                    isSelected = uiState.activeCategory == 1,
                    onClick = { viewModel.selectCategory(1) }
                )

                SidebarCategoryButton(
                    title = "REGIÓN & STREAMING",
                    subtitle = "País: ${uiState.watchRegion}",
                    isSelected = uiState.activeCategory == 2,
                    onClick = { viewModel.selectCategory(2) }
                )

                SidebarCategoryButton(
                    title = "BOTONES & ESTILO",
                    subtitle = when (uiState.buttonStyle) {
                        BUTTON_STYLE_ICONS_ONLY -> "Solo Iconos"
                        BUTTON_STYLE_TEXT_ONLY -> "Solo Letras"
                        else -> "Iconos y Letras"
                    },
                    isSelected = uiState.activeCategory == 3,
                    onClick = { viewModel.selectCategory(3) }
                )

                SidebarCategoryButton(
                    title = "ACTUALIZACIONES",
                    subtitle = if (uiState.updateInfo?.hasUpdate == true) "¡Actualización disponible!" else "v1.0 (Al día)",
                    isSelected = uiState.activeCategory == 4,
                    onClick = { viewModel.selectCategory(4) }
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x14FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver al Inicio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // =========================================================================
            // PANEL DERECHO DE CONTENIDO
            // =========================================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Header del Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categoryTitle = when (uiState.activeCategory) {
                        0 -> "Servidor Jellyfin"
                        1 -> "Integración con Trakt.tv"
                        2 -> "Región de Catálogo"
                        3 -> "Estilo de Botones e Interfaz"
                        else -> "Actualizaciones de Tujelly (GitHub)"
                    }
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Jellyfin Status Pill
                        val jfLabel = when {
                            uiState.username.isNotBlank() -> "Jellyfin: ${uiState.username}"
                            uiState.isJellyfinConnected -> "Jellyfin Conectado"
                            else -> "Jellyfin Pendiente"
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (uiState.isJellyfinConnected) Color(0xFF14291D) else Color(0xFF2D2415),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isJellyfinConnected) Color(0xFF34D399) else Color(0xFFF59E0B),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = jfLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Trakt Status Pill
                        val traktLabel = when {
                            uiState.traktUsername != null -> "Trakt: @${uiState.traktUsername}"
                            uiState.isTraktConnected -> "Trakt Vinculado"
                            else -> "Trakt Desconectado"
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (uiState.isTraktConnected) Color(0xFF2C1618) else Color(0xFF1B1C28),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isTraktConnected) Color(0xFFF87171) else Color(0xFF64748B),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = traktLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (uiState.activeCategory) {
                        0 -> {
                            // -----------------------------------------------------------------
                            // CATEGORÍA 1: SERVIDOR JELLYFIN
                            // -----------------------------------------------------------------
                            LuxurySettingTile(
                                title = "URL del Servidor Jellyfin",
                                value = uiState.serverUrl,
                                placeholder = "Introduce la URL de tu servidor",
                                description = "Dirección HTTP o HTTPS de tu servidor de Jellyfin",
                                onClick = { viewModel.openEditDialog(SettingField.SERVER_URL) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.testServerConnection() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF1E2034),
                                        contentColor = Color.White,
                                        focusedContainerColor = Color(0xFF4F46E5),
                                        focusedContentColor = Color.White
                                    )
                                ) {
                                    Text("Probar Conexión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = { viewModel.startJellyfinQuickConnect() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF312E81),
                                        contentColor = Color(0xFFE0E7FF),
                                        focusedContainerColor = Color(0xFF6366F1),
                                        focusedContentColor = Color.White
                                    ),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("Generar QR Quick Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Quick Connect QR Box
                            if (uiState.jellyfinQrUrl != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF101222), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF34D399), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = uiState.jellyfinQrUrl,
                                            contentDescription = "QR Jellyfin",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Column {
                                        Text(text = "Código Quick Connect:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = uiState.jellyfinQuickConnectCode ?: "...",
                                            color = Color(0xFF34D399),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Escanea el código con tu móvil o entra en tu panel Web de Jellyfin para autorizar la TV.",
                                            color = Color(0xFFCBD5E1),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Inicio de Sesión Manual",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LuxurySettingTile(
                                    title = "Usuario Jellyfin",
                                    value = uiState.username,
                                    placeholder = "Nombre de usuario",
                                    onClick = { viewModel.openEditDialog(SettingField.USERNAME) },
                                    modifier = Modifier.weight(1f)
                                )
                                LuxurySettingTile(
                                    title = "Contraseña",
                                    value = if (uiState.password.isNotBlank()) "••••••••" else if (uiState.isJellyfinConnected) "Sesión Activa" else "",
                                    placeholder = "Sin contraseña",
                                    onClick = { viewModel.openEditDialog(SettingField.PASSWORD) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.saveAndConnect() },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color(0xFF6366F1),
                                    focusedContentColor = Color.White
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Guardar y Sincronizar Catálogo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        1 -> {
                            // -----------------------------------------------------------------
                            // CATEGORÍA 2: TRAKT.TV
                            // -----------------------------------------------------------------
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.startTraktDeviceFlow() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF991B1B),
                                        contentColor = Color.White,
                                        focusedContainerColor = Color(0xFFDC2626),
                                        focusedContentColor = Color.White
                                    ),
                                    enabled = !uiState.isWaitingTraktAuth
                                ) {
                                    Text(
                                        text = if (uiState.isWaitingTraktAuth) "Generando Código..." else "Vincular por QR (Device Code)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (uiState.traktToken.isNotBlank()) {
                                    Button(
                                        onClick = { viewModel.validateTraktToken() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = Color(0xFF1E2034),
                                            contentColor = Color.White,
                                            focusedContainerColor = Color(0xFF4F46E5),
                                            focusedContentColor = Color.White
                                        )
                                    ) {
                                        Text("Verificar Conexión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (uiState.isTraktConnected) {
                                    Button(
                                        onClick = { viewModel.disconnectTrakt() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = Color(0xFF371215),
                                            contentColor = Color(0xFFF87171),
                                            focusedContainerColor = Color(0xFF991B1B),
                                            focusedContentColor = Color.White
                                        )
                                    ) {
                                        Text("Desvincular Trakt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // QR Device Code Box
                            if (uiState.traktQrUrl != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF120C10), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFF87171), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = uiState.traktQrUrl,
                                            contentDescription = "QR Trakt",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column {
                                        Text(
                                            text = "PASO 1: Abre en tu móvil o navegador:",
                                            color = Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "trakt.tv/activate",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "PASO 2: Escribe este código:",
                                            color = Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = uiState.traktUserCode ?: "...",
                                            color = Color(0xFFF59E0B),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 2.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Esperando confirmación en Trakt desde tu dispositivo...",
                                            color = Color(0xFF34D399),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Ajustes Avanzados Trakt",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LuxurySettingTile(
                                title = "Trakt Client ID",
                                value = uiState.traktClientId,
                                description = "ID de la aplicación Trakt.tv utilizada por Tujelly",
                                onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_ID) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            LuxurySettingTile(
                                title = "Trakt Client Secret",
                                value = if (uiState.traktClientSecret.isNotBlank()) "••••••••" else "",
                                description = "Clave secreta opcional para aplicaciones Trakt personalizadas",
                                onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_SECRET) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            LuxurySettingTile(
                                title = "Trakt Access Token Directo",
                                value = if (uiState.traktToken.isNotBlank()) "Token activo (••••${uiState.traktToken.takeLast(4)})" else "",
                                placeholder = "Pega un token personal directamente",
                                description = "Token OAuth personal para omitir el flujo de vinculación QR",
                                onClick = { viewModel.openEditDialog(SettingField.TRAKT_MANUAL_TOKEN) }
                            )
                        }

                        2 -> {
                            // -----------------------------------------------------------------
                            // CATEGORÍA 3: REGIÓN & STREAMING
                            // -----------------------------------------------------------------
                            LuxurySettingTile(
                                title = "Región de Catálogo (País)",
                                value = uiState.watchRegion,
                                description = "Código de país de 2 letras (ES para España, MX, US, AR, CO, etc.)",
                                onClick = { viewModel.openEditDialog(SettingField.WATCH_REGION) }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131422), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF222438), RoundedCornerShape(10.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Plataformas de Streaming",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Marca las plataformas cuyas tendencias quieres cruzar con tu biblioteca Jellyfin.",
                                                color = Color(0xFF94A3B8),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    com.example.tujelly.data.model.SUPPORTED_PLATFORMS.forEach { platform ->
                                        val selected = platform.id in uiState.selectedPlatforms
                                        PlatformToggleRow(
                                            platform = platform,
                                            isSelected = selected,
                                            onToggle = { viewModel.togglePlatform(platform.id) }
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // -----------------------------------------------------------------
                            // CATEGORÍA 4: TEMA VISUAL DE TUJELLY (2 MODOS GLOBALES)
                            // -----------------------------------------------------------------
                            Text(
                                text = "Tema Visual de TuJelly",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Elige la personalidad visual de la app. Tu selección adapta armónicamente el logotipo, el foco del mando, la medusa de carga, los logos de plataformas y las insignias de estado.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Opción 1: Original TuJelly (Bioluminiscente Neón)
                            MasterThemeSelectionCard(
                                title = "Original TuJelly (Bioluminiscente)",
                                description = "Colores neón cian y violeta combinados con el logotipo oficial. Resalte de foco en azul cian, logos de plataformas a color e insignias vivas.",
                                isMonochrome = false,
                                isSelected = !uiState.isMonochrome,
                                onClick = { viewModel.setAppTheme(APP_THEME_ORIGINAL) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Opción 2: Monocromático (Blanco Puro Minimalista)
                            MasterThemeSelectionCard(
                                title = "Monocromático (Blanco Puro Minimalista)",
                                description = "Estética minimalista estilo Apple TV / tvOS de alto contraste. Logotipo, medusa de carga, logos de plataformas e insignias en blanco puro sobre cristal oscuro.",
                                isMonochrome = true,
                                isSelected = uiState.isMonochrome,
                                onClick = { viewModel.setAppTheme(APP_THEME_MONOCHROME) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Personalización de Botones de Acción",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Elige cómo deseas ver los botones de acción en la pantalla de detalle (solo iconos, solo texto o ambos).",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Opción 1: Solo Iconos
                            ButtonStyleSelectionCard(
                                title = "Solo Iconos (Minimalista)",
                                description = "Muestra únicamente iconos planos sin texto (▶, ♡, 🎬, 📺). Diseño limpio, compacto y elegante.",
                                isSelected = uiState.buttonStyle == BUTTON_STYLE_ICONS_ONLY,
                                onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_ICONS_ONLY) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Opción 2: Solo Letras
                            ButtonStyleSelectionCard(
                                title = "Solo Letras / Texto",
                                description = "Muestra únicamente el texto de la acción sin iconos (Reproducir, Favoritos, Ver Tráiler, App Jellyfin).",
                                isSelected = uiState.buttonStyle == BUTTON_STYLE_TEXT_ONLY,
                                onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_TEXT_ONLY) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Opción 3: Iconos con Letras
                            ButtonStyleSelectionCard(
                                title = "Iconos con Letras (Completo)",
                                description = "Muestra el icono y el texto de cada acción con espaciado equilibrado.",
                                isSelected = uiState.buttonStyle == BUTTON_STYLE_ICONS_AND_TEXT,
                                onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_ICONS_AND_TEXT) }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Caja de Vista Previa en Vivo
                            val previewAccent = TvAccent.getColor(uiState.accentColor)
                            val previewContentColor = TvAccent.getFocusedContentColor(uiState.accentColor)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF111220), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF222438), RoundedCornerShape(12.dp))
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "VISTA PREVIA EN VIVO",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "(El botón 'Reproducir' simula estar enfocado)",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Demo Play (Simulated FOCUSED with accent color!)
                                        Button(
                                            onClick = {},
                                            colors = ButtonDefaults.colors(
                                                containerColor = previewAccent,
                                                contentColor = previewContentColor,
                                                focusedContainerColor = previewAccent,
                                                focusedContentColor = previewContentColor
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                                if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                                                    if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        }

                                        // Demo Favorite (UNFOCUSED dark glass)
                                        Button(
                                            onClick = {},
                                            colors = ButtonDefaults.colors(
                                                containerColor = Color(0x18FFFFFF),
                                                contentColor = Color(0xFFCBD5E1),
                                                focusedContainerColor = previewAccent,
                                                focusedContentColor = previewContentColor
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                                                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(17.dp))
                                                }
                                                if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                                                    if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                                                    Text("En Favoritos", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                }
                                            }
                                        }

                                        // Demo Trailer (UNFOCUSED dark glass)
                                        Button(
                                            onClick = {},
                                            colors = ButtonDefaults.colors(
                                                containerColor = Color(0x18FFFFFF),
                                                contentColor = Color(0xFFCBD5E1),
                                                focusedContainerColor = previewAccent,
                                                focusedContentColor = previewContentColor
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                                                    Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(17.dp))
                                                }
                                                if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                                                    if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ver Tráiler", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            // -----------------------------------------------------------------
                            // CATEGORÍA 5: ACTUALIZACIONES DE GITHUB
                            // -----------------------------------------------------------------
                            val updateInfo = uiState.updateInfo
                            LuxurySettingTile(
                                title = "Versión Actual Instalada",
                                value = updateInfo?.currentVersion ?: "v1.0",
                                placeholder = "v1.0",
                                description = "Versión de la app ejecutándose en este dispositivo Android TV",
                                onClick = { viewModel.checkForAppUpdates() }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { viewModel.checkForAppUpdates() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF1E2034),
                                        contentColor = Color.White,
                                        focusedContainerColor = Color(0xFF6366F1),
                                        focusedContentColor = Color.White
                                    ),
                                    enabled = !uiState.isCheckingUpdate
                                ) {
                                    Text(
                                        text = if (uiState.isCheckingUpdate) "Buscando en GitHub..." else "Buscar Actualizaciones",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (updateInfo?.hasUpdate == true && updateInfo.apkUrl != null) {
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = Color(0xFF059669),
                                            contentColor = Color.White,
                                            focusedContainerColor = Color(0xFF10B981),
                                            focusedContentColor = Color.White
                                        )
                                    ) {
                                        val progressText = uiState.downloadProgress?.let { " (${(it * 100).toInt()}%)" } ?: ""
                                        Text(
                                            text = "Descargar e Instalar ${updateInfo.latestVersion}$progressText",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            val notes = updateInfo?.releaseNotes
                            if (!notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF111220), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF222438), RoundedCornerShape(12.dp))
                                        .padding(18.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "NOVEDADES DE ${updateInfo.latestVersion.uppercase()}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = notes,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Mensaje de notificación / estado
                    if (uiState.statusMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131424), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF312E81), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = uiState.statusMessage!!,
                                color = Color(0xFFE0E7FF),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // =============================================================================
        // MODAL DIÁLOGO DE EDICIÓN PARA TV
        // =============================================================================
        if (uiState.activeDialogField != null) {
            val field = uiState.activeDialogField!!
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            var isPasswordVisible by remember(field) { mutableStateOf(false) }

            LaunchedEffect(field) {
                delay(150)
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0000000))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .background(Color(0xFF11131A), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = field.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = field.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = uiState.activeDialogValue,
                            onValueChange = { viewModel.onDialogValueChange(it) },
                            singleLine = true,
                            visualTransformation = if (field.isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = if (field.isPassword) KeyboardType.Password else KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.commitDialogValue() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionDown -> {
                                                focusManager.moveFocus(FocusDirection.Down)
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                focusManager.moveFocus(FocusDirection.Up)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x18FFFFFF),
                                unfocusedContainerColor = Color(0x10FFFFFF),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                cursorColor = Color.White
                            )
                        )

                        if (field.isPassword) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (isPasswordVisible) Color(0x33FFFFFF) else Color(0x18FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isPasswordVisible) "👁 Ocultar contraseña (visible)" else "👁 Mostrar contraseña (oculta)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.commitDialogValue() },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { viewModel.dismissDialog() },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0x14FFFFFF),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ButtonStyleSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x28FFFFFF) else Color(0xFF131422),
            focusedContainerColor = Color(0x40FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222438)),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(1.5.dp, Color.White),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "SELECCIONADO",
                    containerColor = Color.White,
                    textColor = Color(0xFF0F172A),
                    borderColor = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MasterThemeSelectionCard(
    title: String,
    description: String,
    isMonochrome: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x28FFFFFF) else Color(0xFF131422),
            focusedContainerColor = Color(0x40FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) (if (isMonochrome) Color.White else Color(0xFF00A4DC)) else Color(0xFF222438)),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (isMonochrome) Color.White else Color(0xFF00A4DC)),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muestra visual del tema (Medusa + Acento)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isMonochrome) Color(0xFF181926) else Color(0x2200A4DC),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isMonochrome) Color(0x66FFFFFF) else Color(0x6600A4DC),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = if (isMonochrome) {
                    com.example.tujelly.R.drawable.ic_jelly_symbol_mono
                } else {
                    com.example.tujelly.R.drawable.ic_jelly_symbol
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isMonochrome) Color.White else Color(0xFF00A4DC),
                                CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "ACTIVO",
                    containerColor = if (isMonochrome) Color.White else Color(0xFF00A4DC),
                    textColor = Color(0xFF0F172A),
                    borderColor = if (isMonochrome) Color.White else Color(0xFF00A4DC)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AccentColorSelectionCard(
    title: String,
    description: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x28FFFFFF) else Color(0xFF131422),
            focusedContainerColor = Color(0x40FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0xFF222438)),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, accentColor),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "SELECCIONADO",
                    containerColor = accentColor,
                    textColor = if (accentColor == Color.White) Color(0xFF0F172A) else Color(0xFF0F172A),
                    borderColor = accentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IndicatorThemeSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    isColorMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x28FFFFFF) else Color(0xFF131422),
            focusedContainerColor = Color(0x40FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222438)),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muestra visual de muestra de badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de muestra: Favorito
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xE60F172A), CircleShape)
                        .border(1.dp, if (isColorMode) Color(0x88EF4444) else Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = if (isColorMode) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Badge de muestra: Visto
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xE60F172A), CircleShape)
                        .border(1.dp, if (isColorMode) Color(0x9910B981) else Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (isColorMode) Color(0xFF10B981) else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Badge de muestra: Progreso 7/20
                Box(
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(5.dp))
                        .border(1.dp, if (isColorMode) Color(0x9900A4DC) else Color(0x66FFFFFF), RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "7/20",
                        color = if (isColorMode) Color(0xFF38BDF8) else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "ACTIVO",
                    containerColor = Color.White,
                    textColor = Color(0xFF0F172A),
                    borderColor = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlatformLogoStyleSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    isColorMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x28FFFFFF) else Color(0xFF131422),
            focusedContainerColor = Color(0x40FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222438)),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muestra visual de logos
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini badge Netflix
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(44.dp)
                        .background(Color(0xFF111319), RoundedCornerShape(6.dp))
                        .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = if (isColorMode) com.example.tujelly.R.drawable.ic_brand_netflix else com.example.tujelly.R.drawable.ic_brand_netflix_mono),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Mini badge Disney+
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(44.dp)
                        .background(Color(0xFF111319), RoundedCornerShape(6.dp))
                        .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = if (isColorMode) com.example.tujelly.R.drawable.ic_brand_disney else com.example.tujelly.R.drawable.ic_brand_disney_mono),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Mini badge Movistar+
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(44.dp)
                        .background(Color(0xFF111319), RoundedCornerShape(6.dp))
                        .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = if (isColorMode) com.example.tujelly.R.drawable.ic_brand_movistar else com.example.tujelly.R.drawable.ic_brand_movistar_mono),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "ACTIVO",
                    containerColor = Color.White,
                    textColor = Color(0xFF0F172A),
                    borderColor = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlatformToggleRow(
    platform: com.example.tujelly.data.model.StreamPlatform,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color(0x22FFFFFF) else Color(0x10FFFFFF),
            focusedContainerColor = Color(0x28FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(0.75.dp, if (isSelected) Color(0x664F46E5) else Color(0x18FFFFFF))),
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(52.dp)
                    .background(Color(0xFF111319), RoundedCornerShape(6.dp))
                    .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = platform.iconRes),
                    contentDescription = platform.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platform.name,
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF4F46E5) else Color(0x18FFFFFF))
                    .border(1.dp, if (isSelected) Color(0xFF6366F1) else Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(text = "✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
