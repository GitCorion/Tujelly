package com.example.tujelly.ui.screens.genre

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.tmdb.TmdbApiService
import com.example.tujelly.data.local.ACCENT_CYAN
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import com.example.tujelly.domain.usecase.FilterToLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

typealias GenreFormat = com.example.tujelly.domain.model.MediaFormatFilter

sealed interface GenreUiState {
    object Loading : GenreUiState
    data class Success(
        val genreName: String,
        val sections: List<HomeSection>,
        val format: GenreFormat = GenreFormat.ALL,
        val focusedItem: MediaItem? = null
    ) : GenreUiState
    data class Error(val message: String) : GenreUiState
}

class GenreViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(application)
    private val mediaRepository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao())
    private val filterToLibraryUseCase = FilterToLibraryUseCase(mediaRepository)

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    val accentColor: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ACCENT_CYAN)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BUTTON_STYLE_ICONS_ONLY)

    private var formatSections: List<Pair<GenreFormat, HomeSection>> = emptyList()
    private var currentFormat: GenreFormat = GenreFormat.ALL
    private var currentGenre: String = ""

    fun loadGenreFeed(genreName: String) {
        viewModelScope.launch {
            _uiState.value = GenreUiState.Loading
            currentGenre = genreName
            currentFormat = GenreFormat.ALL

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val collected = mutableListOf<Pair<GenreFormat, HomeSection>>()
            val shownMediaIds = mutableSetOf<String>()

            // 1. Cargar ítems locales del género
            val localMovies = runCatching { mediaRepository.getMoviesByGenre(genreName, 150) }.getOrDefault(emptyList())
            val localSeries = runCatching { mediaRepository.getSeriesByGenre(genreName, 150) }.getOrDefault(emptyList())

            val localMovieItems = localMovies.map {
                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
            }
            val localSeriesItems = localSeries.map {
                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
            }

            // Sanitizar notas locales para evitar que 10.0 falsos sin votos lideren el Top 10
            val sanitizedLocalMovies = sanitizeRatings(localMovieItems)
            val sanitizedLocalSeries = sanitizeRatings(localSeriesItems)

            // 2. Descubrimiento TMDB (Obtiene producciones verdaderas aclamadas de este género)
            var tmdbMovieItems: List<MediaItem> = emptyList()
            var tmdbSeriesItems: List<MediaItem> = emptyList()

            if (prefs.tmdbApiKey.isNotBlank()) {
                try {
                    val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
                    val movieGenreId = tmdbMovieGenreId(genreName)
                    val tvGenreId = tmdbTvGenreId(genreName)

                    val movieGenre = runCatching {
                        api.discoverMoviesByGenre(apiKey = prefs.tmdbApiKey, genreId = movieGenreId)
                    }.getOrNull()?.results ?: emptyList()

                    val tvGenre = if (tvGenreId != null) {
                        runCatching {
                            api.discoverTvByGenre(apiKey = prefs.tmdbApiKey, genreId = tvGenreId)
                        }.getOrNull()?.results ?: emptyList()
                    } else emptyList()

                    val matchedMovies = filterToLibraryUseCase.filterTmdbItems(
                        tmdbItems = movieGenre.take(40),
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        maxCandidates = 40
                    )
                    tmdbMovieItems = matchedMovies.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                    }

                    val matchedSeries = filterToLibraryUseCase.filterTmdbItems(
                        tmdbItems = tvGenre.take(40),
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        maxCandidates = 40
                    )
                    tmdbSeriesItems = matchedSeries.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                    }
                } catch (_: Exception) {}
            }

            // =========================================================================
            // CURACIÓN INTELIGENTE DE SECCIONES (Sin duplicados ni 10.0 inflados)
            // =========================================================================

            // A) TOP 10 IMPRESCINDIBLES (Prioriza producciones aclamadas por TMDB + locales verdaderas)
            val curatedTopCandidates = (tmdbMovieItems + tmdbSeriesItems + sanitizedLocalMovies + sanitizedLocalSeries)
                .distinctBy { it.id }
                .filter { (it.rating ?: 0f) >= 6.8f }
                .sortedByDescending { item ->
                    val isTmdb = item.source == MediaSource.TMDB_TRENDING
                    val baseRating = item.rating ?: 0f
                    // Ponderar: Las producciones globales de TMDB y locales verificadas lideran la clasificación
                    if (isTmdb) baseRating * 10f + 5f else baseRating * 10f
                }
                .take(10)

            if (curatedTopCandidates.isNotEmpty()) {
                shownMediaIds.addAll(curatedTopCandidates.map { it.id })
                collected.add(
                    GenreFormat.ALL to HomeSection(
                        title = "Top 10 Imprescindibles",
                        items = curatedTopCandidates,
                        badge = null,
                        isRanked = true
                    )
                )
            }

            // B) JOYAS OCULTAS EN $genreName (Solo títulos NO mostrados en Top 10, no vistos y con buena nota)
            val allLocalSanitized = (sanitizedLocalMovies + sanitizedLocalSeries).distinctBy { it.id }
            val hiddenGems = allLocalSanitized
                .filter { it.id !in shownMediaIds && !it.isPlayed && (it.rating ?: 0f) in 7.0f..9.5f }
                .sortedByDescending { it.rating ?: 0f }
                .take(15)

            if (hiddenGems.isNotEmpty()) {
                shownMediaIds.addAll(hiddenGems.map { it.id })
                collected.add(
                    GenreFormat.ALL to HomeSection(
                        title = "Joyas Ocultas",
                        items = hiddenGems,
                        badge = null
                    )
                )
            }

            // C) PELÍCULAS DE $genreName (Sin repetir las del Top 10)
            val curatedMovies = (sanitizedLocalMovies + tmdbMovieItems)
                .distinctBy { it.id }
                .filter { it.id !in shownMediaIds }
                .take(20)

            if (curatedMovies.isNotEmpty()) {
                shownMediaIds.addAll(curatedMovies.map { it.id })
                collected.add(
                    GenreFormat.MOVIES to HomeSection(
                        title = "Películas",
                        items = curatedMovies,
                        badge = null
                    )
                )
            }

            // D) SERIES DE $genreName (Sin repetir las del Top 10)
            val curatedSeries = (sanitizedLocalSeries + tmdbSeriesItems)
                .distinctBy { it.id }
                .filter { it.id !in shownMediaIds }
                .take(20)

            if (curatedSeries.isNotEmpty()) {
                shownMediaIds.addAll(curatedSeries.map { it.id })
                collected.add(
                    GenreFormat.SERIES to HomeSection(
                        title = "Series",
                        items = curatedSeries,
                        badge = null
                    )
                )
            }

            // E) NOVEDADES RECIENTES (Sin repetir lo ya mostrado)
            val recentMovies = runCatching { mediaRepository.getRecentMoviesByGenre(genreName, 15) }
                .getOrDefault(emptyList())
                .map { it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN) }

            val recentSeries = runCatching { mediaRepository.getRecentSeriesByGenre(genreName, 15) }
                .getOrDefault(emptyList())
                .map { it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN) }

            val recentCombined = (recentMovies + recentSeries)
                .distinctBy { it.id }
                .filter { it.id !in shownMediaIds }
                .sortedByDescending { it.year ?: 0 }
                .take(15)

            if (recentCombined.isNotEmpty()) {
                collected.add(
                    GenreFormat.ALL to HomeSection(
                        title = "Novedades Recientes",
                        items = recentCombined,
                        badge = null
                    )
                )
            }

            formatSections = collected
            applyFormat()
        }
    }

    fun setFormat(format: GenreFormat) {
        if (currentFormat == format) return
        currentFormat = format
        applyFormat()
    }

    private fun applyFormat() {
        if (formatSections.isEmpty()) {
            _uiState.value = GenreUiState.Error("No se encontraron contenidos para el género '$currentGenre'.")
            return
        }

        val filtered = when (currentFormat) {
            GenreFormat.ALL -> formatSections.map { it.second }
            GenreFormat.MOVIES -> formatSections.mapNotNull { (format, section) ->
                if (format == GenreFormat.SERIES) return@mapNotNull null
                val movieItems = section.items.filter { it.type.equals("Movie", ignoreCase = true) }
                if (movieItems.isNotEmpty()) section.copy(items = movieItems) else null
            }
            GenreFormat.SERIES -> formatSections.mapNotNull { (format, section) ->
                if (format == GenreFormat.MOVIES) return@mapNotNull null
                val seriesItems = section.items.filter {
                    it.type.equals("Series", ignoreCase = true) || it.type.equals("Episode", ignoreCase = true)
                }
                if (seriesItems.isNotEmpty()) section.copy(items = seriesItems) else null
            }
        }

        val firstItem = filtered.firstOrNull()?.items?.firstOrNull()
        _uiState.value = GenreUiState.Success(
            genreName = currentGenre,
            sections = filtered,
            format = currentFormat,
            focusedItem = firstItem
        )
    }

    fun setFocusedItem(item: MediaItem) {
        val currentState = _uiState.value
        if (currentState is GenreUiState.Success) {
            _uiState.value = currentState.copy(focusedItem = item)
        }
    }

    /**
     * Sanitiza las notas de las películas/series locales.
     * Si un título tiene una nota llana de 10.0 (típico de un único voto en Jellyfin),
     * ajusta su nota efectiva a 7.2f a menos que esté respaldado por una gran producción o TMDB.
     */
    private fun sanitizeRatings(items: List<MediaItem>): List<MediaItem> {
        return items.map { item ->
            val raw = item.rating ?: 0f
            if (raw >= 9.8f) {
                // Ajustar notas artificiales de 10.0 sin volumen de votos a 7.2f
                item.copy(rating = 7.2f)
            } else {
                item
            }
        }
    }

    private fun tmdbMovieGenreId(genreName: String): String = when (genreName.lowercase()) {
        "acción", "accion" -> "28"
        "aventura" -> "12"
        "animación", "animacion" -> "16"
        "comedia" -> "35"
        "crimen" -> "80"
        "documental" -> "99"
        "drama" -> "18"
        "familia", "familiar" -> "10751"
        "fantasía", "fantasia" -> "14"
        "historia" -> "36"
        "terror" -> "27"
        "música", "musica" -> "10402"
        "misterio" -> "9648"
        "romance" -> "10749"
        "ciencia ficción", "ciencia ficcion" -> "878"
        "thriller", "suspense" -> "53"
        "bélica", "belica" -> "10752"
        "western" -> "37"
        else -> "28"
    }

    private fun tmdbTvGenreId(genreName: String): String? = when (genreName.lowercase()) {
        "acción", "accion", "aventura" -> "10759"
        "animación", "animacion" -> "16"
        "comedia" -> "35"
        "crimen" -> "80"
        "documental" -> "99"
        "drama" -> "18"
        "familia", "familiar" -> "10751"
        "misterio" -> "9648"
        "ciencia ficción", "ciencia ficcion", "fantasía", "fantasia" -> "10765"
        "bélica", "belica" -> "10768"
        "western" -> "37"
        "infantil", "kids" -> "10762"
        else -> null
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "api_key=$token" else ""

        val posterUrl = when {
            primaryImageTag?.startsWith("tmdb:") == true -> {
                "https://image.tmdb.org/t/p/w500${primaryImageTag.removePrefix("tmdb:")}"
            }
            !primaryImageTag.isNullOrEmpty() -> {
                val tagParam = "&tag=$primaryImageTag"
                "$baseUrl/Items/$id/Images/Primary?$authParam$tagParam"
            }
            else -> {
                "$baseUrl/Items/$id/Images/Primary?$authParam"
            }
        }

        val backdropUrl = when {
            backdropImageTag?.startsWith("tmdb:") == true -> {
                "https://image.tmdb.org/t/p/w1280${backdropImageTag.removePrefix("tmdb:")}"
            }
            !backdropImageTag.isNullOrEmpty() -> {
                val backdropTagParam = "&tag=$backdropImageTag"
                "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"
            }
            else -> {
                "$baseUrl/Items/$id/Images/Backdrop/0?$authParam"
            }
        }

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
            source = source,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
