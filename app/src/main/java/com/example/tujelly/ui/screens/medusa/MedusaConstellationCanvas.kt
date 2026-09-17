package com.example.tujelly.ui.screens.medusa

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cinta de Constelación Compacta y Sobria para Medusa 2.0.
 *
 * Muestra la cadena activa de temáticas como estrellas de luz conectadas horizontalmente,
 * ocupando una altura mínima y dejando todo el protagonismo de la pantalla a las películas.
 */
@Composable
fun MedusaConstellationCanvas(
    activeChain: List<MedusaMosaicTag>,
    matchingCount: Int,
    formatNounPlural: String,
    isMonochrome: Boolean = false,
    accentColor: Color = Color(0xFF00A4DC),
    modifier: Modifier = Modifier
) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "MedusaConstellationRibbon")

    val starlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starlightPulse"
    )

    val filamentPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "filamentPhase"
    )

    val activeColor = remember(isMonochrome, accentColor) {
        if (isMonochrome) Color.White else accentColor
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (activeChain.isEmpty()) {
                // Estado en reposo: sutil indicación tipográfica sin estorbos
                val guidanceText = "✦ SELECCIONA UNA O VARIAS TEMÁTICAS PARA FORMAR TU CONSTELACIÓN"
                val guidanceStyle = TextStyle(
                    color = if (isMonochrome) Color(0x66FFFFFF) else activeColor.copy(alpha = 0.60f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.8.sp
                )
                val measured = textMeasurer.measure(guidanceText, guidanceStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = guidanceText,
                    style = guidanceStyle,
                    topLeft = Offset((w - measured.size.width) / 2f, (h - measured.size.height) / 2f)
                )
            } else {
                // Estado activo: distribución horizontal compacta de las estrellas conectadas
                val count = activeChain.size
                val centerY = h / 2f
                val paddingStart = 48.dp.toPx()
                val availableWidth = w - (paddingStart * 2)
                val step = if (count > 1) availableWidth / (count + 0.5f) else availableWidth / 2f

                val positions = activeChain.mapIndexed { i, _ ->
                    Offset(paddingStart + (i * step) + 60.dp.toPx(), centerY)
                }

                // A. Filamentos finos entre estrellas consecutivas
                for (i in 0 until positions.size - 1) {
                    val p1 = positions[i]
                    val p2 = positions[i + 1]

                    drawLine(
                        color = activeColor.copy(alpha = 0.40f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Partícula luminosa viajera
                    val tx = p1.x + (p2.x - p1.x) * filamentPhase
                    val ty = p1.y + (p2.y - p1.y) * filamentPhase
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        radius = 2.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }

                // B. Estrellas de la constelación con su etiqueta
                activeChain.forEachIndexed { i, tag ->
                    val pos = positions[i]

                    // Anillo fino de estrella
                    drawCircle(
                        color = activeColor.copy(alpha = 0.70f * starlightPulse),
                        radius = 6.dp.toPx() * starlightPulse,
                        center = pos,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Núcleo blanco
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pos
                    )

                    // Etiqueta tipográfica al lado de la estrella
                    val labelText = "${i + 1}. ${tag.label.uppercase()}"
                    val labelStyle = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.4.sp
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelText,
                        style = labelStyle,
                        topLeft = Offset(pos.x + 12.dp.toPx(), pos.y - 7.dp.toPx())
                    )
                }
            }
        }
    }
}
