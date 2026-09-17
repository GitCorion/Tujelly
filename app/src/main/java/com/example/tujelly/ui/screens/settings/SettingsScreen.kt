package com.example.tujelly.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tujelly.R
import com.example.tujelly.data.local.APP_THEME_MONOCHROME
import com.example.tujelly.data.local.APP_THEME_ORIGINAL
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_AND_TEXT
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.data.local.PERFORMANCE_MODE_AUTO
import com.example.tujelly.data.local.PERFORMANCE_MODE_HIGH
import com.example.tujelly.data.local.PERFORMANCE_MODE_LOW
import com.example.tujelly.data.model.SUPPORTED_PLATFORMS
import com.example.tujelly.data.model.StreamPlatform
import com.example.tujelly.ui.components.TvNavTab
import com.example.tujelly.ui.components.TvTopBar
import com.example.tujelly.ui.theme.TvAccent
import com.example.tujelly.ui.theme.TvColors
import com.example.tujelly.ui.theme.TvPill
import kotlinx.coroutines.delay

// =============================================================================
// PANTALLA PRINCIPAL DE AJUSTES (MASTER-DETAIL TV EXPERIENCE)
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onOpenMedusa: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusColor = if (uiState.isMonochrome) Color.White else TvAccent.getColor(uiState.accentColor)
    val focusContent = if (uiState.isMonochrome) Color(0xFF0F172A) else TvAccent.getFocusedContentColor(uiState.accentColor)

    // Cierre automático tras conexión satisfactoria manual
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            delay(1500)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior universal persistente para Android TV
            TvTopBar(
                selectedTab = TvNavTab.SETTINGS,
                onNavigateHome = onNavigateHome,
                onOpenMedusa = onOpenMedusa,
                onOpenFavorites = onOpenFavorites,
                onOpenSearch = onOpenSearch,
                onOpenSettings = { /* Ya en Ajustes */ },
                accentColorKey = uiState.accentColor,
                buttonStyleKey = uiState.buttonStyle,
                isMonochrome = uiState.isMonochrome
            )

            // Contenedor Maestro-Detalle con márgenes seguros TV (48.dp)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, end = 48.dp, bottom = 24.dp)
            ) {
                // =============================================================
                // CARRIL LATERAL DE NAVEGACIÓN (260dp)
                // =============================================================
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(Color(0x06FFFFFF), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AJUSTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextTertiary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 0. Servidor Jellyfin
                    val jfSubtitle = if (uiState.isJellyfinConnected) {
                        val total = uiState.syncedMoviesCount + uiState.syncedSeriesCount
                        if (total > 0) "${formatNumber(total)} títulos" else "Conectado"
                    } else {
                        "Sin conectar"
                    }
                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Dns,
                        title = "Servidor Jellyfin",
                        subtitle = jfSubtitle,
                        isSelected = uiState.activeCategory == 0,
                        onClick = { viewModel.selectCategory(0) },
                        focusColor = focusColor,
                        isMonochrome = uiState.isMonochrome
                    )

                    // 1. Trakt.tv
                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.SyncAlt,
                        title = "Trakt.tv",
                        subtitle = if (uiState.isTraktConnected) "@${uiState.traktUsername ?: "activo"}" else "Desconectado",
                        isSelected = uiState.activeCategory == 1,
                        onClick = { viewModel.selectCategory(1) },
                        focusColor = focusColor,
                        isMonochrome = uiState.isMonochrome
                    )

                    // 2. Región & Streaming
                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Public,
                        title = "Región & Streaming",
                        subtitle = "${uiState.watchRegion} • ${uiState.selectedPlatforms.size} activas",
                        isSelected = uiState.activeCategory == 2,
                        onClick = { viewModel.selectCategory(2) },
                        focusColor = focusColor,
                        isMonochrome = uiState.isMonochrome
                    )

                    // 3. Apariencia & Rendimiento
                    val themeSummary = if (uiState.isMonochrome) "Monocromático" else "Original Neón"
                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Palette,
                        title = "Apariencia",
                        subtitle = themeSummary,
                        isSelected = uiState.activeCategory == 3,
                        onClick = { viewModel.selectCategory(3) },
                        focusColor = focusColor,
                        isMonochrome = uiState.isMonochrome
                    )

                    // 4. Actualizaciones
                    val updateAvailable = uiState.updateInfo?.hasUpdate == true
                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Actualizaciones",
                        subtitle = if (updateAvailable) "¡Nueva versión!" else "v${uiState.updateInfo?.currentVersion ?: "2.0.2"}",
                        isSelected = uiState.activeCategory == 4,
                        onClick = { viewModel.selectCategory(4) },
                        focusColor = focusColor,
                        isMonochrome = uiState.isMonochrome
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // =============================================================
                // PANEL DE CONTENIDO DETALLADO DERECHO
                // =============================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Cabecera elegante del panel
                    val sectionTitle = when (uiState.activeCategory) {
                        0 -> "Servidor Jellyfin"
                        1 -> "Integración con Trakt.tv"
                        2 -> "Región & Streaming"
                        3 -> "Apariencia & Rendimiento"
                        else -> "Actualizaciones de Software"
                    }
                    val sectionSubtitle = when (uiState.activeCategory) {
                        0 -> if (uiState.isJellyfinConnected) "Conectado a tu servidor y biblioteca local." else "Conecta tu servidor Jellyfin para disfrutar de tu contenido."
                        1 -> "Sincroniza tu historial, progreso y valoraciones con Trakt."
                        2 -> "Configura tu país y las plataformas de streaming disponibles."
                        3 -> "Personaliza el estilo visual, botones y efectos de la app."
                        else -> "Comprueba la versión instalada y descarga novedades."
                    }

                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = sectionTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sectionSubtitle,
                            fontSize = 12.sp,
                            color = TvColors.TextSecondary
                        )
                    }

                    // Contenido Scrolleable de la Categoría Activa
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (uiState.activeCategory) {
                            0 -> JellyfinCategoryContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                focusColor = focusColor,
                                focusContent = focusContent,
                                isMonochrome = uiState.isMonochrome
                            )
                            1 -> TraktCategoryContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                focusColor = focusColor,
                                focusContent = focusContent,
                                isMonochrome = uiState.isMonochrome
                            )
                            2 -> RegionCategoryContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                focusColor = focusColor,
                                focusContent = focusContent,
                                isMonochrome = uiState.isMonochrome
                            )
                            3 -> AppearanceCategoryContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                focusColor = focusColor,
                                focusContent = focusContent,
                                isMonochrome = uiState.isMonochrome
                            )
                            4 -> UpdatesCategoryContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                focusColor = focusColor,
                                focusContent = focusContent,
                                isMonochrome = uiState.isMonochrome
                            )
                        }

                        // Banner de notificación de estado interactivo
                        if (uiState.statusMessage != null) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = focusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = uiState.statusMessage!!,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Modal Diálogo de Edición TV Refinado
        if (uiState.activeDialogField != null) {
            TvEditDialog(
                field = uiState.activeDialogField!!,
                initialValue = uiState.activeDialogValue,
                onValueChange = { viewModel.onDialogValueChange(it) },
                onCommit = { viewModel.commitDialogValue() },
                onDismiss = { viewModel.dismissDialog() },
                focusColor = focusColor,
                focusContent = focusContent
            )
        }
    }
}

// =============================================================================
// CATEGORÍA 0: SERVIDOR JELLYFIN
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun JellyfinCategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean
) {
    if (uiState.isJellyfinConnected) {
        // =====================================================================
        // ESTADO 1: USUARIO CONECTADO (EXPERIENCIA LÓGICA Y ELEGANTE)
        // =====================================================================

        // 1. Tarjeta de Sesión Activa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = focusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.serverName ?: "Servidor Jellyfin",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.serverUrl}  •  Usuario: ${uiState.username}",
                            fontSize = 12.sp,
                            color = TvColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    TvPill(
                        text = "EN LÍNEA",
                        containerColor = Color(0x1FFFFFFF),
                        textColor = Color.White,
                        borderColor = Color(0x33FFFFFF)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Fila de acciones lógicas de sesión activa
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.forceSyncCatalog() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x18FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        enabled = !uiState.isSyncingCatalog
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSyncingCatalog) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizando...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar Biblioteca", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.testServerConnection() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x18FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Comprobar Conexión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = { viewModel.disconnectJellyfin() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x14FFFFFF),
                            contentColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFEF4444),
                            focusedContentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. Métricas de la Biblioteca Sincronizada
        Spacer(modifier = Modifier.height(24.dp))
        TvSectionHeader(title = "CONTENIDO EN MEMORIA LOCAL")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvMetricStatCard(
                title = "Películas",
                count = uiState.syncedMoviesCount,
                icon = Icons.Rounded.Movie,
                focusColor = focusColor,
                modifier = Modifier.weight(1f)
            )
            TvMetricStatCard(
                title = "Series",
                count = uiState.syncedSeriesCount,
                icon = Icons.Rounded.Tv,
                focusColor = focusColor,
                modifier = Modifier.weight(1f)
            )
            TvMetricStatCard(
                title = "Capítulos",
                count = uiState.syncedEpisodesCount,
                icon = Icons.Rounded.VideoLibrary,
                focusColor = focusColor,
                modifier = Modifier.weight(1f)
            )
        }

        val lastSync = uiState.lastSyncTimestamp
        if (!lastSync.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            val formatted = lastSync.replace("T", " ").substringBefore(".")
            Text(
                text = "Última sincronización con servidor: $formatted",
                fontSize = 11.sp,
                color = TvColors.TextTertiary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    } else {
        // =====================================================================
        // ESTADO 2: USUARIO DESCONECTADO (FLUJO LÓGICO PASO A PASO)
        // =====================================================================

        // PASO 1: Servidor
        TvSectionHeader(title = "PASO 1: INTRODUCE LA URL DE TU SERVIDOR")
        Text(
            text = "Indica la dirección IP o dominio de tu Jellyfin para que TuJelly pueda comunicarse con él.",
            fontSize = 12.sp,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        TvSettingItemCard(
            icon = Icons.Rounded.Link,
            title = "Dirección URL del Servidor",
            description = "Ejemplo: http://192.168.1.100:8096 o https://jellyfin.tudominio.com",
            value = uiState.serverUrl,
            placeholder = "Pulsar para introducir la URL del servidor",
            onClick = { viewModel.openEditDialog(SettingField.SERVER_URL) },
            focusColor = focusColor
        )

        if (uiState.serverUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.testServerConnection() },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x14FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comprobar Servidor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // PASO 2: Elegir método de inicio de sesión
        TvSectionHeader(title = "PASO 2: INICIA SESIÓN EN TU CUENTA")
        Text(
            text = "Puedes autorizar la TV escaneando un código QR con tu móvil o escribiendo tus credenciales.",
            fontSize = 12.sp,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Opción A: Quick Connect (QR)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = if (uiState.serverUrl.isNotBlank()) focusColor else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Método Rápido: Quick Connect (QR)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (uiState.serverUrl.isBlank()) {
                    Text(
                        text = "Introduce primero la URL del servidor en el Paso 1 arriba para poder generar el código QR de Quick Connect.",
                        fontSize = 12.sp,
                        color = TvColors.TextTertiary
                    )
                } else {
                    Text(
                        text = "Genera un código temporal para autorizar este televisor desde tu móvil o navegador sin escribir contraseñas.",
                        fontSize = 12.sp,
                        color = TvColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.startJellyfinQuickConnect() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x18FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isWaitingJellyfinQuickConnect) "Esperando confirmación..." else "Generar QR Quick Connect",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (uiState.jellyfinQrUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0CFFFFFF), RoundedCornerShape(14.dp))
                                .border(0.75.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = uiState.jellyfinQrUrl,
                                    contentDescription = "Código QR Jellyfin",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1. Abre en tu navegador o app móvil de Jellyfin:",
                                    fontSize = 11.sp,
                                    color = TvColors.TextTertiary
                                )
                                Text(
                                    text = "Ajustes > Quick Connect",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "2. Introduce este código de 6 dígitos:",
                                    fontSize = 11.sp,
                                    color = TvColors.TextTertiary
                                )
                                Text(
                                    text = uiState.jellyfinQuickConnectCode ?: "...",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opción B: Usuario y Contraseña Manual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = focusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Método Manual: Usuario y Contraseña",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvSettingItemCard(
                        icon = Icons.Rounded.Person,
                        title = "Nombre de Usuario",
                        description = "Usuario registrado en tu servidor",
                        value = uiState.username,
                        placeholder = "Pulsar para escribir usuario",
                        onClick = { viewModel.openEditDialog(SettingField.USERNAME) },
                        focusColor = focusColor
                    )

                    TvSettingItemCard(
                        icon = Icons.Rounded.Lock,
                        title = "Contraseña",
                        description = "Clave de acceso de la cuenta",
                        value = if (uiState.password.isNotBlank()) "••••••••" else "",
                        placeholder = "Pulsar para escribir contraseña (o dejar vacía)",
                        onClick = { viewModel.openEditDialog(SettingField.PASSWORD) },
                        focusColor = focusColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.saveAndConnect() },
                    colors = ButtonDefaults.colors(
                        containerColor = focusColor,
                        contentColor = focusContent,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    enabled = !uiState.isLoading && uiState.serverUrl.isNotBlank()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = focusContent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectando...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectar con Credenciales", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// CATEGORÍA 1: TRAKT.TV
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TraktCategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean
) {
    if (uiState.isTraktConnected) {
        // Trakt Conectado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SyncAlt,
                            contentDescription = null,
                            tint = focusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cuenta Trakt.tv Vinculada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sincronizando con @${uiState.traktUsername ?: "usuario"}",
                            fontSize = 12.sp,
                            color = TvColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    TvPill(
                        text = "VINCULADO",
                        containerColor = Color(0x1FFFFFFF),
                        textColor = Color.White,
                        borderColor = Color(0x33FFFFFF)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.validateTraktToken() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x18FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Text("Verificar Conexión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.disconnectTrakt() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x14FFFFFF),
                            contentColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFEF4444),
                            focusedContentColor = Color.White
                        )
                    ) {
                        Text("Desvincular Cuenta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    } else {
        // Trakt Desconectado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Sincroniza tus listas de seguimiento, marcas de visto y valoraciones en la nube.",
                    fontSize = 13.sp,
                    color = TvColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.startTraktDeviceFlow() },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x18FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    enabled = !uiState.isWaitingTraktAuth
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isWaitingTraktAuth) "Generando Código..." else "Vincular con Código QR (Device Code)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (uiState.traktQrUrl != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0CFFFFFF), RoundedCornerShape(14.dp))
                            .border(0.75.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = uiState.traktQrUrl,
                                contentDescription = "Código QR Trakt",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Entra desde tu móvil o PC en:",
                                fontSize = 11.sp,
                                color = TvColors.TextTertiary
                            )
                            Text(
                                text = "trakt.tv/activate",
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "2. Escribe este código:",
                                fontSize = 11.sp,
                                color = TvColors.TextTertiary
                            )
                            Text(
                                text = uiState.traktUserCode ?: "...",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Ajustes Avanzados de API (sin romper ancho de texto)
    TvSectionHeader(title = "AJUSTES AVANZADOS (OPCIONAL)")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvSettingItemCard(
            icon = Icons.Rounded.Key,
            title = "Trakt Client ID",
            description = "Identificador de aplicación OAuth en Trakt.tv",
            value = uiState.traktClientId,
            onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_ID) },
            focusColor = focusColor
        )

        TvSettingItemCard(
            icon = Icons.Rounded.Lock,
            title = "Trakt Client Secret",
            description = "Clave secreta opcional para aplicaciones Trakt personalizadas",
            value = if (uiState.traktClientSecret.isNotBlank()) "••••••••" else "",
            placeholder = "Opcional",
            onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_SECRET) },
            focusColor = focusColor
        )

        TvSettingItemCard(
            icon = Icons.Rounded.Link,
            title = "Token de Acceso Directo",
            description = "Pega un token personal para omitir el flujo QR",
            value = if (uiState.traktToken.isNotBlank()) uiState.traktToken else "",
            placeholder = "Sin token manual",
            onClick = { viewModel.openEditDialog(SettingField.TRAKT_MANUAL_TOKEN) },
            focusColor = focusColor
        )
    }
}

// =============================================================================
// CATEGORÍA 2: REGIÓN & STREAMING
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RegionCategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean
) {
    TvSectionHeader(title = "PAÍS Y MERCADO")

    TvSettingItemCard(
        icon = Icons.Rounded.Public,
        title = "Región de Catálogo",
        description = "Código de país de 2 letras (ej. ES, MX, US, AR, CO)",
        value = uiState.watchRegion,
        onClick = { viewModel.openEditDialog(SettingField.WATCH_REGION) },
        focusColor = focusColor
    )

    Spacer(modifier = Modifier.height(24.dp))

    TvSectionHeader(title = "PLATAFORMAS DE STREAMING ACTIVAS")
    Text(
        text = "Marca las plataformas cuyas novedades quieres cruzar con tu catálogo Jellyfin.",
        color = TvColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SUPPORTED_PLATFORMS.forEach { platform ->
            val isSelected = platform.id in uiState.selectedPlatforms
            TvPlatformToggleCard(
                platform = platform,
                isSelected = isSelected,
                onToggle = { viewModel.togglePlatform(platform.id) },
                focusColor = focusColor,
                focusContent = focusContent,
                isMonochrome = isMonochrome
            )
        }
    }
}

// =============================================================================
// CATEGORÍA 3: APARIENCIA & RENDIMIENTO
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppearanceCategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean
) {
    // 1. Tema Maestro
    TvSectionHeader(title = "TEMA VISUAL DE TUJELLY")
    Text(
        text = "Elige la identidad estética global de la interfaz.",
        color = TvColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TvMasterThemeCard(
            title = "Original TuJelly (Bioluminiscente)",
            description = "Colores bioluminiscentes vivos neón cian y violeta de gran contraste.",
            iconRes = R.drawable.ic_jelly_symbol,
            isSelected = !uiState.isMonochrome,
            onClick = { viewModel.setAppTheme(APP_THEME_ORIGINAL) },
            focusColor = focusColor,
            focusContent = focusContent,
            modifier = Modifier.weight(1f)
        )

        TvMasterThemeCard(
            title = "Monocromático (Blanco Minimalista)",
            description = "Estética minimalista estilo Apple TV / tvOS de alto contraste sobre cristal oscuro.",
            iconRes = R.drawable.ic_jelly_symbol_mono,
            isSelected = uiState.isMonochrome,
            onClick = { viewModel.setAppTheme(APP_THEME_MONOCHROME) },
            focusColor = focusColor,
            focusContent = focusContent,
            modifier = Modifier.weight(1f)
        )
    }

    // 2. Estilo de Botones
    Spacer(modifier = Modifier.height(24.dp))
    TvSectionHeader(title = "ESTILO DE BOTONES DE ACCIÓN")
    Text(
        text = "Selecciona cómo deseas ver los botones de acción en las fichas de contenido.",
        color = TvColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButtonStyleCard(
            title = "Solo Iconos (Minimalista)",
            description = "Iconos limpios sin texto (▶, ♡, 🎬). Ligero y compacto.",
            sampleContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            },
            isSelected = uiState.buttonStyle == BUTTON_STYLE_ICONS_ONLY,
            onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_ICONS_ONLY) },
            focusColor = focusColor,
            focusContent = focusContent
        )

        TvButtonStyleCard(
            title = "Solo Letras / Texto",
            description = "Muestra únicamente el texto de la acción sin iconos.",
            sampleContent = {
                Text("Reproducir • Favoritos • Tráiler", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            },
            isSelected = uiState.buttonStyle == BUTTON_STYLE_TEXT_ONLY,
            onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_TEXT_ONLY) },
            focusColor = focusColor,
            focusContent = focusContent
        )

        TvButtonStyleCard(
            title = "Iconos con Letras (Completo)",
            description = "Combina el icono representativo y el texto de acción.",
            sampleContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reproducir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            isSelected = uiState.buttonStyle == BUTTON_STYLE_ICONS_AND_TEXT,
            onClick = { viewModel.updateButtonStyle(BUTTON_STYLE_ICONS_AND_TEXT) },
            focusColor = focusColor,
            focusContent = focusContent
        )
    }

    // 3. Rendimiento y Efectos Visuales
    Spacer(modifier = Modifier.height(24.dp))
    TvSectionHeader(title = "RENDIMIENTO Y PARTÍCULAS")
    Text(
        text = "Modula la complejidad de las animaciones y partículas estelares de la vista Medusa.",
        color = TvColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvPerformanceCard(
            title = "Automático (Recomendado)",
            description = "Detecta la memoria RAM y núcleos de la CPU de forma inteligente.",
            isSelected = uiState.performanceMode == PERFORMANCE_MODE_AUTO,
            onClick = { viewModel.updatePerformanceMode(PERFORMANCE_MODE_AUTO) },
            focusColor = focusColor,
            focusContent = focusContent
        )

        TvPerformanceCard(
            title = "Máxima Calidad (100% Partículas)",
            description = "Física completa para Smart TVs potentes o Nvidia Shield.",
            isSelected = uiState.performanceMode == PERFORMANCE_MODE_HIGH,
            onClick = { viewModel.updatePerformanceMode(PERFORMANCE_MODE_HIGH) },
            focusColor = focusColor,
            focusContent = focusContent
        )

        TvPerformanceCard(
            title = "Ahorro de Recursos (30% Partículas)",
            description = "Optimizado para Fire TV Stick u otros dongles para garantizar 60 FPS estables.",
            isSelected = uiState.performanceMode == PERFORMANCE_MODE_LOW,
            onClick = { viewModel.updatePerformanceMode(PERFORMANCE_MODE_LOW) },
            focusColor = focusColor,
            focusContent = focusContent
        )
    }

    // 4. Muelle de Previsualización en Vivo
    Spacer(modifier = Modifier.height(24.dp))
    TvSectionHeader(title = "VISTA PREVIA EN TIEMPO REAL")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
            .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "Simulación de botones en ficha de detalle (el primer botón simula estar enfocado):",
                color = TvColors.TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Simulación botón enfocado
                Button(
                    onClick = {},
                    colors = ButtonDefaults.colors(
                        containerColor = focusColor,
                        contentColor = focusContent,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                            if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Simulación botón sin foco 1
                Button(
                    onClick = {},
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x18FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                        if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                            if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                            Text("Favoritos", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // Simulación botón sin foco 2
                Button(
                    onClick = {},
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x18FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                        if (uiState.buttonStyle != BUTTON_STYLE_ICONS_ONLY) {
                            if (uiState.buttonStyle != BUTTON_STYLE_TEXT_ONLY) Spacer(modifier = Modifier.width(6.dp))
                            Text("Tráiler", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// CATEGORÍA 4: ACTUALIZACIONES
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpdatesCategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean
) {
    val updateInfo = uiState.updateInfo
    val hasUpdate = updateInfo?.hasUpdate == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
            .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = focusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasUpdate) "¡Nueva Versión ${updateInfo?.latestVersion} Disponible!" else "TuJelly está Actualizado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasUpdate) {
                            "Hay una nueva versión lista para instalar en tu Android TV."
                        } else {
                            "Estás ejecutando la versión ${updateInfo?.currentVersion ?: "2.0.2"}."
                        },
                        fontSize = 12.sp,
                        color = TvColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TvPill(
                    text = if (hasUpdate) "DISPONIBLE" else "AL DÍA",
                    containerColor = Color(0x1FFFFFFF),
                    textColor = Color.White,
                    borderColor = Color(0x33FFFFFF)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.checkForAppUpdates() },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x18FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    enabled = !uiState.isCheckingUpdate
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isCheckingUpdate) "Comprobando..." else "Buscar Actualizaciones",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (hasUpdate && updateInfo?.apkUrl != null) {
                    Button(
                        onClick = { viewModel.downloadAndInstallUpdate() },
                        colors = ButtonDefaults.colors(
                            containerColor = focusColor,
                            contentColor = focusContent,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val progressText = uiState.downloadProgress?.let { " (${(it * 100).toInt()}%)" } ?: ""
                            Text(
                                text = "Descargar e Instalar$progressText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Notas de la versión
    val notes = updateInfo?.releaseNotes
    if (!notes.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(20.dp))
        TvSectionHeader(title = "NOVEDADES DE LA VERSIÓN ${updateInfo.latestVersion.uppercase()}")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(0.75.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = notes,
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// =============================================================================
// COMPONENTES REUTILIZABLES DE DISEÑO TV GLASSMORPHISM
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsNavRailItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusColor: Color,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) focusColor.copy(alpha = 0.12f) else Color.Transparent,
            focusedContainerColor = Color(0x24FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.75.dp,
                    if (isSelected) focusColor.copy(alpha = 0.35f) else Color.Transparent
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador sutil de selección en barra vertical izquierda
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(focusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Icono en contenedor glaseado
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x12FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected && !isMonochrome) focusColor else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected && !isMonochrome) focusColor else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = TvColors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    description: String? = null,
    placeholder: String = "No configurado",
    onClick: () -> Unit,
    focusColor: Color,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    // Formateo seguro para cadenas largas (ej. Client ID o Tokens de 64 caracteres)
    val displayValue = remember(value) {
        if (value.length > 22) {
            "${value.take(8)}...${value.takeLast(6)}"
        } else {
            value
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x0CFFFFFF),
            focusedContainerColor = Color(0x22FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(0.75.dp, Color(0x14FFFFFF))),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x12FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) focusColor else Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Columna de título con ancho asegurado mediante weight(1f)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = TvColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Chip con el valor con ancho máximo garantizado
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .background(Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.widthIn(max = 220.dp)
                ) {
                    Text(
                        text = displayValue.ifBlank { placeholder },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (value.isBlank()) Color(0xFF64748B) else Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMetricStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    focusColor: Color,
    modifier: Modifier = Modifier
) {
    val formatted = formatNumber(count)

    Row(
        modifier = modifier
            .background(Color(0x0CFFFFFF), RoundedCornerShape(14.dp))
            .border(0.75.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x18FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = focusColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextTertiary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = formatted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPlatformToggleCard(
    platform: StreamPlatform,
    isSelected: Boolean,
    onToggle: () -> Unit,
    focusColor: Color,
    focusContent: Color,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) {
                if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.12f)
            } else Color(0x0AFFFFFF),
            focusedContainerColor = Color(0x24FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.75.dp,
                    if (isSelected) (if (isMonochrome) Color(0x44FFFFFF) else focusColor.copy(alpha = 0.45f)) else Color(0x14FFFFFF)
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Insignia con el Logo Oficial de la Plataforma
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1118))
                    .border(0.75.dp, Color(0x24FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = platform.iconRes),
                    contentDescription = platform.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = platform.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Switch / Checkbox interactivo
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) (if (isMonochrome) Color.White else focusColor) else Color(0x18FFFFFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMasterThemeCard(
    title: String,
    description: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusColor: Color,
    focusContent: Color,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) focusColor.copy(alpha = 0.14f) else Color(0x0CFFFFFF),
            focusedContainerColor = Color(0x24FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.75.dp,
                    if (isSelected) focusColor.copy(alpha = 0.50f) else Color(0x14FFFFFF)
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x18FFFFFF))
                        .border(0.75.dp, Color(0x24FFFFFF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isSelected) {
                    TvPill(
                        text = "ACTIVO",
                        containerColor = focusColor,
                        textColor = focusContent,
                        borderColor = focusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = TvColors.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvButtonStyleCard(
    title: String,
    description: String,
    sampleContent: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusColor: Color,
    focusContent: Color,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) focusColor.copy(alpha = 0.12f) else Color(0x0CFFFFFF),
            focusedContainerColor = Color(0x24FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.75.dp,
                    if (isSelected) focusColor.copy(alpha = 0.40f) else Color(0x14FFFFFF)
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TvColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Muestra visual
            Box(
                modifier = Modifier
                    .background(Color(0x18FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                sampleContent()
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "SELECCIONADO",
                    containerColor = focusColor,
                    textColor = focusContent,
                    borderColor = focusColor
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPerformanceCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusColor: Color,
    focusContent: Color,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) focusColor.copy(alpha = 0.12f) else Color(0x0CFFFFFF),
            focusedContainerColor = Color(0x24FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.75.dp,
                    if (isSelected) focusColor.copy(alpha = 0.40f) else Color(0x14FFFFFF)
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x12FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = if (isSelected) focusColor else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TvColors.TextTertiary
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPill(
                    text = "ACTIVO",
                    containerColor = focusColor,
                    textColor = focusContent,
                    borderColor = focusColor
                )
            }
        }
    }
}

@Composable
private fun TvSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TvColors.TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// =============================================================================
// MODAL DIÁLOGO DE EDICIÓN PARA TV CON ENFOQUE FLUIDO
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvEditDialog(
    field: SettingField,
    initialValue: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit,
    focusColor: Color,
    focusContent: Color
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isPasswordVisible by remember(field) { mutableStateOf(false) }

    var textFieldValue by remember(field) {
        mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length)))
    }

    LaunchedEffect(initialValue) {
        if (textFieldValue.text != initialValue) {
            textFieldValue = TextFieldValue(initialValue, TextRange(initialValue.length))
        }
    }

    LaunchedEffect(field) {
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF12141C))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(focusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (field.isPassword) Icons.Rounded.Lock else Icons.Rounded.Link,
                        contentDescription = null,
                        tint = focusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = field.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = field.description,
                    fontSize = 12.sp,
                    color = TvColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newTfv ->
                        textFieldValue = newTfv
                        onValueChange(newTfv.text)
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = field.placeholder,
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    trailingIcon = {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(onClick = { onValueChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Limpiar texto",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = if (field.isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = if (field.isPassword) KeyboardType.Password else KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onCommit() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x18FFFFFF),
                        unfocusedContainerColor = Color(0x0CFFFFFF),
                        focusedBorderColor = focusColor,
                        unfocusedBorderColor = Color(0x28FFFFFF),
                        cursorColor = focusColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (field.isPassword) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x12FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCommit,
                        colors = ButtonDefaults.colors(
                            containerColor = focusColor,
                            contentColor = focusContent,
                            focusedContainerColor = focusColor,
                            focusedContentColor = focusContent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x18FFFFFF),
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

// Utilidad para formatear números de catálogo con punto de miles
private fun formatNumber(count: Int): String {
    return String.format(java.util.Locale.GERMANY, "%,d", count)
}
