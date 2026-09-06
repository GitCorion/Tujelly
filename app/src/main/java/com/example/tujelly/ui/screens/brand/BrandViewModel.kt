package com.example.tujelly.ui.screens.brand

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.tmdb.TmdbApiService
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrandInfo(
    val id: String,
    val name: String,
    val providerId: String,
    val iconRes: Int,
    val iconMonoRes: Int,
    val accentColor: androidx.compose.ui.graphics.Color,
    val gradientStart: androidx.compose.ui.graphics.Color
)sealed interface BrandUiState {
    object Loading : BrandUiState
    data class Success(
        val brand: BrandInfo,
        val sections: List<HomeSection>,
        val focusedItem: MediaItem? = null
    ) : BrandUiState
    data class Error(val message: String) : BrandUiState
}

class BrandViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(application)
    private val mediaRepository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao())
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.tujelly.data.local.ACCENT_CYAN)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow<BrandUiState>(BrandUiState.Loading)
    val uiState: StateFlow<BrandUiState> = _uiState.asStateFlow()

    fun loadBrandFeed(brandId: String) {
        viewModelScope.launch {
            _uiState.value = BrandUiState.Loading

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.jellyfinServerUrl.isBlank() || prefs.jellyfinAccessToken.isBlank()) {
                _uiState.value = BrandUiState.Error("Servidor Jellyfin no configurado.")
                return@launch
            }

            val platform = com.example.tujelly.data.model.platformById(brandId)
                ?: com.example.tujelly.data.model.SUPPORTED_PLATFORMS.first()

            val brand = BrandInfo(
                id = platform.id,
                name = platform.name,
                providerId = platform.providerId,
                iconRes = platform.iconRes,
                iconMonoRes = platform.iconMonoRes,
                accentColor = platform.accentColor,
                gradientStart = platform.gradientStart
            )

            val sections = mutableListOf<HomeSection>()
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)

            // 1. En Tendencia en [Plataforma]
            try {
                val trendingMovies = runCatching { api.discoverMoviesByProvider(apiKey = prefs.tmdbApiKey, providerId = brand.providerId, watchRegion = prefs.watchRegion, page = 1) }.getOrNull()?.results ?: emptyList()
                val trendingTv = runCatching { api.discoverTvByProvider(apiKey = prefs.tmdbApiKey, providerId = brand.providerId, watchRegion = prefs.watchRegion, page = 1) }.getOrNull()?.results ?: emptyList()

                val matchedTrending = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = (trendingMovies + trendingTv).take(30),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 30
                )
                if (matchedTrending.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "En Tendencia en ${brand.name}",
                            items = matchedTrending.take(15).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = brand.name
                        )
                    )
                }
            } catch (_: Exception) {}

            // 2. Series Destacadas en [Plataforma]
            try {
                val tvItems = runCatching { api.discoverTvByProvider(apiKey = prefs.tmdbApiKey, providerId = brand.providerId, watchRegion = prefs.watchRegion, page = 2) }.getOrNull()?.results ?: emptyList()
                val matchedTv = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = tvItems.take(30),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 30
                )
                if (matchedTv.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Series Destacadas en ${brand.name}",
                            items = matchedTv.take(15).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "SERIES"
                        )
                    )
                }
            } catch (_: Exception) {}

            // 3. Cine Aclamado por la Crítica en [Plataforma]
            try {
                val topMovies = runCatching { api.discoverMoviesByProvider(apiKey = prefs.tmdbApiKey, providerId = brand.providerId, watchRegion = prefs.watchRegion, sortBy = "vote_average.desc", voteCountGte = 200, page = 1) }.getOrNull()?.results ?: emptyList()
                val matchedTop = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = topMovies.take(30),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 30
                )
                if (matchedTop.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Cine Aclamado en ${brand.name}",
                            items = matchedTop.take(15).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "TOP CRÍTICA"
                        )
                    )
                }
            } catch (_: Exception) {}

            val firstItem = sections.firstOrNull()?.items?.firstOrNull()
            _uiState.value = BrandUiState.Success(
                brand = brand,
                sections = sections,
                focusedItem = firstItem
            )
        }
    }

    fun setFocusedItem(item: MediaItem) {
        val currentState = _uiState.value
        if (currentState is BrandUiState.Success) {
            _uiState.value = currentState.copy(focusedItem = item)
        }
    }

    private fun com.example.tujelly.data.local.db.JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "api_key=$token" else ""
        val tagParam = if (!primaryImageTag.isNullOrEmpty()) "&tag=$primaryImageTag" else ""
        val posterUrl = "$baseUrl/Items/$id/Images/Primary?$authParam$tagParam"

        val backdropTagParam = if (!backdropImageTag.isNullOrEmpty()) "&tag=$backdropImageTag" else ""
        val backdropUrl = "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"

        val effectiveLogoId = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesId else id
        val logoUrl = if (baseUrl.isNotBlank()) "$baseUrl/Items/$effectiveLogoId/Images/Logo?$authParam" else null

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
            logoUrl = logoUrl,
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
