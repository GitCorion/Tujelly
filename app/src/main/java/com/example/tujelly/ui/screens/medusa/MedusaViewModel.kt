package com.example.tujelly.ui.screens.medusa

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.AppDatabase
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.medusa.MedusaBrain
import com.example.tujelly.data.medusa.MedusaBrainRepository
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sin

data class MedusaStar(
    val id: String,
    val label: String,
    val baseNormX: Float,
    val baseNormY: Float,
    val textOnLeft: Boolean,
    val relatedStarIds: List<String>,
    val primaryGenres: List<String>,
    val secondaryKeywords: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null
) {
    val normX: Float get() = baseNormX
    val normY: Float get() = baseNormY
}

enum class MedusaFormatFilter(val displayName: String) {
    ALL("Todos"),
    MOVIES("Películas"),
    SERIES("Series")
}

data class MedusaUiState(
    val allStars: List<MedusaStar> = emptyList(),
    val selectedPath: List<String> = emptyList(),
    val activeBranchIds: List<String> = emptyList(),
    val starPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    val rawRecommendations: List<MediaItem> = emptyList(),
    val selectedFormat: MedusaFormatFilter = MedusaFormatFilter.ALL,
    val focusedItem: MediaItem? = null,
    val isLoading: Boolean = false
) {
    val recommendations: List<MediaItem> get() {
        return when (selectedFormat) {
            MedusaFormatFilter.ALL -> rawRecommendations
            MedusaFormatFilter.MOVIES -> rawRecommendations.filter { it.type.equals("Movie", ignoreCase = true) }
            MedusaFormatFilter.SERIES -> rawRecommendations.filter { it.type.equals("Series", ignoreCase = true) }
        }
    }

    val selectedStarIds: Set<String> get() = selectedPath.toSet()
    val selectedCount: Int get() = selectedPath.size
    val isFormed: Boolean get() = selectedPath.isNotEmpty()

    val starMap: Map<String, MedusaStar> by lazy {
        allStars.associateBy { it.id }
    }

    /**
     * Flujo celestial del tentáculo:
     * - En reposo: todas las estrellas del firmamento son visibles.
     * - Con tentáculo activo: solo la columna vertebral seleccionada y sus ramas activas (máximo 4).
     */
    val visibleStarIds: Set<String> get() {
        if (selectedPath.isEmpty()) {
            return allStars.map { it.id }.toSet()
        }
        return (selectedPath + activeBranchIds).toSet()
    }
}

class MedusaViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val jellyfinDao = database.jellyfinDao()

    private val brainRepository = MedusaBrainRepository(application)
    private val neuralEngine = MedusaNeuralEngine()
    private var brain: MedusaBrain? = null
    private var itemRelevance: Map<String, List<NeuronRef>> = emptyMap()

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
        loadConstellation()
        observeCatalogChanges()
    }

    /**
     * Recalcula la constelación cuando cambia el catálogo (sincronización) o
     * los favoritos, de modo que la Medusa reacciona en vivo al historial.
     */
    private fun observeCatalogChanges() {
        viewModelScope.launch {
            combine(
                database.jellyfinDao().getMediaCountFlow().distinctUntilChanged(),
                database.jellyfinDao().getFavoritesFlow().distinctUntilChanged()
            ) { _, _ -> Unit }
                .drop(1)
                .collect { loadConstellation() }
        }
    }

    fun loadConstellation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Cerebro semántico: caché -> asset embebido. El refresco de GitHub
            // se lanza en background para no bloquear la carga de la UI.
            val loadedBrain = brainRepository.loadBrain()
            brain = loadedBrain

            viewModelScope.launch {
                brainRepository.refreshFromGitHub()
                    .onSuccess { Log.d("MedusaViewModel", "Cerebro actualizado desde GitHub: v$it") }
                    .onFailure { Log.d("MedusaViewModel", "Sin cerebro remoto (usando local): ${it.message}") }
            }

            val movies = runCatching { jellyfinDao.getMovies() }.getOrDefault(emptyList())
            val series = runCatching { jellyfinDao.getSeries() }.getOrDefault(emptyList())
            val catalog = movies + series

            val constellation = if (loadedBrain != null) {
                itemRelevance = neuralEngine.buildItemRelevance(loadedBrain, catalog)
                neuralEngine.buildStars(loadedBrain, catalog)
            } else {
                itemRelevance = emptyMap()
                emptyList()
            }

            val initialPositions = calculateCongregatingPositions(constellation, emptyList(), emptyList())

            _uiState.value = MedusaUiState(
                allStars = constellation,
                selectedPath = emptyList(),
                activeBranchIds = emptyList(),
                starPositions = initialPositions,
                isLoading = false
            )

            Log.d("MedusaViewModel", "Constelación neuronal: ${constellation.size} neuronas sobre ${catalog.size} títulos")
        }
    }

    fun toggleStar(starId: String) {
        val currentState = _uiState.value
        val currentPath = currentState.selectedPath.toMutableList()

        if (currentPath.contains(starId)) {
            val index = currentPath.indexOf(starId)
            if (index == currentPath.lastIndex) {
                // Si pulsa el último nodo activo, lo retira
                currentPath.removeAt(index)
            } else {
                // Si pulsa un nodo anterior del tentáculo, rebobina hasta ese punto
                val subPath = currentPath.subList(0, index + 1).toList()
                currentPath.clear()
                currentPath.addAll(subPath)
            }
        } else {
            // Se añade como siguiente eslabón del tentáculo
            currentPath.add(starId)
        }

        val activeBranches = computeActiveBranches(currentState.allStars, currentPath)
        val newPositions = calculateCongregatingPositions(currentState.allStars, currentPath, activeBranches)

        _uiState.value = currentState.copy(
            selectedPath = currentPath,
            activeBranchIds = activeBranches,
            starPositions = newPositions
        )

        Log.d("MedusaViewModel", "toggleStar: path=$currentPath, branches=$activeBranches")
        updateRecommendations()
    }

    fun resetConstellation() {
        val currentState = _uiState.value
        val restPositions = calculateCongregatingPositions(currentState.allStars, emptyList(), emptyList())
        _uiState.value = currentState.copy(
            selectedPath = emptyList(),
            activeBranchIds = emptyList(),
            starPositions = restPositions,
            rawRecommendations = emptyList(),
            selectedFormat = MedusaFormatFilter.ALL,
            focusedItem = null,
            isLoading = false
        )
    }

    fun setFormatFilter(filter: MedusaFormatFilter) {
        val current = _uiState.value
        if (current.selectedFormat == filter) return
        val filtered = when (filter) {
            MedusaFormatFilter.ALL -> current.rawRecommendations
            MedusaFormatFilter.MOVIES -> current.rawRecommendations.filter { it.type.equals("Movie", ignoreCase = true) }
            MedusaFormatFilter.SERIES -> current.rawRecommendations.filter { it.type.equals("Series", ignoreCase = true) }
        }
        _uiState.value = current.copy(
            selectedFormat = filter,
            focusedItem = filtered.firstOrNull()
        )
    }

    fun setFocusedItem(item: MediaItem) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
    }

    /**
     * Calcula hasta 4 ramas afines directas desde el último nodo seleccionado.
     * Esto evita la acumulación y asegura un espaciado amplio y elegante.
     */
    private fun computeActiveBranches(allStars: List<MedusaStar>, selectedPath: List<String>): List<String> {
        if (selectedPath.isEmpty()) return emptyList()
        val starMap = allStars.associateBy { it.id }
        val lastStarId = selectedPath.last()
        val lastStar = starMap[lastStarId] ?: return emptyList()

        val selectedSet = selectedPath.toSet()
        return lastStar.relatedStarIds
            .filter { it !in selectedSet }
            .distinct()
            .take(4)
    }

    /**
     * Calcula las posiciones del firmamento estelar:
     * - En reposo: constelación completa en su cúpula y cuerpo original.
     * - Con tentáculo activo:
     *   - La columna vertebral seleccionada desciende en el centro.
     *   - Sus ramas activas (3 a 4 opciones) descienden en abanico amplio desde la punta del tentáculo.
     */
    private fun calculateCongregatingPositions(
        allStars: List<MedusaStar>,
        selectedPath: List<String>,
        activeBranches: List<String>
    ): Map<String, Pair<Float, Float>> {
        val result = mutableMapOf<String, Pair<Float, Float>>()

        if (selectedPath.isEmpty()) {
            for (star in allStars) {
                result[star.id] = Pair(star.baseNormX, star.baseNormY)
            }
            return result
        }

        val numSelected = selectedPath.size

        // 1. Columna vertebral del tentáculo seleccionado
        if (numSelected == 1) {
            result[selectedPath[0]] = Pair(0.50f, 0.16f)
        } else {
            // Nodos ancestros: perlas superiores a lo largo del cordón
            val numAncestors = numSelected - 1
            val startY = 0.08f
            val endY = (0.08f + (numAncestors - 1) * 0.07f).coerceAtMost(0.18f)
            val stepY = if (numAncestors > 1) (endY - startY) / (numAncestors - 1) else 0f

            for (i in 0 until numAncestors) {
                val ancestorId = selectedPath[i]
                val waveX = 0.50f + (if (i % 2 == 0) -0.012f else 0.012f)
                val targetY = if (numAncestors == 1) 0.12f else (startY + i * stepY)
                result[ancestorId] = Pair(waveX, targetY)
            }

            // Nodo activo actual: cápsula interactiva en el centro focal
            val activeNodeId = selectedPath.last()
            result[activeNodeId] = Pair(0.50f, 0.28f)
        }

        // 2. Ramas o tentáculos afines descendentes desde la punta activa
        val branchCount = activeBranches.size
        if (branchCount > 0) {
            val branchBaseY = 0.62f

            val minX = 0.18f
            val maxX = 0.82f
            val stepX = if (branchCount > 1) (maxX - minX) / (branchCount - 1) else 0f

            activeBranches.forEachIndexed { index, branchId ->
                val targetX = if (branchCount == 1) 0.50f else (minX + index * stepX).coerceIn(0.12f, 0.88f)
                val normalizedIndex = if (branchCount > 1) index.toFloat() / (branchCount - 1) else 0.5f
                val catenarySag = sin(normalizedIndex * Math.PI.toFloat()) * 0.05f
                val targetY = (branchBaseY + catenarySag).coerceIn(0.50f, 0.78f)

                result[branchId] = Pair(targetX, targetY)
            }
        }

        // 3. Estrellas no activas: se alejan radialmente hacia el fondo cósmico exterior
        for (star in allStars) {
            if (star.id !in result) {
                val dirX = star.baseNormX - 0.50f
                val dirY = star.baseNormY - 0.30f
                val length = kotlin.math.sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(0.01f)
                val driftDistance = 0.45f
                val targetX = (star.baseNormX + (dirX / length) * driftDistance).coerceIn(-0.25f, 1.25f)
                val targetY = (star.baseNormY + (dirY / length) * driftDistance).coerceIn(-0.25f, 1.25f)
                result[star.id] = Pair(targetX, targetY)
            }
        }

        return result
    }

    private fun updateRecommendations() {
        val currentState = _uiState.value
        if (!currentState.isFormed) {
            _uiState.value = currentState.copy(rawRecommendations = emptyList(), focusedItem = null, isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val prefs = runCatching { userPreferencesRepository.userPreferencesFlow.first() }.getOrNull()
            val serverUrl = prefs?.jellyfinServerUrl ?: ""
            val token = prefs?.jellyfinAccessToken ?: ""

            val movies = jellyfinDao.getMovies()
            val series = jellyfinDao.getSeries()
            val allCandidates = movies + series

            val selectedNeuronIds = currentState.selectedStarIds
            val ranked = brain?.let { b ->
                neuralEngine.scoreItems(b, allCandidates, itemRelevance, selectedNeuronIds)
            } ?: emptyList()

            val finalEntities = if (ranked.isNotEmpty()) {
                ranked
            } else {
                allCandidates.sortedByDescending { it.communityRating ?: 0f }.take(20)
            }

            val items = finalEntities.map { it.toMediaItem(serverUrl, token) }
            Log.d("MedusaViewModel", "Loaded ${items.size} recommendations for ${selectedNeuronIds.size} neuronas")

            _uiState.value = _uiState.value.copy(
                rawRecommendations = items,
                focusedItem = items.firstOrNull(),
                isLoading = false
            )
        }
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
        val posterUrl = if (!primaryImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Primary?quality=90&fillWidth=400&fillHeight=600"
        } else null

        val backdropUrl = if (!backdropImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Backdrop/0?quality=90&maxWidth=1920"
        } else posterUrl

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
            totalEpisodes = totalItemCount,
            playedEpisodes = if (unplayedItemCount != null && totalItemCount != null) {
                (totalItemCount - unplayedItemCount).coerceAtLeast(0)
            } else null
        )
    }

}
