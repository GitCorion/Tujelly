package com.example.tujelly.ui.screens.medusa

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
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

data class MedusaUiState(
    val mosaicTags: List<MedusaMosaicTag> = emptyList(),
    val activeChain: List<MedusaMosaicTag> = emptyList(),
    val matchingMovies: List<MediaItem> = emptyList(),
    val format: MediaFormat = MediaFormat.ALL,
    val showMoviesOverlay: Boolean = false,
    val selectedMovie: MediaItem? = null,
    val totalCatalogCount: Int = 0,
    val isLoading: Boolean = true,
    val focusedTag: MedusaMosaicTag? = null
)

typealias ConstellationUiState = MedusaUiState

class MedusaViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val jellyfinDao = database.jellyfinDao()
    private val mediaRepository = MediaRepository(jellyfinDao, userPreferencesRepository)
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

    private val matrixEngine = MedusaTagMatrixEngine()
    private var rawCatalog: List<JellyfinMediaEntity> = emptyList()
    private var serverUrl: String = ""
    private var accessToken: String = ""
    private var lastTmdbCanon: List<JellyfinMediaEntity> = emptyList()
    private var lastTmdbTrending: List<JellyfinMediaEntity> = emptyList()
    private var lastRecentWatched: List<JellyfinMediaEntity> = emptyList()
    private var lastFavorites: List<JellyfinMediaEntity> = emptyList()

    private var filterJob: Job? = null

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ACCENT_CYAN)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BUTTON_STYLE_ICONS_ONLY)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(MedusaUiState())
    val uiState: StateFlow<MedusaUiState> = _uiState.asStateFlow()

    init {
        TagTranslations.initCache(application)
        viewModelScope.launch {
            TagTranslations.syncRemoteDictionary(application)
        }
        loadData()
    }

    fun loadData() {
        filterJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            serverUrl = prefs.jellyfinServerUrl
            accessToken = prefs.jellyfinAccessToken

            val movies = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getMovies() }.getOrDefault(emptyList()) }
            val series = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getSeries() }.getOrDefault(emptyList()) }
            rawCatalog = movies + series

            val favorites = rawCatalog.filter { it.isFavorite }
            val recentWatched = rawCatalog.filter { it.isPlayed || it.playbackPositionTicks > 0 }
            val localTop = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getTopRatedLocal(30) }.getOrDefault(emptyList()) }

            lastFavorites = favorites
            lastRecentWatched = recentWatched

            val format = _uiState.value.format

            val (initialTags, initialMovies) = withContext(Dispatchers.Default) {
                matrixEngine.initialize(
                    catalog = rawCatalog,
                    tmdbTopRated = localTop,
                    tmdbTrending = localTop,
                    recentWatched = recentWatched,
                    favorites = favorites
                )
                val tags = matrixEngine.generateMosaicTags(emptyList(), format, targetCapacity = 20)
                val movs = matrixEngine.getMatchingMovies(emptyList(), format, serverUrl, accessToken)
                Pair(tags, movs)
            }

            _uiState.update { current ->
                current.copy(
                    mosaicTags = initialTags,
                    matchingMovies = initialMovies,
                    selectedMovie = initialMovies.firstOrNull(),
                    activeChain = emptyList(),
                    totalCatalogCount = rawCatalog.size,
                    isLoading = false
                )
            }

            // Enriquecer en segundo plano con TMDB si hay conexión sin bloquear la UI
            val tmdbKey = prefs.tmdbApiKey.trim()
            if (tmdbKey.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val canonDto: List<com.example.tujelly.data.remote.tmdb.TmdbItemDto> = runCatching {
                        mediaRepository.getTmdbTopRated(tmdbKey).getOrDefault(emptyList())
                    }.getOrDefault(emptyList())
                    val matchedCanon = runCatching { filterToLibraryUseCase.filterTmdbItems(canonDto, maxCandidates = 30) }.getOrDefault(emptyList())

                    val trendingDto: List<com.example.tujelly.data.remote.tmdb.TmdbItemDto> = runCatching {
                        mediaRepository.getTmdbTrendingDay(tmdbKey).getOrDefault(emptyList())
                    }.getOrDefault(emptyList())
                    val matchedTrending = runCatching { filterToLibraryUseCase.filterTmdbItems(trendingDto, maxCandidates = 30) }.getOrDefault(emptyList())

                    if (matchedCanon.isNotEmpty() || matchedTrending.isNotEmpty()) {
                        lastTmdbCanon = matchedCanon
                        lastTmdbTrending = matchedTrending
                        val updatedTags = withContext(Dispatchers.Default) {
                            matrixEngine.initialize(
                                catalog = rawCatalog,
                                tmdbTopRated = matchedCanon.ifEmpty { localTop },
                                tmdbTrending = matchedTrending.ifEmpty { localTop },
                                recentWatched = recentWatched,
                                favorites = favorites
                            )
                            matrixEngine.generateMosaicTags(_uiState.value.activeChain, _uiState.value.format, targetCapacity = 20)
                        }
                        _uiState.update { it.copy(mosaicTags = updatedTags) }
                    }
                }
            }
        }
    }

    /**
     * Aplica el filtrado de forma reactiva y totalmente asíncrona en Dispatchers.Default:
     * 1. Actualiza inmediatamente el estado de la cadena activa para que el mando TV responda a 60 FPS sin latencia.
     * 2. Cancela cualquier cálculo en vuelo previo para evitar saturar la CPU en ráfagas de clicks.
     * 3. Calcula en segundo plano las nuevas etiquetas supervivientes y las películas resultantes.
     */
    private fun applyFilter(
        newChain: List<MedusaMosaicTag>,
        format: MediaFormat,
        updateChainImmediately: Boolean = true
    ) {
        if (updateChainImmediately) {
            _uiState.update { it.copy(activeChain = newChain, format = format) }
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            val newTags = matrixEngine.generateMosaicTags(newChain, format, targetCapacity = 20)
            val newMovies = matrixEngine.getMatchingMovies(newChain, format, serverUrl, accessToken)

            _uiState.update { current ->
                current.copy(
                    activeChain = newChain,
                    format = format,
                    mosaicTags = newTags,
                    matchingMovies = newMovies,
                    selectedMovie = newMovies.firstOrNull()
                )
            }
        }
    }

    /**
     * Alterna la selección de una etiqueta en la Constelación de Medusa:
     * - Si la etiqueta ya estaba en la cadena activa: la desconecta junto con las posteriores.
     * - Si no estaba: la conecta como un filtro "AND" estricto.
     */
    fun toggleTag(tag: MedusaMosaicTag) {
        val currentChain = _uiState.value.activeChain.toMutableList()
        val index = currentChain.indexOfFirst { it.rawTag.equals(tag.rawTag, ignoreCase = true) }

        val newChain = if (index >= 0) {
            // Desconectar esta y las posteriores
            currentChain.take(index)
        } else {
            // Añadir a la cadena de temáticas
            currentChain + tag.copy(isSelected = true)
        }

        applyFilter(newChain, _uiState.value.format)
    }

    fun removeTagFromChain(tag: MedusaMosaicTag) {
        val currentChain = _uiState.value.activeChain
        val newChain = currentChain.filterNot { it.rawTag.equals(tag.rawTag, ignoreCase = true) }
        applyFilter(newChain, _uiState.value.format)
    }

    fun resetChain() {
        _uiState.update { it.copy(showMoviesOverlay = false) }
        applyFilter(emptyList(), _uiState.value.format)
    }

    fun setFormat(format: MediaFormat) {
        if (_uiState.value.format == format) return
        applyFilter(_uiState.value.activeChain, format)
    }

    fun onTagFocused(tag: MedusaMosaicTag) {
        _uiState.update { it.copy(focusedTag = tag) }
    }

    fun toggleMoviesOverlay() {
        if (_uiState.value.matchingMovies.isEmpty()) return
        val current = _uiState.value.showMoviesOverlay
        _uiState.update {
            it.copy(
                showMoviesOverlay = !current,
                selectedMovie = if (!current) it.matchingMovies.firstOrNull() else null
            )
        }
    }

    fun selectMovie(movie: MediaItem) {
        _uiState.update { it.copy(selectedMovie = movie) }
    }

    fun dismissMovieSelection() {
        _uiState.update { it.copy(selectedMovie = null) }
    }

    /**
     * Tecla ATRÁS:
     * 1. Si está viendo la película seleccionada -> la deselecciona.
     * 2. Si el visor overlay está abierto -> lo cierra.
     * 3. Si hay etiquetas activas conectadas -> desconecta la última (el mosaico se re-expande de forma asíncrona).
     * 4. Si la cadena está vacía -> devuelve false para volver al Home.
     */
    fun onBackPress(): Boolean {
        if (_uiState.value.selectedMovie != null) {
            _uiState.update { it.copy(selectedMovie = null) }
            return true
        }

        if (_uiState.value.showMoviesOverlay) {
            _uiState.update { it.copy(showMoviesOverlay = false) }
            return true
        }

        val chain = _uiState.value.activeChain
        if (chain.isNotEmpty()) {
            val newChain = chain.dropLast(1)
            applyFilter(newChain, _uiState.value.format)
            return true
        }

        return false
    }

    fun surpriseMe(): MediaItem? {
        val currentMatches = _uiState.value.matchingMovies
        if (currentMatches.isNotEmpty()) {
            return currentMatches.filter { !it.isPlayed }.randomOrNull() ?: currentMatches.randomOrNull()
        }
        val pool = rawCatalog.filter { !it.isPlayed && (it.communityRating ?: 0f) >= 7.0f }
            .ifEmpty { rawCatalog }
        return pool.randomOrNull()?.let {
            MediaItem(
                id = it.id,
                title = it.title,
                overview = it.overview,
                type = it.type,
                posterUrl = it.primaryImageTag?.let { tag -> "$serverUrl/Items/${it.id}/Images/Primary?tag=$tag" },
                rating = it.communityRating,
                year = it.productionYear
            )
        }
    }
}
