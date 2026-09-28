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
import com.example.tujelly.util.JellyfinImageUtils
import com.example.tujelly.util.toOptimizedMediaItem
import kotlinx.coroutines.async
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
    private val mediaRepository = MediaRepository(database.jellyfinDao(), userPreferencesRepository, database.tmdbVoteCacheDao())

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadDetail(itemId: String) {
        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val baseUrl = prefs.jellyfinServerUrl
                val token = prefs.jellyfinAccessToken
                val local = database.jellyfinDao().getItemById(itemId)

                // 1. Si está en BD local, pintar de INMEDIATO (0 ms) sin pantalla de carga
                var initialEntity = local
                if (initialEntity != null) {
                    var backdropUrl = JellyfinImageUtils.getDetailBackdropUrl(
                        baseUrl = baseUrl,
                        itemId = initialEntity.id,
                        imageTag = initialEntity.backdropImageTag,
                        token = token
                    )
                    // Si es episodio, resolver serie y backdrop del padre
                    if (initialEntity.type.equals("Episode", ignoreCase = true) && !initialEntity.seriesId.isNullOrBlank()) {
                        val parentSeries = database.jellyfinDao().getItemById(initialEntity.seriesId)
                        if (parentSeries != null) {
                            if (initialEntity.seriesName.isNullOrBlank()) {
                                initialEntity = initialEntity.copy(seriesName = parentSeries.title)
                            }
                            if (backdropUrl == null && !parentSeries.backdropImageTag.isNullOrBlank()) {
                                backdropUrl = JellyfinImageUtils.getDetailBackdropUrl(
                                    baseUrl = baseUrl,
                                    itemId = parentSeries.id,
                                    imageTag = parentSeries.backdropImageTag,
                                    token = token
                                )
                            }
                        }
                    }

                    val posterUrl = JellyfinImageUtils.getPosterUrl(
                        baseUrl = baseUrl,
                        itemId = initialEntity.id,
                        imageTag = initialEntity.primaryImageTag,
                        token = token
                    )
                    val logoUrl = JellyfinImageUtils.getLogoUrl(
                        baseUrl = baseUrl,
                        itemId = initialEntity.id,
                        token = token
                    )
                    val isTv = initialEntity.type.equals("Series", ignoreCase = true) || initialEntity.type.equals("Episode", ignoreCase = true)

                    _uiState.value = DetailUiState.Success(
                        entity = initialEntity,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl,
                        baseUrl = baseUrl,
                        isFavorite = initialEntity.isFavorite,
                        isPlayed = initialEntity.isPlayed || (initialEntity.unplayedItemCount == 0 && (initialEntity.totalItemCount ?: 0) > 0),
                        buttonStyle = prefs.buttonStyle,
                        accentColor = prefs.accentColor,
                        isLoadingEpisodes = isTv
                    )
                } else {
                    _uiState.value = DetailUiState.Loading
                }

                // 2. Resolver entidad completa si no estaba en local o si falta overview
                val entity = if (initialEntity != null && !initialEntity.overview.isNullOrBlank()) {
                    initialEntity
                } else {
                    val remote = mediaRepository.getItemDetail(
                        serverUrl = baseUrl,
                        userId = prefs.jellyfinUserId,
                        token = token,
                        itemId = itemId
                    ).getOrNull()
                    remote ?: initialEntity
                }

                if (entity == null) {
                    _uiState.value = DetailUiState.Error("Elemento no encontrado en tu biblioteca")
                    return@launch
                }

                var finalEntity = entity
                val isTv = finalEntity.type.equals("Series", ignoreCase = true) || finalEntity.type.equals("Episode", ignoreCase = true)
                val tmdbLong = finalEntity.tmdbId?.toLongOrNull()

                // Si aún falta overview, intentar TMDB en segundo plano
                if (finalEntity.overview.isNullOrBlank() && tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                    try {
                        val tmdbApi = com.example.tujelly.data.remote.NetworkClientFactory.createService(
                            "https://api.themoviedb.org/3/",
                            com.example.tujelly.data.remote.tmdb.TmdbApiService::class.java
                        )
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

                // Si no teníamos estado previo (caso local == null) o si finalEntity tiene nuevos datos (overview, etc)
                val posterUrl = JellyfinImageUtils.getPosterUrl(
                    baseUrl = baseUrl,
                    itemId = finalEntity.id,
                    imageTag = finalEntity.primaryImageTag,
                    token = token
                )
                var backdropUrl = JellyfinImageUtils.getDetailBackdropUrl(
                    baseUrl = baseUrl,
                    itemId = finalEntity.id,
                    imageTag = finalEntity.backdropImageTag,
                    token = token
                )
                if (finalEntity.type.equals("Episode", ignoreCase = true) && !finalEntity.seriesId.isNullOrBlank()) {
                    val parentSeries = database.jellyfinDao().getItemById(finalEntity.seriesId)
                    if (parentSeries != null) {
                        if (finalEntity.seriesName.isNullOrBlank()) {
                            finalEntity = finalEntity.copy(seriesName = parentSeries.title)
                        }
                        if (backdropUrl == null && !parentSeries.backdropImageTag.isNullOrBlank()) {
                            backdropUrl = JellyfinImageUtils.getDetailBackdropUrl(
                                baseUrl = baseUrl,
                                itemId = parentSeries.id,
                                imageTag = parentSeries.backdropImageTag,
                                token = token
                            )
                        }
                    }
                }
                val logoUrl = JellyfinImageUtils.getLogoUrl(
                    baseUrl = baseUrl,
                    itemId = finalEntity.id,
                    token = token
                )

                // Actualizar UI con la entidad enriquecida
                (_uiState.value as? DetailUiState.Success)?.let { curr ->
                    _uiState.value = curr.copy(
                        entity = finalEntity,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl
                    )
                } ?: run {
                    _uiState.value = DetailUiState.Success(
                        entity = finalEntity,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl,
                        baseUrl = baseUrl,
                        isFavorite = finalEntity.isFavorite,
                        isPlayed = finalEntity.isPlayed || (finalEntity.unplayedItemCount == 0 && (finalEntity.totalItemCount ?: 0) > 0),
                        buttonStyle = prefs.buttonStyle,
                        accentColor = prefs.accentColor,
                        isLoadingEpisodes = isTv
                    )
                }

                // 3. Cargar en paralelo sin bloquear la pantalla: Trailer, Series/Episodios, Recomendaciones, Colecciones
                // Trailer
                if (tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                    launch {
                        val trailer = mediaRepository.getTmdbTrailerUrl(prefs.tmdbApiKey, tmdbLong, isTv = isTv)
                        if (trailer != null) {
                            (_uiState.value as? DetailUiState.Success)?.let { curr ->
                                _uiState.value = curr.copy(trailerUrl = trailer)
                            }
                        }
                    }
                }

                // Series / Temporadas / Episodios
                if (isTv && finalEntity.type.equals("Series", ignoreCase = true)) {
                    launch {
                        val statusDeferred = async {
                            mediaRepository.getSeriesStatus(prefs.tmdbApiKey, tmdbLong, null)
                        }
                        val seasonsDeferred = async {
                            mediaRepository.getSeasons(baseUrl, prefs.jellyfinUserId, token, finalEntity.id)
                        }
                        val nextUpDeferred = async {
                            mediaRepository.getNextUpEpisode(baseUrl, prefs.jellyfinUserId, token, finalEntity.id)
                        }
                        val episodeStatsDeferred = async {
                            mediaRepository.getSeriesEpisodeStats(baseUrl, prefs.jellyfinUserId, token, finalEntity.id)
                        }

                        val seasons = seasonsDeferred.await()
                        val nextUp = nextUpDeferred.await()
                        val selectedSeasonId = if (nextUp != null) {
                            seasons.firstOrNull { it.seasonNumber == nextUp.seasonNumber }?.id
                        } else null ?: (seasons.firstOrNull { it.seasonNumber >= 1 }?.id ?: seasons.firstOrNull()?.id)

                        val episodes = if (selectedSeasonId != null) {
                            mediaRepository.getEpisodes(baseUrl, prefs.jellyfinUserId, token, finalEntity.id, selectedSeasonId)
                        } else emptyList()
                        val nextUpEpisode = nextUp ?: episodes.firstOrNull()
                        val episodeStats = episodeStatsDeferred.await()
                        val seriesStatus = statusDeferred.await()

                        (_uiState.value as? DetailUiState.Success)?.let { curr ->
                            val updatedEntity = if (episodeStats != null) {
                                curr.entity.copy(
                                    totalItemCount = episodeStats.totalUniqueEpisodes,
                                    unplayedItemCount = episodeStats.unplayedUniqueEpisodes,
                                    isPlayed = episodeStats.unplayedUniqueEpisodes == 0 && episodeStats.totalUniqueEpisodes > 0
                                )
                            } else curr.entity
                            _uiState.value = curr.copy(
                                entity = updatedEntity,
                                seriesStatus = seriesStatus,
                                seasons = seasons,
                                episodes = episodes,
                                selectedSeasonId = selectedSeasonId,
                                nextUpEpisode = nextUpEpisode,
                                isLoadingEpisodes = false,
                                isPlayed = updatedEntity.isPlayed || (updatedEntity.unplayedItemCount == 0 && (updatedEntity.totalItemCount ?: 0) > 0)
                            )
                        }
                    }
                } else if (isTv && finalEntity.type.equals("Episode", ignoreCase = true) && !finalEntity.seriesId.isNullOrBlank()) {
                    launch {
                        val sId = finalEntity.seriesId
                        val seasons = mediaRepository.getSeasons(baseUrl, prefs.jellyfinUserId, token, sId)
                        val epSeasonNum = finalEntity.seasonNumber ?: 1
                        val selectedSeasonId = seasons.firstOrNull { it.seasonNumber == epSeasonNum }?.id ?: seasons.firstOrNull()?.id
                        val episodes = if (selectedSeasonId != null) {
                            mediaRepository.getEpisodes(baseUrl, prefs.jellyfinUserId, token, sId, selectedSeasonId)
                        } else emptyList()
                        (_uiState.value as? DetailUiState.Success)?.let { curr ->
                            _uiState.value = curr.copy(
                                seasons = seasons,
                                episodes = episodes,
                                selectedSeasonId = selectedSeasonId,
                                isLoadingEpisodes = false
                            )
                        }
                    }
                }

                // Recomendaciones y género
                launch {
                    try {
                        val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)
                        var similarItems: List<MediaItem> = emptyList()
                        if (tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                            val tmdbRecs = mediaRepository.getTmdbRecommendations(prefs.tmdbApiKey, tmdbLong, isTv).getOrDefault(emptyList())
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

                        val firstGenre = finalEntity.genres?.split(",", ";")?.firstOrNull()?.trim()
                        var genreItems: List<MediaItem> = emptyList()
                        if (!firstGenre.isNullOrBlank()) {
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

                        (_uiState.value as? DetailUiState.Success)?.let { curr ->
                            _uiState.value = curr.copy(
                                similarItems = similarItems,
                                genreItems = genreItems,
                                genreName = firstGenre
                            )
                        }
                    } catch (_: Exception) {}
                }

                // Colecciones / BoxSets
                val isCollection = finalEntity.type.equals("BoxSet", ignoreCase = true) ||
                        finalEntity.type.equals("CollectionFolder", ignoreCase = true) ||
                        finalEntity.type.equals("Playlist", ignoreCase = true) ||
                        finalEntity.title.contains("Colección", ignoreCase = true) ||
                        finalEntity.title.contains("Collection", ignoreCase = true)

                if (isCollection) {
                    launch {
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
                        val collectionItems = colEntities.map { it.toMediaItem(baseUrl, token, MediaSource.JELLYFIN) }
                        (_uiState.value as? DetailUiState.Success)?.let { curr ->
                            _uiState.value = curr.copy(
                                collectionItems = collectionItems,
                                isCollection = true
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                if (_uiState.value !is DetailUiState.Success) {
                    _uiState.value = DetailUiState.Error(e.localizedMessage ?: "Error al cargar el contenido")
                }
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
        return toOptimizedMediaItem(baseUrl, token, source)
    }
}
