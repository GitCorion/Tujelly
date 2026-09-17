package com.example.tujelly.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
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
import kotlinx.coroutines.delay

// =============================================================================
// PANTALLA DE AJUSTES: DISEÑO LIMPIO Y ELEGANTE TIPO APPLE TV / PLEX / NETFLIX
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

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, end = 48.dp, bottom = 24.dp)
            ) {
                // =============================================================
                // BARRA LATERAL DE CATEGORÍAS (220dp)
                // =============================================================
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(Color(0x06FFFFFF), RoundedCornerShape(14.dp))
                        .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "AJUSTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextTertiary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )

                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Dns,
                        title = "Jellyfin",
                        isSelected = uiState.activeCategory == 0,
                        onClick = { viewModel.selectCategory(0) },
                        focusColor = focusColor
                    )

                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.SyncAlt,
                        title = "Trakt.tv",
                        isSelected = uiState.activeCategory == 1,
                        onClick = { viewModel.selectCategory(1) },
                        focusColor = focusColor
                    )

                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Public,
                        title = "Streaming",
                        isSelected = uiState.activeCategory == 2,
                        onClick = { viewModel.selectCategory(2) },
                        focusColor = focusColor
                    )

                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.Palette,
                        title = "Apariencia",
                        isSelected = uiState.activeCategory == 3,
                        onClick = { viewModel.selectCategory(3) },
                        focusColor = focusColor
                    )

                    TvSettingsNavRailItem(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Acerca de",
                        isSelected = uiState.activeCategory == 4,
                        onClick = { viewModel.selectCategory(4) },
                        focusColor = focusColor
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // =============================================================
                // PANEL PRINCIPAL DE CONFIGURACIÓN
                // =============================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val sectionTitle = when (uiState.activeCategory) {
                        0 -> "Jellyfin"
                        1 -> "Trakt.tv"
                        2 -> "Streaming"
                        3 -> "Apariencia & Rendimiento"
                        else -> "Acerca de"
                    }

                    Text(
                        text = sectionTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (uiState.activeCategory) {
                            0 -> JellyfinCategory(uiState, viewModel, focusColor, focusContent)
                            1 -> TraktCategory(uiState, viewModel, focusColor, focusContent)
                            2 -> StreamingCategory(uiState, viewModel, focusColor, focusContent)
                            3 -> AppearanceCategory(uiState, viewModel, focusColor, focusContent)
                            4 -> AboutCategory(uiState, viewModel, focusColor, focusContent)
                        }

                        if (uiState.statusMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x14FFFFFF), RoundedCornerShape(10.dp))
                                    .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = focusColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
// CATEGORÍA 0: JELLYFIN
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun JellyfinCategory(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color
) {
    if (uiState.isJellyfinConnected) {
        TvSectionHeader(title = "SERVIDOR Y SESIÓN")

        TvSettingRow(
            title = "Servidor",
            value = uiState.serverUrl,
            onClick = { viewModel.openEditDialog(SettingField.SERVER_URL) }
        )

        TvSettingRow(
            title = "Usuario",
            value = uiState.username
        )

        TvSettingRow(
            title = "Estado",
            value = "Conectado"
        )

        val totalTitles = uiState.syncedMoviesCount + uiState.syncedSeriesCount
        TvSettingRow(
            title = "Biblioteca sincronizada",
            value = if (totalTitles > 0) {
                "${formatNumber(totalTitles)} títulos (${formatNumber(uiState.syncedMoviesCount)} películas, ${formatNumber(uiState.syncedSeriesCount)} series)"
            } else {
                "Sin títulos en memoria"
            }
        )

        if (!uiState.lastSyncTimestamp.isNullOrBlank()) {
            val formatted = uiState.lastSyncTimestamp?.replace("T", " ")?.substringBefore(".") ?: ""
            TvSettingRow(
                title = "Última sincronización",
                value = formatted
            )
        }

        TvSectionHeader(title = "ACCIONES")

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Button(
                onClick = { viewModel.forceSyncCatalog() },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x14FFFFFF),
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
                        Text("Sincronizando...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sincronizar biblioteca", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

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
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comprobar conexión", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = { viewModel.disconnectJellyfin() },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x14FFFFFF),
                    contentColor = Color(0xFFEF4444),
                    focusedContainerColor = Color(0xFFEF4444),
                    focusedContentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar sesión", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        // Desconectado
        TvSectionHeader(title = "DIRECCIÓN DEL SERVIDOR")

        TvSettingRow(
            title = "Servidor",
            value = uiState.serverUrl.ifBlank { "Introducir URL..." },
            onClick = { viewModel.openEditDialog(SettingField.SERVER_URL) }
        )

        if (uiState.serverUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
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
                    Text("Comprobar servidor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            var authMethod by remember { mutableIntStateOf(0) }

            TvSectionHeader(title = "MÉTODO DE INICIO DE SESIÓN")

            TvSegmentedSelector(
                options = listOf(
                    0 to "Quick Connect (QR)",
                    1 to "Usuario y contraseña"
                ),
                selectedOption = authMethod,
                onOptionSelected = { authMethod = it },
                focusColor = focusColor,
                focusContent = focusContent
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (authMethod == 0) {
                // Quick Connect
                Button(
                    onClick = { viewModel.startJellyfinQuickConnect() },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x14FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.isWaitingJellyfinQuickConnect) "Esperando confirmación..." else "Generar código QR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (uiState.jellyfinQrUrl != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    TvActivationCodeBox(
                        qrUrl = uiState.jellyfinQrUrl!!,
                        step1Text = "Abre en tu navegador o móvil Jellyfin > Quick Connect",
                        code = uiState.jellyfinQuickConnectCode ?: "..."
                    )
                }
            } else {
                // Manual
                TvSettingRow(
                    title = "Usuario",
                    value = uiState.username.ifBlank { "Escribir usuario..." },
                    onClick = { viewModel.openEditDialog(SettingField.USERNAME) }
                )

                TvSettingRow(
                    title = "Contraseña",
                    value = if (uiState.password.isNotBlank()) "••••••••" else "Escribir contraseña...",
                    onClick = { viewModel.openEditDialog(SettingField.PASSWORD) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveAndConnect() },
                    colors = ButtonDefaults.colors(
                        containerColor = focusColor,
                        contentColor = focusContent,
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.isLoading) "Conectando..." else "Iniciar sesión",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
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
private fun TraktCategory(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color
) {
    if (uiState.isTraktConnected) {
        TvSectionHeader(title = "CUENTA VINCULADA")

        TvSettingRow(
            title = "Usuario Trakt",
            value = "@${uiState.traktUsername ?: "activo"}"
        )

        TvSettingRow(
            title = "Estado",
            value = "Vinculado"
        )

        TvSectionHeader(title = "ACCIONES")

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Button(
                onClick = { viewModel.validateTraktToken() },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x14FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                )
            ) {
                Text("Comprobar conexión", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { viewModel.disconnectTrakt() },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x14FFFFFF),
                    contentColor = Color(0xFFEF4444),
                    focusedContainerColor = Color(0xFFEF4444),
                    focusedContentColor = Color.White
                )
            ) {
                Text("Desvincular cuenta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        TvSectionHeader(title = "VINCULAR CUENTA")

        Button(
            onClick = { viewModel.startTraktDeviceFlow() },
            colors = ButtonDefaults.colors(
                containerColor = Color(0x14FFFFFF),
                contentColor = Color.White,
                focusedContainerColor = focusColor,
                focusedContentColor = focusContent
            ),
            enabled = !uiState.isWaitingTraktAuth
        ) {
            Text(
                text = if (uiState.isWaitingTraktAuth) "Generando código..." else "Vincular con código QR",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (uiState.traktQrUrl != null) {
            Spacer(modifier = Modifier.height(14.dp))
            TvActivationCodeBox(
                qrUrl = uiState.traktQrUrl!!,
                step1Text = "Entra desde tu móvil o PC en trakt.tv/activate",
                code = uiState.traktUserCode ?: "..."
            )
        }
    }

    TvSectionHeader(title = "AJUSTES AVANZADOS DE API")

    TvSettingRow(
        title = "Client ID",
        value = formatKey(uiState.traktClientId),
        onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_ID) }
    )

    TvSettingRow(
        title = "Client Secret",
        value = if (uiState.traktClientSecret.isNotBlank()) "••••••••" else "Por defecto",
        onClick = { viewModel.openEditDialog(SettingField.TRAKT_CLIENT_SECRET) }
    )

    TvSettingRow(
        title = "Token manual",
        value = if (uiState.traktToken.isNotBlank()) formatKey(uiState.traktToken) else "Sin token",
        onClick = { viewModel.openEditDialog(SettingField.TRAKT_MANUAL_TOKEN) }
    )
}

// =============================================================================
// CATEGORÍA 2: STREAMING
// =============================================================================

@Composable
private fun StreamingCategory(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color
) {
    TvSectionHeader(title = "REGIÓN")

    TvSettingRow(
        title = "País del catálogo",
        value = uiState.watchRegion,
        onClick = { viewModel.openEditDialog(SettingField.WATCH_REGION) }
    )

    TvSectionHeader(title = "PLATAFORMAS DISPONIBLES")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SUPPORTED_PLATFORMS.forEach { platform ->
            val isSelected = platform.id in uiState.selectedPlatforms
            TvPlatformToggleRow(
                platform = platform,
                isSelected = isSelected,
                onToggle = { viewModel.togglePlatform(platform.id) },
                focusColor = focusColor,
                focusContent = focusContent
            )
        }
    }
}

// =============================================================================
// CATEGORÍA 3: APARIENCIA & RENDIMIENTO
// =============================================================================

@Composable
private fun AppearanceCategory(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color
) {
    TvSectionHeader(title = "TEMA")

    TvSegmentedSelector(
        options = listOf(
            APP_THEME_ORIGINAL to "Medusa Neón",
            APP_THEME_MONOCHROME to "Monocromático"
        ),
        selectedOption = if (uiState.isMonochrome) APP_THEME_MONOCHROME else APP_THEME_ORIGINAL,
        onOptionSelected = { viewModel.setAppTheme(it) },
        focusColor = focusColor,
        focusContent = focusContent
    )

    TvSectionHeader(title = "BOTONES EN FICHAS")

    TvSegmentedSelector(
        options = listOf(
            BUTTON_STYLE_ICONS_ONLY to "Solo iconos",
            BUTTON_STYLE_TEXT_ONLY to "Solo texto",
            BUTTON_STYLE_ICONS_AND_TEXT to "Iconos y texto"
        ),
        selectedOption = uiState.buttonStyle,
        onOptionSelected = { viewModel.updateButtonStyle(it) },
        focusColor = focusColor,
        focusContent = focusContent
    )

    TvSectionHeader(title = "RENDIMIENTO Y PARTÍCULAS")

    TvSegmentedSelector(
        options = listOf(
            PERFORMANCE_MODE_AUTO to "Automático",
            PERFORMANCE_MODE_HIGH to "Alto",
            PERFORMANCE_MODE_LOW to "Ahorro"
        ),
        selectedOption = uiState.performanceMode,
        onOptionSelected = { viewModel.updatePerformanceMode(it) },
        focusColor = focusColor,
        focusContent = focusContent
    )
}

// =============================================================================
// CATEGORÍA 4: ACERCA DE
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutCategory(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    focusColor: Color,
    focusContent: Color
) {
    val updateInfo = uiState.updateInfo
    val hasUpdate = updateInfo?.hasUpdate == true

    TvSectionHeader(title = "SISTEMA")

    TvSettingRow(
        title = "Aplicación",
        value = "Medusa"
    )

    TvSettingRow(
        title = "Versión instalada",
        value = "${updateInfo?.currentVersion ?: "2.0.2"} (Build 38)"
    )

    TvSettingRow(
        title = "Estado",
        value = if (hasUpdate) "Actualización disponible (${updateInfo?.latestVersion})" else "Al día"
    )

    TvSectionHeader(title = "ACTUALIZACIONES")

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Button(
            onClick = { viewModel.checkForAppUpdates() },
            colors = ButtonDefaults.colors(
                containerColor = Color(0x14FFFFFF),
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
                    text = if (uiState.isCheckingUpdate) "Comprobando..." else "Buscar actualizaciones",
                    fontSize = 13.sp,
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
                        text = "Descargar e instalar$progressText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    val notes = updateInfo?.releaseNotes
    if (!notes.isNullOrBlank()) {
        TvSectionHeader(title = "NOVEDADES DE LA VERSIÓN ${updateInfo.latestVersion.uppercase()}")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(10.dp))
                .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Text(
                text = notes,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// =============================================================================
// COMPONENTES REUTILIZABLES (ESTILO TV MINIMALISTA)
// =============================================================================

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsNavRailItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x18FFFFFF) else Color.Transparent,
            focusedContainerColor = Color(0x28FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.5.dp,
                    if (isSelected) Color(0x28FFFFFF) else Color.Transparent
                )
            ),
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) focusColor else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingRow(
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0x0CFFFFFF),
                focusedContainerColor = Color(0x28FFFFFF)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(border = BorderStroke(0.5.dp, Color(0x14FFFFFF))),
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
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                if (value != null) {
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                } else if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(Color(0x08FFFFFF), RoundedCornerShape(10.dp))
                .border(0.5.dp, Color(0x0EFFFFFF), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.weight(1f)
                )

                if (value != null) {
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun <T> TvSegmentedSelector(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    focusColor: Color,
    focusContent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selectedOption
            Surface(
                onClick = { onOptionSelected(option) },
                modifier = Modifier.weight(1f),
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) Color(0x20FFFFFF) else Color(0x08FFFFFF),
                    focusedContainerColor = Color(0x35FFFFFF)
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(
                        border = BorderStroke(
                            if (isSelected) 1.dp else 0.5.dp,
                            if (isSelected) focusColor.copy(alpha = 0.6f) else Color(0x14FFFFFF)
                        )
                    ),
                    focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White))
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = focusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPlatformToggleRow(
    platform: StreamPlatform,
    isSelected: Boolean,
    onToggle: () -> Unit,
    focusColor: Color,
    focusContent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x12FFFFFF) else Color(0x06FFFFFF),
            focusedContainerColor = Color(0x28FFFFFF)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    0.5.dp,
                    if (isSelected) Color(0x22FFFFFF) else Color(0x0EFFFFFF)
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
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .width(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1118))
                    .border(0.5.dp, Color(0x24FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
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
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isSelected) focusColor else Color(0x18FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = focusContent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvActivationCodeBox(
    qrUrl: String,
    step1Text: String,
    code: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = qrUrl,
                contentDescription = "Código QR",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "1. $step1Text",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "2. Introduce este código:",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = code,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
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
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp)
    )
}

// =============================================================================
// MODAL DIÁLOGO DE EDICIÓN PARA TV
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

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {},
            modifier = Modifier
                .width(480.dp)
                .padding(24.dp),
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF141722),
                focusedContainerColor = Color(0xFF141722)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(border = BorderStroke(1.dp, Color(0x33FFFFFF)))
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = field.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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

private fun formatNumber(count: Int): String {
    return String.format(java.util.Locale.GERMANY, "%,d", count)
}

private fun formatKey(value: String): String {
    if (value.isBlank()) return "Sin configurar"
    return if (value.length > 20) "${value.take(8)}...${value.takeLast(6)}" else value
}
