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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Lienzo de Constelación Semántica Continua para TV:
 * - Dibuja cientos de etiquetas cinematográficas interconectadas como una nebulosa interestelar.
 * - Conecta etiquetas sucesivas en un sendero luminoso estelar.
 * - Cámara suave que mantiene centrada la etiqueta activa en la pantalla.
 * - Nivel de Detalle (LOD) dinámico optimizado para 60 FPS en Android TV.
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

    // Cámara espacial continua: amortiguación cinematográfica
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
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
                    Key.Back, Key.Escape -> {
                        onZoomOut()
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

                    val clicked = nodes.minByOrNull { node ->
                        val sx = w * 0.5f + (node.worldX - curCamX) * w * curZoom
                        val sy = h * 0.5f + (node.worldY - curCamY) * h * curZoom
                        hypot(sx - tapOffset.x, sy - tapOffset.y)
                    }

                    if (clicked != null) {
                        val sx = w * 0.5f + (clicked.worldX - curCamX) * w * curZoom
                        val sy = h * 0.5f + (clicked.worldY - curCamY) * h * curZoom
                        if (hypot(sx - tapOffset.x, sy - tapOffset.y) <= 50.dp.toPx()) {
                            onNodeClicked(clicked.id)
                        }
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

            val driftX = sin(cosmicDrift) * 25.dp.toPx()
            val driftY = cos(cosmicDrift * 0.8f) * 18.dp.toPx()

            if (isMonochrome) {
                // Velo plateado etéreo / niebla estelar blanca
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0EFFFFFF), Color(0x04CBD5E1), Color.Transparent),
                        center = Offset(w * 0.30f + driftX, h * 0.35f + driftY),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.30f + driftX, h * 0.35f + driftY)
                )

                // Velo grafito / slate profundo
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
                // Tema Original TuJelly (Bioluminiscente neón a juego con el logo)
                // Velo violeta / púrpura neón
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

                // Velo índigo / azul noche
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x121E1B4B), Color.Transparent),
                        center = Offset(w * 0.50f + driftX * 0.7f, h * 0.75f - driftY * 0.7f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.50f + driftX * 0.7f, h * 0.75f - driftY * 0.7f)
                )
            }

            val nodeMap = nodes.associateBy { it.id }

            // =========================================================================
            // 2. FILAMENTOS SEMÁNTICOS AMBIENTALES Y LÍNEAS AL PORTAL
            // =========================================================================
            filaments.forEach { filament ->
                val from = nodeMap[filament.fromNodeId]
                val to = nodeMap[filament.toNodeId]

                if (from != null && to != null) {
                    val pos1 = getNodeWorldPos(from, activeChain, compatibleNodeIds)
                    val pos2 = getNodeWorldPos(to, activeChain, compatibleNodeIds)
                    val p1 = projectToScreen(pos1.x, pos1.y, w, h, curCamX, curCamY, curZoom)
                    val p2 = projectToScreen(pos2.x, pos2.y, w, h, curCamX, curCamY, curZoom)

                    if (isLineInViewport(p1, p2, w, h)) {
                        val isPortalFilament = from.isPortal || to.isPortal || filament.fromNodeId == PORTAL_NODE_ID || filament.toNodeId == PORTAL_NODE_ID
                        val isConnectedToFocus = from.id == focusedNodeId || to.id == focusedNodeId

                        if (isPortalFilament) {
                            // Filamento de haz energético hacia el portal de películas
                            val portalGlow = if (isMonochrome) Color(0x35FFFFFF) else Color(0x40C084FC)
                            val portalPulse = if (isMonochrome) Color(0xCCFFFFFF) else Color(0xCCE9D5FF)

                            drawLine(
                                color = portalGlow,
                                start = p1,
                                end = p2,
                                strokeWidth = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = portalPulse.copy(alpha = 0.75f * starlightPulse),
                                start = p1,
                                end = p2,
                                strokeWidth = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), phase = cosmicDrift * 15f),
                                cap = StrokeCap.Round
                            )
                        } else {
                            val stroke = if (isConnectedToFocus) 1.0.dp.toPx() else 0.5.dp.toPx()
                            val color = if (isConnectedToFocus) {
                                if (isMonochrome) Color(0x55FFFFFF) else Color(0x5500E5FF)
                            } else {
                                if (isMonochrome) Color(0x10FFFFFF) else Color(0x1294A3B8)
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
            // 3. CAMINO DE LA CONSTELACIÓN ACTIVA (Etiquetas que el usuario ha conectado)
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
                        // Pase 1: Halo de resplandor exterior suave
                        drawLine(
                            color = if (isMonochrome) Color(0x35FFFFFF) else Color(0x4000E5FF),
                            start = p1,
                            end = p2,
                            strokeWidth = 5.0.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Pase 2: Cordón de luz estelar interior nítido
                        drawLine(
                            color = if (isMonochrome) Color.White else Color(0xFFE0F7FA),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Vista previa de conexión: cordón punteado desde el último nodo conectado al enfocado
            val focusedNode = nodeMap[focusedNodeId]
            if (activeChain.isNotEmpty() && focusedNode != null && !focusedNode.isPortal && activeChain.none { it.id == focusedNode.id }) {
                val lastConnected = activeChain.last()
                val pos1 = getNodeWorldPos(lastConnected, activeChain, compatibleNodeIds)
                val pos2 = getNodeWorldPos(focusedNode, activeChain, compatibleNodeIds)
                val p1 = projectToScreen(pos1.x, pos1.y, w, h, curCamX, curCamY, curZoom)
                val p2 = projectToScreen(pos2.x, pos2.y, w, h, curCamX, curCamY, curZoom)

                if (isLineInViewport(p1, p2, w, h)) {
                    drawLine(
                        color = (if (isMonochrome) Color(0x77FFFFFF) else Color(0x7700E5FF)).copy(alpha = 0.5f * starlightPulse),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                        cap = StrokeCap.Round
                    )
                }
            }

            // =========================================================================
            // 4. RENDERIZADO DE NODOS POR CAPAS Z-INDEX ("VENIR AL FRENTE SIN MIEDO")
            // =========================================================================
            val pad = 140.dp.toPx()
            val chainMap = activeChain.mapIndexed { idx, n -> n.id to (idx + 1) }.toMap()

            // 1. Calcular posiciones en pantalla de todos los nodos en el viewport (con atracción gravitacional de nodos compatibles)
            val visibleNodesWithPos = nodes.mapNotNull { node ->
                val effPos = getNodeWorldPos(node, activeChain, compatibleNodeIds)
                val p = projectToScreen(effPos.x, effPos.y, w, h, curCamX, curCamY, curZoom)
                if (p.x >= -pad && p.x <= w + pad && p.y >= -pad && p.y <= h + pad) {
                    node to p
                } else null
            }

            val focusedPair = visibleNodesWithPos.firstOrNull { it.first.id == focusedNodeId }
            val focusedScreenPos = focusedPair?.second

            // 2. Partición estricta por capas Z-Index:
            // Capa 1: Nodos secundarios de fondo (no conectados ni enfocados)
            // Capa 2: Nodos de la cadena activa (conectados, excepto el enfocado)
            // Capa 3: Nodo Portal (si no está enfocado)
            // Capa 4 (TOP ABSOLUTO): NODO ENFOCADO
            val backgroundNodes = visibleNodesWithPos.filter { (node, _) ->
                node.id != focusedNodeId && chainMap[node.id] == null && !node.isPortal && node.id != PORTAL_NODE_ID
            }
            val connectedNodes = visibleNodesWithPos.filter { (node, _) ->
                node.id != focusedNodeId && chainMap[node.id] != null && !node.isPortal && node.id != PORTAL_NODE_ID
            }
            val portalNode = visibleNodesWithPos.firstOrNull { (node, _) ->
                (node.isPortal || node.id == PORTAL_NODE_ID) && node.id != focusedNodeId
            }

            val portalScreenPos = portalNode?.second ?: if (focusedPair?.first?.isPortal == true || focusedPair?.first?.id == PORTAL_NODE_ID) focusedPair?.second else null

            // Capa 1: Nodos de fondo (con desvanecimiento de etiquetas cercanas al foco o al portal para no ensuciar)
            backgroundNodes.forEach { (node, p) ->
                val isNearFocused = focusedScreenPos != null &&
                        hypot(p.x - focusedScreenPos.x, p.y - focusedScreenPos.y) < 85.dp.toPx()
                val isNearPortal = portalScreenPos != null &&
                        hypot(p.x - portalScreenPos.x, p.y - portalScreenPos.y) < 130.dp.toPx()
                val isCompatible = compatibleNodeIds == null || node.id in compatibleNodeIds

                drawTagNode(
                    node = node,
                    screenPos = p,
                    currentZoom = curZoom,
                    isFocused = false,
                    chainOrder = null,
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome,
                    suppressLabel = isNearFocused || isNearPortal,
                    isCompatible = isCompatible,
                    particleScale = particleScale
                )
            }

            // Capa 2: Nodos conectados en la constelación activa
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
                    isCompatible = true,
                    particleScale = particleScale
                )
            }

            // Capa 3: Nodo Portal si no está enfocado
            if (portalNode != null) {
                drawPortalNode(
                    node = portalNode.first,
                    screenPos = portalNode.second,
                    currentZoom = curZoom,
                    isFocused = false,
                    cosmicRotation = cosmicDrift,
                    starlightPulse = starlightPulse,
                    textMeasurer = textMeasurer,
                    isMonochrome = isMonochrome
                )
            }

            // Capa 4: NODO ENFOCADO (MÁXIMA PRIORIDAD - DIBUJADO ENCIMA DE TODO)
            if (focusedPair != null) {
                val (fNode, fPos) = focusedPair
                if (fNode.isPortal || fNode.id == PORTAL_NODE_ID) {
                    drawPortalNode(
                        node = fNode,
                        screenPos = fPos,
                        currentZoom = curZoom,
                        isFocused = true,
                        cosmicRotation = cosmicDrift,
                        starlightPulse = starlightPulse,
                        textMeasurer = textMeasurer,
                        isMonochrome = isMonochrome
                    )
                } else {
                    val isCompatible = compatibleNodeIds == null || fNode.id in compatibleNodeIds
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
                        isCompatible = isCompatible,
                        particleScale = particleScale
                    )
                }
            }
        }
    }
}

/**
 * Renderizado refinado de una etiqueta cinematográfica ("Trabajo Fino").
 */
private fun DrawScope.drawTagNode(
    node: SpatialNebulaNode,
    screenPos: Offset,
    currentZoom: Float,
    isFocused: Boolean,
    chainOrder: Int?, // 1, 2, 3... si está conectada en la constelación
    starlightPulse: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    isMonochrome: Boolean = false,
    suppressLabel: Boolean = false,
    isCompatible: Boolean = true,
    particleScale: Float = 1.0f
) {
    val isConnected = chainOrder != null
    val isClusterCenter = node.id.startsWith("CLUSTER_")
    val categoryColor = getCategoryColor(node.category, isMonochrome)

    // Regla de oro de Nivel de Detalle (LOD):
    // Los nodos conectados y el enfocado SIEMPRE son visibles con su texto y máxima claridad
    val isVisibleLOD = when {
        isFocused || isConnected -> true
        isClusterCenter -> currentZoom < 1.75f
        node.importance >= 1.15f -> currentZoom >= 1.40f // Tropos anillo 1 florecen al conectar o acercarse
        node.importance >= 0.95f -> currentZoom >= 2.15f // Motivos anillo 2 florecen en zoom profundo
        else -> currentZoom >= 2.95f                     // Micro-detalles anillo 3
    }

    if (!isVisibleLOD) {
        // En modo ahorro / gama baja (particleScale < 0.5f), omitir el renderizado del polvo cósmico lejano para reducir fill-rate
        if (particleScale < 0.5f) return

        // En lejanía: partícula estelar tenue ("polvo cósmico que abrume")
        val distantAlpha = if (isCompatible) 0.30f else 0.08f
        drawCircle(
            color = categoryColor.copy(alpha = distantAlpha),
            radius = 1.6.dp.toPx(),
            center = screenPos
        )
        return
    }

    // 1. Núcleo Estelar
    val starRadius = when {
        isFocused -> 5.5.dp.toPx()
        isConnected -> 5.0.dp.toPx()
        !isCompatible -> 1.8.dp.toPx()
        isClusterCenter -> 4.5.dp.toPx()
        node.importance >= 1.15f -> 3.2.dp.toPx()
        else -> 2.2.dp.toPx()
    }

    // Resplandor y halo para nodo enfocado o conectado
    if (isFocused) {
        val haloColor = if (!isCompatible) {
            if (isMonochrome) Color(0x20FFFFFF) else Color(0x28EF4444)
        } else if (isMonochrome) {
            Color(0x35FFFFFF)
        } else {
            Color(0x3500E5FF)
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(haloColor, Color.Transparent),
                center = screenPos,
                radius = 28.dp.toPx()
            ),
            radius = 28.dp.toPx(),
            center = screenPos
        )
    } else if (isConnected) {
        val haloColor = if (isMonochrome) Color(0x25FFFFFF) else Color(0x35C084FC)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(haloColor, Color.Transparent),
                center = screenPos,
                radius = 24.dp.toPx()
            ),
            radius = 24.dp.toPx(),
            center = screenPos
        )
    }

    // Punto central de luz
    val coreColor = when {
        isFocused && !isCompatible -> if (isMonochrome) Color.White else Color(0xFFEF4444)
        isFocused -> if (isMonochrome) Color.White else Color(0xFF00E5FF)
        isConnected -> if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFFC084FC)
        !isCompatible -> categoryColor.copy(alpha = 0.20f)
        isClusterCenter -> if (isMonochrome) Color(0xFFCBD5E1) else Color(0xFFB388FF)
        else -> categoryColor
    }

    drawCircle(
        color = coreColor,
        radius = starRadius,
        center = screenPos
    )
    if (isCompatible || isFocused || isConnected) {
        drawCircle(
            color = Color.White.copy(alpha = if (isFocused || isConnected) 0.95f else 0.70f),
            radius = starRadius * 0.45f,
            center = screenPos
        )
    }

    // Si se suprime la etiqueta (por proximidad al nodo enfocado) y no es ni enfocado ni conectado, solo dibujamos la estrella
    if (suppressLabel && !isFocused && !isConnected) {
        return
    }

    // 2. Pastilla Tipográfica de la Etiqueta
    val labelText = if (isConnected) "✦ $chainOrder. ${node.label}" else node.label
    val textColor = when {
        isFocused && !isCompatible -> if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFFFCA5A5)
        isFocused -> if (isMonochrome) Color.White else Color(0xFF00E5FF)
        isConnected -> if (isMonochrome) Color.White else Color(0xFFF3E8FF)
        !isCompatible -> Color(0x2EFFFFFF)
        isClusterCenter -> if (isMonochrome) Color(0xFFF8FAFC) else Color(0xFFE9D5FF)
        node.importance > 1.1f -> Color.White
        else -> Color(0xDDFFFFFF)
    }

    val fontSize = when {
        isFocused -> 13.sp
        isConnected -> 11.sp
        isClusterCenter -> 10.5.sp
        node.importance > 1.1f -> 9.5.sp
        else -> 8.5.sp
    }

    val textLayout = textMeasurer.measure(
        text = labelText,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (isFocused) FontWeight.ExtraBold else if (isConnected || isClusterCenter) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = if (isFocused) 1.2.sp else 0.6.sp
        )
    )

    val labelX = screenPos.x - textLayout.size.width / 2f
    val labelY = screenPos.y + starRadius + (if (isFocused) 6.dp.toPx() else 3.dp.toPx())
    val padX = if (isFocused) 11.dp.toPx() else 7.dp.toPx()
    val padY = if (isFocused) 5.dp.toPx() else 3.dp.toPx()

    // Para el nodo enfocado: dibujar placa de oclusión oscura previa ("venir al frente sin miedo")
    if (isFocused) {
        drawRoundRect(
            color = Color(0xF8030508),
            topLeft = Offset(labelX - padX - 4.dp.toPx(), labelY - padY - 2.dp.toPx()),
            size = Size(textLayout.size.width + (padX + 4.dp.toPx()) * 2, textLayout.size.height + (padY + 2.dp.toPx()) * 2),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Fill
        )
    }

    // Fondo de cápsula
    val capsuleBg = when {
        isFocused && !isCompatible -> if (isMonochrome) Color(0xFF1E293B) else Color(0xFF260D12)
        isFocused -> if (isMonochrome) Color(0xFF1E293B) else Color(0xFF0A1220)
        isConnected -> if (isMonochrome) Color(0xF00F172A) else Color(0xF01A0E2E)
        !isCompatible -> Color(0x1805070B)
        else -> Color(0xCC070B14)
    }

    drawRoundRect(
        color = capsuleBg,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Fill
    )

    // Borde de cápsula
    val borderColor = when {
        isFocused && !isCompatible -> if (isMonochrome) Color(0x99FFFFFF) else Color(0xCCEF4444)
        isFocused -> (if (isMonochrome) Color.White else Color(0xFF00E5FF))
        isConnected -> (if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFFC084FC)).copy(alpha = 0.85f)
        !isCompatible -> Color(0x06FFFFFF)
        else -> Color(0x20FFFFFF)
    }
    val borderWidth = when {
        isFocused -> 2.2.dp.toPx()
        isConnected -> 1.0.dp.toPx()
        else -> 0.6.dp.toPx()
    }

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = borderWidth)
    )

    // Dibujar texto
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(labelX, labelY)
    )

    // Sub-etiqueta elegante de conteo si está enfocado
    if (isFocused) {
        val countText = if (isCompatible) "✦ ${node.movieCount} OBRAS DISPONIBLES" else "✕ SIN OBRAS COMPATIBLES"
        val countLayout = textMeasurer.measure(
            text = countText,
            style = TextStyle(
                color = if (!isCompatible) {
                    if (isMonochrome) Color(0xFFAAAAAA) else Color(0xFFF87171)
                } else if (isMonochrome) {
                    Color.White
                } else {
                    Color(0xFFC084FC)
                },
                fontSize = 8.sp,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.0.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val countX = screenPos.x - countLayout.size.width / 2f
        val countY = labelY + textLayout.size.height + padY + 4.dp.toPx()

        drawRoundRect(
            color = Color(0xF205070B),
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
 * Renderizado del Nodo Portal / Núcleo de Películas ("Obras Maestras").
 * Vórtice gravitatorio con halo multicapa, anillo orbital estelar y badge de acción TV.
 */
private fun DrawScope.drawPortalNode(
    node: SpatialNebulaNode,
    screenPos: Offset,
    currentZoom: Float,
    isFocused: Boolean,
    cosmicRotation: Float,
    starlightPulse: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    isMonochrome: Boolean = false
) {
    val coreRadius = (if (isFocused) 15.dp else 12.dp).toPx()
    val haloRadius = (if (isFocused) 44.dp else 30.dp).toPx()

    // 1. Resplandor exterior cósmico multicapa (Efecto Vórtice Gravitatorio)
    val haloGlowPrimary = if (isMonochrome) {
        if (isFocused) Color(0x60FFFFFF) else Color(0x35FFFFFF)
    } else {
        if (isFocused) Color(0x6000E5FF) else Color(0x45B388FF)
    }
    val haloGlowSecondary = if (isMonochrome) {
        if (isFocused) Color(0x20FFFFFF) else Color(0x15FFFFFF)
    } else {
        if (isFocused) Color(0x2000E5FF) else Color(0x207C3AED)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(haloGlowPrimary, haloGlowSecondary, Color.Transparent),
            center = screenPos,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = screenPos
    )

    // 2. Anillo orbital rotatorio punteado
    val ringRadius = coreRadius * 1.55f
    val ringColor = if (isMonochrome) {
        if (isFocused) Color.White else Color(0xFFE2E8F0)
    } else {
        if (isFocused) Color(0xFF00E5FF) else Color(0xFFC084FC)
    }
    drawCircle(
        color = ringColor.copy(alpha = 0.85f * starlightPulse),
        radius = ringRadius,
        center = screenPos,
        style = Stroke(
            width = (if (isFocused) 1.8.dp else 1.2.dp).toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()), phase = cosmicRotation * 18f)
        )
    )

    // 3. Núcleo estelar de energía
    val coreColors = if (isMonochrome) {
        listOf(
            Color.White,
            if (isFocused) Color.White else Color(0xFFE2E8F0),
            if (isFocused) Color(0xFFCBD5E1) else Color(0xFF64748B)
        )
    } else {
        listOf(
            Color.White,
            if (isFocused) Color(0xFF00E5FF) else Color(0xFFB388FF),
            if (isFocused) Color(0xFF0288D1) else Color(0xFF6B21A8)
        )
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

    // 4. Cápsula / Badge de texto interactivo para TV
    val labelText = if (isFocused) "${node.label}  ➔ [PULSA OK]" else node.label
    val badgeTextColor = if (isMonochrome) {
        if (isFocused) Color(0xFF0F172A) else Color.White
    } else {
        if (isFocused) Color(0xFF05070B) else Color(0xFFF3E8FF)
    }

    val textLayout = textMeasurer.measure(
        text = labelText,
        style = TextStyle(
            color = badgeTextColor,
            fontSize = if (isFocused) 12.sp else 10.5.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isFocused) 1.2.sp else 0.8.sp
        )
    )

    val labelX = screenPos.x - textLayout.size.width / 2f
    val labelY = screenPos.y + ringRadius + (if (isFocused) 6.dp.toPx() else 4.dp.toPx())
    val padX = 10.dp.toPx()
    val padY = 4.dp.toPx()

    // Placa de oclusión oscura previa para legibilidad 100% garantizada
    drawRoundRect(
        color = Color(0xF8030508),
        topLeft = Offset(labelX - padX - 4.dp.toPx(), labelY - padY - 2.dp.toPx()),
        size = Size(textLayout.size.width + (padX + 4.dp.toPx()) * 2, textLayout.size.height + (padY + 2.dp.toPx()) * 2),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        style = Fill
    )

    // Fondo del badge con contraste TV
    val badgeBgColor = if (isMonochrome) {
        if (isFocused) Color.White else Color(0xF01E293B)
    } else {
        if (isFocused) Color(0xFF00E5FF) else Color(0xF01A0E2E)
    }
    drawRoundRect(
        color = badgeBgColor,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Fill
    )

    // Borde iluminado
    val badgeBorderColor = if (isMonochrome) {
        if (isFocused) Color.White else Color(0x60FFFFFF)
    } else {
        if (isFocused) Color.White else Color(0xFFC084FC).copy(alpha = 0.85f)
    }
    drawRoundRect(
        color = badgeBorderColor,
        topLeft = Offset(labelX - padX, labelY - padY),
        size = Size(textLayout.size.width + padX * 2, textLayout.size.height + padY * 2),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Stroke(width = (if (isFocused) 1.6.dp else 1.0.dp).toPx())
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(labelX, labelY)
    )
}

private fun getCategoryColor(category: String, isMonochrome: Boolean = false): Color {
    if (isMonochrome) {
        return when (category) {
            "Crimen", "Misterio" -> Color(0xFFE2E8F0)
            "Aventura" -> Color(0xFFFFFFFF)
            "Terror", "Suspense" -> Color(0xFFCBD5E1)
            "Drama", "Historia" -> Color(0xFFE2E8F0)
            "Comedia" -> Color(0xFFFFFFFF)
            "Bélica", "Western" -> Color(0xFF94A3B8)
            "Fantasía", "Animación" -> Color(0xFFFFFFFF)
            else -> Color(0xFFCBD5E1)
        }
    }
    return when (category) {
        "Crimen", "Misterio" -> Color(0xFF818CF8) // Indigo
        "Aventura" -> Color(0xFF2DD4BF)          // Teal / Cyan
        "Terror", "Suspense" -> Color(0xFFF43F5E) // Rose
        "Drama", "Historia" -> Color(0xFFCBD5E1)  // Silver
        "Comedia" -> Color(0xFF38BDF8)           // Sky Cyan
        "Bélica", "Western" -> Color(0xFFA78BFA)  // Violet
        "Fantasía", "Animación" -> Color(0xFF00E5FF) // Electric Cyan
        else -> Color(0xFF94A3B8)                // Muted Slate
    }
}

private fun getNodeWorldPos(
    node: SpatialNebulaNode,
    activeChain: List<SpatialNebulaNode>,
    compatibleNodeIds: Set<String>?
): Offset {
    if (activeChain.isEmpty() || compatibleNodeIds == null) {
        return Offset(node.worldX, node.worldY)
    }
    if (node.isPortal || node.id == PORTAL_NODE_ID || activeChain.any { it.id == node.id }) {
        return Offset(node.worldX, node.worldY)
    }
    if (!compatibleNodeIds.contains(node.id)) {
        return Offset(node.worldX, node.worldY)
    }

    val focalX = activeChain.map { it.worldX }.average().toFloat()
    val focalY = activeChain.map { it.worldY }.average().toFloat()

    val attractionFactor = (0.35f + (activeChain.size - 1) * 0.15f).coerceAtMost(0.65f)
    val effX = node.worldX + (focalX - node.worldX) * attractionFactor
    val effY = node.worldY + (focalY - node.worldY) * attractionFactor

    return Offset(effX, effY)
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
