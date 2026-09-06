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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lienzo astronómico dinámico para la Medusa.
 * Dibuja los trazos conectores únicamente entre las estrellas sugeridas y las seleccionadas,
 * actualizándose en tiempo real según las posiciones de congregación.
 */
@Composable
fun MedusaConstellationView(
    modifier: Modifier = Modifier,
    allStars: List<MedusaStar> = emptyList(),
    visibleStarIds: Set<String> = emptySet(),
    selectedStarIds: Set<String> = emptySet(),
    starPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    accentColor: Color = Color(0xFF38BDF8)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AstroTwinkle")

    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val backgroundStars = remember {
        List(40) {
            CosmicDot(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 1.5f + 0.6f,
                phase = Random.nextFloat() * (2 * PI).toFloat(),
                alpha = Random.nextFloat() * 0.35f + 0.10f
            )
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Estrellas de fondo titilantes
            backgroundStars.forEach { s ->
                val a = (s.alpha + 0.14f * sin(twinklePhase + s.phase)).coerceIn(0.04f, 0.50f)
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = s.radius,
                    center = Offset(s.x * w, s.y * h)
                )
            }

            // 2. Trazos estructurales de fondo de la medusa
            val pApex = Offset(w * 0.50f, h * 0.08f)
            val pL1 = Offset(w * 0.44f, h * 0.11f)
            val pL2 = Offset(w * 0.37f, h * 0.15f)
            val pL3 = Offset(w * 0.31f, h * 0.20f)
            val pL4 = Offset(w * 0.27f, h * 0.28f)
            val pL5 = Offset(w * 0.26f, h * 0.37f)
            val pL6 = Offset(w * 0.30f, h * 0.44f)

            val pR1 = Offset(w * 0.56f, h * 0.11f)
            val pR2 = Offset(w * 0.63f, h * 0.15f)
            val pR3 = Offset(w * 0.69f, h * 0.20f)
            val pR4 = Offset(w * 0.73f, h * 0.28f)
            val pR5 = Offset(w * 0.74f, h * 0.37f)
            val pR6 = Offset(w * 0.70f, h * 0.44f)

            val perimeterStars = listOf(pL6, pL5, pL4, pL3, pL2, pL1, pApex, pR1, pR2, pR3, pR4, pR5, pR6)
            for (i in 0 until perimeterStars.size - 1) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = perimeterStars[i],
                    end = perimeterStars[i + 1],
                    strokeWidth = 0.6f,
                    cap = StrokeCap.Round
                )
            }

            perimeterStars.forEach { pt ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 1.4f,
                    center = pt
                )
            }

            // 3. Conexiones dinámicas entre estrellas visibles y sus relaciones
            val starMap = allStars.associateBy { it.id }
            val drawnPairs = mutableSetOf<String>()

            for (star in allStars) {
                if (!visibleStarIds.contains(star.id)) continue

                val pos1 = starPositions[star.id] ?: Pair(star.baseNormX, star.baseNormY)
                val p1 = Offset(w * pos1.first, h * pos1.second)

                for (relatedId in star.relatedStarIds) {
                    if (!visibleStarIds.contains(relatedId)) continue

                    val pairKey = if (star.id < relatedId) "${star.id}-$relatedId" else "$relatedId-${star.id}"
                    if (drawnPairs.contains(pairKey)) continue
                    drawnPairs.add(pairKey)

                    val relatedStar = starMap[relatedId] ?: continue
                    val pos2 = starPositions[relatedId] ?: Pair(relatedStar.baseNormX, relatedStar.baseNormY)
                    val p2 = Offset(w * pos2.first, h * pos2.second)

                    val isStar1Selected = selectedStarIds.contains(star.id)
                    val isStar2Selected = selectedStarIds.contains(relatedId)

                    if (isStar1Selected && isStar2Selected) {
                        // Ambas seleccionadas: haz estelar brillante que forma el camino iluminado congregado
                        val auraColor = accentColor.copy(alpha = 0.35f * pulseGlow)
                        val beamColor = Color.White.copy(alpha = (0.95f * pulseGlow).coerceIn(0.70f, 1f))
                        drawLine(auraColor, p1, p2, strokeWidth = 5.5f, cap = StrokeCap.Round)
                        drawLine(beamColor, p1, p2, strokeWidth = 2.0f, cap = StrokeCap.Round)
                    } else if (isStar1Selected || isStar2Selected) {
                        // Una seleccionada y la otra sugerida: rayo guía elegante hacia el núcleo
                        val guideColor = Color.White.copy(alpha = 0.45f)
                        drawLine(guideColor, p1, p2, strokeWidth = 1.2f, cap = StrokeCap.Round)
                    } else {
                        // Conexión entre sugerencias visibles
                        val faintColor = Color.White.copy(alpha = 0.18f)
                        drawLine(faintColor, p1, p2, strokeWidth = 0.85f, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

private data class CosmicDot(val x: Float, val y: Float, val radius: Float, val phase: Float, val alpha: Float)
