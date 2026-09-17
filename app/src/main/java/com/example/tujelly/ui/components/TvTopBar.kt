package com.example.tujelly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.tujelly.R
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.data.repository.SyncProgress
import com.example.tujelly.ui.theme.TvAccent

enum class TvNavTab {
    HOME,
    MEDUSA,
    FAVORITES,
    SEARCH,
    SETTINGS,
    NONE
}

/**
 * Cabecera de navegación universal para Android TV.
 * Mantiene la persistencia del logotipo de TuJelly y los accesos rápidos
 * (Inicio, Medusa, Favoritos, Buscar, Ajustes) en todas las pantallas principales.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTopBar(
    selectedTab: TvNavTab,
    onNavigateHome: () -> Unit,
    onOpenMedusa: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    accentColorKey: String,
    buttonStyleKey: String,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier,
    syncProgress: SyncProgress? = null,
    localMediaCount: Int = 0,
    titleBadge: @Composable (() -> Unit)? = null,
    medusaFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onDownFromMedusa: (() -> Unit)? = null
) {
    val focusColor = if (isMonochrome) Color.White else TvAccent.getColor(accentColorKey)
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else TvAccent.getFocusedContentColor(accentColorKey)
    val showTopIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showTopText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY
    val autoUpdateInfo by com.example.tujelly.data.remote.github.AppUpdateManager.updateInfo.collectAsState()
    val hasAvailableUpdate = autoUpdateInfo?.hasUpdate == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sección izquierda: Logo TuJelly
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerLogo = if (isMonochrome) {
                R.drawable.ic_tujelly_header_mono
            } else {
                R.drawable.ic_tujelly_header
            }
            Image(
                painter = painterResource(id = headerLogo),
                contentDescription = "Medusa",
                modifier = Modifier.height(52.dp)
            )
        }

        // Sección central: Insignia de contexto / Logo de Plataforma (Centrado perfectamente)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.wrapContentWidth()
        ) {
            if (titleBadge != null) {
                titleBadge()
            } else if (syncProgress != null && syncProgress.isSyncing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(if (isMonochrome) Color(0x22FFFFFF) else Color(0x2238BDF8), RoundedCornerShape(16.dp))
                        .border(0.75.dp, if (isMonochrome) Color(0x55FFFFFF) else Color(0x5538BDF8), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        color = if (isMonochrome) Color.White else Color(0xFF38BDF8),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else if (syncProgress != null && syncProgress.message.isNotBlank() &&
                (syncProgress.message.contains("401") || syncProgress.message.contains("Error") || syncProgress.message.contains("caducada", ignoreCase = true))
            ) {
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }

        // Sección derecha: Pestañas de navegación
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            // 1. Botón Inicio
            val isHomeActive = selectedTab == TvNavTab.HOME
            Button(
                onClick = onNavigateHome,
                colors = ButtonDefaults.colors(
                    containerColor = if (isHomeActive) (if (isMonochrome) Color(0x24FFFFFF) else focusColor.copy(alpha = 0.20f)) else Color(0x0AFFFFFF),
                    contentColor = if (isHomeActive) (if (isMonochrome) Color.White else focusColor) else (if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFF94A3B8)),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                shape = ButtonDefaults.shape(shape = CircleShape),
                border = if (isHomeActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = CircleShape
                        ),
                        focusedBorder = Border.None
                    )
                } else ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None
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
                            fontWeight = if (isHomeActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 2. Botón Descubrir
            val isDiscoverActive = selectedTab == TvNavTab.MEDUSA
            var isDiscoverFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onOpenMedusa,
                modifier = Modifier
                    .then(if (medusaFocusRequester != null) Modifier.focusRequester(medusaFocusRequester) else Modifier)
                    .onFocusChanged { isDiscoverFocused = it.isFocused }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                            if (onDownFromMedusa != null) {
                                onDownFromMedusa()
                                true
                            } else false
                        } else false
                    },
                colors = ButtonDefaults.colors(
                    containerColor = if (isDiscoverActive) (if (isMonochrome) Color(0x24FFFFFF) else focusColor.copy(alpha = 0.20f)) else Color(0x0AFFFFFF),
                    contentColor = if (isDiscoverActive) (if (isMonochrome) Color.White else focusColor) else (if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFF94A3B8)),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                shape = ButtonDefaults.shape(shape = CircleShape),
                border = if (isDiscoverActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = CircleShape
                        ),
                        focusedBorder = Border.None
                    )
                } else ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showTopIcons) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = "Descubrir",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (showTopText) {
                        if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Descubrir",
                            fontWeight = if (isDiscoverActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 3. Botón Favoritos
            val isFavoritesActive = selectedTab == TvNavTab.FAVORITES
            Button(
                onClick = onOpenFavorites,
                colors = ButtonDefaults.colors(
                    containerColor = if (isFavoritesActive) (if (isMonochrome) Color(0x24FFFFFF) else focusColor.copy(alpha = 0.20f)) else Color(0x0AFFFFFF),
                    contentColor = if (isFavoritesActive) (if (isMonochrome) Color.White else focusColor) else (if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFF94A3B8)),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                shape = ButtonDefaults.shape(shape = CircleShape),
                border = if (isFavoritesActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = CircleShape
                        ),
                        focusedBorder = Border.None
                    )
                } else ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None
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
                            fontWeight = if (isFavoritesActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 4. Botón Buscar
            val isSearchActive = selectedTab == TvNavTab.SEARCH
            Button(
                onClick = onOpenSearch,
                colors = ButtonDefaults.colors(
                    containerColor = if (isSearchActive) (if (isMonochrome) Color(0x24FFFFFF) else focusColor.copy(alpha = 0.20f)) else Color(0x0AFFFFFF),
                    contentColor = if (isSearchActive) (if (isMonochrome) Color.White else focusColor) else (if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFF94A3B8)),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                shape = ButtonDefaults.shape(shape = CircleShape),
                border = if (isSearchActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = CircleShape
                        ),
                        focusedBorder = Border.None
                    )
                } else ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None
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
                            fontWeight = if (isSearchActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 5. Botón Ajustes
            val isSettingsActive = selectedTab == TvNavTab.SETTINGS
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.colors(
                    containerColor = if (isSettingsActive) (if (isMonochrome) Color(0x24FFFFFF) else focusColor.copy(alpha = 0.20f)) else Color(0x0AFFFFFF),
                    contentColor = if (isSettingsActive) (if (isMonochrome) Color.White else focusColor) else (if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFF94A3B8)),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                shape = ButtonDefaults.shape(shape = CircleShape),
                border = if (isSettingsActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = CircleShape
                        ),
                        focusedBorder = Border.None
                    )
                } else ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None
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
                            fontWeight = if (isSettingsActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (hasAvailableUpdate) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    } else if (hasAvailableUpdate) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
            }
        }
    }
}
