package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.medusa.MedusaBrain
import com.example.tujelly.data.medusa.MedusaNeuron

/**
 * Afinidad entre un título local y una neurona (producto escalar conceptual).
 */
data class NeuronRef(val neuronId: String, val weight: Double)

/**
 * Motor neuronal on-device de la Medusa.
 *
 * Convierte el cerebro genérico (18 neuronas semánticas) en una constelación
 * viva calculada a partir de la biblioteca local y el historial del usuario:
 *   - Clasifica cada título en sus neuronas (itemRelevance en tiempo real).
 *   - Calcula la afinidad de usuario (Hebbian) de cada neurona.
 *   - Proyecta la morfología (cúpula / cuerpo / tentáculos).
 *   - Puntúa recomendaciones siguiendo pesos sinápticos y saltos mutantes.
 */
class MedusaNeuralEngine {

    fun buildStars(brain: MedusaBrain, items: List<JellyfinMediaEntity>): List<MedusaStar> {
        val relevance = buildItemRelevance(brain, items)
        val affinities = computeUserAffinities(brain, items, relevance)
        val positions = assignMedusaMorphology(brain, affinities)

        return brain.neurons.map { neuron ->
            val pos = positions[neuron.id] ?: Pair(0.5f, 0.5f)
            val normX = pos.first
            val normY = pos.second

            val related = neuron.synapticWeights.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .toMutableList()
            neuron.mutantJump?.let { if (it !in related) related.add(it) }

            MedusaStar(
                id = neuron.id,
                label = neuron.label,
                baseNormX = normX,
                baseNormY = normY,
                textOnLeft = normX < 0.5f,
                relatedStarIds = related.distinct().take(8),
                primaryGenres = neuron.genres,
                secondaryKeywords = neuron.keywords
            )
        }
    }

    /**
     * Matriz de pertenencia títulos -> neuronas, calculada en vivo.
     */
    fun buildItemRelevance(brain: MedusaBrain, items: List<JellyfinMediaEntity>): Map<String, List<NeuronRef>> {
        val result = mutableMapOf<String, List<NeuronRef>>()
        for (item in items) {
            val genres = splitGenres(item.genres)
            val titleLower = item.title.lowercase()
            val scored = brain.neurons
                .map { neuron -> neuron to neuronAffinity(neuron, genres, titleLower) }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .take(2)
            if (scored.isEmpty()) continue
            val total = scored.sumOf { it.second }
            result[item.id] = scored.map { NeuronRef(it.first.id, it.second / total) }
        }
        return result
    }

    /**
     * Puntúa el catálogo contra las neuronas seleccionadas, propagando la señal
     * por la matriz de pesos sinápticos y el salto mutante.
     */
    fun scoreItems(
        brain: MedusaBrain,
        items: List<JellyfinMediaEntity>,
        itemRelevance: Map<String, List<NeuronRef>>,
        selectedNeuronIds: Set<String>
    ): List<JellyfinMediaEntity> {
        if (selectedNeuronIds.isEmpty()) return emptyList()
        val neuronsById = brain.neurons.associateBy { it.id }

        val scored = items.mapNotNull { item ->
            val refs = itemRelevance[item.id] ?: return@mapNotNull null
            var score = 0.0

            for (ref in refs) {
                if (ref.neuronId in selectedNeuronIds) {
                    score += ref.weight * 100.0
                    continue
                }
                for (selectedId in selectedNeuronIds) {
                    val selected = neuronsById[selectedId] ?: continue
                    val syn = selected.synapticWeights[ref.neuronId]
                    if (syn != null) score += ref.weight * syn * 60.0
                    if (selected.mutantJump == ref.neuronId) score += ref.weight * 40.0
                }
            }

            if (score <= 0.0) return@mapNotNull null

            score += (item.communityRating ?: 6.0).toDouble() * 2.5
            if (!item.backdropImageTag.isNullOrBlank()) score += 4.0
            if (!item.isPlayed) score += 6.0

            item to score
        }

        return scored.sortedByDescending { it.second }.take(25).map { it.first }
    }

    // =========================================================================
    // Afinidad de usuario (Hebbian) y morfología
    // =========================================================================

    private fun computeUserAffinities(
        brain: MedusaBrain,
        items: List<JellyfinMediaEntity>,
        relevance: Map<String, List<NeuronRef>>
    ): Map<String, Double> {
        val raw = brain.neurons.associate { it.id to 0.0 }.toMutableMap()
        for (item in items) {
            val refs = relevance[item.id] ?: continue
            val hebbian = hebbianWeight(item)
            for (ref in refs) {
                raw[ref.neuronId] = (raw[ref.neuronId] ?: 0.0) + hebbian * ref.weight
            }
        }
        val min = raw.values.minOrNull() ?: 0.0
        val max = raw.values.maxOrNull() ?: 1.0
        return if (max - min < 1e-6) {
            raw.mapValues { 0.5 }
        } else {
            raw.mapValues { (it.value - min) / (max - min) }
        }
    }

    /**
     * Proyecta las 18 neuronas en la morfología celestial de la Medusa (cúpula, cuerpo y tentáculos).
     * Las neuronas con mayor afinidad de usuario ocupan la corona superior.
     * La distribución 2D previene solapamientos y líneas diagonales apiñadas.
     */
    private fun assignMedusaMorphology(brain: MedusaBrain, affinities: Map<String, Double>): Map<String, Pair<Float, Float>> {
        val sorted = brain.neurons.sortedByDescending { affinities[it.id] ?: 0.0 }
        val slots = listOf(
            // Cúpula superior (los 5 conceptos favoritos/más vistos en la corona)
            Pair(0.50f, 0.09f),
            Pair(0.32f, 0.16f),
            Pair(0.68f, 0.16f),
            Pair(0.18f, 0.23f),
            Pair(0.82f, 0.23f),

            // Cuerpo y manto (7 conceptos centrales)
            Pair(0.50f, 0.28f),
            Pair(0.34f, 0.31f),
            Pair(0.66f, 0.31f),
            Pair(0.19f, 0.39f),
            Pair(0.39f, 0.42f),
            Pair(0.61f, 0.42f),
            Pair(0.81f, 0.39f),

            // Tentáculos inferiores flotantes (6 conceptos periféricos/nicho)
            Pair(0.22f, 0.54f),
            Pair(0.78f, 0.54f),
            Pair(0.42f, 0.58f),
            Pair(0.58f, 0.58f),
            Pair(0.30f, 0.72f),
            Pair(0.70f, 0.72f)
        )

        val result = mutableMapOf<String, Pair<Float, Float>>()
        sorted.forEachIndexed { index, neuron ->
            val slot = slots.getOrElse(index) { Pair(0.50f, 0.50f) }
            result[neuron.id] = slot
        }
        return result
    }

    // =========================================================================
    // Helpers de afinidad
    // =========================================================================

    private fun neuronAffinity(neuron: MedusaNeuron, genres: Set<String>, titleLower: String): Double {
        var score = 0.0
        for (g in neuron.genres) {
            if (genres.contains(g.lowercase())) score += 1.0
        }
        for (k in neuron.keywords) {
            if (k.length >= 3 && titleLower.contains(k.lowercase())) score += 0.5
        }
        return score
    }

    private fun splitGenres(genres: String?): Set<String> {
        if (genres.isNullOrBlank()) return emptySet()
        return genres.split(",", ";", "/").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    private fun hebbianWeight(entity: JellyfinMediaEntity): Double {
        var weight = 1.0
        if (entity.isFavorite) {
            weight *= 2.0
        } else if (entity.isPlayed || entity.playbackPositionTicks > 0) {
            weight *= 1.5
        }
        val rating = (entity.communityRating ?: 6.0f).toDouble().coerceIn(0.0, 10.0)
        weight *= (0.6 + rating / 10.0)
        return weight
    }
}
