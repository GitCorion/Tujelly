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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            serverUrl = prefs.jellyfinServerUrl
            accessToken = prefs.jellyfinAccessToken

            val movies = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getMovies() }.getOrDefault(emptyList()) }
            val series = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getSeries() }.getOrDefault(emptyList()) }
            rawCatalog = movies + series

            val favorites = rawCatalog.filter { it.isFavorite }
            val recentWatched = rawCatalog.filter { it.isPlayed || it.playbackPositionTicks > 0 }
            val localTop = withContext(Dispatchers.IO) { runCatching { jellyfinDao.getTopRatedLocal(30) }.getOrDefault(emptyList()) }

            withContext(Dispatchers.Default) {
                matrixEngine.initialize(
                    catalog = rawCatalog,
                    tmdbTopRated = localTop,
                    tmdbTrending = localTop,
                    recentWatched = recentWatched,
                    favorites = favorites
                )
            }

            val format = _uiState.value.format
            val initialTags = matrixEngine.generateMosaicTags(emptyList(), format, targetCapacity = 20)
            val initialMovies = matrixEngine.getMatchingMovies(emptyList(), format, serverUrl, accessToken)

            _uiState.value = _uiState.value.copy(
                mosaicTags = initialTags,
                matchingMovies = initialMovies,
                selectedMovie = initialMovies.firstOrNull(),
                activeChain = emptyList(),
                totalCatalogCount = rawCatalog.size,
                isLoading = false
            )

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
                        matrixEngine.initialize(
                            catalog = rawCatalog,
                            tmdbTopRated = matchedCanon.ifEmpty { localTop },
                            tmdbTrending = matchedTrending.ifEmpty { localTop },
                            recentWatched = recentWatched,
                            favorites = favorites
                        )
                        val updatedTags = matrixEngine.generateMosaicTags(_uiState.value.activeChain, _uiState.value.format, targetCapacity = 20)
                        _uiState.value = _uiState.value.copy(mosaicTags = updatedTags)
                    }
                }
            }
        }
    }

    /**
     * Alterna la selección de una etiqueta en la Constelación de Medusa:
     * - Si la etiqueta ya estaba en la cadena activa: la desconecta junto con las posteriores.
     * - Si no estaba: la conecta como un filtro "AND" estricto.
     * En ambos casos, las etiquetas incompatibles desaparecen instantáneamente y son reemplazadas
     * por las mejores micro-etiquetas co-ocurrentes supervivientes.
     */
    fun toggleTag(tag: MedusaMosaicTag) {
        val currentChain = _uiState.value.activeChain.toMutableList()
        val index = currentChain.indexOfFirst { it.rawTag.equals(tag.rawTag, ignoreCase = true) }

        val newChain = if (index >= 0) {
            // Desconectar esta y las posteriores
            currentChain.take(index)
        } else {
            // Añadir al árbol de sensaciones
            currentChain + tag.copy(isSelected = true)
        }

        val format = _uiState.value.format
        val newTags = matrixEngine.generateMosaicTags(newChain, format, targetCapacity = 20)
        val newMovies = matrixEngine.getMatchingMovies(newChain, format, serverUrl, accessToken)

        _uiState.value = _uiState.value.copy(
            activeChain = newChain,
            mosaicTags = newTags,
            matchingMovies = newMovies,
            selectedMovie = newMovies.firstOrNull()
        )
    }

    fun removeTagFromChain(tag: MedusaMosaicTag) {
        val currentChain = _uiState.value.activeChain
        val newChain = currentChain.filterNot { it.rawTag.equals(tag.rawTag, ignoreCase = true) }

        val format = _uiState.value.format
        val newTags = matrixEngine.generateMosaicTags(newChain, format, targetCapacity = 20)
        val newMovies = matrixEngine.getMatchingMovies(newChain, format, serverUrl, accessToken)

        _uiState.value = _uiState.value.copy(
            activeChain = newChain,
            mosaicTags = newTags,
            matchingMovies = newMovies,
            selectedMovie = newMovies.firstOrNull()
        )
    }

    fun resetChain() {
        val format = _uiState.value.format
        val newTags = matrixEngine.generateMosaicTags(emptyList(), format, targetCapacity = 20)
        val newMovies = matrixEngine.getMatchingMovies(emptyList(), format, serverUrl, accessToken)

        _uiState.value = _uiState.value.copy(
            activeChain = emptyList(),
            mosaicTags = newTags,
            matchingMovies = newMovies,
            showMoviesOverlay = false,
            selectedMovie = newMovies.firstOrNull()
        )
    }

    fun setFormat(format: MediaFormat) {
        if (_uiState.value.format == format) return
        val chain = _uiState.value.activeChain
        val newTags = matrixEngine.generateMosaicTags(chain, format, targetCapacity = 20)
        val newMovies = matrixEngine.getMatchingMovies(chain, format, serverUrl, accessToken)

        _uiState.value = _uiState.value.copy(
            format = format,
            mosaicTags = newTags,
            matchingMovies = newMovies,
            selectedMovie = newMovies.firstOrNull()
        )
    }

    fun onTagFocused(tag: MedusaMosaicTag) {
        _uiState.value = _uiState.value.copy(focusedTag = tag)
    }

    fun toggleMoviesOverlay() {
        if (_uiState.value.matchingMovies.isEmpty()) return
        val current = _uiState.value.showMoviesOverlay
        _uiState.value = _uiState.value.copy(
            showMoviesOverlay = !current,
            selectedMovie = if (!current) _uiState.value.matchingMovies.firstOrNull() else null
        )
    }

    fun selectMovie(movie: MediaItem) {
        _uiState.value = _uiState.value.copy(selectedMovie = movie)
    }

    fun dismissMovieSelection() {
        _uiState.value = _uiState.value.copy(selectedMovie = null)
    }

    /**
     * Tecla ATRÁS:
     * 1. Si está viendo la película seleccionada -> la deselecciona.
     * 2. Si el visor overlay está abierto -> lo cierra.
     * 3. Si hay etiquetas activas conectadas -> desconecta la última (el mosaico se re-expande).
     * 4. Si la cadena está vacía -> devuelve false para volver al Home.
     */
    fun onBackPress(): Boolean {
        if (_uiState.value.selectedMovie != null) {
            _uiState.value = _uiState.value.copy(selectedMovie = null)
            return true
        }

        if (_uiState.value.showMoviesOverlay) {
            _uiState.value = _uiState.value.copy(showMoviesOverlay = false)
            return true
        }

        val chain = _uiState.value.activeChain
        if (chain.isNotEmpty()) {
            val newChain = chain.dropLast(1)
            val format = _uiState.value.format
            val newTags = matrixEngine.generateMosaicTags(newChain, format)
            val newMovies = matrixEngine.getMatchingMovies(newChain, format, serverUrl, accessToken)

            _uiState.value = _uiState.value.copy(
                activeChain = newChain,
                mosaicTags = newTags,
                matchingMovies = newMovies
            )
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
