package com.example.tujelly.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché de stats de votación de TMDB para evitar el problema N+1:
 * en lugar de llamar a TMDB una vez por película/serie al construir el feed,
 * guardamos el resultado y lo reutilizamos durante [TTL_MS] (24 horas).
 *
 * Esto elimina ~10 segundos de carga en el home feed cuando la caché está caliente.
 */
@Entity(
    tableName = "tmdb_vote_cache",
    indices = [Index(value = ["tmdbId", "isTv"], unique = true)]
)
data class TmdbVoteCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Long,
    val isTv: Boolean,
    val voteAverage: Float,
    val voteCount: Int,
    /** Timestamp Unix en ms cuando se guardó este registro */
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 24 horas en ms. Los ratings de TMDB no cambian en horas. */
        const val TTL_MS = 24L * 60 * 60 * 1000
    }

    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > TTL_MS
}
