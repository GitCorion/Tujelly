package com.example.tujelly.ui.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FavoriteFilter {
    ALL,
    MOVIES,
    SERIES
}

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(
        val allItems: List<MediaItem>,
        val filteredItems: List<MediaItem>,
        val activeFilter: FavoriteFilter,
        val isSyncing: Boolean = false
    ) : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
}

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.jellyfinDao())

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ACCENT_CYAN)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BUTTON_STYLE_ICONS_ONLY)

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var currentFilter = FavoriteFilter.ALL

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            val prefs = userPreferencesRepository.userPreferencesFlow.first()

            // 1. First observe local DB favorites
            val local = mediaRepository.getFavoritesLocal()
            val mediaItems = local.map { it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken) }
            _uiState.value = FavoritesUiState.Success(
                allItems = mediaItems,
                filteredItems = applyFilter(mediaItems, currentFilter),
                activeFilter = currentFilter,
                isSyncing = true
            )

            // 2. Fetch updated favorites from Jellyfin remote API
            if (prefs.jellyfinServerUrl.isNotBlank() && prefs.jellyfinAccessToken.isNotBlank()) {
                val remoteResult = mediaRepository.getFavorites(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken
                )
                val syncedList = remoteResult.getOrNull() ?: local
                val syncedItems = syncedList.map { it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken) }
                _uiState.value = FavoritesUiState.Success(
                    allItems = syncedItems,
                    filteredItems = applyFilter(syncedItems, currentFilter),
                    activeFilter = currentFilter,
                    isSyncing = false
                )
            } else {
                _uiState.value = FavoritesUiState.Success(
                    allItems = mediaItems,
                    filteredItems = applyFilter(mediaItems, currentFilter),
                    activeFilter = currentFilter,
                    isSyncing = false
                )
            }
        }
    }

    fun setFilter(filter: FavoriteFilter) {
        currentFilter = filter
        val state = _uiState.value as? FavoritesUiState.Success ?: return
        _uiState.value = state.copy(
            activeFilter = filter,
            filteredItems = applyFilter(state.allItems, filter)
        )
    }

    private fun applyFilter(items: List<MediaItem>, filter: FavoriteFilter): List<MediaItem> {
        return when (filter) {
            FavoriteFilter.ALL -> items
            FavoriteFilter.MOVIES -> items.filter { it.type.equals("Movie", ignoreCase = true) }
            FavoriteFilter.SERIES -> items.filter {
                it.type.equals("Series", ignoreCase = true) || it.type.equals("TvProgram", ignoreCase = true)
            }
        }
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
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
            source = MediaSource.JELLYFIN,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = true,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
