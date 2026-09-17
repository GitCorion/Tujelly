package com.example.tujelly.ui.screens.medusa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

object MedusaCategoryColors {
    val Cyan = Color(0xFF00A4DC)
    val SkyBlue = Color(0xFF38BDF8)
    val Violet = Color(0xFF7C3AED)
    val Purple = Color(0xFF8B5CF6)
    val Indigo = Color(0xFF6366F1)

    fun getColorFor(category: String, isMonochrome: Boolean = false): Color {
        if (isMonochrome) return Color.White
        // Solo azulitos y morados (identidad oficial TuJelly), cero colorines
        return when (category) {
            "Acción", "Aventura", "Ciencia Ficción" -> Cyan
            "Terror", "Fantasía", "Animación" -> Violet
            "Comedia", "Romance", "Musical" -> Purple
            "Drama", "Crimen", "Documental" -> SkyBlue
            else -> Indigo
        }
    }
}

/**
 * Cápsula de temática ultra-elegante con cristal obsidian, micro-animación de escala y foco para Android TV.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MedusaMosaicChip(
    tag: MedusaMosaicTag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMonochrome: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    androidx.compose.runtime.LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "mosaicChipScale"
    )

    val categoryColor = remember(tag.category, isMonochrome) {
        MedusaCategoryColors.getColorFor(tag.category, isMonochrome)
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            tag.isSelected -> if (isMonochrome) Color.White else Color(0xFF00E5FF)
            else -> categoryColor.copy(alpha = 0.35f)
        },
        label = "mosaicBorderColor"
    )

    val borderWidth = if (isFocused) 2.dp else if (tag.isSelected) 1.4.dp else 0.8.dp

    val backgroundBrush = remember(isFocused, tag.isSelected, categoryColor) {
        when {
            isFocused && tag.isSelected -> Brush.horizontalGradient(
                listOf(categoryColor.copy(alpha = 0.6f), Color(0x661E1B4B))
            )
            isFocused -> Brush.horizontalGradient(
                listOf(Color(0x38FFFFFF), Color(0x20334155))
            )
            tag.isSelected -> Brush.horizontalGradient(
                listOf(categoryColor.copy(alpha = 0.35f), Color(0x400F172A))
            )
            else -> Brush.horizontalGradient(
                listOf(Color(0x22131926), Color(0x180A0E17))
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Izquierdo: Punto / Icono bioluminiscente + Nombre del tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (tag.isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isMonochrome) Color.White else Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF05070B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = tag.label,
                    color = when {
                        isFocused -> Color.White
                        tag.isSelected -> if (isMonochrome) Color.White else Color(0xFFF0FDF4)
                        else -> Color(0xFFE2E8F0)
                    },
                    fontSize = 13.sp,
                    fontWeight = if (tag.isSelected || isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Lado Derecho: Píldora con el conteo de obras que tienen esta etiqueta
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isFocused) Color(0x35000000)
                        else if (tag.isSelected) categoryColor.copy(alpha = 0.25f)
                        else Color(0x20FFFFFF)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${tag.movieCount}",
                    color = when {
                        isFocused -> Color.White
                        tag.isSelected -> if (isMonochrome) Color.White else Color(0xFF00E5FF)
                        else -> Color(0xBB94A3B8)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
