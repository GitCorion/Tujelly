package com.example.tujelly.data.medusa

import kotlinx.serialization.Serializable

/**
 * Cerebro semántico genérico de la Medusa.
 *
 * Es un artefacto colaborativo y versionado en GitHub: 18 neuronas con su
 * etiqueta, fingerprint de géneros, keywords semánticas y topología sináptica.
 * NO contiene títulos ni datos de usuario; la afinidad personal se calcula
 * on-device a partir de la biblioteca local.
 */
@Serializable
data class MedusaBrain(
    val version: String = "2.0-neural-manifold",
    val brainVersion: Int = 1,
    val totalNeurons: Int = 0,
    val neurons: List<MedusaNeuron> = emptyList()
)

@Serializable
data class MedusaNeuron(
    val id: String,
    val label: String,
    val genres: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val synapticWeights: Map<String, Double> = emptyMap(),
    val mutantJump: String? = null
)
