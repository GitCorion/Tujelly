package com.example.tujelly.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val sections: List<HomeSection> = emptyList(),
    val results: List<MediaItem> = emptyList(),
    val totalHits: Int = 0,
    val focusedItem: MediaItem? = null,
    val statusMessage: String? = null,
    val validNextChars: Set<Char> = emptySet(),
    val suggestions: List<String> = emptyList(),
    val isPredictiveActive: Boolean = true
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.jellyfinDao())

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ACCENT_CYAN)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BUTTON_STYLE_ICONS_ONLY)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var indexedCatalog: SearchPredictiveEngine.IndexedCatalog = SearchPredictiveEngine.IndexedCatalog.EMPTY
    private var searchJob: Job? = null
    private var predictiveJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val rawTitles = mediaRepository.getAllLocalTitles()
            val indexed = withContext(Dispatchers.Default) {
                SearchPredictiveEngine.IndexedCatalog.build(rawTitles)
            }
            indexedCatalog = indexed
            updatePredictiveState(_uiState.value.query)
        }
    }

    private fun updatePredictiveState(query: String) {
        predictiveJob?.cancel()
        predictiveJob = viewModelScope.launch(Dispatchers.Default) {
            val isPredictive = _uiState.value.isPredictiveActive
            val nextChars = if (isPredictive) {
                SearchPredictiveEngine.findValidNextCharacters(query, indexedCatalog)
            } else {
                SearchPredictiveEngine.ALL_KEYBOARD_CHARS
            }
            val suggestions = if (query.isNotBlank()) {
                SearchPredictiveEngine.generateAutocompleteSuggestions(query, indexedCatalog, limit = 5)
            } else {
                emptyList()
            }
            _uiState.update { current ->
                current.copy(
                    validNextChars = nextChars,
                    suggestions = suggestions
                )
            }
        }
    }

    fun appendChar(c: Char) {
        val newQuery = _uiState.value.query + c
        onQueryChange(newQuery)
    }

    fun onBackspace() {
        val current = _uiState.value.query
        if (current.isNotEmpty()) {
            val newQuery = current.dropLast(1)
            onQueryChange(newQuery)
        }
    }

    fun onClear() {
        onQueryChange("")
    }

    fun selectSuggestion(suggestion: String) {
        onQueryChange(suggestion)
    }

    fun togglePredictive() {
        val newActive = !_uiState.value.isPredictiveActive
        _uiState.update { it.copy(isPredictiveActive = newActive) }
        updatePredictiveState(_uiState.value.query)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        updatePredictiveState(newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            predictiveJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = emptyList(),
                    results = emptyList(),
                    totalHits = 0,
                    focusedItem = null,
                    statusMessage = null,
                    suggestions = emptyList(),
                    validNextChars = if (it.isPredictiveActive) indexedCatalog.allStartingChars else SearchPredictiveEngine.ALL_KEYBOARD_CHARS
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce adaptado a Android TV y Odroid N2+ para evitar ráfagas de I/O
            val queryClean = newQuery.trim()
            if (queryClean.isBlank()) return@launch

            val prefs = userPreferencesRepository.userPreferencesFlow.first()

            // 1. Capa 1: Búsqueda Local Instantánea en Room DB
            val localResults = withContext(Dispatchers.IO) {
                mediaRepository.searchLocalMedia(queryClean, limit = 50)
            }
            updateUiWithResults(
                results = localResults,
                baseUrl = prefs.jellyfinServerUrl,
                token = prefs.jellyfinAccessToken,
                queryClean = queryClean,
                isLoading = true
            )

            // 2. Capa 2: Búsqueda Remota en Jellyfin (tolerancia a erratas con Meilisearch)
            if (prefs.jellyfinServerUrl.isNotBlank() && prefs.jellyfinAccessToken.isNotBlank()) {
                val remoteResults = withContext(Dispatchers.IO) {
                    mediaRepository.searchRemoteAndCache(
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        query = queryClean,
                        limit = 30
                    ).getOrDefault(emptyList())
                }

                if (remoteResults.isNotEmpty()) {
                    // 3. Capa 3: Fusión Inteligente y Deduplicación
                    val combinedMap = LinkedHashMap<String, JellyfinMediaEntity>()
                    remoteResults.forEach { combinedMap[it.id] = it }
                    localResults.forEach { if (!combinedMap.containsKey(it.id)) combinedMap[it.id] = it }

                    val mergedResults = combinedMap.values.toList()
                    updateUiWithResults(
                        results = mergedResults,
                        baseUrl = prefs.jellyfinServerUrl,
                        token = prefs.jellyfinAccessToken,
                        queryClean = queryClean,
                        isLoading = false
                    )
                    return@launch
                }
            }

            // Si no hay resultados remotos o no hay red, mostramos los locales
            updateUiWithResults(
                results = localResults,
                baseUrl = prefs.jellyfinServerUrl,
                token = prefs.jellyfinAccessToken,
                queryClean = queryClean,
                isLoading = false
            )
        }
    }

    private fun updateUiWithResults(
        results: List<JellyfinMediaEntity>,
        baseUrl: String,
        token: String,
        queryClean: String,
        isLoading: Boolean
    ) {
        val allMediaItems = results.map { it.toMediaItem(baseUrl, token) }
        val movies = results.filter { it.type.equals("Movie", ignoreCase = true) }
        val series = results.filter { !it.type.equals("Movie", ignoreCase = true) }

        val sections = mutableListOf<HomeSection>()

        if (movies.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Películas",
                    items = movies.map { it.toMediaItem(baseUrl, token) },
                    badge = null
                )
            )
        }

        if (series.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Series",
                    items = series.map { it.toMediaItem(baseUrl, token) },
                    badge = null
                )
            )
        }

        val totalHits = results.size
        val currentFocused = _uiState.value.focusedItem
        val newFocused = if (currentFocused != null && results.any { it.id == currentFocused.id }) {
            currentFocused
        } else {
            allMediaItems.firstOrNull()
        }

        val statusMessage = when {
            totalHits == 0 && !isLoading -> "No se encontraron títulos para '$queryClean'"
            totalHits == 0 && isLoading -> "Buscando en el servidor..."
            else -> null
        }

        _uiState.update { current ->
            val newFocused = if (current.focusedItem != null && results.any { it.id == current.focusedItem.id }) {
                current.focusedItem
            } else {
                allMediaItems.firstOrNull()
            }
            current.copy(
                isLoading = isLoading,
                sections = sections,
                results = allMediaItems,
                totalHits = totalHits,
                focusedItem = newFocused,
                statusMessage = statusMessage
            )
        }
    }

    fun setFocusedItem(item: MediaItem) {
        _uiState.update { it.copy(focusedItem = item) }
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
            isFavorite = isFavorite,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
