package com.example.tujelly.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JellyfinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<JellyfinMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: JellyfinMediaEntity)

    @Query("SELECT * FROM jellyfin_media")
    fun getAllItems(): Flow<List<JellyfinMediaEntity>>

    @Query("SELECT * FROM jellyfin_media WHERE tmdbId = :tmdbId ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByTmdbId(tmdbId: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE imdbId = :imdbId ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByImdbId(imdbId: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE (:tmdbId IS NOT NULL AND tmdbId = :tmdbId) OR (:imdbId IS NOT NULL AND imdbId = :imdbId) ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByTmdbOrImdb(tmdbId: String?, imdbId: String?): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE (LOWER(title) = LOWER(:title) OR LOWER(originalTitle) = LOWER(:title) OR LOWER(seriesName) = LOWER(:title)) AND productionYear = :year ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByTitleAndYear(title: String, year: Int): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE LOWER(title) = LOWER(:title) OR LOWER(originalTitle) = LOWER(:title) OR LOWER(seriesName) = LOWER(:title) ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByTitle(title: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE LOWER(title) LIKE '%' || LOWER(:title) || '%' OR LOWER(originalTitle) LIKE '%' || LOWER(:title) || '%' OR LOWER(seriesName) LIKE '%' || LOWER(:title) || '%' ORDER BY CASE WHEN type = 'Series' THEN 0 WHEN type = 'Movie' THEN 1 ELSE 2 END LIMIT 1")
    suspend fun findByFuzzyTitle(title: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE seriesId = :seriesId AND type = 'Episode'")
    suspend fun getEpisodesForSeries(seriesId: String): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE (LOWER(title) = LOWER(:title) OR LOWER(originalTitle) = LOWER(:title)) AND type = 'Series' LIMIT 1")
    suspend fun getSeriesByTitle(title: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE type IN ('Movie', 'Series') AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(originalTitle) LIKE '%' || LOWER(:query) || '%') ORDER BY communityRating DESC LIMIT :limit")
    suspend fun searchLocalMedia(query: String, limit: Int = 40): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): JellyfinMediaEntity?

    @Query("SELECT * FROM jellyfin_media WHERE type = 'Movie' ORDER BY title ASC")
    suspend fun getMovies(): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE type = 'Series' ORDER BY title ASC")
    suspend fun getSeries(): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE type IN ('Movie', 'Series') ORDER BY communityRating DESC LIMIT :limit")
    suspend fun getTopRatedLocal(limit: Int = 20): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE type = 'Movie' AND communityRating >= 7.0 ORDER BY communityRating DESC LIMIT :limit")
    suspend fun getTopMoviesLocal(limit: Int = 20): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE type = 'Series' ORDER BY communityRating DESC LIMIT :limit")
    suspend fun getTopSeriesLocal(limit: Int = 20): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE type IN ('Movie', 'Series') AND (LOWER(genres) LIKE '%' || LOWER(:genre) || '%' OR LOWER(title) LIKE '%' || LOWER(:genre) || '%') ORDER BY communityRating DESC LIMIT :limit")
    suspend fun getItemsByGenre(genre: String, limit: Int = 40): List<JellyfinMediaEntity>

    @Query("SELECT genres FROM jellyfin_media WHERE (isPlayed = 1 OR playbackPositionTicks > 0) AND genres IS NOT NULL AND genres != '' LIMIT 300")
    suspend fun getWatchedGenres(): List<String>

    @Query("SELECT genres FROM jellyfin_media WHERE genres IS NOT NULL AND genres != '' LIMIT 500")
    suspend fun getAllGenresRaw(): List<String>

    @Query("SELECT COUNT(*) FROM jellyfin_media")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM jellyfin_media WHERE type IN ('Movie', 'Series')")
    fun getMediaCountFlow(): Flow<Int>

    @Query("SELECT * FROM jellyfin_media WHERE isFavorite = 1 ORDER BY title ASC")
    suspend fun getFavorites(): List<JellyfinMediaEntity>

    @Query("SELECT * FROM jellyfin_media WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoritesFlow(): Flow<List<JellyfinMediaEntity>>

    @Query("UPDATE jellyfin_media SET isFavorite = :isFavorite WHERE id = :itemId")
    suspend fun updateFavoriteStatus(itemId: String, isFavorite: Boolean)

    @Query("DELETE FROM jellyfin_media")
    suspend fun clearAll()

    @Query("SELECT id, overview, backdropImageTag FROM jellyfin_media WHERE overview IS NOT NULL")
    suspend fun getCachedOverviews(): List<CachedOverviewDto>
}

data class CachedOverviewDto(
    val id: String,
    val overview: String?,
    val backdropImageTag: String? = null
)
