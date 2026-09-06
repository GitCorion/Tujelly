package com.example.tujelly.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.UserPreferences
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import com.example.tujelly.domain.usecase.GetHomeFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object EmptySettings : HomeUiState
    data class EmptyLibrary(val serverUrl: String) : HomeUiState
    data class Success(
        val sections: List<HomeSection>,
        val genres: List<String> = emptyList(),
        val focusedItem: MediaItem? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.jellyfinDao(), userPreferencesRepository)
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)
    private val getHomeFeedUseCase = GetHomeFeedUseCase(mediaRepository, filterToLibraryUseCase)

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ACCENT_CYAN)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY)

    val syncProgress: StateFlow<com.example.tujelly.data.repository.SyncProgress> = MediaRepository.syncProgress
    val localMediaCount: StateFlow<Int> = mediaRepository.getMediaCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var wasSyncing = false
            MediaRepository.syncProgress.collect { progress ->
                if (progress.isSyncing) {
                    wasSyncing = true
                } else if (wasSyncing && !progress.isSyncing) {
                    wasSyncing = false
                    val prefs = userPreferencesRepository.userPreferencesFlow.first()
                    if (prefs.jellyfinServerUrl.isNotBlank() && prefs.jellyfinAccessToken.isNotBlank() && mediaRepository.getLocalCount() > 0) {
                        loadFeedForPrefs(prefs)
                    }
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                if (prefs.jellyfinServerUrl.isBlank() || prefs.jellyfinAccessToken.isBlank()) {
                    _uiState.value = HomeUiState.EmptySettings
                } else {
                    loadFeedForPrefs(prefs)
                }
            }
        }
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.jellyfinServerUrl.isBlank() || prefs.jellyfinAccessToken.isBlank()) {
                _uiState.value = HomeUiState.EmptySettings
            } else {
                loadFeedForPrefs(prefs)
            }
        }
    }

    private fun loadFeedForPrefs(prefs: UserPreferences) {
        _uiState.value = HomeUiState.Loading

        // Launch background sync (delta sync if already populated, or full sync if empty)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            mediaRepository.startBackgroundSync(
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken,
                lastSyncTimestamp = prefs.jellyfinLastSync
            )
        }
        viewModelScope.launch {
            try {
                var hasEmitted = false
                val userGenres = mediaRepository.getMostWatchedGenres()

                getHomeFeedUseCase(prefs).collect { sections ->
                    if (sections.isNotEmpty()) {
                        hasEmitted = true
                        val currentFocused = (_uiState.value as? HomeUiState.Success)?.focusedItem
                        val firstItem = currentFocused ?: sections.firstOrNull()?.items?.firstOrNull()
                        _uiState.value = HomeUiState.Success(
                            sections = sections,
                            genres = userGenres,
                            focusedItem = firstItem
                        )
                    }
                }
                if (!hasEmitted) {
                    _uiState.value = HomeUiState.EmptyLibrary(prefs.jellyfinServerUrl)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Error conectando con Jellyfin")
            }
        }
    }

    fun setFocusedItem(item: MediaItem) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.value = currentState.copy(focusedItem = item)
        }
    }
}
