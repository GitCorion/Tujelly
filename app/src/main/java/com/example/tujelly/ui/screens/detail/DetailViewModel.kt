package com.example.tujelly.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.EpisodeItem
import com.example.tujelly.domain.model.SeasonItem
import com.example.tujelly.domain.model.SeriesStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val entity: JellyfinMediaEntity,
        val posterUrl: String?,
        val backdropUrl: String?,
        val trailerUrl: String? = null,
        val baseUrl: String? = null,
        val isFavorite: Boolean = false,
        val buttonStyle: String = "ICONS_ONLY",
        val accentColor: String = com.example.tujelly.data.local.ACCENT_CYAN,
        val seriesStatus: SeriesStatus? = null,
        val seasons: List<SeasonItem> = emptyList(),
        val episodes: List<EpisodeItem> = emptyList(),
        val selectedSeasonId: String? = null,
        val nextUpEpisode: EpisodeItem? = null,
        val isLoadingEpisodes: Boolean = false
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.jellyfinDao())

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

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
                    val baseUrl = prefs.jellyfinServerUrl
                    val token = prefs.jellyfinAccessToken
                    val authParam = if (token.isNotBlank()) "&api_key=$token" else ""
                    val posterUrl = entity.primaryImageTag?.let { tag -> "$baseUrl/Items/${entity.id}/Images/Primary?tag=$tag$authParam" }
                    val backdropUrl = entity.backdropImageTag?.let { tag -> "$baseUrl/Items/${entity.id}/Images/Backdrop/0?tag=$tag$authParam" }

                    val isTv = entity.type.equals("Series", ignoreCase = true) || entity.type.equals("Episode", ignoreCase = true)
                    val tmdbLong = entity.tmdbId?.toLongOrNull()
                    val trailerUrl = if (tmdbLong != null && prefs.tmdbApiKey.isNotBlank()) {
                        mediaRepository.getTmdbTrailerUrl(prefs.tmdbApiKey, tmdbLong, isTv = isTv)
                    } else null

                    var seriesStatus: SeriesStatus? = null
                    var seasons: List<SeasonItem> = emptyList()
                    var episodes: List<EpisodeItem> = emptyList()
                    var selectedSeasonId: String? = null
                    var nextUpEpisode: EpisodeItem? = null

                    if (isTv && entity.type.equals("Series", ignoreCase = true)) {
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
                            seriesId = entity.id
                        )

                        // 3. Fetch Next Up or first episode
                        nextUpEpisode = mediaRepository.getNextUpEpisode(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = entity.id
                        )

                        // 4. Default to nextUp's season or the first season
                        selectedSeasonId = seasons.firstOrNull { it.seasonNumber == (nextUpEpisode?.seasonNumber ?: 1) }?.id
                            ?: seasons.firstOrNull()?.id

                        // 5. Fetch episodes for selected season
                        episodes = mediaRepository.getEpisodes(
                            serverUrl = baseUrl,
                            userId = prefs.jellyfinUserId,
                            token = token,
                            seriesId = entity.id,
                            seasonId = selectedSeasonId
                        )

                        if (nextUpEpisode == null && episodes.isNotEmpty()) {
                            nextUpEpisode = episodes.firstOrNull()
                        }
                    }

                    _uiState.value = DetailUiState.Success(
                        entity = entity,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        trailerUrl = trailerUrl,
                        baseUrl = baseUrl,
                        isFavorite = entity.isFavorite,
                        buttonStyle = prefs.buttonStyle,
                        accentColor = prefs.accentColor,
                        seriesStatus = seriesStatus,
                        seasons = seasons,
                        episodes = episodes,
                        selectedSeasonId = selectedSeasonId,
                        nextUpEpisode = nextUpEpisode
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
}
