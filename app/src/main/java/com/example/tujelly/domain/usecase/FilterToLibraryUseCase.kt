package com.example.tujelly.domain.usecase

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.remote.tmdb.TmdbItemDto
import com.example.tujelly.data.remote.trakt.TraktMediaDto
import com.example.tujelly.data.repository.MediaRepository

class FilterToLibraryUseCase(
    private val mediaRepository: MediaRepository
) {
    /**
     * Filters Trakt items against the user's Jellyfin library.
     * Uses strictly local SQLite (Room) index lookups (0.1ms per item).
     * NEVER sends remote search queries to Jellyfin to prevent server CPU overload.
     */
    suspend fun filterTraktItems(
        traktItems: List<TraktMediaDto>,
        serverUrl: String,
        userId: String,
        token: String,
        maxCandidates: Int = 20
    ): List<JellyfinMediaEntity> {
        val seenIds = mutableSetOf<String>()
        val candidates = traktItems.take(maxCandidates)

        return candidates.mapNotNull { item ->
            val tmdbId = item.ids.tmdb?.toString()
            val imdbId = item.ids.imdb
            val title = item.title
            val year = item.year

            mediaRepository.findLocal(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = title,
                year = year
            )
        }.filter { seenIds.add(it.id) }
    }

    /**
     * Filters TMDb items (Trending, Streaming Providers, Recommendations) against Jellyfin library.
     * Uses strictly local SQLite (Room) index lookups (0.1ms per item).
     * NEVER sends remote search queries to Jellyfin to prevent server CPU overload.
     */
    suspend fun filterTmdbItems(
        tmdbItems: List<TmdbItemDto>,
        serverUrl: String,
        userId: String,
        token: String,
        maxCandidates: Int = 20
    ): List<JellyfinMediaEntity> {
        val seenIds = mutableSetOf<String>()
        val candidates = tmdbItems.take(maxCandidates)

        return candidates.mapNotNull { item ->
            val tmdbId = item.id.toString()
            val title = item.title ?: item.name ?: return@mapNotNull null
            val year = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull()

            mediaRepository.findLocal(
                tmdbId = tmdbId,
                imdbId = null,
                title = title,
                year = year
            )
        }.filter { seenIds.add(it.id) }
    }
}
