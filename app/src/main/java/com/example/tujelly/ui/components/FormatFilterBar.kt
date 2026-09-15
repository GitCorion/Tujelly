package com.example.tujelly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.domain.model.MediaFormatFilter
import com.example.tujelly.ui.theme.TvAccent

/**
 * Barra de filtros de formato universal (Todos / Películas / Series).
 * Proporciona homogeneidad visual absoluta en todas las pantallas principales (Inicio, Marcas, Géneros, Favoritos).
 * Diseño fino, elegante, centrado y más compacto que el menú superior para TV.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FormatFilterBar(
    selected: MediaFormatFilter,
    onSelect: (MediaFormatFilter) -> Unit,
    accentColorKey: String,
    buttonStyleKey: String,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier
) {
    val focusColor = if (isMonochrome) Color.White else TvAccent.getColor(accentColorKey)
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else TvAccent.getFocusedContentColor(accentColorKey)

    val showIcons = buttonStyleKey != BUTTON_STYLE_TEXT_ONLY
    val showText = true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaFormatFilter.entries.forEach { format ->
                val isSelected = selected == format
                Button(
                    onClick = { onSelect(format) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) {
                            if (isMonochrome) Color(0x28FFFFFF) else focusColor.copy(alpha = 0.22f)
                        } else {
                            if (isMonochrome) Color(0x0CFFFFFF) else Color(0x0E00A4DC)
                        },
                        contentColor = if (isSelected) {
                            if (isMonochrome) Color.White else focusColor
                        } else {
                            if (isMonochrome) Color(0xFF94A3B8) else Color(0xFF94A3B8)
                        },
                        focusedContainerColor = focusColor,
                        focusedContentColor = focusContent
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
                    border = if (isSelected) {
                        ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.70f)),
                                shape = RoundedCornerShape(14.dp)
                            ),
                            focusedBorder = Border.None
                        )
                    } else {
                        ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(0.75.dp, if (isMonochrome) Color(0x14FFFFFF) else focusColor.copy(alpha = 0.18f)),
                                shape = RoundedCornerShape(14.dp)
                            ),
                            focusedBorder = Border.None
                        )
                    },
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showIcons) {
                            val icon = when (format) {
                                MediaFormatFilter.ALL -> Icons.Rounded.Apps
                                MediaFormatFilter.MOVIES -> Icons.Rounded.Movie
                                MediaFormatFilter.SERIES -> Icons.Rounded.Tv
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = format.label,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        if (showText) {
                            if (showIcons) Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = format.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
