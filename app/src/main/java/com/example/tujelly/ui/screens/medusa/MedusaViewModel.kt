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

data class ConstellationUiState(
    val nodes: List<SpatialNebulaNode> = emptyList(),
    val filaments: List<SpatialFilament> = emptyList(),
    val focusedNodeId: String? = null,
    val activeChain: List<SpatialNebulaNode> = emptyList(),
    val visibleNodeIds: Set<String>? = null, // Nodos que sobreviven a la poda cósmica
    val compatibleNodeIds: Set<String>? = null, // Mantenido para compatibilidad
    val incompatibleWarning: String? = null,
    val matchingMovies: List<MediaItem> = emptyList(),
    val format: MediaFormat = MediaFormat.ALL,
    val targetCameraX: Float = 0.5f,
    val targetCameraY: Float = 0.5f,
    val targetZoom: Float = 1.0f,
    val showMoviesOverlay: Boolean = false,
    val selectedMovie: MediaItem? = null,
    val totalCatalogCount: Int = 0,
    val isLoading: Boolean = true
) {
    val focusedNode: SpatialNebulaNode? get() = nodes.firstOrNull { it.id == focusedNodeId }
}

class MedusaViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val jellyfinDao = database.jellyfinDao()
    private val mediaRepository = MediaRepository(jellyfinDao, userPreferencesRepository)
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

    private val spatialEngine = NebulaSpatialEngine()
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

    private val _uiState = MutableStateFlow(ConstellationUiState())
    val uiState: StateFlow<ConstellationUiState> = _uiState.asStateFlow()

    init {
        // Inicializar caché local de traducciones y sincronizar en background con GitHub
        TagTranslations.initCache(application)
        viewModelScope.launch {
            TagTranslations.syncRemoteDictionary(application)
        }
        loadConstellation()
    }

    fun loadConstellation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            serverUrl = prefs.jellyfinServerUrl
            accessToken = prefs.jellyfinAccessToken

            val movies = runCatching { jellyfinDao.getMovies() }.getOrDefault(emptyList())
            val series = runCatching { jellyfinDao.getSeries() }.getOrDefault(emptyList())
            rawCatalog = movies + series

            val favorites = rawCatalog.filter { it.isFavorite }
            val recentWatched = rawCatalog.filter { it.isPlayed || it.playbackPositionTicks > 0 }

            // 4 Vectores Inteligentes: Canon TMDB + Tendencias TMDB + Historial + Favoritos
            val tmdbKey = prefs.tmdbApiKey.trim()
            val (tmdbCanon, tmdbTrending) = withContext(Dispatchers.IO) {
                if (tmdbKey.isNotBlank()) {
                    val canonDto: List<com.example.tujelly.data.remote.tmdb.TmdbItemDto> = runCatching {
                        mediaRepository.getTmdbTopRated(tmdbKey).getOrDefault(emptyList())
                    }.getOrDefault(emptyList())
                    val matchedCanon = runCatching { filterToLibraryUseCase.filterTmdbItems(canonDto, maxCandidates = 30) }.getOrDefault(emptyList())

                    val trendingDto: List<com.example.tujelly.data.remote.tmdb.TmdbItemDto> = runCatching {
                        mediaRepository.getTmdbTrendingDay(tmdbKey).getOrDefault(emptyList())
                    }.getOrDefault(emptyList())
                    val matchedTrending = runCatching { filterToLibraryUseCase.filterTmdbItems(trendingDto, maxCandidates = 30) }.getOrDefault(emptyList())

                    Pair(matchedCanon, matchedTrending)
                } else {
                    val localTop = runCatching { jellyfinDao.getTopRatedLocal(30) }.getOrDefault(emptyList())
                    Pair(localTop, localTop)
                }
            }

            lastRecentWatched = recentWatched
            lastFavorites = favorites
            lastTmdbCanon = tmdbCanon
            lastTmdbTrending = tmdbTrending

            withContext(Dispatchers.Default) {
                spatialEngine.buildDynamicUniverse(
                    catalog = rawCatalog,
                    tmdbTopRated = tmdbCanon,
                    tmdbTrending = tmdbTrending,
                    recentWatched = recentWatched,
                    favorites = favorites,
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    sessionSeed = System.currentTimeMillis()
                )
            }

            val initialMatches = spatialEngine.getMatchingMoviesForChain(
                chain = emptyList(),
                catalog = rawCatalog,
                serverUrl = serverUrl,
                accessToken = accessToken,
                format = _uiState.value.format
            )

            spatialEngine.updateSunNode(
                chain = emptyList(),
                movieCount = 0,
                previewPosters = emptyList(),
                format = _uiState.value.format
            )

            val allNodes = spatialEngine.getAllNodes()
            val allFilaments = spatialEngine.getAllFilaments()
            val initialSun = allNodes.firstOrNull { it.id == SUN_CORE_ID } ?: allNodes.firstOrNull()
            val visibleIds = spatialEngine.getVisibleNodeIds(emptyList(), _uiState.value.format)

            _uiState.value = _uiState.value.copy(
                nodes = allNodes,
                filaments = allFilaments,
                focusedNodeId = initialSun?.id,
                targetCameraX = initialSun?.worldX ?: 0.5f,
                targetCameraY = initialSun?.worldY ?: 0.5f,
                targetZoom = 1.0f,
                matchingMovies = emptyList(),
                visibleNodeIds = visibleIds,
                compatibleNodeIds = visibleIds,
                totalCatalogCount = rawCatalog.size,
                isLoading = false
            )
        }
    }

    /**
     * Navegación D-Pad en el espacio cósmico de Medusa.
     * Solo navega por nodos visibles supervivientes de la poda y permite entrar al Sol Central.
     */
    fun onNavigate(direction: DPadDirection): Boolean {
        val currentId = _uiState.value.focusedNodeId ?: return false
        val currentZoom = _uiState.value.targetZoom
        val chain = _uiState.value.activeChain
        val format = _uiState.value.format

        val nextNode = spatialEngine.findNextNeighbor(currentId, direction, currentZoom, chain, format) ?: return false

        _uiState.value = _uiState.value.copy(
            focusedNodeId = nextNode.id,
            targetCameraX = nextNode.worldX,
            targetCameraY = nextNode.worldY
        )
        return true
    }

    /**
     * Conecta / desconecta la estrella enfocada a la constelación.
     * Si se pulsa en el Sol Central -> Abre el visor de películas descubiertas.
     */
    fun toggleConnectFocused() {
        val focused = _uiState.value.focusedNode ?: return

        // Pulsar [OK] en el Sol Central -> Abrir visor de películas solo si hay una cadena activa con resultados
        if (focused.id == SUN_CORE_ID || focused.isSun || focused.isPortal) {
            if (_uiState.value.activeChain.isNotEmpty() && _uiState.value.matchingMovies.isNotEmpty()) {
                toggleMoviesOverlay()
            }
            return
        }

        val currentChain = _uiState.value.activeChain.toMutableList()

        if (currentChain.any { it.id == focused.id }) {
            // Ya estaba conectada: desconectar esta y las posteriores (deshacer)
            val index = currentChain.indexOfFirst { it.id == focused.id }
            val newChain = currentChain.take(index)
            val newZoom = calculateZoomForChain(newChain.size)

            val matches = spatialEngine.getMatchingMoviesForChain(newChain, rawCatalog, serverUrl, accessToken, _uiState.value.format)
            val posters = matches.take(3).mapNotNull { it.posterUrl }
            spatialEngine.updateSunNode(newChain, matches.size, posters, _uiState.value.format)
            val visibleIds = spatialEngine.getVisibleNodeIds(newChain, _uiState.value.format)

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = newChain,
                visibleNodeIds = visibleIds,
                compatibleNodeIds = visibleIds,
                incompatibleWarning = null,
                targetZoom = newZoom,
                matchingMovies = matches
            )
        } else {
            // Validar si la nueva etiqueta es compatible con la intersección "AND" estricta
            val currentVisible = _uiState.value.visibleNodeIds ?: _uiState.value.compatibleNodeIds
            if (currentVisible != null && focused.id !in currentVisible) {
                return
            } else if (currentVisible == null && !spatialEngine.canExtendChain(currentChain, focused, _uiState.value.format)) {
                return
            }

            // Conectar nueva etiqueta a la cadena
            currentChain.add(focused)
            val newZoom = calculateZoomForChain(currentChain.size)

            val matches = spatialEngine.getMatchingMoviesForChain(currentChain, rawCatalog, serverUrl, accessToken, _uiState.value.format)
            val posters = matches.take(3).mapNotNull { it.posterUrl }
            spatialEngine.updateSunNode(currentChain, matches.size, posters, _uiState.value.format)
            val visibleIds = spatialEngine.getVisibleNodeIds(currentChain, _uiState.value.format)

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = currentChain,
                visibleNodeIds = visibleIds,
                compatibleNodeIds = visibleIds,
                incompatibleWarning = null,
                targetZoom = newZoom,
                targetCameraX = focused.worldX,
                targetCameraY = focused.worldY,
                matchingMovies = matches
            )
        }
    }

    fun onNodeClicked(nodeId: String) {
        val node = spatialEngine.getNode(nodeId) ?: return
        _uiState.value = _uiState.value.copy(
            focusedNodeId = node.id,
            targetCameraX = node.worldX,
            targetCameraY = node.worldY
        )
        toggleConnectFocused()
    }

    fun onPlayPressed() {
        if (_uiState.value.matchingMovies.isNotEmpty()) {
            toggleMoviesOverlay()
        }
    }

    fun setFormat(format: MediaFormat) {
        if (_uiState.value.format == format) return
        val chain = _uiState.value.activeChain
        val matches = spatialEngine.getMatchingMoviesForChain(chain, rawCatalog, serverUrl, accessToken, format)
        val posters = matches.take(3).mapNotNull { it.posterUrl }
        spatialEngine.updateSunNode(chain, matches.size, posters, format)
        val visibleIds = spatialEngine.getVisibleNodeIds(chain, format)

        _uiState.value = _uiState.value.copy(
            format = format,
            matchingMovies = matches,
            visibleNodeIds = visibleIds,
            compatibleNodeIds = visibleIds,
            nodes = spatialEngine.getAllNodes(),
            filaments = spatialEngine.getAllFilaments()
        )
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
     * Manejo inteligente de la tecla Atrás:
     * 1. Si está viendo el visor de películas -> lo cierra y vuelve a enfocar el Sol Central.
     * 2. Si hay estrellas conectadas -> desconecta la última (la galaxia se re-expande) y se aleja.
     * 3. Si no hay estrellas conectadas -> devuelve false para que la pantalla vuelva al inicio.
     */
    fun onBackPress(): Boolean {
        if (_uiState.value.selectedMovie != null) {
            _uiState.value = _uiState.value.copy(selectedMovie = null)
            return true
        }

        if (_uiState.value.showMoviesOverlay) {
            val sun = spatialEngine.getNode(SUN_CORE_ID)
            _uiState.value = _uiState.value.copy(
                showMoviesOverlay = false,
                focusedNodeId = SUN_CORE_ID,
                targetCameraX = sun?.worldX ?: 0.5f,
                targetCameraY = sun?.worldY ?: 0.5f
            )
            return true
        }

        val chain = _uiState.value.activeChain
        if (chain.isNotEmpty()) {
            val newChain = chain.dropLast(1)
            val prevNode = newChain.lastOrNull() ?: spatialEngine.getNode(SUN_CORE_ID)
            val newZoom = calculateZoomForChain(newChain.size)

            val matches = spatialEngine.getMatchingMoviesForChain(newChain, rawCatalog, serverUrl, accessToken, _uiState.value.format)
            val posters = matches.take(3).mapNotNull { it.posterUrl }
            spatialEngine.updateSunNode(newChain, matches.size, posters, _uiState.value.format)
            val visibleIds = spatialEngine.getVisibleNodeIds(newChain, _uiState.value.format)

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = newChain,
                visibleNodeIds = visibleIds,
                compatibleNodeIds = visibleIds,
                incompatibleWarning = null,
                focusedNodeId = prevNode?.id ?: SUN_CORE_ID,
                targetCameraX = prevNode?.worldX ?: 0.5f,
                targetCameraY = prevNode?.worldY ?: 0.5f,
                targetZoom = newZoom,
                matchingMovies = matches
            )
            return true
        }

        return false
    }

    fun resetConstellation() {
        if (rawCatalog.isNotEmpty()) {
            spatialEngine.buildDynamicUniverse(
                catalog = rawCatalog,
                tmdbTopRated = lastTmdbCanon,
                tmdbTrending = lastTmdbTrending,
                recentWatched = lastRecentWatched,
                favorites = lastFavorites,
                serverUrl = serverUrl,
                accessToken = accessToken,
                sessionSeed = System.currentTimeMillis()
            )
        }
        val matches = spatialEngine.getMatchingMoviesForChain(emptyList(), rawCatalog, serverUrl, accessToken, _uiState.value.format)
        val posters = matches.take(3).mapNotNull { it.posterUrl }
        spatialEngine.updateSunNode(emptyList(), matches.size, posters, _uiState.value.format)
        val visibleIds = spatialEngine.getVisibleNodeIds(emptyList(), _uiState.value.format)

        _uiState.value = _uiState.value.copy(
            nodes = spatialEngine.getAllNodes(),
            filaments = spatialEngine.getAllFilaments(),
            activeChain = emptyList(),
            visibleNodeIds = visibleIds,
            compatibleNodeIds = visibleIds,
            incompatibleWarning = null,
            focusedNodeId = SUN_CORE_ID,
            targetCameraX = 0.5f,
            targetCameraY = 0.5f,
            targetZoom = 1.0f,
            matchingMovies = matches,
            showMoviesOverlay = false,
            selectedMovie = null
        )
    }

    private fun calculateZoomForChain(chainSize: Int): Float {
        return when (chainSize) {
            0 -> 1.0f
            1 -> 1.35f
            2 -> 1.65f
            else -> 1.95f
        }
    }
}
