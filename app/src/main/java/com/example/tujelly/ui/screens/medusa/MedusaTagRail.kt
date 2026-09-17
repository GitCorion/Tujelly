package com.example.tujelly.ui.screens.medusa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

/**
 * Rail horizontal minimalista y sobrio de temáticas para TV.
 * Sin cabeceras redundantes ("temáticas afines sobra"), sin puntos de colores ni números.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MedusaTagRail(
    tags: List<MedusaMosaicTag>,
    onTagClick: (MedusaMosaicTag) -> Unit,
    modifier: Modifier = Modifier,
    firstPillFocusRequester: FocusRequester? = null,
    isMonochrome: Boolean = false,
    accentColor: Color = Color(0xFF00A4DC),
    onTagFocused: (MedusaMosaicTag) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    var focusedTagId by remember { mutableStateOf<String?>(null) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var isRailFocused by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }
    var userJustClickedTag by remember { mutableStateOf(false) }

    // Red de seguridad de foco: cuando la lista de candidatas cambia tras pulsar un tag,
    // asegurar que el foco permanece en el rail sobre la etiqueta que ocupa la siguiente posición disponible,
    // sin escapar jamás a TvTopBar ni a otros contenedores.
    LaunchedEffect(tags, clickCount) {
        if (tags.isNotEmpty() && (isRailFocused || userJustClickedTag)) {
            userJustClickedTag = false
            isRailFocused = true
            val targetIndex = focusedIndex.coerceIn(0, tags.lastIndex)
            val targetTag = tags[targetIndex]
            focusedTagId = targetTag.id
            focusedIndex = targetIndex

            // Intento 1: Inmediato si el nodo ya está compuesto
            try {
                firstPillFocusRequester?.requestFocus()
            } catch (_: Exception) {}

            // Intento 2: Ceder un ciclo de despacho para que Compose termine el layout
            // y vincule los modificadores a los nuevos nodos de la LazyRow
            kotlinx.coroutines.yield()

            try {
                firstPillFocusRequester?.requestFocus()
            } catch (_: Exception) {
                try {
                    focusRequesters[targetTag.id]?.requestFocus()
                } catch (_: Exception) {}
            }

            try {
                listState.animateScrollToItem(targetIndex)
            } catch (_: Exception) {}
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged {
                if (it.hasFocus) {
                    isRailFocused = true
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = tags,
            key = { _, tag -> tag.id }
        ) { index, tag ->
            val itemRequester = focusRequesters.getOrPut(tag.id) { FocusRequester() }

            val targetFocusIndex = focusedIndex.coerceIn(0, (tags.size - 1).coerceAtLeast(0))
            val pillModifier = Modifier
                .focusRequester(itemRequester)
                .then(
                    if (index == targetFocusIndex && firstPillFocusRequester != null) {
                        Modifier.focusRequester(firstPillFocusRequester)
                    } else Modifier
                )

            MedusaTagPill(
                tag = tag,
                onClick = {
                    userJustClickedTag = true
                    clickCount++
                    val nextIndex = if (index < tags.size - 1) index else (index - 1).coerceAtLeast(0)
                    focusedIndex = nextIndex
                    val nextTag = tags.getOrNull(nextIndex)
                    if (nextTag != null) {
                        focusedTagId = nextTag.id
                    }
                    onTagClick(tag)
                },
                modifier = pillModifier,
                isMonochrome = isMonochrome,
                accentColor = accentColor,
                onFocusChange = { focused ->
                    if (focused) {
                        isRailFocused = true
                        focusedTagId = tag.id
                        focusedIndex = index
                        onTagFocused(tag)
                    }
                }
            )
        }
    }
}

/**
 * Píldora de temática sobria, minimalista y elegante para Android TV.
 * Cero puntos de colores, cero números: pura tipografía y cristal obsidian.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MedusaTagPill(
    tag: MedusaMosaicTag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMonochrome: Boolean = false,
    accentColor: Color = Color(0xFF00A4DC),
    onFocusChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
    }

    val focusColor = if (isMonochrome) Color.White else accentColor
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else Color(0xFF0F172A)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "pillScale"
    )

    // Exactamente los mismos estados y colores que FormatFilterBar y TvTopBar
    val containerColor = when {
        isFocused -> focusColor
        tag.isSelected -> if (isMonochrome) Color(0x28FFFFFF) else focusColor.copy(alpha = 0.22f)
        else -> if (isMonochrome) Color(0x0CFFFFFF) else Color(0x0E00A4DC)
    }

    val contentColor = when {
        isFocused -> focusContent
        tag.isSelected -> if (isMonochrome) Color.White else focusColor
        else -> if (isMonochrome) Color(0xFF94A3B8) else Color(0xFFCBD5E1)
    }

    val borderColor = when {
        isFocused -> Color.Transparent
        tag.isSelected -> if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.70f)
        else -> if (isMonochrome) Color(0x14FFFFFF) else focusColor.copy(alpha = 0.18f)
    }

    val borderWidth = if (tag.isSelected) 1.dp else 0.75.dp

    Box(
        modifier = modifier
            .scale(scale)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(containerColor)
            .then(
                if (!isFocused) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(15.dp))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (tag.isSelected) {
                Text(
                    text = "✓",
                    color = contentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = tag.label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (tag.isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
