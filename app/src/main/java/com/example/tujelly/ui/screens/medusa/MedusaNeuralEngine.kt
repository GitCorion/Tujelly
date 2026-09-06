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
        val yByNeuron = assignMorphologyY(brain, affinities)
        val xByNeuron = assignMorphologyX(brain)

        return brain.neurons.map { neuron ->
            val normX = xByNeuron[neuron.id] ?: 0.5f
            val normY = yByNeuron[neuron.id] ?: 0.5f

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
                relatedStarIds = related.distinct().take(4),
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

    private fun assignMorphologyY(brain: MedusaBrain, affinities: Map<String, Double>): Map<String, Float> {
        val sortedIds = brain.neurons
            .sortedByDescending { affinities[it.id] ?: 0.0 }
            .map { it.id }

        val cupola = sortedIds.take(5)
        val body = sortedIds.drop(5).take(8)
        val tentacles = sortedIds.drop(13)

        val result = mutableMapOf<String, Float>()
        cupola.forEachIndexed { i, id -> result[id] = 0.08f + (0.22f - 0.08f) * (i / 4f) }
        body.forEachIndexed { i, id -> result[id] = 0.25f + (0.50f - 0.25f) * (i / 7f) }
        tentacles.forEachIndexed { i, id -> result[id] = 0.52f + (0.78f - 0.52f) * (i / 4f) }
        return result
    }

    /**
     * Distribuye X en [0.15, 0.85] ordenando las neuronas por similitud angular
     * (overlap de géneros/keywords) para que las afines queden próximas.
     */
    private fun assignMorphologyX(brain: MedusaBrain): Map<String, Float> {
        val neurons = brain.neurons
        if (neurons.isEmpty()) return emptyMap()

        val byId = neurons.associateBy { it.id }
        val remaining = neurons.map { it.id }.toMutableList()
        val order = mutableListOf<String>()

        var current = remaining.removeAt(0)
        order.add(current)
        while (remaining.isNotEmpty()) {
            val cur = byId[current] ?: break
            val next = remaining.maxByOrNull { id -> neuronSimilarity(cur, byId[id] ?: return@maxByOrNull 0.0) } ?: break
            remaining.remove(next)
            order.add(next)
            current = next
        }
        order.addAll(remaining)

        val denom = (order.size - 1).coerceAtLeast(1)
        val result = mutableMapOf<String, Float>()
        order.forEachIndexed { index, id ->
            result[id] = 0.15f + (index.toFloat() / denom) * 0.70f
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

    private fun neuronSimilarity(a: MedusaNeuron, b: MedusaNeuron): Double {
        val genreSim = jaccard(a.genres.map { it.lowercase() }.toSet(), b.genres.map { it.lowercase() }.toSet())
        val keywordSim = jaccard(a.keywords.map { it.lowercase() }.toSet(), b.keywords.map { it.lowercase() }.toSet())
        return genreSim + 0.5 * keywordSim
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        val union = a.union(b).size.toDouble()
        if (union == 0.0) return 0.0
        return a.intersect(b).size.toDouble() / union
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
