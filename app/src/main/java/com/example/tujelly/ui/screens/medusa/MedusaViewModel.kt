package com.example.tujelly.ui.screens.medusa

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaItem
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
    val compatibleNodeIds: Set<String>? = null,
    val incompatibleWarning: String? = null,
    val matchingMovies: List<MediaItem> = emptyList(),
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

    private val spatialEngine = NebulaSpatialEngine()
    private var rawCatalog: List<JellyfinMediaEntity> = emptyList()
    private var serverUrl: String = ""
    private var accessToken: String = ""

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

            withContext(Dispatchers.Default) {
                spatialEngine.buildUniverse(rawCatalog, serverUrl, accessToken)
            }

            val initialMatches = spatialEngine.getMatchingMoviesForChain(
                chain = emptyList(),
                catalog = rawCatalog,
                serverUrl = serverUrl,
                accessToken = accessToken
            )

            spatialEngine.updatePortalNode(chain = emptyList(), movieCount = initialMatches.size)
            val allNodes = spatialEngine.getAllNodes()
            val allFilaments = spatialEngine.getAllFilaments()
            val initialNode = allNodes.firstOrNull { it.id == "CLUSTER_0" } ?: allNodes.firstOrNull()

            _uiState.value = _uiState.value.copy(
                nodes = allNodes,
                filaments = allFilaments,
                focusedNodeId = initialNode?.id,
                targetCameraX = initialNode?.worldX ?: 0.5f,
                targetCameraY = initialNode?.worldY ?: 0.5f,
                targetZoom = 1.0f,
                matchingMovies = initialMatches,
                totalCatalogCount = rawCatalog.size,
                isLoading = false
            )
        }
    }

    /**
     * Navegación D-Pad en el espacio semántico.
     * Actualiza el foco y recentra la cámara en el nodo seleccionado.
     * Retorna true si encontró un nodo vecino en esa dirección, o false si llegó al límite.
     */
    fun onNavigate(direction: DPadDirection): Boolean {
        val currentId = _uiState.value.focusedNodeId ?: return false
        val currentZoom = _uiState.value.targetZoom
        val chain = _uiState.value.activeChain
        val compatIds = _uiState.value.compatibleNodeIds
        val nextNode = spatialEngine.findNextNeighbor(currentId, direction, currentZoom, chain, compatIds) ?: return false

        _uiState.value = _uiState.value.copy(
            focusedNodeId = nextNode.id,
            targetCameraX = nextNode.worldX,
            targetCameraY = nextNode.worldY
        )
        return true
    }

    /**
     * Conecta / desconecta la etiqueta enfocada en la constelación.
     * Si el nodo enfocado es el Portal de Películas, abre el visor de películas.
     */
    fun toggleConnectFocused() {
        val focused = _uiState.value.focusedNode ?: return

        // Si se pulsa OK en el Portal de Películas -> Abrir visor de películas
        if (focused.id == PORTAL_NODE_ID || focused.isPortal) {
            toggleMoviesOverlay()
            return
        }

        val currentChain = _uiState.value.activeChain.toMutableList()

        if (currentChain.any { it.id == focused.id }) {
            // Ya estaba conectada: desconectar esta y las posteriores
            val index = currentChain.indexOfFirst { it.id == focused.id }
            val newChain = currentChain.take(index)
            val newZoom = calculateZoomForChain(newChain.size)
            
            val matches = spatialEngine.getMatchingMoviesForChain(newChain, rawCatalog, serverUrl, accessToken)
            spatialEngine.updatePortalNode(newChain, matches.size)
            val compatIds = if (newChain.isNotEmpty()) spatialEngine.getCompatibleNodeIds(newChain) else null

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = newChain,
                compatibleNodeIds = compatIds,
                incompatibleWarning = null,
                targetZoom = newZoom,
                matchingMovies = matches
            )
        } else {
            // Validar si la nueva etiqueta es compatible con la constelación activa
            if (!spatialEngine.canExtendChain(currentChain, focused)) {
                return
            }

            // Conectar nueva etiqueta a la cadena
            currentChain.add(focused)
            val newZoom = calculateZoomForChain(currentChain.size)
            
            val matches = spatialEngine.getMatchingMoviesForChain(currentChain, rawCatalog, serverUrl, accessToken)
            spatialEngine.updatePortalNode(currentChain, matches.size)
            val compatIds = spatialEngine.getCompatibleNodeIds(currentChain)

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = currentChain,
                compatibleNodeIds = compatIds,
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
     * 1. Si está viendo el visor de películas -> lo cierra y vuelve a la constelación.
     * 2. Si hay etiquetas conectadas -> desconecta la última etiqueta y se aleja un nivel.
     * 3. Si no hay etiquetas conectadas -> devuelve false para que la pantalla vuelva al inicio.
     */
    fun onBackPress(): Boolean {
        if (_uiState.value.selectedMovie != null) {
            _uiState.value = _uiState.value.copy(selectedMovie = null)
            return true
        }

        if (_uiState.value.showMoviesOverlay) {
            val portal = spatialEngine.getNode(PORTAL_NODE_ID)
            _uiState.value = _uiState.value.copy(
                showMoviesOverlay = false,
                focusedNodeId = PORTAL_NODE_ID,
                targetCameraX = portal?.worldX ?: _uiState.value.targetCameraX,
                targetCameraY = portal?.worldY ?: _uiState.value.targetCameraY
            )
            return true
        }

        val chain = _uiState.value.activeChain
        if (chain.isNotEmpty()) {
            val newChain = chain.dropLast(1)
            val prevNode = newChain.lastOrNull() ?: _uiState.value.focusedNode
            val newZoom = calculateZoomForChain(newChain.size)
            val matches = spatialEngine.getMatchingMoviesForChain(newChain, rawCatalog, serverUrl, accessToken)
            spatialEngine.updatePortalNode(newChain, matches.size)
            val compatIds = if (newChain.isNotEmpty()) spatialEngine.getCompatibleNodeIds(newChain) else null

            _uiState.value = _uiState.value.copy(
                nodes = spatialEngine.getAllNodes(),
                filaments = spatialEngine.getAllFilaments(),
                activeChain = newChain,
                compatibleNodeIds = compatIds,
                incompatibleWarning = null,
                focusedNodeId = prevNode?.id ?: _uiState.value.focusedNodeId,
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
        val matches = spatialEngine.getMatchingMoviesForChain(emptyList(), rawCatalog, serverUrl, accessToken)
        spatialEngine.updatePortalNode(emptyList(), matches.size)
        val initialNode = spatialEngine.getAllNodes().firstOrNull { it.id == "CLUSTER_0" } ?: spatialEngine.getAllNodes().firstOrNull()

        _uiState.value = _uiState.value.copy(
            nodes = spatialEngine.getAllNodes(),
            filaments = spatialEngine.getAllFilaments(),
            activeChain = emptyList(),
            compatibleNodeIds = null,
            incompatibleWarning = null,
            focusedNodeId = initialNode?.id,
            targetCameraX = initialNode?.worldX ?: 0.5f,
            targetCameraY = initialNode?.worldY ?: 0.5f,
            targetZoom = 1.0f,
            matchingMovies = matches,
            showMoviesOverlay = false,
            selectedMovie = null
        )
    }

    private fun calculateZoomForChain(chainSize: Int): Float {
        return when (chainSize) {
            0 -> 1.0f
            1 -> 1.45f
            2 -> 1.80f
            3 -> 2.10f
            else -> 2.35f
        }
    }
}
