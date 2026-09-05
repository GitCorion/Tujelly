package com.example.tujelly.ui.screens.genre

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.tmdb.TmdbApiService
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GenreUiState {
    object Loading : GenreUiState
    data class Success(
        val genreName: String,
        val sections: List<HomeSection>,
        val focusedItem: MediaItem? = null
    ) : GenreUiState
    data class Error(val message: String) : GenreUiState
}

class GenreViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(application)
    private val mediaRepository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao())
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    fun loadGenreFeed(genreName: String) {
        viewModelScope.launch {
            _uiState.value = GenreUiState.Loading

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val sections = mutableListOf<HomeSection>()

            // 1. Local items for this genre
            val localItems = mediaRepository.getItemsByGenre(genreName)
            if (localItems.isNotEmpty()) {
                sections.add(
                    HomeSection(
                        title = "$genreName en tu biblioteca",
                        items = localItems.take(20).map {
                            it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                        },
                        badge = "EN BIBLIOTECA"
                    )
                )
            }

            // Map genre name to TMDB genre ID
            val tmdbGenreId = when (genreName.lowercase()) {
                "acción", "accion" -> "28"
                "aventura" -> "12"
                "animación", "animacion" -> "16"
                "comedia" -> "35"
                "crimen" -> "80"
                "documental" -> "99"
                "drama" -> "18"
                "familia", "familiar" -> "10751"
                "fantasía", "fantasia" -> "14"
                "historia" -> "36"
                "terror" -> "27"
                "música", "musica" -> "10402"
                "misterio" -> "9648"
                "romance" -> "10749"
                "ciencia ficción", "ciencia ficcion" -> "878"
                "thriller", "suspense" -> "53"
                "bélica", "belica" -> "10752"
                "western" -> "37"
                else -> "28"
            }

            // 2. Discover TMDB items for this genre matched to library
            if (prefs.tmdbApiKey.isNotBlank()) {
                try {
                    val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
                    val movieGenre = runCatching { api.discoverMoviesByGenre(apiKey = prefs.tmdbApiKey, genreId = tmdbGenreId) }.getOrNull()?.results ?: emptyList()
                    val tvGenre = runCatching { api.discoverTvByGenre(apiKey = prefs.tmdbApiKey, genreId = tmdbGenreId) }.getOrNull()?.results ?: emptyList()

                    val matchedTmdb = filterToLibraryUseCase.filterTmdbItems(
                        tmdbItems = (movieGenre + tvGenre).take(30),
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        maxCandidates = 30
                    )

                    if (matchedTmdb.isNotEmpty()) {
                        sections.add(
                            HomeSection(
                                title = "Grandes Éxitos de $genreName",
                                items = matchedTmdb.take(15).map {
                                    it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                                },
                                badge = "POPULAR"
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            if (sections.isEmpty()) {
                _uiState.value = GenreUiState.Error("No se encontraron contenidos para el género '$genreName'.")
            } else {
                val firstItem = sections.firstOrNull()?.items?.firstOrNull()
                _uiState.value = GenreUiState.Success(
                    genreName = genreName,
                    sections = sections,
                    focusedItem = firstItem
                )
            }
        }
    }

    fun setFocusedItem(item: MediaItem) {
        val currentState = _uiState.value
        if (currentState is GenreUiState.Success) {
            _uiState.value = currentState.copy(focusedItem = item)
        }
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "api_key=$token" else ""
        val tagParam = if (!primaryImageTag.isNullOrEmpty()) "&tag=$primaryImageTag" else ""
        val posterUrl = "$baseUrl/Items/$id/Images/Primary?$authParam$tagParam"

        val backdropTagParam = if (!backdropImageTag.isNullOrEmpty()) "&tag=$backdropImageTag" else ""
        val backdropUrl = "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"

        val total = totalItemCount
        val unplayed = unplayedItemCount
        val played = if (total != null && unplayed != null) (total - unplayed).coerceAtLeast(0) else null

        return MediaItem(
            id = id,
            title = title,
            overview = overview,
            type = type,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = communityRating,
            year = productionYear,
            source = source,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
