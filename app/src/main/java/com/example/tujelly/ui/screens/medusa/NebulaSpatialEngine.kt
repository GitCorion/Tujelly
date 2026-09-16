package com.example.tujelly.ui.screens.medusa

import androidx.compose.ui.geometry.Offset
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

/**
 * Nodo en la Gran Galaxia Estelar de Medusa.
 */
data class SpatialNebulaNode(
    val id: String,
    val label: String,
    val rawTag: String = "",
    val category: String = "General",
    var worldX: Float, // Coordenada normalizada en el espacio (0.0f - 1.0f)
    var worldY: Float,
    val minZoomVisible: Float = 0.4f,
    val maxZoomVisible: Float = 10.0f,
    val movieCount: Int = 0,
    val importance: Float = 1.0f, // 1.0f estándar, >= 1.6f Supernovas (Top/Trending/Favoritos)
    val isCaptain: Boolean = false, // Estrella Capitana rectora del sector (etiqueta visible en reposo)
    val isPortal: Boolean = false, // Compatibilidad hacia atrás
    val isSun: Boolean = false,    // Sol / Agujero Negro Central
    val previewPosters: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

const val SUN_CORE_ID = "SUN_CORE"
const val PORTAL_NODE_ID = SUN_CORE_ID

val PILLAR_GENRES = listOf(
    "ciencia ficcion",
    "accion",
    "comedia",
    "drama",
    "terror",
    "suspense",
    "aventura",
    "fantasia",
    "animacion",
    "crimen",
    "romance",
    "familia",
    "belica",
    "misterio",
    "documental"
)

/**
 * Filamento vectorial de gravedad y afinidad entre dos estrellas.
 */
data class SpatialFilament(
    val fromNodeId: String,
    val toNodeId: String,
    val affinity: Float = 1.0f,
    val isActiveRay: Boolean = false // Rayo de energía activo conectado al Sol central
)

enum class DPadDirection { LEFT, RIGHT, UP, DOWN }

enum class MediaFormat { ALL, MOVIES, SERIES }

/**
 * Motor Espacial y Gravitacional definitivo de Medusa:
 * - Engendra un firmamento denso de 100+ tags reales extraídos de 4 vectores:
 *   1. Canon de TMDB (obras maestras presentes en el servidor).
 *   2. Tendencias de TMDB (lo que es viral hoy y tienes en tu catálogo).
 *   3. Historial del usuario (últimas obras vistas / en progreso).
 *   4. Favoritos del usuario.
 *   5. Catálogo general de Jellyfin.
 * - Sol / Agujero Negro Central en (0.5, 0.5) donde convergen los rayos y se concentran las carátulas.
 * - Lógica "AND" estricta pura (sin degradación 'OR').
 * - Poda cósmica en vivo: los nodos que no tienen intersección desaparecen de la pantalla y del D-Pad.
 */
class NebulaSpatialEngine {

    private val allNodes = mutableListOf<SpatialNebulaNode>()
    private val allFilaments = mutableListOf<SpatialFilament>()
    private val nodeById = mutableMapOf<String, SpatialNebulaNode>()

    // Caché de entidades con sus tags normalizados en minúsculas para búsquedas ultra rápidas
    private var tokenizedCatalog: List<Pair<JellyfinMediaEntity, Set<String>>> = emptyList()
    private var baseFilaments: List<SpatialFilament> = emptyList()

    fun getAllNodes(): List<SpatialNebulaNode> = allNodes
    fun getAllFilaments(): List<SpatialFilament> = allFilaments
    fun getNode(id: String): SpatialNebulaNode? = nodeById[id]

    /**
     * Construye la Gran Galaxia a partir de la biblioteca completa y los 4 vectores inteligentes.
     * Cero elementos fijos o cableados:
     * - Las Estrellas Capitanas se eligen dinámicamente y rotan según sessionSeed.
     * - Los satélites se agrupan en constelaciones alrededor de su Capitana por afinidad real.
     * - Cuanto mayor es la afinidad (películas compartidas), más cerca de su Capitana se sitúa la estrella.
     * - La orientación del firmamento rota orgánicamente con sessionSeed.
     */
    fun buildDynamicUniverse(
        catalog: List<JellyfinMediaEntity>,
        tmdbTopRated: List<JellyfinMediaEntity> = emptyList(),
        tmdbTrending: List<JellyfinMediaEntity> = emptyList(),
        recentWatched: List<JellyfinMediaEntity> = emptyList(),
        favorites: List<JellyfinMediaEntity> = emptyList(),
        serverUrl: String = "",
        accessToken: String = "",
        sessionSeed: Long = System.currentTimeMillis()
    ) {
        allNodes.clear()
        allFilaments.clear()
        nodeById.clear()

        // 1. Tokenización y normalización canónica de todo el catálogo (tags + géneros unificados)
        tokenizedCatalog = catalog.map { entity ->
            val tagsSet = extractCanonicalTags(entity)
            Pair(entity, tagsSet)
        }

        if (tokenizedCatalog.isEmpty()) {
            createEmptyUniverse()
            return
        }

        // 2. Extraer tags de los 4 vectores canónicos
        val canonTags = tmdbTopRated.flatMap { extractCanonicalTags(it) }.toSet()
        val trendingTags = tmdbTrending.flatMap { extractCanonicalTags(it) }.toSet()
        val recentTags = recentWatched.flatMap { extractCanonicalTags(it) }.toSet()
        val favTags = favorites.flatMap { extractCanonicalTags(it) }.toSet()

        // 3. Conteo y puntuación de relevancia para cada tag canónico del catálogo
        val tagCounts = mutableMapOf<String, Int>()
        for ((_, tags) in tokenizedCatalog) {
            for (tag in tags) {
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }

        // Filtrar tags con masa crítica (al menos 2 obras en tu servidor) y no en lista negra
        val maxAllowedFrequency = (catalog.size * 0.70f).toInt().coerceAtLeast(15)
        val validTags = tagCounts.filter { (tag, count) ->
            count >= 2 && count <= maxAllowedFrequency && tag.length >= 3 && !isBlacklistedTag(tag)
        }

        if (validTags.isEmpty()) {
            createEmptyUniverse()
            return
        }

        // Puntuación gravitacional para clasificar Supernovas vs Estrellas estándar
        val scoredTags = validTags.map { (tag, count) ->
            var score = count.toFloat() + ln(count.toFloat() + 1f) * 2f
            if (favTags.contains(tag)) score += 8f
            if (trendingTags.contains(tag)) score += 6f
            if (canonTags.contains(tag)) score += 5f
            if (recentTags.contains(tag)) score += 4f
            Triple(tag, count, score)
        }.sortedByDescending { it.third }

        // Deduplicación estricta por nombre de visualización: garantiza CERO repetidos en pantalla
        val seenDisplayNames = mutableSetOf<String>()
        val deduplicatedUniverseTags = mutableListOf<Triple<String, Int, Float>>()
        for (triple in scoredTags) {
            val (canonicalKey, _, _) = triple
            val displayName = TagTranslations.getDisplayName(canonicalKey)
            val normalizedDisplay = TagTranslations.stripAccents(displayName).lowercase(Locale.ROOT)
            if (seenDisplayNames.add(normalizedDisplay)) {
                deduplicatedUniverseTags.add(triple)
                if (deduplicatedUniverseTags.size >= 80) break
            }
        }
        val universeTags = deduplicatedUniverseTags

        // 4. Selección Dinámica y Rotativa de Estrellas Capitanas
        // REGLA FUNDAMENTAL: Solo los géneros pilares cinematográficos pueden ser Capitanas (NUNCA tags de nicho como LGBTQ+, Biográfico, Western, Amistad, Asesinato, etc.).
        val pillarCandidates = universeTags.filter { it.first in PILLAR_GENRES }
        val captainCandidatePool = if (pillarCandidates.size >= 4) {
            pillarCandidates
        } else {
            // Si hay pocos pilares presentes en la biblioteca (ej. catálogo pequeño de test),
            // complementar con tags de alta masa crítica que no sean de nicho
            pillarCandidates + universeTags.filter { it.first !in PILLAR_GENRES && !isNicheNonCaptainTag(it.first) }
        }

        val targetCaptainCount = when {
            captainCandidatePool.size >= 8 -> 7
            captainCandidatePool.size >= 6 -> 6
            captainCandidatePool.size >= 4 -> 4
            captainCandidatePool.size >= 2 -> 2
            else -> captainCandidatePool.size
        }

        val rotationShift = (abs(sessionSeed) % captainCandidatePool.size.coerceAtLeast(1)).toInt()
        val rotatedPool = captainCandidatePool.indices.map { idx ->
            captainCandidatePool[(idx + rotationShift) % captainCandidatePool.size]
        }

        // Selección diversa: evitar que dos capitanas sean subgéneros hiper-solapados
        val selectedCaptains = mutableListOf<Triple<String, Int, Float>>()
        for (candidate in rotatedPool) {
            if (selectedCaptains.size >= targetCaptainCount) break
            val (candTag, _, _) = candidate

            val isTooOverlapping = selectedCaptains.any { (prevTag, _, _) ->
                val shared = tokenizedCatalog.count { (_, tags) -> tags.contains(candTag) && tags.contains(prevTag) }
                val minCount = minOf(tagCounts[candTag] ?: 1, tagCounts[prevTag] ?: 1)
                (shared.toFloat() / minCount.toFloat()) > 0.45f
            }

            if (!isTooOverlapping) {
                selectedCaptains.add(candidate)
            }
        }

        // Rellenar si el filtro de solapamiento descartó demasiados
        if (selectedCaptains.size < targetCaptainCount) {
            for (candidate in rotatedPool) {
                if (selectedCaptains.size >= targetCaptainCount) break
                if (selectedCaptains.none { it.first == candidate.first }) {
                    selectedCaptains.add(candidate)
                }
            }
        }

        val captainTagsSet = selectedCaptains.map { it.first }.toSet()
        val remainingTags = universeTags.filter { it.first !in captainTagsSet }

        // 5. Agrupación por Afinidad Equilibrada (Balanceo de Carga por Sector)
        // Ninguna Capitana puede acaparar más de 6 satélites, garantizando un firmamento 360° perfectamente homogéneo
        val maxSatellitesPerCaptain = 6
        val satellitesPerCaptain = selectedCaptains.associate { it.first to mutableListOf<Pair<Triple<String, Int, Float>, Int>>() }

        for (satTriple in remainingTags) {
            val (satTag, _, _) = satTriple
            val affinities = selectedCaptains.map { capTriple ->
                val capTag = capTriple.first
                val shared = tokenizedCatalog.count { (_, tags) -> tags.contains(satTag) && tags.contains(capTag) }
                Pair(capTag, shared)
            }.sortedByDescending { it.second }

            var assigned = false
            for ((capTag, shared) in affinities) {
                val currentList = satellitesPerCaptain[capTag] ?: continue
                if (currentList.size < maxSatellitesPerCaptain && shared > 0) {
                    currentList.add(Pair(satTriple, shared))
                    assigned = true
                    break
                }
            }

            if (!assigned) {
                val leastLoadedCaptain = satellitesPerCaptain.entries
                    .filter { it.value.size < maxSatellitesPerCaptain }
                    .minByOrNull { it.value.size }
                if (leastLoadedCaptain != null) {
                    val shared = tokenizedCatalog.count { (_, tags) -> tags.contains(satTag) && tags.contains(leastLoadedCaptain.key) }
                    leastLoadedCaptain.value.add(Pair(satTriple, shared.coerceAtLeast(0)))
                }
            }
        }

        // 6. Posicionamiento en el Espacio:
        // - Las Capitanas orbitan en un anillo elíptico equilibrado alrededor del Sol Central
        // - Los satélites se expanden en abanico RADIAL HACIA AFUERA del Sol Central
        // - Retracción suave de bordes: jamás se empuja un nodo hacia la pared de la pantalla
        val baseRotationAngle = ((abs(sessionSeed) % 360) * (PI / 180.0)).toFloat()
        val numCaptains = selectedCaptains.size
        var nodeCounter = 0
        val createdTagNodes = mutableListOf<SpatialNebulaNode>()
        val generatedFilaments = mutableListOf<SpatialFilament>()

        val capOrbitRadiusX = 0.28f
        val capOrbitRadiusY = 0.23f
        val minX = 0.08f
        val maxX = 0.92f
        val minY = 0.10f
        val maxY = 0.90f

        selectedCaptains.forEachIndexed { capIdx, capTriple ->
            val (rawTag, count, score) = capTriple
            val capAngle = baseRotationAngle + (capIdx.toFloat() / numCaptains.toFloat()) * (2 * PI).toFloat()

            // Coordenadas elípticas de la Capitana (aspect ratio 16:9 TV)
            val capX = (0.50f + cos(capAngle) * capOrbitRadiusX).coerceIn(0.20f, 0.80f)
            val capY = (0.50f + sin(capAngle) * capOrbitRadiusY).coerceIn(0.20f, 0.80f)

            val displayName = TagTranslations.getDisplayName(rawTag)
            val captainNode = SpatialNebulaNode(
                id = "NODE_${nodeCounter++}",
                label = displayName,
                rawTag = rawTag,
                category = displayName,
                worldX = capX,
                worldY = capY,
                minZoomVisible = 0.30f,
                movieCount = count,
                importance = 2.4f,
                isCaptain = true,
                keywords = listOf(rawTag, displayName.lowercase()),
                isSun = false,
                isPortal = false
            )
            addNode(captainNode)
            createdTagNodes.add(captainNode)

            // Posicionar satélites ordenados por afinidad decreciente
            val satsInCluster = satellitesPerCaptain[rawTag]?.sortedByDescending { it.second } ?: emptyList()
            val maxAffinityInCluster = satsInCluster.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            val mCount = satsInCluster.size

            // Ángulo hacia afuera del Sol Central
            val outwardAngle = atan2(capY - 0.50f, capX - 0.50f)

            satsInCluster.forEachIndexed { satIdx, (sTriple, sharedCount) ->
                val (satRawTag, satCount, satScore) = sTriple
                val satDisplayName = TagTranslations.getDisplayName(satRawTag)

                val normalizedAffinity = (sharedCount.toFloat() / maxAffinityInCluster.toFloat()).coerceIn(0.10f, 1.0f)

                // Dos niveles radiales escalonados para que respiren visualmente
                val isInnerTier = (satIdx % 2 == 0)
                val baseDist = if (isInnerTier) 0.075f else 0.120f
                val satDist = baseDist * (1.10f - 0.20f * normalizedAffinity)

                // Abanico angular hacia afuera (evita crowding hacia el centro)
                val fanArc = 1.25f // ~72 grados
                val angleOffset = if (mCount > 1) {
                    ((satIdx.toFloat() / (mCount - 1).toFloat()) - 0.5f) * fanArc
                } else 0f
                val satAngle = outwardAngle + angleOffset

                var rawSatX = capX + cos(satAngle) * satDist * 1.25f
                var rawSatY = capY + sin(satAngle) * satDist * 0.88f

                // Retracción de frontera: si se sale del margen seguro, se escala el radio hacia la Capitana
                var edgeScale = 1.0f
                if (rawSatX > maxX && rawSatX != capX) edgeScale = minOf(edgeScale, (maxX - capX) / (rawSatX - capX))
                if (rawSatX < minX && rawSatX != capX) edgeScale = minOf(edgeScale, (minX - capX) / (rawSatX - capX))
                if (rawSatY > maxY && rawSatY != capY) edgeScale = minOf(edgeScale, (maxY - capY) / (rawSatY - capY))
                if (rawSatY < minY && rawSatY != capY) edgeScale = minOf(edgeScale, (minY - capY) / (rawSatY - capY))
                edgeScale = edgeScale.coerceIn(0.40f, 1.0f)

                val satX = (capX + (rawSatX - capX) * edgeScale).coerceIn(minX, maxX)
                val satY = (capY + (rawSatY - capY) * edgeScale).coerceIn(minY, maxY)

                val satNode = SpatialNebulaNode(
                    id = "NODE_${nodeCounter++}",
                    label = satDisplayName,
                    rawTag = satRawTag,
                    category = displayName,
                    worldX = satX,
                    worldY = satY,
                    minZoomVisible = 0.55f,
                    movieCount = satCount,
                    importance = if (satScore >= 14f) 1.75f else 1.0f,
                    isCaptain = false,
                    keywords = listOf(satRawTag, satDisplayName.lowercase()),
                    isSun = false,
                    isPortal = false
                )
                addNode(satNode)
                createdTagNodes.add(satNode)

                // Filamento directo entre el satélite y su Capitana
                val filamentAffinity = (0.4f + 0.6f * normalizedAffinity).coerceIn(0.4f, 1.0f)
                generatedFilaments.add(
                    SpatialFilament(
                        fromNodeId = satNode.id,
                        toNodeId = captainNode.id,
                        affinity = filamentAffinity
                    )
                )
            }
        }

        // 7. Relajación de Fuerzas Físicas Simétricas (Anti-Solapamiento sin estampida contra los bordes)
        val satellitesOnly = createdTagNodes.filter { !it.isCaptain }
        val captainsOnly = createdTagNodes.filter { it.isCaptain }

        repeat(4) {
            for (a in satellitesOnly.indices) {
                val nodeA = satellitesOnly[a]
                for (b in a + 1 until satellitesOnly.size) {
                    val nodeB = satellitesOnly[b]
                    val dx = (nodeB.worldX - nodeA.worldX) * 1.6f
                    val dy = nodeB.worldY - nodeA.worldY
                    val dist = hypot(dx, dy)
                    val minDist = 0.052f
                    if (dist < minDist && dist > 0.0005f) {
                        val overlap = (minDist - dist) * 0.5f
                        val nx = (dx / dist) * overlap * 0.5f
                        val ny = (dy / dist) * overlap * 0.5f
                        nodeA.worldX = (nodeA.worldX - nx).coerceIn(minX, maxX)
                        nodeA.worldY = (nodeA.worldY - ny).coerceIn(minY, maxY)
                        nodeB.worldX = (nodeB.worldX + nx).coerceIn(minX, maxX)
                        nodeB.worldY = (nodeB.worldY + ny).coerceIn(minY, maxY)
                    }
                }

                // Repulsión contra las Capitanas
                for (cap in captainsOnly) {
                    val dx = (nodeA.worldX - cap.worldX) * 1.6f
                    val dy = nodeA.worldY - cap.worldY
                    val dist = hypot(dx, dy)
                    val minCapDist = 0.056f
                    if (dist < minCapDist && dist > 0.0005f) {
                        val push = (minCapDist - dist)
                        nodeA.worldX = (nodeA.worldX + (dx / dist) * push * 0.6f).coerceIn(minX, maxX)
                        nodeA.worldY = (nodeA.worldY + (dy / dist) * push * 0.6f).coerceIn(minY, maxY)
                    }
                }

                // Zona de seguridad del Sol Central (radio 0.18f)
                val distSun = hypot((nodeA.worldX - 0.50f) * 1.3f, nodeA.worldY - 0.50f)
                if (distSun < 0.185f && distSun > 0.0005f) {
                    val push = (0.185f - distSun)
                    nodeA.worldX = (nodeA.worldX + ((nodeA.worldX - 0.50f) / distSun) * push).coerceIn(minX, maxX)
                    nodeA.worldY = (nodeA.worldY + ((nodeA.worldY - 0.50f) / distSun) * push).coerceIn(minY, maxY)
                }
            }
        }

        // 8. Filamentos del Anillo de Constelaciones (Conecta Capitanas en una diadema estelar limpia)
        if (selectedCaptains.size >= 3) {
            for (i in selectedCaptains.indices) {
                val capA = createdTagNodes.first { it.rawTag == selectedCaptains[i].first }
                val nextIdx = (i + 1) % selectedCaptains.size
                val capB = createdTagNodes.first { it.rawTag == selectedCaptains[nextIdx].first }
                generatedFilaments.add(
                    SpatialFilament(
                        fromNodeId = capA.id,
                        toNodeId = capB.id,
                        affinity = 0.35f
                    )
                )
            }
        }

        baseFilaments = generatedFilaments
        allFilaments.addAll(generatedFilaments)

        // 8. Instanciar el Sol Central (SUN_CORE_ID) en (0.5, 0.5)
        updateSunNode(
            chain = emptyList(),
            movieCount = 0,
            previewPosters = emptyList(),
            format = MediaFormat.ALL
        )
    }

    /**
     * Actualiza el Sol Central con el estado de la cadena y las películas descubiertas.
     */
    fun updateSunNode(
        chain: List<SpatialNebulaNode>,
        movieCount: Int,
        previewPosters: List<String> = emptyList(),
        format: MediaFormat = MediaFormat.ALL
    ) {
        allNodes.removeAll { it.id == SUN_CORE_ID }
        nodeById.remove(SUN_CORE_ID)
        allFilaments.clear()
        allFilaments.addAll(baseFilaments)

        val sunLabel = when {
            chain.isEmpty() -> "SOL CENTRAL"
            else -> "✦ $movieCount ${formatNoun(format).uppercase()}"
        }

        val sunNode = SpatialNebulaNode(
            id = SUN_CORE_ID,
            label = sunLabel,
            category = "Sol Gravitacional",
            worldX = 0.50f,
            worldY = 0.50f,
            minZoomVisible = 0.2f,
            maxZoomVisible = 20.0f,
            movieCount = movieCount,
            importance = 3.5f,
            isSun = true,
            isPortal = true, // Permite compatibilidad con código existente
            previewPosters = previewPosters
        )
        addNode(sunNode)

        // Trazar rayos de luz activos desde cada nodo de la cadena hacia el Sol Central
        chain.forEach { chainedNode ->
            allFilaments.add(
                SpatialFilament(
                    fromNodeId = chainedNode.id,
                    toNodeId = SUN_CORE_ID,
                    affinity = 2.5f,
                    isActiveRay = true
                )
            )
        }
    }

    /**
     * Poda Cósmica en Vivo:
     * Calcula qué estrellas sobreviven según la intersección ESTRICTA "AND" de la cadena activa.
     * Cualquier nodo con 0 películas en común con la cadena desaparece de la pantalla.
     */
    fun getVisibleNodeIds(
        chain: List<SpatialNebulaNode>,
        format: MediaFormat = MediaFormat.ALL
    ): Set<String> {
        if (chain.isEmpty()) {
            return allNodes.map { it.id }.toSet()
        }

        val surviving = mutableSetOf<String>()
        surviving.add(SUN_CORE_ID)
        chain.forEach { surviving.add(it.id) }

        val chainRawTags = chain.map { it.rawTag.lowercase() }
        val catalogForFormat = tokenizedCatalog.filter { (entity, _) -> entity.matchesFormat(format) }

        // 1. Obras que satisfacen TODOS los tags de la cadena activa (AND estricto)
        val matchingMovies = catalogForFormat.filter { (_, tags) ->
            chainRawTags.all { chainTag -> tags.contains(chainTag) }
        }

        if (matchingMovies.isEmpty()) {
            return surviving
        }

        // 2. Unión de todos los tags que coexisten en las obras supervivientes (O(M) una sola vez)
        val survivingTagsUnion = HashSet<String>()
        for ((_, tags) in matchingMovies) {
            survivingTagsUnion.addAll(tags)
        }

        // 3. Verificación O(1) por nodo (en vez de O(N*M))
        for (node in allNodes) {
            if (node.isSun || node.id in surviving) continue
            val raw = node.rawTag.lowercase()
            if (survivingTagsUnion.contains(raw)) {
                surviving.add(node.id)
            }
        }

        return surviving
    }

    /**
     * Filtra obras aplicando estrictamente "AND" (todas las etiquetas deben coincidir).
     * Cero degradación, cero lógica 'OR'.
     */
    fun getMatchingMoviesForChain(
        chain: List<SpatialNebulaNode>,
        catalog: List<JellyfinMediaEntity>,
        serverUrl: String,
        accessToken: String,
        format: MediaFormat = MediaFormat.ALL
    ): List<MediaItem> {
        if (chain.isEmpty()) return emptyList()

        val chainTags = chain.map { it.rawTag.lowercase() }

        // Utilizar tokenizedCatalog precalculado en O(1) por obra en lugar de re-tokenizar miles de entidades en el hilo principal
        val sourceCatalog = if (tokenizedCatalog.isNotEmpty()) {
            tokenizedCatalog.filter { (entity, _) -> entity.matchesFormat(format) }
        } else {
            catalog.filter { it.matchesFormat(format) }.map { Pair(it, extractCanonicalTags(it)) }
        }

        val strictMatches = sourceCatalog.filter { (_, entityTags) ->
            chainTags.all { cTag -> entityTags.contains(cTag) }
        }.map { it.first }

        return strictMatches
            .sortedWith(
                compareByDescending<JellyfinMediaEntity> { !it.isPlayed }
                    .thenByDescending { it.communityRating ?: 0f }
                    .thenByDescending { it.productionYear ?: 0 }
            )
            .take(40)
            .map { it.toMediaItem(serverUrl, accessToken) }
    }

    /**
     * Valida si un nodo puede ser conectado a la cadena activa.
     */
    fun canExtendChain(
        currentChain: List<SpatialNebulaNode>,
        candidate: SpatialNebulaNode,
        format: MediaFormat = MediaFormat.ALL
    ): Boolean {
        if (candidate.isSun || candidate.id == SUN_CORE_ID) return true
        if (currentChain.any { it.id == candidate.id }) return false
        val visibleIds = getVisibleNodeIds(currentChain, format)
        return candidate.id in visibleIds
    }

    /**
     * Navegación D-Pad orientada al firmamento podado y al Sol Central.
     * Solo navega a nodos visibles y permite saltar al Sol desde cualquier ángulo.
     */
    fun findNextNeighbor(
        currentId: String,
        direction: DPadDirection,
        currentZoom: Float,
        activeChain: List<SpatialNebulaNode> = emptyList(),
        format: MediaFormat = MediaFormat.ALL
    ): SpatialNebulaNode? {
        val current = nodeById[currentId] ?: return allNodes.firstOrNull { it.id == SUN_CORE_ID }
        val visibleIds = getVisibleNodeIds(activeChain, format)

        // Candidatos válidos: solo nodos supervivientes de la poda
        val candidates = allNodes.filter { it.id in visibleIds && it.id != current.id }
        if (candidates.isEmpty()) return allNodes.firstOrNull { it.id == SUN_CORE_ID }

        val cx = current.worldX
        val cy = current.worldY

        val scored = candidates.mapNotNull { target ->
            val dx = target.worldX - cx
            val dy = target.worldY - cy

            val isValidDirection = when (direction) {
                DPadDirection.LEFT -> dx < -0.012f
                DPadDirection.RIGHT -> dx > 0.012f
                DPadDirection.UP -> dy < -0.012f
                DPadDirection.DOWN -> dy > 0.012f
            }

            if (isValidDirection) {
                val distance = hypot(dx, dy)
                val anglePenalty = when (direction) {
                    DPadDirection.LEFT, DPadDirection.RIGHT -> abs(dy) * 1.6f
                    DPadDirection.UP, DPadDirection.DOWN -> abs(dx) * 1.6f
                }
                // Si el objetivo es el Sol Central, otorgar una fuerte atracción magnética
                val sunBonus = if (target.isSun || target.id == SUN_CORE_ID) -0.09f else 0f
                Pair(target, distance + anglePenalty + sunBonus)
            } else null
        }

        return scored.minByOrNull { it.second }?.first
    }

    private fun addNode(node: SpatialNebulaNode) {
        allNodes.add(node)
        nodeById[node.id] = node
    }

    /**
     * Descompone géneros compuestos de TMDB (especialmente de Series de TV) en sus
     * géneros atómicos correspondientes para evitar etiquetas redundantes/duplicadas
     * como "Ciencia Ficción y Fantasía" coexistiendo con "Ciencia Ficción" y "Fantasía".
     */
    private fun decomposeCompositeGenre(raw: String): List<String> {
        val clean = raw.trim().lowercase(Locale.ROOT)
        val unaccented = TagTranslations.stripAccents(clean)
        return when {
            (unaccented.contains("sci-fi") || unaccented.contains("scifi") ||
             unaccented.contains("science fiction") || unaccented.contains("ciencia ficcion")) &&
            (unaccented.contains("fantasy") || unaccented.contains("fantasia")) -> {
                listOf("ciencia ficcion", "fantasia")
            }
            (unaccented.contains("action") || unaccented.contains("accion")) &&
            (unaccented.contains("adventure") || unaccented.contains("aventura")) -> {
                listOf("accion", "aventura")
            }
            (unaccented.contains("war") || unaccented.contains("guerra") || unaccented.contains("belica")) &&
            (unaccented.contains("politics") || unaccented.contains("politica")) -> {
                listOf("belica", "politica")
            }
            else -> listOf(clean)
        }
    }

    /**
     * Extrae las temáticas canónicas de una obra, resolviendo sinónimos y traducciones
     * (inglés y español) a su etiqueta canónica única en español.
     * Ejemplo: "Animation", "animación", "animacion" -> "animacion"
     * "Family", "familia" -> "familia"
     * "Crime", "crimen" -> "crimen"
     */
    private fun extractCanonicalTags(entity: JellyfinMediaEntity): Set<String> {
        val rawTokens = mutableSetOf<String>()
        entity.tags?.split(",")?.forEach { t ->
            val clean = t.trim().lowercase(Locale.ROOT)
            if (clean.length >= 3) {
                for (decomposed in decomposeCompositeGenre(clean)) {
                    if (!isBlacklistedTag(decomposed)) {
                        rawTokens.add(decomposed)
                    }
                }
            }
        }
        entity.genres?.split(",")?.forEach { g ->
            val clean = g.trim().lowercase(Locale.ROOT)
            if (clean.length >= 3) {
                for (decomposed in decomposeCompositeGenre(clean)) {
                    if (!isBlacklistedTag(decomposed)) {
                        rawTokens.add(decomposed)
                    }
                }
            }
        }

        val canonicalSet = mutableSetOf<String>()
        for (raw in rawTokens) {
            if (isValidGalaxyTag(raw)) {
                val displayName = TagTranslations.getDisplayName(raw)
                val canonicalKey = TagTranslations.stripAccents(displayName).lowercase(Locale.ROOT)
                if (!isBlacklistedTag(canonicalKey)) {
                    canonicalSet.add(canonicalKey)
                }
            }
        }
        return canonicalSet
    }

    private fun isValidGalaxyTag(tag: String): Boolean {
        val clean = tag.lowercase().trim()
        if (clean.length < 3) return false
        if (isBlacklistedTag(clean)) return false
        if (TagTranslations.hasTranslation(clean)) return true
        if (TagTranslations.isSpanishText(clean)) return true
        return false
    }

    private fun isBlacklistedTag(tag: String): Boolean {
        val lower = tag.lowercase().trim()
        val unaccented = TagTranslations.stripAccents(lower)

        // Géneros compuestos redundantes de TMDB (se descomponen en sus partes atómicas)
        if ((unaccented.contains("ciencia ficcion") || unaccented.contains("sci-fi") || unaccented.contains("scifi") || unaccented.contains("science fiction")) &&
            (unaccented.contains("fantasia") || unaccented.contains("fantasy"))) return true

        if ((unaccented.contains("accion") || unaccented.contains("action")) &&
            (unaccented.contains("aventura") || unaccented.contains("adventure"))) return true

        if ((unaccented.contains("guerra") || unaccented.contains("belica") || unaccented.contains("war")) &&
            (unaccented.contains("politica") || unaccented.contains("politics"))) return true

        // Metadata técnica de scrapers
        if (lower.contains("creditsstinger") ||
            lower.contains("aftercredits") ||
            lower.contains("duringcredits") ||
            lower.contains("short film") ||
            lower.contains("feature length") ||
            lower.contains("stand-alone") ||
            lower.contains("special") ||
            lower.contains("remake") ||
            lower.contains("reboot") ||
            lower.contains("sequel") ||
            lower.contains("prequel") ||
            lower.contains("spin-off") ||
            lower.contains("live action") ||
            lower.contains("director") ||
            lower.contains("protagonist") ||
            lower.contains("based on") ||
            lower.contains("play or musical") ||
            lower.contains("miniseries") ||
            lower.contains("anthology") ||
            lower.contains("pelicula de tv") ||
            lower.contains("película de tv") ||
            lower.contains("pelicula para tv") ||
            lower.contains("película para tv") ||
            lower.contains("tv movie") ||
            lower.contains("telefilme") ||
            lower.contains("made for tv")
        ) return true

        // Contenido explícito / sensible no deseado en constelación
        if (lower in setOf("rape", "suicide", "nudity", "sexual abuse", "gore", "violence", "pedophilia", "incest")) return true

        // Frases genéricas de relaciones familiares de TMDB
        if (lower.contains("relationship") || lower == "loss of loved one" || lower.contains("death")) return true

        // Países y ciudades que no son géneros
        if (lower in setOf(
            "germany", "england", "great britain", "united kingdom", "uk", "usa", "united states",
            "france", "canada", "mexico", "japan", "spain", "italy", "australia",
            "los angeles", "new york", "chicago", "paris", "london", "tokyo", "berlin",
            "aleman", "alemania", "francia", "inglaterra", "estados unidos", "japon"
        )) return true

        return false
    }

    private fun createEmptyUniverse() {
        val sunNode = SpatialNebulaNode(
            id = SUN_CORE_ID,
            label = "SOL CENTRAL",
            worldX = 0.5f,
            worldY = 0.5f,
            isSun = true,
            isPortal = true
        )
        addNode(sunNode)
    }

    private fun isNicheNonCaptainTag(tag: String): Boolean {
        val lower = tag.lowercase(Locale.ROOT)
        return lower in setOf(
            "lgbtq+", "lgbt", "tematica lgbtq+", "biografico", "biografia",
            "western", "amistad", "asesinato", "musica", "deportes", "navidad",
            "instituto", "colegio", "universidad", "viajes en el tiempo",
            "pueblo pequeno", "trauma", "secuestro", "infidelidad", "hospital",
            "abogados", "perros y mascotas", "shounen", "sitcom", "humor"
        )
    }

    private fun JellyfinMediaEntity.matchesFormat(format: MediaFormat): Boolean = when (format) {
        MediaFormat.ALL -> true
        MediaFormat.MOVIES -> type.equals("Movie", ignoreCase = true)
        MediaFormat.SERIES -> type.equals("Series", ignoreCase = true)
    }

    private fun formatNoun(format: MediaFormat): String = when (format) {
        MediaFormat.ALL -> "títulos"
        MediaFormat.MOVIES -> "películas"
        MediaFormat.SERIES -> "series"
    }


    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
        val posterUrl = if (!primaryImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Primary?quality=90&fillWidth=400&fillHeight=600"
        } else null

        val backdropUrl = if (!backdropImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Backdrop/0?quality=90&maxWidth=1920"
        } else posterUrl

        return MediaItem(
            id = id,
            title = title,
            overview = overview,
            type = type,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = communityRating,
            year = productionYear,
            source = MediaSource.JELLYFIN,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite,
            totalEpisodes = totalItemCount,
            playedEpisodes = if (unplayedItemCount != null && totalItemCount != null) {
                (totalItemCount - unplayedItemCount).coerceAtLeast(0)
            } else null
        )
    }
}
