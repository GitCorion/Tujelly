package com.example.tujelly.ui.screens.medusa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Lienzo de la Gran Galaxia Medusa para TV:
 * - Dibuja cientos de estrellas (tags reales de la biblioteca).
 * - Sol / Agujero Negro Central en (0.5, 0.5) con rayos gravitacionales hacia las etiquetas conectadas.
 * - Poda cósmica en vivo: los nodos sin intersección "AND" desaparecen de la pantalla.
 * - Supernovas luminosas para los tags de los 4 vectores (TMDB Top, Trending, Favoritos, Recientes).
 * - Navegación D-Pad suave optimizada para 60 FPS en Android TV.
 */
@Composable
fun ConstellationCanvas(
    nodes: List<SpatialNebulaNode>,
    filaments: List<SpatialFilament>,
    focusedNodeId: String?,
    activeChain: List<SpatialNebulaNode>,
    targetCameraX: Float,
    targetCameraY: Float,
    targetZoom: Float,
    onNavigateDirection: (DPadDirection) -> Boolean,
    onSelectFocused: () -> Unit,
    onPlayPressed: () -> Unit,
    onNodeClicked: (String) -> Unit,
    onZoomOut: () -> Unit,
    onResetConstellation: (() -> Unit)? = null,
    onRequestFocusBottom: (() -> Unit)? = null,
    onRequestFocusTop: (() -> Unit)? = null,
    compatibleNodeIds: Set<String>? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isMonochrome: Boolean = false,
    particleScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Pulso cósmico sutil para dar vida a la nebulosa
    val infiniteTransition = rememberInfiniteTransition(label = "NebulaContinuousPulse")
    val cosmicDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmicDrift"
    )

    val starlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starlightPulse"
    )

    // Cámara espacial continua amortiguada para TV
    val cameraX = remember { Animatable(targetCameraX) }
    val cameraY = remember { Animatable(targetCameraY) }
    val cameraZoom = remember { Animatable(targetZoom) }

    LaunchedEffect(targetCameraX) {
        cameraX.animateTo(targetCameraX, tween(550, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetCameraY) {
        cameraY.animateTo(targetCameraY, tween(550, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetZoom) {
        cameraZoom.animateTo(targetZoom, tween(600, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val coroutineScope = rememberCoroutineScope()
    var backJob by remember { mutableStateOf<Job?>(null) }
    var backLongPressTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape) {
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (backJob == null && !backLongPressTriggered) {
                            backJob = coroutineScope.launch {
                                delay(500)
                                backLongPressTriggered = true
                                onResetConstellation?.invoke()
                            }
                        }
                        return@onKeyEvent true
                    } else if (keyEvent.type == KeyEventType.KeyUp) {
                        backJob?.cancel()
                        backJob = null
                        if (backLongPressTriggered) {
                            backLongPressTriggered = false
                        } else {
                            onZoomOut()
                        }
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }

                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        onNavigateDirection(DPadDirection.LEFT)
                    }
                    Key.DirectionRight -> {
                        val moved = onNavigateDirection(DPadDirection.RIGHT)
                        if (!moved && onRequestFocusBottom != null) {
                            onRequestFocusBottom()
                            true
                        } else {
                            moved
                        }
                    }
                    Key.DirectionUp -> {
                        val moved = onNavigateDirection(DPadDirection.UP)
                        if (!moved && onRequestFocusTop != null) {
                            onRequestFocusTop()
                            true
                        } else {
                            moved
                        }
                    }
                    Key.DirectionDown -> {
                        val moved = onNavigateDirection(DPadDirection.DOWN)
                        if (!moved && onRequestFocusBottom != null) {
                            onRequestFocusBottom()
                            true
                        } else {
                            moved
                        }
                    }
                    Key.DirectionCenter, Key.Enter -> {
                        onSelectFocused()
                        true
                    }
                    Key.MediaPlay, Key.MediaPlayPause, Key.Spacebar, Key.Menu, Key.F -> {
                        onPlayPressed()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(nodes) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val curZoom = cameraZoom.value
                    val curCamX = cameraX.value
                    val curCamY = cameraY.value

                    val clicked = nodes.firstOrNull { node ->
                        if (compatibleNodeIds != null && node.id !in compatibleNodeIds) return@firstOrNull false
                        val effPos = getNodeWorldPos(node, activeChain, compatibleNodeIds)
                        val p = projectToScreen(effPos.x, effPos.y, w, h, curCamX, curCamY, curZoom)
                        val clickRadius = if (node.isSun || node.id == SUN_CORE_ID) 50.dp.toPx() else 28.dp.toPx()
                        hypot(tapOffset.x - p.x, tapOffset.y - p.y) <= clickRadius
                    }
                    if (clicked != null) {
                        onNodeClicked(clicked.id)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val curZoom = cameraZoom.value
            val curCamX = cameraX.value
            val curCamY = cameraY.value

            // =========================================================================
            // 1. ESPACIO OBSIDIAN Y VELOS CÓSMICOS
            // =========================================================================
            drawRect(color = Color(0xFF05070B))

            val driftX = sin(cosmicDrift) * 22.dp.toPx()
            val driftY = cos(cosmicDrift * 0.8f) * 16.dp.toPx()

            if (isMonochrome) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0EFFFFFF), Color(0x04CBD5E1), Color.Transparent),
                        center = Offset(w * 0.30f + driftX, h * 0.35f + driftY),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.30f + driftX, h * 0.35f + driftY)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x15334155), Color(0x061E293B), Color.Transparent),
                        center = Offset(w * 0.75f - driftX, h * 0.60f - driftY),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.75f - driftX, h * 0.60f - driftY)
                )
            } else {
                // Velo violeta / púrpura cósmico
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x187C3AED), Color(0x074C1D95), Color.Transparent),
                        center = Offset(w * 0.30f + driftX, h * 0.35f + driftY),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.30f + driftX, h * 0.35f + driftY)
                )
                // Velo cian eléctrico
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1800E5FF), Color(0x06005577), Color.Transparent),
                        center = Offset(w * 0.75f - driftX, h * 0.60f - driftY),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.75f - driftX, h * 0.60f - driftY)
                )
                // Velo central cálido alrededor del Sol
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x14F59E0B), Color(0x087C3AED), Color.Transparent),
                        center = Offset(w * 0.50f + driftX * 0.5f, h * 0.50f + driftY * 0.5f),
                        radius = w * 0.40f
                    ),
                    radius = w * 0.40f,
                    center = Offset(w * 0.50f + driftX * 0.5f, h * 0.50f + driftY * 0.5f)
                )
            }

            val nodeMap = nodes.associateBy { it.id }

            // =========================================================================
            // 2. FILAMENTOS SEMÁNTICOS Y RAYOS ENERGÉTICOS HACIA EL SOL CENTRAL
            // =========================================================================
            filaments.forEach { filament ->
                // Poda de filamentos: Si alguno de los dos extremos no sobrevive a la poda, omitir
                if (compatibleNodeIds != null && (filament.fromNodeId !in compatibleNodeIds || filament.toNodeId !in compatibleNodeIds)) {
                    return@forEach
                }

                val from = nodeMap[filament.fromNodeId]
                val to = nodeMap[filament.toNodeId]

                if (from != null && to != null) {
                    val pos1 = getNodeWorldPos(from, activeChain, compatibleNodeIds)
                    val pos2 = getNodeWorldPos(to, activeChain, compatibleNodeIds)
                    val p1 = projectToScreen(pos1.x, pos1.y, w, h, curCamX, curCamY, curZoom)
                    val p2 = projectToScreen(pos2.x, pos2.y, w, h, curCamX, curCamY, curZoom)

                    if (isLineInViewport(p1, p2, w, h)) {
                        val isSunRay = from.isSun || to.isSun || filament.isActiveRay ||
                                filament.fromNodeId == SUN_CORE_ID || filament.toNodeId == SUN_CORE_ID

                        if (isSunRay) {
                            // Rayo de energía pulsante que fluye desde el tag hacia el Sol Central
                            val rayGlow = if (isMonochrome) Color(0x50FFFFFF) else Color(0x6000E5FF)
                            val rayCore = if (isMonochrome) Color.White else Color(0xFFE0F7FA)

                            drawLine(
                                color = rayGlow,
                                start = p1,
                                end = p2,
                                strokeWidth = 4.0.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = rayCore.copy(alpha = 0.85f * starlightPulse),
                                start = p1,
                                end = p2,
                                strokeWidth = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10.dp.toPx(), 6.dp.toPx()),
                                    phase = -cosmicDrift * 20f // Flujo animado hacia el Sol
                                ),
                                cap = StrokeCap.Round
                            )

                            // Paquete de plasma/fotones cósmicos que viaja hacia el Sol
                            val (starPos, sunPos) = if (to.isSun || to.id == SUN_CORE_ID) Pair(p1, p2) else Pair(p2, p1)
                            val photonProgress = ((cosmicDrift * 0.85f) % 1.0f).let { if (it < 0f) it + 1f else it }
                            val photonX = starPos.x + (sunPos.x - starPos.x) * photonProgress
                            val photonY = starPos.y + (sunPos.y - starPos.y) * photonProgress
                            val photonCenter = Offset(photonX, photonY)
                            val photonRadius = 5.5.dp.toPx()

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White,
                                        if (isMonochrome) Color(0x70FFFFFF) else Color(0x9000E5FF),
                                        Color.Transparent
                                    ),
                                    center = photonCenter,
                                    radius = photonRadius
                                ),
                                radius = photonRadius,
                                center = photonCenter
                            )
                        } else {
                            val isConnectedToFocus = from.id == focusedNodeId || to.id == focusedNodeId
                            val stroke = if (isConnectedToFocus) 1.2.dp.toPx() else 0.6.dp.toPx()
                            val color = if (isConnectedToFocus) {
                                if (isMonochrome) Color(0x65FFFFFF) else Color(0x6500E5FF)
                            } else {
                                if (isMonochrome) Color(0x15FFFFFF) else Color(0x1894A3B8)
                            }

                            drawLine(
                                color = color,
                                start = p1,
                                end = p2,
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. CAMINO DE LA CONSTELACIÓN ACTIVA (Cadena conectada)
            // =========================================================================
            if (activeChain.size >= 2) {
                for (i in 0 until activeChain.size - 1) {
                    val fromNode = activeChain[i]
                    val toNode = activeChain[i + 1]

                    val pos1 = getNodeWorldPos(fromNode, activeChain, compatibleNodeIds)
                    val pos2 = getNodeWorldPos(toNode, activeChain, compatibleNodeIds)
                    val p1 = projectToScreen(pos1.x, pos1.y, w, h, curCamX, curCamY, curZoom)
                    val p2 = projectToScreen(pos2.x, pos2.y, w, h, curCamX, curCamY, curZoom)

                    if (isLineInViewport(p1, p2, w, h)) {
                        drawLine(
                            color = if (isMonochrome) Color(0x40FFFFFF) else Color(0x50C084FC),
                            start = p1,
                            end = p2,
                            strokeWidth = 5.0.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = if (isMonochrome) Color.White else Color(0xFFF3E8FF),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // =========================================================================
            // 4. RENDERIZADO DE NODOS CON PODA CÓSMICA EN VIVO
            // =========================================================================
            val pad = 140.dp.toPx()
            val chainMap = activeChain.mapIndexed { idx, n -> n.id to (idx + 1) }.toMap()

            // Poda estricta: Los nodos con 0 coincidencias NO se añaden a la lista de dibujado
            val visibleNodesWithPos = nodes.mapNotNull { node ->
                if (compatibleNodeIds != null && node.id !in compatibleNodeIds) {
                    return@mapNotNull null
                }
                val effPos = getNodeWorldPos(node, activeChain, compatibleNodeIds)
                val p = projectToScreen(effPos.x, effPos.y, w, h, curCamX, curCamY, curZoom)
                if (p.x >= -pad && p.x <= w + pad && p.y >= -pad && p.y <= h + pad) {
                    node to p
                } else null
            }

            val focusedPair = visibleNodesWithPos.firstOrNull { it.first.id == focusedNodeId }
            val focusedScreenPos = focusedPair?.second

            // Partición por capas Z-Index:
            // Capa 1: Nodos secundarios de fondo (sobrevivientes de la poda)
            // Capa 2: Nodos de la cadena activa
            // Capa 3: Sol Central (si no está enfocado)
            // Capa 4: NODO ENFOCADO (Máxima prioridad en primer plano)
            val isSunId = { id: String -> id == SUN_CORE_ID }
            val isSunNode = { n: SpatialNebulaNode -> n.isSun || isSunId(n.id) }

            val backgroundNodes = visibleNodesWithPos.filter { (node, _) ->
                node.id != focusedNodeId && chainMap[node.id] == null && !isSunNode(node)
            }
            val connectedNodes = visibleNodesWithPos.filter { (node, _) ->
                node.id != focusedNodeId && chainMap[node.id] != null && !isSunNode(node)
            }
            val sunNodePair = visibleNodesWithPos.firstOrNull { (node, _) ->
                isSunNode(node) && node.id != focusedNodeId
            }

            val sunScreenPos = sunNodePair?.second ?: if (focusedPair != null && isSunNode(focusedPair.first)) focusedPair.second else null

            // Capa 1: Nodos Satélites de fondo (orbes estelares sin caja rectangular)
            val satellites = backgroundNodes.filter { !it.first.isCaptain }
            satellites.forEach { (node, p) ->
                val isNearFocused = focusedScreenPos != null &&
                        hypot(p.x - focusedScreenPos.x, p.y - focusedScreenPos.y) < 70.dp.toPx()
                val isNearSun = sunScreenPos != null &&
                        hypot(p.x - sunScreenPos.x, p.y - sunScreenPos.y) < 95.dp.toPx()

                drawTagNode(
                    node = node,
                    screenPos = p,
                    currentZoom = curZoom,
                    isFocused = false,
                    chainOrder = null,
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome,
                    suppressLabel = isNearFocused || isNearSun,
                    particleScale = particleScale
                )
            }

            // Capa 2: Estrellas Capitanas de fondo (faros rectores de cada sector)
            val captains = backgroundNodes.filter { it.first.isCaptain }
            captains.forEach { (node, p) ->
                val isNearFocused = focusedScreenPos != null &&
                        hypot(p.x - focusedScreenPos.x, p.y - focusedScreenPos.y) < 70.dp.toPx()
                val isNearSun = sunScreenPos != null &&
                        hypot(p.x - sunScreenPos.x, p.y - sunScreenPos.y) < 95.dp.toPx()

                drawTagNode(
                    node = node,
                    screenPos = p,
                    currentZoom = curZoom,
                    isFocused = false,
                    chainOrder = null,
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome,
                    suppressLabel = isNearFocused || isNearSun,
                    particleScale = particleScale
                )
            }

            // Capa 3: Nodos conectados
            connectedNodes.forEach { (node, p) ->
                drawTagNode(
                    node = node,
                    screenPos = p,
                    currentZoom = curZoom,
                    isFocused = false,
                    chainOrder = chainMap[node.id],
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome,
                    suppressLabel = false,
                    particleScale = particleScale
                )
            }

            // Capa 4: Sol Central si no está enfocado
            if (sunNodePair != null) {
                drawSunCoreNode(
                    node = sunNodePair.first,
                    screenPos = sunNodePair.second,
                    isFocused = false,
                    cosmicRotation = cosmicDrift,
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome
                )
            }

            // Capa 4: NODO ENFOCADO (En primer plano absoluto)
            if (focusedPair != null) {
                val (fNode, fPos) = focusedPair
                if (isSunNode(fNode)) {
                    drawSunCoreNode(
                        node = fNode,
                        screenPos = fPos,
                        isFocused = true,
                        cosmicRotation = cosmicDrift,
                        starlightPulse = starlightPulse,
                        textMeasurer = textMeasurer,
                        isMonochrome = isMonochrome
                    )
                } else {
                    drawTagNode(
                        node = fNode,
                        screenPos = fPos,
                        currentZoom = curZoom,
                        isFocused = true,
                        chainOrder = chainMap[fNode.id],
                        starlightPulse = starlightPulse,
                        textMeasurer = textMeasurer,
                        isMonochrome = isMonochrome,
                        suppressLabel = false,
                        particleScale = particleScale
                    )
                }
            }
        }
    }
}

/**
 * Renderizado de una Estrella / Tag en el firmamento.
 * Soporta Supernovas (nodos de alta relevancia provenientes del TMDB Top/Trending/Favoritos).
 */
private fun DrawScope.drawTagNode(
    node: SpatialNebulaNode,
    screenPos: Offset,
    currentZoom: Float,
    isFocused: Boolean,
    chainOrder: Int?,
    starlightPulse: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    isMonochrome: Boolean = false,
    suppressLabel: Boolean = false,
    particleScale: Float = 1.0f
) {
    val isConnected = chainOrder != null
    val isCaptain = node.isCaptain
    val showBadge = isFocused || isConnected || isCaptain
    val categoryColor = getCategoryColor(node.category, isMonochrome)

    // =========================================================================
    // CASO 1: ESTRELLAS SATÉLITE EN REPOSO (Luz celestial pura sin caja)
    // =========================================================================
    if (!showBadge) {
        val isSupernova = node.importance >= 1.6f
        val starRadius = (if (isSupernova) 3.6.dp else 2.5.dp).toPx()
        val haloRadius = (if (isSupernova) 12.dp else 7.dp).toPx()

        // Halo suave sutil
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(categoryColor.copy(alpha = 0.35f * starlightPulse), Color.Transparent),
                center = screenPos,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = screenPos
        )

        // Núcleo estelar
        drawCircle(
            color = Color.White.copy(alpha = 0.88f * starlightPulse),
            radius = starRadius,
            center = screenPos
        )

        // Texto tenue flotante sin caja ni bordes, solo si no está suprimido
        if (!suppressLabel && currentZoom >= 0.75f) {
            val textLayout = textMeasurer.measure(
                text = node.label,
                style = TextStyle(
                    color = Color(0x60FFFFFF),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.3.sp
                )
            )
            val tx = screenPos.x - textLayout.size.width / 2f
            val ty = screenPos.y + starRadius + 2.dp.toPx()
            drawText(textLayoutResult = textLayout, topLeft = Offset(tx, ty))
        }
        return
    }

    // =========================================================================
    // CASO 2: NODOS CON BADGE (Capitanas, Conectadas o Enfocada)
    // =========================================================================
    val labelText = when {
        chainOrder != null -> "$chainOrder. ${node.label}"
        isFocused -> node.label
        isCaptain -> "✦ ${node.label}"
        else -> node.label
    }

    val fontSize = when {
        isFocused -> 12.sp
        isConnected -> 11.sp
        isCaptain -> 10.sp
        else -> 9.5.sp
    }

    val textColor = when {
        isFocused -> if (isMonochrome) Color.White else Color(0xFF05070B)
        isConnected -> if (isMonochrome) Color.White else Color(0xFFF3E8FF)
        isCaptain -> if (isMonochrome) Color.White else Color(0xFFE0F2FE)
        else -> Color.White
    }

    val textLayout = textMeasurer.measure(
        text = labelText,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (isFocused || isConnected || isCaptain) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = if (isFocused) 0.8.sp else 0.5.sp
        )
    )

    val padX = if (isFocused) 13.dp.toPx() else 8.dp.toPx()
    val padY = if (isFocused) 6.dp.toPx() else 3.5.dp.toPx()
    val badgeW = textLayout.size.width + padX * 2
    val badgeH = textLayout.size.height + padY * 2

    // El badge está centrado exactamente en screenPos (elimina el punto desfasado arriba)
    val badgeX = screenPos.x - badgeW / 2f
    val badgeY = screenPos.y - badgeH / 2f

    // Halo luminoso según estado
    if (isFocused) {
        val haloR = badgeW * 0.70f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isMonochrome) Color(0x60FFFFFF) else Color(0x7500E5FF),
                    Color.Transparent
                ),
                center = screenPos,
                radius = haloR
            ),
            radius = haloR,
            center = screenPos
        )

        // Onda expansiva gravitacional de radar / sonar estelar
        val waveT = ((starlightPulse * 1.4f) % 1.0f).let { if (it < 0f) it + 1f else it }
        val waveR = badgeW * 0.55f + waveT * 26.dp.toPx()
        val waveAlpha = (1.0f - waveT) * 0.45f
        drawCircle(
            color = (if (isMonochrome) Color.White else Color(0xFF00E5FF)).copy(alpha = waveAlpha),
            radius = waveR,
            center = screenPos,
            style = Stroke(width = 1.4.dp.toPx())
        )
    } else if (isConnected) {
        val haloR = badgeW * 0.60f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isMonochrome) Color(0x40FFFFFF) else Color(0x50C084FC),
                    Color.Transparent
                ),
                center = screenPos,
                radius = haloR
            ),
            radius = haloR,
            center = screenPos
        )
    } else if (isCaptain) {
        val haloR = badgeW * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    categoryColor.copy(alpha = 0.30f * starlightPulse),
                    Color.Transparent
                ),
                center = screenPos,
                radius = haloR
            ),
            radius = haloR,
            center = screenPos
        )
    }

    // Oclusión oscura previa
    drawRoundRect(
        color = Color(0xF8030508),
        topLeft = Offset(badgeX - 3.dp.toPx(), badgeY - 2.dp.toPx()),
        size = Size(badgeW + 6.dp.toPx(), badgeH + 4.dp.toPx()),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Fill
    )

    // Cápsula principal
    val capsuleBg = when {
        isFocused -> if (isMonochrome) Color.White else Color(0xFF00E5FF)
        isConnected -> if (isMonochrome) Color(0xF01E293B) else Color(0xF01A0E2E)
        isCaptain -> if (isMonochrome) Color(0xD01E293B) else Color(0xD00A182E)
        else -> Color(0xD008101C)
    }

    drawRoundRect(
        color = capsuleBg,
        topLeft = Offset(badgeX, badgeY),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
        style = Fill
    )

    // Borde
    val borderColor = when {
        isFocused -> Color.White
        isConnected -> if (isMonochrome) Color.White else Color(0xFFC084FC)
        isCaptain -> if (isMonochrome) Color(0x90FFFFFF) else Color(0x9038BDF8)
        else -> Color(0x40FFFFFF)
    }

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(badgeX, badgeY),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
        style = Stroke(width = if (isFocused) 2.2.dp.toPx() else 1.0.dp.toPx())
    )

    // Texto de la temática
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(badgeX + padX, badgeY + padY)
    )

    // Sub-etiqueta de obras disponibles al estar enfocado
    if (isFocused) {
        val countText = "✦ ${node.movieCount} TÍTULOS"
        val countLayout = textMeasurer.measure(
            text = countText,
            style = TextStyle(
                color = if (isMonochrome) Color.White else Color(0xFF00E5FF),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.0.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val countX = screenPos.x - countLayout.size.width / 2f
        val countY = badgeY + badgeH + 4.dp.toPx()

        drawRoundRect(
            color = Color(0xF505070B),
            topLeft = Offset(countX - 6.dp.toPx(), countY - 2.dp.toPx()),
            size = Size(countLayout.size.width + 12.dp.toPx(), countLayout.size.height + 4.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Fill
        )
        drawText(
            textLayoutResult = countLayout,
            topLeft = Offset(countX, countY)
        )
    }
}

/**
 * Renderizado del Sol / Agujero Negro Central (SUN_CORE_ID).
 * Esfera gravitacional majestuosa con corona ardiente, partículas rotatorias y badge TV interactivo.
 */
private fun DrawScope.drawSunCoreNode(
    node: SpatialNebulaNode,
    screenPos: Offset,
    isFocused: Boolean,
    cosmicRotation: Float,
    starlightPulse: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    isMonochrome: Boolean = false
) {
    val coreRadius = (if (isFocused) 26.dp else 20.dp).toPx()
    val haloRadius = (if (isFocused) 85.dp else 60.dp).toPx()

    // 1. Resplandor exterior cósmico ardiente (Corona Solar / Disco de Acreción)
    val coronaColors = if (isMonochrome) {
        listOf(
            if (isFocused) Color(0x75FFFFFF) else Color(0x40FFFFFF),
            if (isFocused) Color(0x30CBD5E1) else Color(0x1864748B),
            Color.Transparent
        )
    } else {
        if (node.movieCount > 0) {
            listOf(
                if (isFocused) Color(0x9000E5FF) else Color(0x5500E5FF),
                if (isFocused) Color(0x457C3AED) else Color(0x25C084FC),
                Color.Transparent
            )
        } else {
            listOf(
                if (isFocused) Color(0x8500E5FF) else Color(0x50F59E0B),
                if (isFocused) Color(0x407C3AED) else Color(0x25EF4444),
                Color.Transparent
            )
        }
    }

    val pulseScale = 0.95f + 0.10f * sin(cosmicRotation * 2.2f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = coronaColors,
            center = screenPos,
            radius = haloRadius * pulseScale
        ),
        radius = haloRadius * pulseScale,
        center = screenPos
    )

    // 2. Anillo orbital primario rotatorio punteado (Disco de Acreción Exterior)
    val ringRadius1 = coreRadius * 1.65f
    val ringColor1 = if (isMonochrome) {
        if (isFocused) Color.White else Color(0xFFE2E8F0)
    } else {
        if (isFocused) Color(0xFF00E5FF) else (if (node.movieCount > 0) Color(0xFF38BDF8) else Color(0xFFFBBF24))
    }
    drawCircle(
        color = ringColor1.copy(alpha = 0.88f * starlightPulse),
        radius = ringRadius1,
        center = screenPos,
        style = Stroke(
            width = (if (isFocused) 2.4.dp else 1.5.dp).toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(13.dp.toPx(), 7.dp.toPx()),
                phase = cosmicRotation * 22f
            )
        )
    )

    // 2b. Anillo orbital secundario contra-rotatorio (Disco de Acreción Interior)
    val ringRadius2 = coreRadius * 1.30f
    val ringColor2 = if (isMonochrome) {
        Color(0x80FFFFFF)
    } else {
        if (isFocused) Color(0xFFC084FC) else Color(0xFFF59E0B)
    }
    drawCircle(
        color = ringColor2.copy(alpha = 0.65f),
        radius = ringRadius2,
        center = screenPos,
        style = Stroke(
            width = 1.0.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(5.dp.toPx(), 8.dp.toPx()),
                phase = -cosmicRotation * 17f
            )
        )
    )

    // 3. Núcleo Estelar de Fusión
    val coreColors = if (isMonochrome) {
        listOf(
            Color.White,
            if (isFocused) Color.White else Color(0xFFCBD5E1),
            if (isFocused) Color(0xFF94A3B8) else Color(0xFF334155)
        )
    } else {
        if (node.movieCount > 0) {
            listOf(
                Color.White,
                if (isFocused) Color(0xFFE0F7FA) else Color(0xFF67E8F9),
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF0284C7)
            )
        } else {
            listOf(
                Color.White,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFFFDE68A),
                if (isFocused) Color(0xFF0288D1) else Color(0xFFD97706)
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = coreColors,
            center = screenPos,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = screenPos
    )

    // 4. Badge / Cápsula interactiva del Sol
    val labelText = if (isFocused && node.movieCount > 0) "${node.label}  ➔ [PULSA OK]" else node.label
    val badgeTextColor = if (isMonochrome) {
        if (isFocused) Color(0xFF05070B) else Color.White
    } else {
        if (isFocused) Color(0xFF05070B) else Color(0xFFFEF3C7)
    }

    val textLayout = textMeasurer.measure(
        text = labelText,
        style = TextStyle(
            color = badgeTextColor,
            fontSize = if (isFocused) 13.sp else 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (isFocused) 1.3.sp else 0.9.sp
        )
    )

    val labelX = screenPos.x - textLayout.size.width / 2f
    val labelY = screenPos.y + ringRadius1 + (if (isFocused) 7.dp.toPx() else 5.dp.toPx())
    val padX = 12.dp.toPx()
    val padY = 5.dp.toPx()

    // Oclusión oscura previa
    drawRoundRect(
        color = Color(0xF8030508),
        topLeft = Offset(labelX - padX - 4.dp.toPx(), labelY - padY - 2.dp.toPx()),
        size = Size(textLayout.size.width + (padX + 4.dp.toPx()) * 2, textLayout.size.height + (padY + 2.dp.toPx()) * 2),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        style = Fill
    )

    val badgeBgColor = if (isMonochrome) {
        if (isFocused) Color.White else Color(0xF01E293B)
    } else {
        if (isFocused) Color(0xFF00E5FF) else Color(0xF02A1A05)
    }
    drawRoundRect(
        color = badgeBgColor,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Fill
    )

    val badgeBorderColor = if (isMonochrome) {
        if (isFocused) Color.White else Color(0x60FFFFFF)
    } else {
        if (isFocused) Color.White else Color(0xFFF59E0B).copy(alpha = 0.85f)
    }
    drawRoundRect(
        color = badgeBorderColor,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Stroke(width = (if (isFocused) 2.0.dp else 1.2.dp).toPx())
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(labelX, labelY)
    )
}

private fun getCategoryColor(category: String, isMonochrome: Boolean = false): Color {
    if (isMonochrome) {
        val monoPalette = listOf(Color(0xFFE2E8F0), Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF94A3B8))
        return monoPalette[kotlin.math.abs(category.hashCode()) % monoPalette.size]
    }
    val palette = listOf(
        Color(0xFF00E5FF), // Electric Cyan
        Color(0xFF818CF8), // Indigo
        Color(0xFFF43F5E), // Rose / Crimson
        Color(0xFF38BDF8), // Sky Blue
        Color(0xFFA78BFA), // Violet
        Color(0xFFFBBF24), // Amber Gold
        Color(0xFF34D399), // Emerald
        Color(0xFFFB7185), // Coral Red
        Color(0xFFE879F9), // Fuchsia
        Color(0xFF2DD4BF)  // Teal
    )
    return palette[kotlin.math.abs(category.hashCode()) % palette.size]
}

private fun getNodeWorldPos(
    node: SpatialNebulaNode,
    activeChain: List<SpatialNebulaNode>,
    compatibleNodeIds: Set<String>?
): Offset {
    // Si es el Sol Central, su posición es el centro inmutable (0.5, 0.5)
    if (node.isSun || node.id == SUN_CORE_ID) {
        return Offset(0.50f, 0.50f)
    }
    return Offset(node.worldX, node.worldY)
}

private fun projectToScreen(
    worldX: Float,
    worldY: Float,
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    zoom: Float
): Offset {
    val sx = w * 0.5f + (worldX - camX) * w * zoom
    val sy = h * 0.5f + (worldY - camY) * h * zoom
    return Offset(sx, sy)
}

private fun isLineInViewport(p1: Offset, p2: Offset, w: Float, h: Float): Boolean {
    val margin = 80f
    val minX = minOf(p1.x, p2.x)
    val maxX = maxOf(p1.x, p2.x)
    val minY = minOf(p1.y, p2.y)
    val maxY = maxOf(p1.y, p2.y)
    return maxX >= -margin && minX <= w + margin && maxY >= -margin && minY <= h + margin
}
