package com.example.tujelly.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tujelly.R

@Composable
fun JellyLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    message: String? = null,
    isMonochrome: Boolean = false
) {
    val symbolRes = if (isMonochrome) R.drawable.ic_jelly_symbol_mono else R.drawable.ic_jelly_symbol
    val infiniteTransition = rememberInfiniteTransition(label = "JellySwim")

    // Realistic biological jellyfish propulsion cycle (2200ms total)
    // 1. Upward thrust (fast acceleration, bell narrows & lengthens)
    // 2. Bell blooms wide at the crest
    // 3. Slow, tranquil downward drift through the water
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                8f at 0 using FastOutSlowInEasing
                -14f at 650 using FastOutLinearInEasing // powerful upward thrust
                -12f at 1100 using LinearOutSlowInEasing // gentle coasting at apex
                8f at 2200 using FastOutSlowInEasing // slow fluid descent
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "JellyThrustY"
    )

    // Bell horizontal contraction (contracts narrow during thrust, blooms wide at crest)
    val scaleX by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                1.0f at 0
                0.86f at 380 using FastOutSlowInEasing // compresses narrow as water is expelled
                1.12f at 900 using FastOutSlowInEasing // bell opens wide like an umbrella
                1.0f at 2200 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "JellyBellX"
    )

    // Bell vertical elongation (stretches tall when jetting, flattens slightly when opening)
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                1.0f at 0
                1.16f at 380 using FastOutSlowInEasing // stretches upward
                0.90f at 900 using FastOutSlowInEasing // relaxes wide
                1.0f at 2200 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "JellyBellY"
    )

    // Bioluminescent glow pulse: surges with bright energy during the propulsion thrust
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                0.65f at 0
                1.0f at 380 using LinearEasing // bright pulse of neon light during thrust
                0.80f at 1100
                0.65f at 2200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "JellyGlow"
    )

    // Gentle aquatic tilt: swaying from side to side in the current
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "JellyRotate"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Soft glowing ambient halo behind the jellyfish
            Image(
                painter = painterResource(id = symbolRes),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        this.translationY = offsetY * density
                        this.scaleX = scaleX * 1.15f
                        this.scaleY = scaleY * 1.15f
                        this.rotationZ = rotationAngle
                        this.alpha = glowAlpha * 0.35f
                    }
            )

            // Main crisp bioluminescent jellyfish RenderNode
            Image(
                painter = painterResource(id = symbolRes),
                contentDescription = "Cargando",
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        this.translationY = offsetY * density
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.rotationZ = rotationAngle
                        this.alpha = glowAlpha
                    }
            )
        }

        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
