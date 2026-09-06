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
import kotlin.math.cos
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

data class MedusaUiState(
    val allStars: List<MedusaStar> = emptyList(),
    val selectedStarIds: Set<String> = emptySet(),
    val starPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    val recommendations: List<MediaItem> = emptyList(),
    val focusedItem: MediaItem? = null,
    val isLoading: Boolean = false
) {
    val selectedCount: Int get() = selectedStarIds.size
    val isFormed: Boolean get() = selectedStarIds.size >= 1

    val starMap: Map<String, MedusaStar> by lazy {
        allStars.associateBy { it.id }
    }

    /**
     * Flujo "De más a menos":
     * - Si no hay ninguna selección, se ven TODAS las estrellas de la constelación ("MÁS").
     * - Conforme el usuario elige estrellas ("A MENOS"), se va reduciendo el mapa estelar,
     *   quedando solo las estrellas seleccionadas y sus ideas directamente relacionadas.
     */
    val visibleStarIds: Set<String> get() {
        if (selectedStarIds.isEmpty()) {
            return allStars.map { it.id }.toSet()
        }

        val currentStarMap = allStars.associateBy { it.id }
        val visible = mutableSetOf<String>()
        visible.addAll(selectedStarIds)

        for (id in selectedStarIds) {
            val star = currentStarMap[id]
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

    init {
        loadConstellation()
    }

    fun loadConstellation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Retroalimentación en tiempo real del historial de visualización
            val watchedGenresList = runCatching { jellyfinDao.getWatchedGenres() }.getOrDefault(emptyList())
            val genreFrequency = mutableMapOf<String, Int>()
            for (gStr in watchedGenresList) {
                val splitGenres = gStr.split(",", ";", "/").map { it.trim() }
                for (g in splitGenres) {
                    if (g.isNotBlank()) {
                        genreFrequency[g] = (genreFrequency[g] ?: 0) + 1
                    }
                }
            }
            val topWatchedGenres = genreFrequency.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }

            // 2. Éxitos y tendencias del momento en el catálogo
            val topRatedLocal = runCatching { jellyfinDao.getTopRatedLocal(30) }.getOrDefault(emptyList())
            val trendGenresMap = mutableMapOf<String, Int>()
            topRatedLocal.forEach { item ->
                item.genres?.split(",", ";", "/")?.map { it.trim() }?.forEach { g ->
                    if (g.isNotBlank()) {
                        trendGenresMap[g] = (trendGenresMap[g] ?: 0) + 1
                    }
                }
            }
            val topTrendGenres = trendGenresMap.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }

            // 3. Generar la constelación adaptativa viva
            val dynamicConstellation = buildDynamicFullConstellation(topWatchedGenres, topTrendGenres)
            val initialPositions = calculateCongregatingPositions(dynamicConstellation, emptySet())

            _uiState.value = MedusaUiState(
                allStars = dynamicConstellation,
                selectedStarIds = emptySet(),
                starPositions = initialPositions,
                isLoading = false
            )

            Log.d("MedusaViewModel", "Constelación mutada con éxito (Vistas: $topWatchedGenres, Tendencias: $topTrendGenres)")
        }
    }

    fun toggleStar(starId: String) {
        val currentState = _uiState.value
        val currentSet = currentState.selectedStarIds.toMutableSet()
        if (currentSet.contains(starId)) {
            currentSet.remove(starId)
        } else {
            currentSet.add(starId)
        }

        val newPositions = calculateCongregatingPositions(currentState.allStars, currentSet)

        _uiState.value = currentState.copy(
            selectedStarIds = currentSet,
            starPositions = newPositions
        )

        Log.d("MedusaViewModel", "toggleStar $starId, selected total: ${currentSet.size}")
        updateRecommendations()
    }

    fun resetConstellation() {
        val currentState = _uiState.value
        val restPositions = calculateCongregatingPositions(currentState.allStars, emptySet())
        _uiState.value = currentState.copy(
            selectedStarIds = emptySet(),
            starPositions = restPositions,
            recommendations = emptyList(),
            focusedItem = null,
            isLoading = false
        )
    }

    fun setFocusedItem(item: MediaItem) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
    }

    /**
     * Calcula las posiciones de congregación.
     * Al seleccionar ideas, la constelación se reduce y converge suavemente hacia el centro,
     * agrupando las ideas seleccionadas y sus derivadas directamente relacionadas.
     */
    private fun calculateCongregatingPositions(
        allStars: List<MedusaStar>,
        selectedStarIds: Set<String>
    ): Map<String, Pair<Float, Float>> {
        val starMap = allStars.associateBy { it.id }
        val result = mutableMapOf<String, Pair<Float, Float>>()

        if (selectedStarIds.isEmpty()) {
            for (star in allStars) {
                result[star.id] = Pair(star.baseNormX, star.baseNormY)
            }
            return result
        }

        val cx = 0.50f
        val cy = 0.28f

        val selectedList = selectedStarIds.mapNotNull { starMap[it] }
        val numSelected = selectedList.size

        val coreRadius = if (numSelected <= 1) 0.0f else (0.07f + numSelected * 0.015f).coerceAtMost(0.14f)
        selectedList.forEachIndexed { index, star ->
            val angle = -Math.PI.toFloat() / 2f + (2f * Math.PI.toFloat() * index) / numSelected
            val targetX = (cx + coreRadius * cos(angle)).coerceIn(0.12f, 0.88f)
            val targetY = (cy + coreRadius * sin(angle)).coerceIn(0.10f, 0.85f)
            result[star.id] = Pair(targetX, targetY)
        }

        val visibleStarIds = mutableSetOf<String>()
        visibleStarIds.addAll(selectedStarIds)
        for (id in selectedStarIds) {
            val star = starMap[id]
            if (star != null) {
                visibleStarIds.addAll(star.relatedStarIds)
            }
        }

        val unselectedVisible = visibleStarIds.filter { it !in selectedStarIds }.mapNotNull { starMap[it] }

        val orbitalRadius = 0.20f
        unselectedVisible.forEachIndexed { index, star ->
            val angleStep = (2f * Math.PI.toFloat()) / unselectedVisible.size.coerceAtLeast(1)
            val angle = -Math.PI.toFloat() / 2f + index * angleStep

            val targetX = (cx + orbitalRadius * cos(angle)).coerceIn(0.10f, 0.90f)
            val targetY = (cy + orbitalRadius * sin(angle)).coerceIn(0.08f, 0.88f)
            result[star.id] = Pair(targetX, targetY)
        }

        for (star in allStars) {
            if (star.id !in result) {
                result[star.id] = Pair(star.baseNormX, star.baseNormY)
            }
        }

        return result
    }

    private fun updateRecommendations() {
        val currentState = _uiState.value
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

            val activeStars = currentState.allStars.filter { it.id in currentState.selectedStarIds }

            val scored = allCandidates.mapNotNull { entity ->
                val entityGenresLower = (entity.genres ?: "").lowercase()
                val entityTitleLower = entity.title.lowercase()
                val overviewLower = (entity.overview ?: "").lowercase()
                val year = entity.productionYear

                var score = 0

                for (star in activeStars) {
                    if (star.minYear != null && star.maxYear != null && year != null) {
                        if (year in star.minYear..star.maxYear) {
                            score += 25
                        }
                    }

                    for (g in star.primaryGenres) {
                        val gl = g.lowercase()
                        if (entityGenresLower.contains(gl)) score += 18
                        if (entityTitleLower.contains(gl) || overviewLower.contains(gl)) score += 6
                    }

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

    /**
     * Construye la constelación mutante adaptada dinámicamente según:
     * 1. Las visualizaciones reales e historial reciente del usuario.
     * 2. Los éxitos y tendencias dominantes en la biblioteca.
     */
    private fun buildDynamicFullConstellation(
        topWatchedGenres: List<String>,
        topTrendGenres: List<String>
    ): List<MedusaStar> {
        val stars = mutableListOf<MedusaStar>()

        // Adaptar las estrellas principales a los géneros más vistos del usuario si existen
        val watched1 = topWatchedGenres.getOrNull(0) ?: "Drama"
        val watched2 = topWatchedGenres.getOrNull(1) ?: "Sci-Fi"
        val trend1 = topTrendGenres.getOrNull(0) ?: "Acción"

        // 1. CÚPULA SUPERIOR (Refleja visualizaciones y tendencias vivas)
        stars.add(
            MedusaStar(
                id = "WATCHED_1",
                label = watched1,
                baseNormX = 0.16f,
                baseNormY = 0.14f,
                textOnLeft = true,
                relatedStarIds = listOf("NOIR", "MELANCOLICO", "ANOS_70", "MISTERIO"),
                primaryGenres = listOf(watched1),
                secondaryKeywords = listOf("emoción", "favorito", "reciente")
            )
        )

        stars.add(
            MedusaStar(
                id = "WATCHED_2",
                label = watched2,
                baseNormX = 0.50f,
                baseNormY = 0.08f,
                textOnLeft = false,
                relatedStarIds = listOf("CYBERPUNK", "DISTOPIA", "FANTASIA", "INTENSO"),
                primaryGenres = listOf(watched2),
                secondaryKeywords = listOf("espacio", "futuro", "universo", "adrenalina")
            )
        )

        stars.add(
            MedusaStar(
                id = "TREND_1",
                label = trend1,
                baseNormX = 0.84f,
                baseNormY = 0.14f,
                textOnLeft = false,
                relatedStarIds = listOf("INTENSO", "AVENTURA", "ADRENALINA", "SUPERHEROES"),
                primaryGenres = listOf(trend1),
                secondaryKeywords = listOf("adrenalina", "combate", "misión", "éxito")
            )
        )

        stars.add(
            MedusaStar(
                id = "COMEDIA",
                label = "Comedia",
                baseNormX = 0.34f,
                baseNormY = 0.18f,
                textOnLeft = true,
                relatedStarIds = listOf("ROMANCE", "FAMILIAR"),
                primaryGenres = listOf("Comedia", "Comedy"),
                secondaryKeywords = listOf("humor", "risas", "divertido")
            )
        )

        stars.add(
            MedusaStar(
                id = "AVENTURA",
                label = "Aventura",
                baseNormX = 0.66f,
                baseNormY = 0.18f,
                textOnLeft = false,
                relatedStarIds = listOf("FANTASIA", "WATCHED_2", "SUPERHEROES"),
                primaryGenres = listOf("Aventura", "Adventure"),
                secondaryKeywords = listOf("viaje", "descubrimiento", "épica")
            )
        )

        // 2. CUERPO CENTRAL DE LA MEDUSA
        stars.add(
            MedusaStar(
                id = "NOIR",
                label = "Crimen Noir",
                baseNormX = 0.24f,
                baseNormY = 0.28f,
                textOnLeft = true,
                relatedStarIds = listOf("SUSPENSE", "ANOS_70", "WATCHED_1"),
                primaryGenres = listOf("Crimen", "Crime", "Misterio", "Mystery", "Thriller"),
                secondaryKeywords = listOf("noir", "detective", "policíaco", "investigación")
            )
        )

        stars.add(
            MedusaStar(
                id = "CYBERPUNK",
                label = "Cyberpunk",
                baseNormX = 0.76f,
                baseNormY = 0.28f,
                textOnLeft = false,
                relatedStarIds = listOf("WATCHED_2", "DISTOPIA", "NOIR"),
                primaryGenres = listOf("Ciencia ficción", "Sci-Fi", "Acción"),
                secondaryKeywords = listOf("cyberpunk", "futuro", "ia", "tecnología", "neon")
            )
        )

        stars.add(
            MedusaStar(
                id = "ESTRENOS_2020",
                label = "Éxitos 2020s",
                baseNormX = 0.38f,
                baseNormY = 0.32f,
                textOnLeft = true,
                relatedStarIds = listOf("WATCHED_2", "SUPERHEROES", "ADRENALINA"),
                primaryGenres = listOf("Acción", "Drama", "Sci-Fi"),
                secondaryKeywords = listOf("reciente", "actual", "estreno"),
                minYear = 2020,
                maxYear = 2026
            )
        )

        stars.add(
            MedusaStar(
                id = "FANTASIA",
                label = "Fantasía",
                baseNormX = 0.60f,
                baseNormY = 0.34f,
                textOnLeft = false,
                relatedStarIds = listOf("WATCHED_2", "AVENTURA"),
                primaryGenres = listOf("Fantasía", "Fantasy"),
                secondaryKeywords = listOf("magia", "criaturas", "reinos")
            )
        )

        stars.add(
            MedusaStar(
                id = "ANOS_70",
                label = "Cine 80s",
                baseNormX = 0.20f,
                baseNormY = 0.38f,
                textOnLeft = true,
                relatedStarIds = listOf("NOIR", "SUSPENSE", "WATCHED_1"),
                primaryGenres = listOf("Clásico", "Drama", "Crimen"),
                secondaryKeywords = listOf("70s", "80s", "retro", "clásico"),
                minYear = 1975,
                maxYear = 1989
            )
        )

        stars.add(
            MedusaStar(
                id = "MISTERIO",
                label = "Misterio",
                baseNormX = 0.48f,
                baseNormY = 0.42f,
                textOnLeft = true,
                relatedStarIds = listOf("SUSPENSE", "NOIR", "TERROR"),
                primaryGenres = listOf("Misterio", "Mystery", "Thriller"),
                secondaryKeywords = listOf("secreto", "enigma", "investigación")
            )
        )

        stars.add(
            MedusaStar(
                id = "FAMILIAR",
                label = "Familia",
                baseNormX = 0.78f,
                baseNormY = 0.42f,
                textOnLeft = false,
                relatedStarIds = listOf("AVENTURA", "COMEDIA"),
                primaryGenres = listOf("Animación", "Familia", "Family"),
                secondaryKeywords = listOf("niños", "infantil", "pixar")
            )
        )

        stars.add(
            MedusaStar(
                id = "ROMANCE",
                label = "Romance",
                baseNormX = 0.62f,
                baseNormY = 0.48f,
                textOnLeft = false,
                relatedStarIds = listOf("MELANCOLICO", "COMEDIA"),
                primaryGenres = listOf("Romance", "Romántica"),
                secondaryKeywords = listOf("amor", "pareja", "pasión")
            )
        )

        // 3. TENTÁCULOS INFERIORES
        stars.add(
            MedusaStar(
                id = "SUSPENSE",
                label = "Suspense",
                baseNormX = 0.42f,
                baseNormY = 0.54f,
                textOnLeft = true,
                relatedStarIds = listOf("INTENSO", "TERROR", "MISTERIO"),
                primaryGenres = listOf("Suspense", "Thriller"),
                secondaryKeywords = listOf("intriga", "tensión", "giro")
            )
        )

        stars.add(
            MedusaStar(
                id = "SUPERHEROES",
                label = "Superhéroes",
                baseNormX = 0.82f,
                baseNormY = 0.56f,
                textOnLeft = false,
                relatedStarIds = listOf("TREND_1", "AVENTURA"),
                primaryGenres = listOf("Acción", "Fantasía", "Ciencia ficción"),
                secondaryKeywords = listOf("superhéroes", "marvel", "dc", "cómic")
            )
        )

        stars.add(
            MedusaStar(
                id = "INTENSO",
                label = "Intenso",
                baseNormX = 0.58f,
                baseNormY = 0.58f,
                textOnLeft = false,
                relatedStarIds = listOf("ADRENALINA", "TERROR", "SUPERVIVENCIA"),
                primaryGenres = listOf("Acción", "Action", "Thriller"),
                secondaryKeywords = listOf("adrenalina", "impacto", "ritmo")
            )
        )

        stars.add(
            MedusaStar(
                id = "MELANCOLICO",
                label = "Melancolía",
                baseNormX = 0.28f,
                baseNormY = 0.62f,
                textOnLeft = true,
                relatedStarIds = listOf("ROMANCE", "WATCHED_1"),
                primaryGenres = listOf("Drama", "Romance"),
                secondaryKeywords = listOf("melancolía", "nostalgia", "autor")
            )
        )

        stars.add(
            MedusaStar(
                id = "TERROR",
                label = "Terror",
                baseNormX = 0.72f,
                baseNormY = 0.66f,
                textOnLeft = false,
                relatedStarIds = listOf("INTENSO", "SUSPENSE"),
                primaryGenres = listOf("Terror", "Horror"),
                secondaryKeywords = listOf("miedo", "pesadilla", "sobrenatural")
            )
        )

        stars.add(
            MedusaStar(
                id = "ADRENALINA",
                label = "Adrenalina",
                baseNormX = 0.52f,
                baseNormY = 0.72f,
                textOnLeft = true,
                relatedStarIds = listOf("INTENSO", "TREND_1"),
                primaryGenres = listOf("Acción", "Thriller"),
                secondaryKeywords = listOf("velocidad", "persecución", "extremo")
            )
        )

        stars.add(
            MedusaStar(
                id = "SUPERVIVENCIA",
                label = "Supervivencia",
                baseNormX = 0.36f,
                baseNormY = 0.76f,
                textOnLeft = true,
                relatedStarIds = listOf("INTENSO", "TERROR"),
                primaryGenres = listOf("Acción", "Thriller", "Aventura"),
                secondaryKeywords = listOf("supervivencia", "aislado", "peligro")
            )
        )

        return stars
    }
}
