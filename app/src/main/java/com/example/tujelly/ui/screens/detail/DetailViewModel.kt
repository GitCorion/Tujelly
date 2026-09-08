package com.example.tujelly.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.EpisodeItem
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import com.example.tujelly.domain.model.SeasonItem
import com.example.tujelly.domain.model.SeriesStatus
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val entity: JellyfinMediaEntity,
        val posterUrl: String?,
        val backdropUrl: String?,
        val logoUrl: String? = null,
        val trailerUrl: String? = null,
        val baseUrl: String? = null,
        val isFavorite: Boolean = false,
        val isPlayed: Boolean = false,
        val buttonStyle: String = "ICONS_ONLY",
        val accentColor: String = com.example.tujelly.data.local.ACCENT_CYAN,
        val seriesStatus: SeriesStatus? = null,
        val seasons: List<SeasonItem> = emptyList(),
        val episodes: List<EpisodeItem> = emptyList(),
        val selectedSeasonId: String? = null,
        val nextUpEpisode: EpisodeItem? = null,
        val isLoadingEpisodes: Boolean = false,
        val similarItems: List<MediaItem> = emptyList(),
        val genreItems: List<MediaItem> = emptyList(),
        val genreName: String? = null,
        val collectionItems: List<MediaItem> = emptyList(),
        val isCollection: Boolean = false
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.jellyfinDao())

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadDetail(itemId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val local = database.jellyfinDao().getItemById(itemId)
                val entity = if (local != null && !local.overview.isNullOrBlank()) {
                    local
                } else {
                    mediaRepository.getItemDetail(
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        itemId = itemId
                    ).getOrNull() ?: local
                }

                if (entity != null) {
                    val isTv = entity.type.equals("Series", ignoreCase = true) || entity.type.equals("Episode", ignoreCase = true)
                    val tmdbLong = entity.tmdbId?.toLongOrNull()

                    // Try fetching overview from TMDB if local overview is missing
                    var finalEntity = entity
                    if (finalEntity.overview.isNullOrBlank() && tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                        try {
                            val tmdbApi = com.example.tujelly.data.remote.NetworkClientFactory.createService("https://api.themoviedb.org/3/", com.example.tujelly.data.remote.tmdb.TmdbApiService::class.java)
                            val tmdbOverview: String? = if (isTv) {
                                runCatching { tmdbApi.getTvDetails(tmdbLong, prefs.tmdbApiKey).overview }.getOrNull()
                            } else {
                                runCatching { tmdbApi.getMovieDetails(tmdbLong, prefs.tmdbApiKey).overview }.getOrNull()
                            }
                            if (!tmdbOverview.isNullOrBlank()) {
                                finalEntity = finalEntity.copy(overview = tmdbOverview)
                                database.jellyfinDao().insertOrUpdate(finalEntity)
                            }
                        } catch (_: Exception) {}
                    }

                    val baseUrl = prefs.jellyfinServerUrl
                    val token = prefs.jellyfinAccessToken
                    val authParam = if (token.isNotBlank()) "&api_key=$token" else ""
                    val posterUrl = finalEntity.primaryImageTag?.let { tag -> "$baseUrl/Items/${finalEntity.id}/Images/Primary?tag=$tag$authParam" }
                    var backdropUrl = finalEntity.backdropImageTag?.let { tag -> "$baseUrl/Items/${finalEntity.id}/Images/Backdrop/0?tag=$tag$authParam" }
                    val logoUrl = if (baseUrl.isNotBlank()) "$baseUrl/Items/${finalEntity.id}/Images/Logo$authParam" else null

                    // If viewing an episode, resolve seriesName and parent backdrop if needed
                    val currentSeriesId = finalEntity.seriesId
                    if (finalEntity.type.equals("Episode", ignoreCase = true) && !currentSeriesId.isNullOrBlank()) {
                        val parentSeries = database.jellyfinDao().getItemById(currentSeriesId)
                        if (parentSeries != null) {
                            if (finalEntity.seriesName.isNullOrBlank()) {
                                finalEntity = finalEntity.copy(seriesName = parentSeries.title)
                            }
                            if (backdropUrl == null && !parentSeries.backdropImageTag.isNullOrBlank()) {
                                backdropUrl = "$baseUrl/Items/${parentSeries.id}/Images/Backdrop/0?tag=${parentSeries.backdropImageTag}$authParam"
                            }
                        }
                    }

                    val trailerUrl = if (tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                        mediaRepository.getTmdbTrailerUrl(prefs.tmdbApiKey, tmdbLong, isTv = isTv)
                    } else null

                    var seriesStatus: SeriesStatus? = null
                    var seasons: List<SeasonItem> = emptyList()
                    var episodes: List<EpisodeItem> = emptyList()
                    var selectedSeasonId: String? = null
                    var nextUpEpisode: EpisodeItem? = null

                    if (isTv && finalEntity.type.equals("Series", ignoreCase = true)) {
                        // 1. Determine series status (Ended, Continuing, Canceled)
                        seriesStatus = mediaRepository.getSeriesStatus(
                            tmdbApiKey = prefs.tmdbApiKey,
                            tmdbId = tmdbLong,
                            jellyfinStatus = null
                        )

                        // 2. Fetch Seasons
                        seasons = mediaRepository.getSeasons(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = finalEntity.id
                        )

                        // 3. Fetch Next Up or first episode
                        nextUpEpisode = mediaRepository.getNextUpEpisode(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = finalEntity.id
                        )

                        // 4. Default to nextUp's season or the first regular season (seasonNumber >= 1)
                        selectedSeasonId = if (nextUpEpisode != null) {
                            seasons.firstOrNull { it.seasonNumber == nextUpEpisode.seasonNumber }?.id
                        } else null

                        if (selectedSeasonId == null) {
                            selectedSeasonId = seasons.firstOrNull { it.seasonNumber >= 1 }?.id
                                ?: seasons.firstOrNull()?.id
                        }

                        // 5. Fetch episodes for selected season
                        episodes = mediaRepository.getEpisodes(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = finalEntity.id,
                            seasonId = selectedSeasonId
                        )

                        if (nextUpEpisode == null && episodes.isNotEmpty()) {
                            nextUpEpisode = episodes.firstOrNull()
                        }
                    } else if (isTv && finalEntity.type.equals("Episode", ignoreCase = true) && !finalEntity.seriesId.isNullOrBlank()) {
                        val sId = finalEntity.seriesId!!
                        seasons = mediaRepository.getSeasons(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = sId
                        )
                        val epSeasonNum = finalEntity.seasonNumber ?: 1
                        selectedSeasonId = seasons.firstOrNull { it.seasonNumber == epSeasonNum }?.id ?: seasons.firstOrNull()?.id
                        if (selectedSeasonId != null) {
                            episodes = mediaRepository.getEpisodes(
                                serverUrl = baseUrl,
                                userId = prefs.jellyfinUserId,
                                token = token,
                                seriesId = sId,
                                seasonId = selectedSeasonId
                            )
                        }
                    }

                    // Fetch recommendations and genre items
                    var similarItems: List<MediaItem> = emptyList()
                    var genreItems: List<MediaItem> = emptyList()
                    var primaryGenreName: String? = null

                    try {
                        val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

                        // 1. Similar / Recommended titles
                        if (tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                            val tmdbRecs = mediaRepository.getTmdbRecommendations(
                                apiKey = prefs.tmdbApiKey,
                                tmdbId = tmdbLong,
                                isTv = isTv
                            ).getOrDefault(emptyList())

                            if (tmdbRecs.isNotEmpty()) {
                                val matchedRecs = filterToLibraryUseCase.filterTmdbItems(
                                    tmdbItems = tmdbRecs,
                                    serverUrl = baseUrl,
                                    userId = prefs.jellyfinUserId,
                                    token = token,
                                    maxCandidates = 30
                                ).filter { it.id != finalEntity.id }

                                similarItems = matchedRecs.take(15).map {
                                    it.toMediaItem(baseUrl, token, MediaSource.TMDB_RECOMMENDATION)
                                }
                            }
                        }

                        // 2. Same Genre titles
                        val firstGenre = finalEntity.genres?.split(",", ";")?.firstOrNull()?.trim()
                        if (!firstGenre.isNullOrBlank()) {
                            primaryGenreName = firstGenre
                            val sameGenreEntities = mediaRepository.getItemsByGenre(firstGenre)
                                .filter { candidate ->
                                    candidate.id != finalEntity.id &&
                                            similarItems.none { s -> s.id == candidate.id } &&
                                            (!candidate.backdropImageTag.isNullOrEmpty() || !candidate.overview.isNullOrBlank())
                                }
                                .take(15)

                            genreItems = sameGenreEntities.map {
                                it.toMediaItem(baseUrl, token, MediaSource.JELLYFIN)
                            }
                        }
                    } catch (_: Exception) {}

                    val isCollection = finalEntity.type.equals("BoxSet", ignoreCase = true) ||
                            finalEntity.type.equals("CollectionFolder", ignoreCase = true) ||
                            finalEntity.type.equals("Playlist", ignoreCase = true) ||
                            finalEntity.title.contains("Colección", ignoreCase = true) ||
                            finalEntity.title.contains("Collection", ignoreCase = true)

                    var collectionItems: List<MediaItem> = emptyList()
                    if (isCollection) {
                        var colEntities = mediaRepository.getCollectionItems(baseUrl, prefs.jellyfinUserId, token, finalEntity.id)
                        if (colEntities.isEmpty()) {
                            val cleanName = finalEntity.title
                                .replace(" - Colección", "", ignoreCase = true)
                                .replace(" Colección", "", ignoreCase = true)
                                .replace(" Collection", "", ignoreCase = true)
                                .trim()
                            if (cleanName.isNotBlank()) {
                                colEntities = database.jellyfinDao().searchLocalMedia(cleanName, limit = 20)
                                    .filter { it.id != finalEntity.id && !it.type.equals("BoxSet", ignoreCase = true) }
                            }
                        }
                        collectionItems = colEntities.map { it.toMediaItem(baseUrl, token, MediaSource.JELLYFIN) }
                    }

                    _uiState.value = DetailUiState.Success(
                        entity = finalEntity,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl,
                        trailerUrl = trailerUrl,
                        baseUrl = baseUrl,
                        isFavorite = finalEntity.isFavorite,
                        isPlayed = finalEntity.isPlayed || (finalEntity.unplayedItemCount == 0 && (finalEntity.totalItemCount ?: 0) > 0),
                        buttonStyle = prefs.buttonStyle,
                        accentColor = prefs.accentColor,
                        seriesStatus = seriesStatus,
                        seasons = seasons,
                        episodes = episodes,
                        selectedSeasonId = selectedSeasonId,
                        nextUpEpisode = nextUpEpisode,
                        similarItems = similarItems,
                        genreItems = genreItems,
                        genreName = primaryGenreName,
                        collectionItems = collectionItems,
                        isCollection = isCollection
                    )
                } else {
                    _uiState.value = DetailUiState.Error("Elemento no encontrado en tu biblioteca")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.localizedMessage ?: "Error al cargar el contenido")
            }
        }
    }

    fun selectSeason(seasonId: String) {
        val state = _uiState.value as? DetailUiState.Success ?: return
        if (state.selectedSeasonId == seasonId) return

        _uiState.value = state.copy(selectedSeasonId = seasonId, isLoadingEpisodes = true)
        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val episodes = mediaRepository.getEpisodes(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    seriesId = state.entity.id,
                    seasonId = seasonId
                )
                _uiState.value = (_uiState.value as? DetailUiState.Success)?.copy(
                    episodes = episodes,
                    isLoadingEpisodes = false
                ) ?: _uiState.value
            } catch (e: Exception) {
                _uiState.value = (_uiState.value as? DetailUiState.Success)?.copy(isLoadingEpisodes = false) ?: _uiState.value
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value as? DetailUiState.Success ?: return
        val newFavStatus = !state.isFavorite
        _uiState.value = state.copy(isFavorite = newFavStatus)

        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                mediaRepository.toggleFavorite(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    itemId = state.entity.id,
                    makeFavorite = newFavStatus
                )
            } catch (e: Exception) {
                // Revert on error
                _uiState.value = state.copy(isFavorite = !newFavStatus)
            }
        }
    }

    fun togglePlayed() {
        val state = _uiState.value as? DetailUiState.Success ?: return
        val newPlayedStatus = !state.isPlayed
        val updatedEntity = state.entity.copy(
            isPlayed = newPlayedStatus,
            playbackPositionTicks = 0L,
            unplayedItemCount = if (newPlayedStatus) 0 else state.entity.totalItemCount
        )
        _uiState.value = state.copy(
            isPlayed = newPlayedStatus,
            entity = updatedEntity
        )

        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                mediaRepository.togglePlayed(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    itemId = state.entity.id,
                    makePlayed = newPlayedStatus
                )
            } catch (e: Exception) {
                // Revert on error
                _uiState.value = state.copy(
                    isPlayed = !newPlayedStatus,
                    entity = state.entity
                )
            }
        }
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "&api_key=$token" else ""
        val effectivePosterId = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesId else id
        val effectivePosterTag = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesPrimaryImageTag else primaryImageTag

        val tagParam = if (!effectivePosterTag.isNullOrEmpty()) "&tag=$effectivePosterTag" else ""
        val posterUrl = "$baseUrl/Items/$effectivePosterId/Images/Primary?$authParam$tagParam"

        val backdropTagParam = if (!backdropImageTag.isNullOrEmpty()) "&tag=$backdropImageTag" else ""
        val backdropUrl = "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"

        val effectiveTitle = if (type.equals("Episode", ignoreCase = true) && !seriesName.isNullOrEmpty()) seriesName else title
        val effectiveType = if (type.equals("Episode", ignoreCase = true) && source != MediaSource.JELLYFIN) "Series" else type

        return MediaItem(
            id = id,
            title = effectiveTitle,
            overview = overview,
            type = effectiveType,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = communityRating,
            year = productionYear,
            source = source,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite
        )
    }
}
