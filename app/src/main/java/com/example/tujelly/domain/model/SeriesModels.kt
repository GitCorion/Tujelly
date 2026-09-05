package com.example.tujelly.domain.model

enum class SeriesStatusType {
    CONTINUING, // En emisión / continúa
    ENDED,      // Terminada (concluyó su historia)
    CANCELED,   // Suspendida / Cancelada (se quedó sin final)
    UNKNOWN
}

data class SeriesStatus(
    val type: SeriesStatusType,
    val label: String,
    val description: String? = null
) {
    companion object {
        fun from(tmdbStatus: String?, jellyfinStatus: String?): SeriesStatus {
            val statusClean = tmdbStatus?.trim()?.lowercase()
            return when {
                statusClean == "canceled" || statusClean == "cancelled" -> {
                    SeriesStatus(
                        type = SeriesStatusType.CANCELED,
                        label = "Suspendida (Sin final)",
                        description = "Cancelada por la cadena sin final previsto"
                    )
                }
                statusClean == "ended" -> {
                    SeriesStatus(
                        type = SeriesStatusType.ENDED,
                        label = "Terminada",
                        description = "Serie concluida con su final"
                    )
                }
                statusClean in listOf("returning series", "in production", "planned") -> {
                    SeriesStatus(
                        type = SeriesStatusType.CONTINUING,
                        label = "Continúa",
                        description = "En emisión o próximas temporadas confirmadas"
                    )
                }
                jellyfinStatus?.equals("Ended", ignoreCase = true) == true -> {
                    SeriesStatus(
                        type = SeriesStatusType.ENDED,
                        label = "Terminada",
                        description = "Serie concluida"
                    )
                }
                jellyfinStatus?.equals("Continuing", ignoreCase = true) == true -> {
                    SeriesStatus(
                        type = SeriesStatusType.CONTINUING,
                        label = "Continúa",
                        description = "En emisión"
                    )
                }
                else -> {
                    SeriesStatus(
                        type = SeriesStatusType.UNKNOWN,
                        label = "Serie"
                    )
                }
            }
        }
    }
}

data class SeasonItem(
    val id: String,
    val name: String,
    val seasonNumber: Int,
    val episodeCount: Int = 0
)

data class EpisodeItem(
    val id: String,
    val name: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val overview: String? = null,
    val imageUrl: String? = null,
    val durationMinutes: Int? = null,
    val isPlayed: Boolean = false,
    val playbackPositionTicks: Long = 0L
) {
    val displayCode: String get() = "T${seasonNumber}:E${episodeNumber}"
    val formattedDuration: String? get() = durationMinutes?.takeIf { it > 0 }?.let { "$it min" }
}
