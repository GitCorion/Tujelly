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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
    titleBadge: @Composable (() -> Unit)? = null
) {
    val focusColor = TvAccent.getColor(accentColorKey)
    val focusContent = TvAccent.getFocusedContentColor(accentColorKey)
    val showTopIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showTopText = buttonStyleKey != BUTTON_STYLE_ICONS_ONLY

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sección izquierda: Logo + Badge de contexto o estado de sincronización
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerLogo = if (isMonochrome) {
                R.drawable.ic_tujelly_header_mono
            } else {
                R.drawable.ic_tujelly_header
            }
            Image(
                painter = painterResource(id = headerLogo),
                contentDescription = "TuJelly",
                modifier = Modifier.height(40.dp)
            )

            if (titleBadge != null) {
                Spacer(modifier = Modifier.width(14.dp))
                titleBadge()
            } else if (syncProgress != null && syncProgress.isSyncing) {
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (isMonochrome) Color(0x22FFFFFF) else Color(0x2238BDF8), RoundedCornerShape(16.dp))
                        .border(0.75.dp, if (isMonochrome) Color(0x55FFFFFF) else Color(0x5538BDF8), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    CircularProgressIndicator(
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            } else if (syncProgress != null && syncProgress.message.isNotBlank() &&
                (syncProgress.message.contains("401") || syncProgress.message.contains("Error") || syncProgress.message.contains("caducada", ignoreCase = true))
            ) {
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
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
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
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
                    containerColor = if (isHomeActive) Color(0x24FFFFFF) else Color(0x0AFFFFFF),
                    contentColor = if (isHomeActive) Color.White else Color(0xFF64748B),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                border = if (isHomeActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                } else ButtonDefaults.border()
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

            // 2. Botón Medusa
            val isMedusaActive = selectedTab == TvNavTab.MEDUSA
            var isMedusaFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onOpenMedusa,
                modifier = Modifier.onFocusChanged { isMedusaFocused = it.isFocused },
                colors = ButtonDefaults.colors(
                    containerColor = if (isMedusaActive) Color(0x24FFFFFF) else Color(0x0AFFFFFF),
                    contentColor = if (isMedusaActive) Color.White else Color(0xFF64748B),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                border = if (isMedusaActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                } else ButtonDefaults.border()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showTopIcons) {
                        val medusaSymbol = if (isMonochrome) {
                            if (isMedusaFocused) R.drawable.ic_jelly_symbol_mono_dark else R.drawable.ic_jelly_symbol_mono
                        } else {
                            R.drawable.ic_jelly_symbol
                        }
                        Image(
                            painter = painterResource(id = medusaSymbol),
                            contentDescription = "Medusa",
                            modifier = Modifier.size(16.dp),
                            colorFilter = if (isMonochrome) {
                                if (isMedusaFocused) ColorFilter.tint(focusContent)
                                else if (isMedusaActive) ColorFilter.tint(Color.White)
                                else ColorFilter.tint(Color(0xFF64748B))
                            } else null
                        )
                    }
                    if (showTopText) {
                        if (showTopIcons) Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Medusa",
                            fontWeight = if (isMedusaActive) FontWeight.Bold else FontWeight.SemiBold,
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
                    containerColor = if (isFavoritesActive) Color(0x24FFFFFF) else Color(0x0AFFFFFF),
                    contentColor = if (isFavoritesActive) Color.White else Color(0xFF64748B),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                border = if (isFavoritesActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                } else ButtonDefaults.border()
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
                    containerColor = if (isSearchActive) Color(0x24FFFFFF) else Color(0x0AFFFFFF),
                    contentColor = if (isSearchActive) Color.White else Color(0xFF64748B),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                border = if (isSearchActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                } else ButtonDefaults.border()
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
                    containerColor = if (isSettingsActive) Color(0x24FFFFFF) else Color(0x0AFFFFFF),
                    contentColor = if (isSettingsActive) Color.White else Color(0xFF64748B),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContent
                ),
                border = if (isSettingsActive) {
                    ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, focusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, focusColor),
                            shape = RoundedCornerShape(20.dp)
                        )
                    )
                } else ButtonDefaults.border()
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
                    }
                }
            }
        }
    }
}
