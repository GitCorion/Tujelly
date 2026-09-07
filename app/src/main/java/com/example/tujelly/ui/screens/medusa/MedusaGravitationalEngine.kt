package com.example.tujelly.ui.screens.medusa

import androidx.compose.ui.geometry.Offset
import com.example.tujelly.data.local.db.JellyfinMediaEntity

/**
 * Matriz de coocurrencia simétrica precalculada para ajustar la luminosidad / afinidad
 * de los 24 atractores en tiempo real sobre la biblioteca local (RFC-004 Sección 3).
 */
data class CooccurrenceMatrix(
    val size: Int,
    private val matrix: Array<FloatArray>
) {
    fun getAffinity(attractorAIndex: Int, attractorBIndex: Int): Float {
        if (attractorAIndex !in 0 until size || attractorBIndex !in 0 until size) return 0.5f
        return matrix[attractorAIndex][attractorBIndex]
    }

    companion object {
        fun buildFromCatalog(
            items: List<JellyfinMediaEntity>,
            attractors: List<Attractor>
        ): CooccurrenceMatrix {
            val size = attractors.size
            val matrix = Array(size) { FloatArray(size) }
            if (items.isEmpty()) return CooccurrenceMatrix(size, matrix)

            // Cachear coincidencias por atractor para cómputo O(N*A) en lugar de O(N*A^2)
            val matches = Array(size) { i ->
                BooleanArray(items.size) { itemIndex ->
                    MedusaGravitationalEngine.matchesAttractor(items[itemIndex], attractors[i])
                }
            }

            val totalCount = items.size.toFloat()
            for (i in 0 until size) {
                var countI = 0
                for (k in 0 until items.size) {
                    if (matches[i][k]) countI++
                }
                matrix[i][i] = (countI / totalCount).coerceIn(0f, 1f)

                for (j in i + 1 until size) {
                    var shared = 0
                    for (k in 0 until items.size) {
                        if (matches[i][k] && matches[j][k]) shared++
                    }
                    val normalizedScore = if (countI > 0) {
                        (shared.toFloat() / countI.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    matrix[i][j] = normalizedScore
                    matrix[j][i] = normalizedScore
                }
            }
            return CooccurrenceMatrix(size, matrix)
        }
    }
}

/**
 * Resultado de evaluación gravitacional de una obra del catálogo.
 */
data class ScoredMedia(
    val entity: JellyfinMediaEntity,
    val score: Float,
    val matchedAttractorCount: Int
)

/**
 * Motor de descubrimiento gravitacional "Medusa" (RFC-004).
 */
object MedusaGravitationalEngine {

    /**
     * Comprueba si una obra cumple con los requisitos y exclusiones de un atractor.
     */
    fun matchesAttractor(item: JellyfinMediaEntity, attractor: Attractor): Boolean {
        val genresText = item.genres?.lowercase() ?: ""
        val tagsText = item.tags?.lowercase() ?: ""
        val overviewText = item.overview?.lowercase() ?: ""
        val titleText = item.title.lowercase()

        // 1. Filtro estricto de exclusión por tags
        if (attractor.excludeTags.isNotEmpty()) {
            val hasExcludedTag = attractor.excludeTags.any { exc ->
                tagsText.contains(exc) || overviewText.contains(exc) || genresText.contains(exc)
            }
            if (hasExcludedTag) return false
        }

        // 2. Filtro estricto de exclusión por géneros
        if (attractor.excludeGenres.isNotEmpty()) {
            val hasExcludedGenre = attractor.excludeGenres.any { exc ->
                genresText.contains(exc.lowercase())
            }
            if (hasExcludedGenre) return false
        }

        // 3. Filtro parental por clasificación por edad
        if (attractor.ratingMax != null) {
            val isRestricted = isRatingExceeded(item, attractor.ratingMax)
            if (isRestricted) return false
        }

        // 4. Coincidencia positiva por género o tag
        var matched = false

        if (attractor.genres.isNotEmpty()) {
            matched = attractor.genres.any { g ->
                genresText.contains(g.lowercase())
            }
        }

        if (!matched && attractor.tags.isNotEmpty()) {
            matched = attractor.tags.any { t ->
                tagsText.contains(t.lowercase()) ||
                overviewText.contains(t.lowercase()) ||
                titleText.contains(t.lowercase())
            }
        }

        return matched
    }

    /**
     * Evalúa el catálogo frente a los atractores seleccionados (1 <= k <= 4).
     * Aplica la fórmula RFC-004:
     * Score(m) = P(m) * [ beta * S_tags(m, A_sel) + gamma * U(m) ]
     */
    fun evaluateCatalog(
        catalog: List<JellyfinMediaEntity>,
        activeAttractors: List<Attractor>,
        limit: Int = 40
    ): List<ScoredMedia> {
        if (activeAttractors.isEmpty() || catalog.isEmpty()) {
            return emptyList()
        }

        val k = activeAttractors.size.toFloat()
        val beta = 0.80f
        val gamma = 0.20f

        val results = mutableListOf<ScoredMedia>()

        for (item in catalog) {
            // P(m): Filtro binario estricto
            var isPermitted = true
            var matchedCount = 0
            var tagAccumulator = 0f

            for (attractor in activeAttractors) {
                // Verificar si incumple clasificaciones o exclusiones
                if (attractor.excludeTags.any { exc ->
                        (item.tags?.lowercase()?.contains(exc) == true) ||
                        (item.overview?.lowercase()?.contains(exc) == true)
                    }) {
                    isPermitted = false
                    break
                }
                if (attractor.excludeGenres.any { exc ->
                        item.genres?.lowercase()?.contains(exc.lowercase()) == true
                    }) {
                    isPermitted = false
                    break
                }
                if (attractor.ratingMax != null && isRatingExceeded(item, attractor.ratingMax)) {
                    isPermitted = false
                    break
                }

                // Cálculo de afinidad con este atractor
                val matches = matchesAttractor(item, attractor)
                if (matches) {
                    matchedCount++
                    tagAccumulator += attractor.boostFactor
                }
            }

            if (!isPermitted || matchedCount == 0) {
                continue
            }

            // S_tags normalizado según el grado de solapamiento
            val sTags = (tagAccumulator / k).coerceIn(0f, 2f)

            // U(m): Modificador de historial (+0.15 favoritos, -0.50 ya visto)
            var uMod = 0f
            if (item.isFavorite) uMod += 0.15f
            if (item.isPlayed) uMod -= 0.50f

            // Calidad comunitaria sutil (+0.05 a +0.10 para obras aclamadas)
            val ratingBoost = ((item.communityRating ?: 6f) - 5f).coerceAtLeast(0f) * 0.02f

            val finalScore = (beta * sTags + gamma * uMod + ratingBoost).coerceAtLeast(0.01f)

            results.add(ScoredMedia(item, finalScore, matchedCount))
        }

        // Ordenar primero por mayor cantidad de atractores cruzados, luego por score final
        return results
            .sortedWith(
                compareByDescending<ScoredMedia> { it.matchedAttractorCount }
                    .thenByDescending { it.score }
            )
            .take(limit)
    }

    /**
     * Calcula la afinidad dinámica (0.15f .. 1.0f) para cada atractor según la selección actual
     * y las obras reales existentes en la biblioteca local.
     */
    fun calculateAffinityWeights(
        allAttractors: List<Attractor>,
        activeAttractors: List<Attractor>,
        catalog: List<JellyfinMediaEntity>,
        matrix: CooccurrenceMatrix?
    ): Map<String, Float> {
        if (activeAttractors.isEmpty()) {
            return allAttractors.associate { it.id to 1.0f }
        }

        val activeIds = activeAttractors.map { it.id }.toSet()
        val indexMap = allAttractors.mapIndexed { idx, att -> att.id to idx }.toMap()

        val result = mutableMapOf<String, Float>()

        for (attractor in allAttractors) {
            if (attractor.id in activeIds) {
                result[attractor.id] = 1.0f
                continue
            }

            val targetIndex = indexMap[attractor.id] ?: -1
            if (matrix != null && targetIndex >= 0) {
                // Afinidad promedio frente a todos los atractores seleccionados
                var totalAffinity = 0f
                for (active in activeAttractors) {
                    val activeIndex = indexMap[active.id] ?: -1
                    totalAffinity += matrix.getAffinity(activeIndex, targetIndex)
                }
                val avgAffinity = totalAffinity / activeAttractors.size.toFloat()
                // Mapear a rango [0.20f, 1.0f]
                result[attractor.id] = (avgAffinity * 1.5f).coerceIn(0.20f, 1.0f)
            } else {
                result[attractor.id] = 0.8f
            }
        }

        return result
    }

    /**
     * Calcula el centroide 2D normalizado de atracción para los puntos activos.
     */
    fun calculateCentroid(points: List<Offset>): Offset {
        if (points.isEmpty()) return Offset(0.5f, 0.5f)
        var sumX = 0f
        var sumY = 0f
        points.forEach {
            sumX += it.x
            sumY += it.y
        }
        return Offset(sumX / points.size.toFloat(), sumY / points.size.toFloat())
    }

    /**
     * Valida si un item supera la clasificación máxima permitida (ej: PG, G, PG-13).
     */
    private fun isRatingExceeded(item: JellyfinMediaEntity, ratingMax: String): Boolean {
        val overview = (item.overview ?: "").lowercase()
        val tags = (item.tags ?: "").lowercase()

        when (ratingMax) {
            "G", "TV-Y" -> {
                // Exclusión severa: no puede tener palabras de sangre, terror o violencia
                if (tags.contains("horror") || tags.contains("violence") || tags.contains("terror") ||
                    tags.contains("gore") || tags.contains("blood") || tags.contains("sexual") ||
                    overview.contains("asesinato") || overview.contains("sangre") || overview.contains("muerte")
                ) {
                    return true
                }
            }
            "PG", "TV-PG" -> {
                if (tags.contains("gore") || tags.contains("slasher") || tags.contains("sexual") ||
                    tags.contains("erotic") || tags.contains("nudity") || tags.contains("torture")
                ) {
                    return true
                }
            }
            "PG-13", "TV-14" -> {
                if (tags.contains("gore") || tags.contains("slasher") || tags.contains("torture") ||
                    tags.contains("erotic") || tags.contains("explicit") || tags.contains("hardcore")
                ) {
                    return true
                }
            }
        }
        return false
    }
}
