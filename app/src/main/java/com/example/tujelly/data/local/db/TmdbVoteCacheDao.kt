package com.example.tujelly.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TmdbVoteCacheDao {

    @Query("SELECT * FROM tmdb_vote_cache WHERE tmdbId = :tmdbId AND isTv = :isTv LIMIT 1")
    suspend fun get(tmdbId: Long, isTv: Boolean): TmdbVoteCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TmdbVoteCacheEntity)

    /** Limpia entradas expiradas (> 24h). Llamar periódicamente para no crecer indefinidamente. */
    @Query("DELETE FROM tmdb_vote_cache WHERE cachedAt < :expiryTimestamp")
    suspend fun deleteExpired(expiryTimestamp: Long = System.currentTimeMillis() - TmdbVoteCacheEntity.TTL_MS)
}
