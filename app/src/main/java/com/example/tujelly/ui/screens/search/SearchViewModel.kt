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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val sections: List<HomeSection> = emptyList(),
    val totalHits: Int = 0,
    val focusedItem: MediaItem? = null,
    val statusMessage: String? = null
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

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                sections = emptyList(),
                totalHits = 0,
                focusedItem = null,
                statusMessage = "Escribe un título para buscar en tu servidor Jellyfin"
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(150) // Small debounce for fast typing
            val queryClean = newQuery.trim()
            if (queryClean.isBlank()) return@launch

            val prefs = userPreferencesRepository.userPreferencesFlow.first()

            // 1. Capa 1: Búsqueda Local Instantánea en Room DB (0 ms)
            val localResults = mediaRepository.searchLocalMedia(queryClean, limit = 50)
            updateUiWithResults(
                results = localResults,
                baseUrl = prefs.jellyfinServerUrl,
                token = prefs.jellyfinAccessToken,
                queryClean = queryClean,
                isLoading = true
            )

            // 2. Capa 2: Búsqueda Remota en Jellyfin (impulsada por Meilisearch en el servidor para tolerancia a erratas)
            if (prefs.jellyfinServerUrl.isNotBlank() && prefs.jellyfinAccessToken.isNotBlank()) {
                val remoteResults = mediaRepository.searchRemoteAndCache(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    query = queryClean,
                    limit = 30
                ).getOrDefault(emptyList())

                if (remoteResults.isNotEmpty()) {
                    // 3. Capa 3: Fusión Inteligente y Deduplicación
                    val combinedMap = LinkedHashMap<String, JellyfinMediaEntity>()
                    // Damos prioridad a los resultados de Meilisearch/Servidor (relevancia y tolerancia a erratas)
                    remoteResults.forEach { combinedMap[it.id] = it }
                    // Agregamos coincidencias locales adicionales
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

            // Si la búsqueda remota no devolvió más resultados o no había red, mostramos los locales sin spinner
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
        val movies = results.filter { it.type.equals("Movie", ignoreCase = true) }
        val series = results.filter { !it.type.equals("Movie", ignoreCase = true) }

        val sections = mutableListOf<HomeSection>()

        if (movies.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Películas encontradas",
                    items = movies.map { it.toMediaItem(baseUrl, token) },
                    badge = "PELÍCULAS"
                )
            )
        }

        if (series.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Series encontradas",
                    items = series.map { it.toMediaItem(baseUrl, token) },
                    badge = "SERIES"
                )
            )
        }

        val totalHits = results.size
        val currentFocused = _uiState.value.focusedItem
        val newFocused = if (currentFocused != null && results.any { it.id == currentFocused.id }) {
            currentFocused
        } else {
            sections.firstOrNull()?.items?.firstOrNull()
        }

        val statusMessage = when {
            totalHits == 0 && !isLoading -> "No se encontraron títulos para '$queryClean'"
            totalHits == 0 && isLoading -> "Buscando en el servidor..."
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            isLoading = isLoading,
            sections = sections,
            totalHits = totalHits,
            focusedItem = newFocused,
            statusMessage = statusMessage
        )
    }

    fun setFocusedItem(item: MediaItem) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
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
            source = MediaSource.JELLYFIN,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
