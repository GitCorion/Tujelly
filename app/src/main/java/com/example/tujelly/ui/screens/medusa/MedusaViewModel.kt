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

data class MedusaStar(
    val id: String,
    val label: String,
    val normX: Float,
    val normY: Float,
    val textOnLeft: Boolean,
    val relatedStarIds: List<String>,
    val primaryGenres: List<String>,
    val secondaryKeywords: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null
)

data class MedusaUiState(
    val selectedStarIds: Set<String> = emptySet(),
    val recommendations: List<MediaItem> = emptyList(),
    val focusedItem: MediaItem? = null,
    val isLoading: Boolean = false
) {
    val selectedCount: Int get() = selectedStarIds.size
    val isFormed: Boolean get() = selectedStarIds.size >= 3

    val visibleStarIds: Set<String> get() {
        val visible = MedusaViewModel.rootStarIds.toMutableSet()
        for (id in selectedStarIds) {
            visible.add(id)
            val star = MedusaViewModel.starMap[id]
            if (star != null) {
                visible.addAll(star.relatedStarIds)
            }
        }
        return visible
    }
}

class MedusaViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val jellyfinDao = database.jellyfinDao()

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

    fun toggleStar(starId: String) {
        val currentSet = _uiState.value.selectedStarIds.toMutableSet()
        if (currentSet.contains(starId)) {
            currentSet.remove(starId)
        } else {
            currentSet.add(starId)
        }
        _uiState.value = _uiState.value.copy(selectedStarIds = currentSet)
        Log.d("MedusaViewModel", "toggleStar $starId, selected total: ${currentSet.size}")
        updateRecommendations()
    }

    fun resetConstellation() {
        _uiState.value = MedusaUiState()
    }

    fun setFocusedItem(item: MediaItem) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
    }

    private fun updateRecommendations() {
        val currentState = _uiState.value
        // Solo cuando hay al menos 3 estrellas seleccionadas aparecen las recomendaciones
        if (!currentState.isFormed) {
            _uiState.value = currentState.copy(recommendations = emptyList(), focusedItem = null, isLoading = false)
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

            val activeStars = constellationStars.filter { it.id in _uiState.value.selectedStarIds }

            val scored = allCandidates.mapNotNull { entity ->
                val entityGenresLower = (entity.genres ?: "").lowercase()
                val entityTitleLower = entity.title.lowercase()
                val overviewLower = (entity.overview ?: "").lowercase()
                val year = entity.productionYear

                var score = 0

                for (star in activeStars) {
                    // Filtro de año histórico
                    if (star.minYear != null && star.maxYear != null && year != null) {
                        if (year in star.minYear..star.maxYear) {
                            score += 25
                        }
                    }

                    // Géneros principales
                    for (g in star.primaryGenres) {
                        val gl = g.lowercase()
                        if (entityGenresLower.contains(gl)) score += 18
                        if (entityTitleLower.contains(gl) || overviewLower.contains(gl)) score += 6
                    }

                    // Palabras clave secundarias
                    for (kw in star.secondaryKeywords) {
                        val kwl = kw.lowercase()
                        if (entityGenresLower.contains(kwl) || overviewLower.contains(kwl)) score += 8
                    }
                }

                if (score <= 0) return@mapNotNull null

                val rating = entity.communityRating ?: 6.0f
                score += (rating * 2.5f).toInt()
                if (!entity.backdropImageTag.isNullOrBlank()) score += 4
                if (!entity.isPlayed) score += 6

                Pair(entity, score)
            }

            val finalEntities = if (scored.isNotEmpty()) {
                scored.sortedByDescending { it.second }.take(25).map { it.first }
            } else {
                // Fallback para asegurar que siempre haya recomendaciones visibles
                val partialScored = allCandidates.mapNotNull { entity ->
                    val entityGenresLower = (entity.genres ?: "").lowercase()
                    val matchCount = activeStars.count { star ->
                        star.primaryGenres.any { entityGenresLower.contains(it.lowercase()) }
                    }
                    if (matchCount > 0) Pair(entity, matchCount) else null
                }
                if (partialScored.isNotEmpty()) {
                    partialScored.sortedByDescending { it.second }.take(25).map { it.first }
                } else {
                    allCandidates.sortedByDescending { it.communityRating ?: 0f }.take(20)
                }
            }

            val items = finalEntities.map { it.toMediaItem(serverUrl, token) }
            Log.d("MedusaViewModel", "Loaded ${items.size} recommendations for ${activeStars.map { it.label }}")

            _uiState.value = _uiState.value.copy(
                recommendations = items,
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

    companion object {
        val rootStarIds = setOf("DRAMA", "ACCION", "COMEDIA", "AVENTURA")

        val constellationStars = listOf(
            // --- RAÍCES Y CÚPULA SUPERIOR ---
            MedusaStar(
                id = "DRAMA",
                label = "Drama",
                normX = 0.36f,
                normY = 0.24f,
                textOnLeft = true,
                relatedStarIds = listOf("NOIR", "MELANCOLICO", "ANOS_70"),
                primaryGenres = listOf("Drama"),
                secondaryKeywords = listOf("emoción", "sentimiento", "vida")
            ),
            MedusaStar(
                id = "ACCION",
                label = "Acción",
                normX = 0.50f,
                normY = 0.12f,
                textOnLeft = false,
                relatedStarIds = listOf("INTENSO", "AVENTURA", "SCIFI"),
                primaryGenres = listOf("Acción", "Action"),
                secondaryKeywords = listOf("adrenalina", "combate", "misión")
            ),
            MedusaStar(
                id = "COMEDIA",
                label = "Comedia",
                normX = 0.66f,
                normY = 0.24f,
                textOnLeft = false,
                relatedStarIds = listOf("ROMANCE", "FAMILIAR"),
                primaryGenres = listOf("Comedia", "Comedy"),
                secondaryKeywords = listOf("humor", "risas", "divertido")
            ),
            MedusaStar(
                id = "AVENTURA",
                label = "Aventura",
                normX = 0.44f,
                normY = 0.18f,
                textOnLeft = true,
                relatedStarIds = listOf("FANTASIA", "SCIFI", "ACCION"),
                primaryGenres = listOf("Aventura", "Adventure"),
                secondaryKeywords = listOf("viaje", "descubrimiento", "épica")
            ),

            // --- ESTRELLAS RELACIONADAS RAMIFICADAS (FORMA DE MEDUSA) ---
            MedusaStar(
                id = "NOIR",
                label = "Noir",
                normX = 0.52f,
                normY = 0.27f,
                textOnLeft = false,
                relatedStarIds = listOf("SUSPENSE", "ANOS_70", "LENTO"),
                primaryGenres = listOf("Crimen", "Crime", "Misterio", "Mystery", "Thriller"),
                secondaryKeywords = listOf("noir", "detective", "policíaco", "corrupción")
            ),
            MedusaStar(
                id = "ANOS_70",
                label = "Años 70",
                normX = 0.36f,
                normY = 0.36f,
                textOnLeft = true,
                relatedStarIds = listOf("NOIR", "SUSPENSE"),
                primaryGenres = listOf("Clásico", "Drama", "Crimen"),
                secondaryKeywords = listOf("70s", "retro", "clásico"),
                minYear = 1965,
                maxYear = 1982
            ),
            MedusaStar(
                id = "FANTASIA",
                label = "Fantasía",
                normX = 0.65f,
                normY = 0.18f,
                textOnLeft = false,
                relatedStarIds = listOf("SCIFI", "AVENTURA"),
                primaryGenres = listOf("Fantasía", "Fantasy"),
                secondaryKeywords = listOf("magia", "criaturas", "cuentos")
            ),
            MedusaStar(
                id = "SCIFI",
                label = "Sci-Fi",
                normX = 0.60f,
                normY = 0.34f,
                textOnLeft = false,
                relatedStarIds = listOf("FANTASIA", "SUSPENSE"),
                primaryGenres = listOf("Ciencia ficción", "Sci-Fi"),
                secondaryKeywords = listOf("espacio", "futuro", "tecnología")
            ),
            MedusaStar(
                id = "ROMANCE",
                label = "Romance",
                normX = 0.62f,
                normY = 0.44f,
                textOnLeft = false,
                relatedStarIds = listOf("MELANCOLICO", "COMEDIA"),
                primaryGenres = listOf("Romance", "Romántica"),
                secondaryKeywords = listOf("amor", "pareja", "pasión")
            ),
            MedusaStar(
                id = "FAMILIAR",
                label = "Familiar",
                normX = 0.56f,
                normY = 0.18f,
                textOnLeft = false,
                relatedStarIds = listOf("AVENTURA", "COMEDIA"),
                primaryGenres = listOf("Animación", "Familia", "Family"),
                secondaryKeywords = listOf("niños", "infantil", "disney", "pixar")
            ),

            // --- TENTÁCULO CENTRAL DESCENDENTE ---
            MedusaStar(
                id = "SUSPENSE",
                label = "Suspense",
                normX = 0.48f,
                normY = 0.46f,
                textOnLeft = true,
                relatedStarIds = listOf("INTENSO", "LENTO", "TERROR"),
                primaryGenres = listOf("Suspense", "Thriller", "Misterio"),
                secondaryKeywords = listOf("intriga", "tensión", "giro")
            ),
            MedusaStar(
                id = "LENTO",
                label = "Lento",
                normX = 0.50f,
                normY = 0.61f,
                textOnLeft = true,
                relatedStarIds = listOf("INTENSO", "MELANCOLICO"),
                primaryGenres = listOf("Drama", "Biografía", "Historia"),
                secondaryKeywords = listOf("contemplativo", "profundo", "pausado")
            ),
            MedusaStar(
                id = "INTENSO",
                label = "Intenso",
                normX = 0.61f,
                normY = 0.71f,
                textOnLeft = false,
                relatedStarIds = listOf("ADRENALINA", "TERROR"),
                primaryGenres = listOf("Acción", "Action", "Thriller"),
                secondaryKeywords = listOf("adrenalina", "impacto", "ritmo")
            ),
            MedusaStar(
                id = "ADRENALINA",
                label = "Adrenalina",
                normX = 0.54f,
                normY = 0.85f,
                textOnLeft = true,
                relatedStarIds = emptyList(),
                primaryGenres = listOf("Acción", "Thriller"),
                secondaryKeywords = listOf("velocidad", "persecución", "extremo")
            ),

            // --- TENTÁCULOS LATERALES ---
            MedusaStar(
                id = "MELANCOLICO",
                label = "Melancólico",
                normX = 0.30f,
                normY = 0.56f,
                textOnLeft = true,
                relatedStarIds = listOf("ROMANCE", "LENTO"),
                primaryGenres = listOf("Drama", "Romance"),
                secondaryKeywords = listOf("melancolía", "nostalgia", "tristeza")
            ),
            MedusaStar(
                id = "TERROR",
                label = "Terror",
                normX = 0.72f,
                normY = 0.58f,
                textOnLeft = false,
                relatedStarIds = listOf("INTENSO"),
                primaryGenres = listOf("Terror", "Horror"),
                secondaryKeywords = listOf("miedo", "pesadilla", "sobrenatural")
            )
        )

        val starMap = constellationStars.associateBy { it.id }
    }
}
