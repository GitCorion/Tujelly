package com.example.tujelly.domain.usecase

import com.example.tujelly.data.local.UserPreferences
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetHomeFeedUseCase(
    private val mediaRepository: MediaRepository,
    private val filterToLibraryUseCase: FilterToLibraryUseCase
) {
    operator fun invoke(prefs: UserPreferences): Flow<List<HomeSection>> = flow {
        if (prefs.jellyfinServerUrl.isBlank() || prefs.jellyfinUserId.isBlank() || prefs.jellyfinAccessToken.isBlank()) {
            emit(emptyList())
            return@flow
        }

        // Quick init: if local DB is empty (< 10 items), populate latest items & favorites in 0.5s
        // Never blocks the feed with a full 36,000 item catalog sync!
        runCatching {
            if (mediaRepository.getLocalCount() < 10 && !MediaRepository.syncProgress.value.isSyncing) {
                mediaRepository.syncQuickInit(
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken
                )
            }
        }

        val sections = mutableListOf<HomeSection>()
        val shownMediaIds = mutableSetOf<String>()

        // =========================================================================
        // PASO 0: GRANDES PRODUCCIONES DE TU SERVIDOR (Selección de alta calidad)
        // =========================================================================
        val topMoviesServer = mediaRepository.getTopRatedMoviesServer(
            serverUrl = prefs.jellyfinServerUrl,
            userId = prefs.jellyfinUserId,
            token = prefs.jellyfinAccessToken,
            limit = 100
        )
        val topSeriesServer = mediaRepository.getTopRatedSeriesServer(
            serverUrl = prefs.jellyfinServerUrl,
            userId = prefs.jellyfinUserId,
            token = prefs.jellyfinAccessToken,
            limit = 100
        )

        // 1. Cruzar las producciones más aclamadas globalmente de TMDB con la biblioteca local
        val tmdbTopRated = if (prefs.tmdbApiKey.isNotBlank()) {
            runCatching {
                val topRated = mediaRepository.getTmdbTopRated(prefs.tmdbApiKey).getOrDefault(emptyList())
                filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = topRated,
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 80
                )
            }.getOrDefault(emptyList())
        } else emptyList()

        val tmdbBlockbusters = tmdbTopRated.filter { it.type.equals("Movie", ignoreCase = true) }
        val tmdbTopSeries = tmdbTopRated.filter { it.type.equals("Series", ignoreCase = true) }

        // 2. Filtrar películas con masa crítica de votos para evitar fakes de 10 estrellas sin votos
        val curatedServerMovies = if (prefs.tmdbApiKey.isNotBlank()) {
            filterHighQualityMovies(topMoviesServer, prefs.tmdbApiKey)
        } else {
            topMoviesServer.filter {
                val r = it.communityRating ?: 0f
                r in 7.0f..9.5f && !it.backdropImageTag.isNullOrEmpty()
            }
        }

        // Fusión inteligente: Priorizar verdaderas grandes producciones y completar sin duplicados
        val curatedTopMovies = (tmdbBlockbusters + curatedServerMovies)
            .distinctBy { it.id }
            .take(20)

        // 3. Filtrar series con masa crítica de votos para evitar adulteraciones de 10 estrellas de 1 solo voto
        val curatedServerSeries = if (prefs.tmdbApiKey.isNotBlank()) {
            filterHighQualitySeries(topSeriesServer, prefs.tmdbApiKey)
        } else {
            topSeriesServer.filter {
                val r = it.communityRating ?: 0f
                r in 7.0f..9.5f && !it.backdropImageTag.isNullOrEmpty()
            }
        }

        // Fusión inteligente de Series: Priorizar series de prestigio contrastado mundialmente y completar sin duplicados
        val curatedTopSeries = (tmdbTopSeries + curatedServerSeries)
            .distinctBy { it.id }
            .take(20)

        if (curatedTopMovies.isNotEmpty()) {
            shownMediaIds.addAll(curatedTopMovies.map { it.id })
            sections.add(
                HomeSection(
                    title = "Grandes Producciones de tu colección",
                    items = curatedTopMovies.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                    },
                    badge = "DESTACADO"
                )
            )
        }

        if (curatedTopSeries.isNotEmpty()) {
            shownMediaIds.addAll(curatedTopSeries.map { it.id })
            sections.add(
                HomeSection(
                    title = "Series Destacadas",
                    items = curatedTopSeries.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                    },
                    badge = "SERIES"
                )
            )
        }

        if (sections.isNotEmpty()) {
            emit(sections.toList())
        }

        // =========================================================================
        // PASO 1: CONTINUAR VIENDO (Si hay contenido en progreso)
        // =========================================================================
        val resumeItems = runCatching {
            mediaRepository.getContinueWatching(
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken
            ).getOrNull() ?: emptyList()
        }.getOrDefault(emptyList())

        if (resumeItems.isNotEmpty()) {
            shownMediaIds.addAll(resumeItems.map { it.id })
            val continueSection = HomeSection(
                title = "Continuar Viendo",
                items = resumeItems.take(15).map {
                    it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                },
                badge = "EN CURSO"
            )
            sections.removeAll { it.badge == "EN CURSO" }
            sections.add(0, continueSection)
            emit(sections.toList())
        }

        val recentlyPlayedItems = runCatching {
            mediaRepository.getRecentlyPlayed(
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken
            ).getOrNull() ?: emptyList()
        }.getOrDefault(emptyList())

        // =========================================================================
        // PASO 1.5: AÑADIDO RECIENTEMENTE (Novedades de tu servidor)
        // =========================================================================
        val latestItems = runCatching {
            mediaRepository.getLatest(
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken
            ).getOrNull() ?: emptyList()
        }.getOrDefault(emptyList()).filter {
            it.type.equals("Movie", ignoreCase = true) || it.type.equals("Series", ignoreCase = true)
        }

        if (latestItems.isNotEmpty()) {
            shownMediaIds.addAll(latestItems.map { it.id })
            val latestSection = HomeSection(
                title = "Añadido recientemente",
                items = latestItems.take(20).map {
                    it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                },
                badge = "NOVEDADES"
            )
            val insertIdx = if (sections.any { it.badge == "EN CURSO" }) 1 else 0
            sections.add(insertIdx, latestSection)
            emit(sections.toList())
        }

        // =========================================================================
        // PASO 2: RECOMENDACIÓN PERSONALIZADA ("Porque viste [Título]" o "Recomendados de [Género]")
        // =========================================================================
        val watchedReferenceItem = resumeItems.firstOrNull { it.tmdbId != null }
            ?: recentlyPlayedItems.firstOrNull { it.tmdbId != null }

        var recommendationAdded = false

        if (watchedReferenceItem != null && prefs.tmdbApiKey.isNotBlank()) {
            try {
                val tmdbIdLong = watchedReferenceItem.tmdbId?.toLongOrNull()
                if (tmdbIdLong != null) {
                    val isTv = watchedReferenceItem.type.equals("Series", ignoreCase = true) ||
                            watchedReferenceItem.type.equals("Episode", ignoreCase = true)
                    val recsResult = mediaRepository.getTmdbRecommendations(
                        apiKey = prefs.tmdbApiKey,
                        tmdbId = tmdbIdLong,
                        isTv = isTv
                    ).getOrDefault(emptyList())

                    if (recsResult.isNotEmpty()) {
                        val refGenres = watchedReferenceItem.genres?.split(",", ";")
                            ?.map { it.trim().lowercase() }
                            ?.filter { it.isNotBlank() } ?: emptyList()

                        val matchedRecs = filterToLibraryUseCase.filterTmdbItems(
                            tmdbItems = recsResult,
                            serverUrl = prefs.jellyfinServerUrl,
                            userId = prefs.jellyfinUserId,
                            token = prefs.jellyfinAccessToken,
                            maxCandidates = 30
                        ).filter { candidate ->
                            candidate.id != watchedReferenceItem.id &&
                                    candidate.id !in shownMediaIds &&
                                    (refGenres.isEmpty() || candidate.genres.orEmpty().split(",", ";").any { g -> g.trim().lowercase() in refGenres })
                        }.take(15)

                        // Si tras filtrar por género no hay suficientes, relajar el filtro de género pero mantener no mostrados
                        val finalRecs = if (matchedRecs.size >= 3) matchedRecs else {
                            filterToLibraryUseCase.filterTmdbItems(
                                tmdbItems = recsResult,
                                serverUrl = prefs.jellyfinServerUrl,
                                userId = prefs.jellyfinUserId,
                                token = prefs.jellyfinAccessToken,
                                maxCandidates = 30
                            ).filter { candidate ->
                                candidate.id != watchedReferenceItem.id && candidate.id !in shownMediaIds
                            }.take(15)
                        }

                        if (finalRecs.size >= 3) {
                            shownMediaIds.addAll(finalRecs.map { it.id })
                            sections.add(
                                HomeSection(
                                    title = "Porque viste ${watchedReferenceItem.title}",
                                    items = finalRecs.map {
                                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_RECOMMENDATION)
                                    },
                                    badge = "RECOMENDADO"
                                )
                            )
                            emit(sections.toList())
                            recommendationAdded = true
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback inteligente: si el usuario no ha visto nada aún o no hay suficientes recomendaciones de "Porque viste",
        // recomendar por su género favorito/más presente en la biblioteca
        if (!recommendationAdded) {
            try {
                val topGenre = mediaRepository.getMostWatchedGenres().firstOrNull() ?: "Acción"
                val genreItems = mediaRepository.getItemsByGenre(topGenre)
                    .filter { it.id !in shownMediaIds && (!it.backdropImageTag.isNullOrEmpty() || !it.overview.isNullOrBlank()) }
                    .take(15)

                if (genreItems.size >= 3) {
                    shownMediaIds.addAll(genreItems.map { it.id })
                    sections.add(
                        HomeSection(
                            title = "Recomendados de $topGenre",
                            items = genreItems.map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                            },
                            badge = "RECOMENDADO"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}
        }

        // =====================================================================
        // PASO 3: TOP 10 DE HOY EN ESPAÑA / REGIÓN
        // =====================================================================
        if (prefs.tmdbApiKey.isNotBlank()) {
            try {
                val dailyTrending = mediaRepository.getTmdbTrendingDay(apiKey = prefs.tmdbApiKey)
                    .getOrDefault(emptyList())
                if (dailyTrending.isNotEmpty()) {
                    val matchedDaily = filterToLibraryUseCase.filterTmdbItems(
                        tmdbItems = dailyTrending,
                        serverUrl = prefs.jellyfinServerUrl,
                        userId = prefs.jellyfinUserId,
                        token = prefs.jellyfinAccessToken,
                        maxCandidates = 50
                    )
                    if (matchedDaily.isNotEmpty()) {
                        shownMediaIds.addAll(matchedDaily.map { it.id })
                        val top10Section = HomeSection(
                            title = "Top 10 en España hoy",
                            items = matchedDaily.take(10).mapIndexed { idx, it ->
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING).copy(rank = idx + 1)
                            },
                            badge = "TOP 10",
                            isRanked = true
                        )
                        val insertIdx = if (sections.any { it.badge == "EN CURSO" }) 1 else 0
                        sections.add(insertIdx, top10Section)
                        emit(sections.toList())
                    }
                }
            } catch (_: Exception) {}

            // =====================================================================
            // PASO 4: PLATAFORMAS OFICIALES DE STREAMING (PARALELIZADO & CRUZADO CON TU JELLYFIN)
            // =====================================================================
            val selected = prefs.selectedPlatforms
            val activeProviders = if (selected.isEmpty()) {
                com.example.tujelly.data.model.SUPPORTED_PLATFORMS
            } else {
                com.example.tujelly.data.model.SUPPORTED_PLATFORMS.filter { it.id in selected }
            }

            try {
                coroutineScope {
                    val providerDeferreds = activeProviders.map { platform ->
                        async {
                            val result = mediaRepository.getTmdbByProvider(
                                apiKey = prefs.tmdbApiKey,
                                providerId = platform.providerId,
                                region = prefs.watchRegion
                            )
                            val matched = filterToLibraryUseCase.filterTmdbItems(
                                tmdbItems = result.getOrDefault(emptyList()),
                                serverUrl = prefs.jellyfinServerUrl,
                                userId = prefs.jellyfinUserId,
                                token = prefs.jellyfinAccessToken,
                                maxCandidates = 50
                            )
                            if (matched.isNotEmpty()) {
                                platform to matched
                            } else null
                        }
                    }

                    val providerResults = providerDeferreds.awaitAll().filterNotNull()
                    var addedAny = false
                    for ((platform, matched) in providerResults) {
                        val uniqueMatched = matched.filter { it.id !in shownMediaIds }
                        if (uniqueMatched.isNotEmpty()) {
                            shownMediaIds.addAll(uniqueMatched.map { it.id })
                            sections.add(
                                HomeSection(
                                    title = "Populares en ${platform.name}",
                                    items = uniqueMatched.take(20).map {
                                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                                    },
                                    badge = platform.name
                                )
                            )
                            addedAny = true
                        }
                    }
                    if (addedAny) {
                        emit(sections.toList())
                    }
                }
            } catch (_: Exception) {}
        }

        // =========================================================================
        // PASO 5: PELÍCULAS RECOMENDADAS (Basado en géneros y variedad excluida de Grandes Producciones)
        // =========================================================================
        val recommendedMovies = mediaRepository.getRecommendedMovies(
            excludeIds = shownMediaIds,
            limit = 20
        )

        val effectiveRecommended = if (recommendedMovies.isNotEmpty()) {
            recommendedMovies
        } else {
            topMoviesServer.filter { it.id !in shownMediaIds }
        }

        if (effectiveRecommended.isNotEmpty()) {
            shownMediaIds.addAll(effectiveRecommended.map { it.id })
            sections.add(
                HomeSection(
                    title = "Películas Recomendadas",
                    items = effectiveRecommended.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                    },
                    badge = "PELÍCULAS"
                )
            )
            emit(sections.toList())
        }

        // =========================================================================
        // PASO 6: TRAKT.TV (Recomendaciones, Watchlist y Tendencias)
        // =========================================================================
        val traktClientId = prefs.traktClientId.ifBlank { com.example.tujelly.data.local.DEFAULT_TRAKT_CLIENT_ID }
        if (prefs.traktAccessToken.isNotBlank()) {
            // 1. Recommendations
            try {
                val traktRecsResult = mediaRepository.getTraktRecommendations(
                    traktToken = prefs.traktAccessToken,
                    clientId = traktClientId
                )
                val rawTraktRecs = traktRecsResult.getOrDefault(emptyList()).take(20)
                val matchedTraktRecs = filterToLibraryUseCase.filterTraktItems(
                    traktItems = rawTraktRecs,
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 20
                )
                if (matchedTraktRecs.isNotEmpty()) {
                    shownMediaIds.addAll(matchedTraktRecs.map { it.id })
                    sections.add(
                        HomeSection(
                            title = "Recomendado por la comunidad Trakt",
                            items = matchedTraktRecs.map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TRAKT_RECOMMENDATION)
                            },
                            badge = "TRAKT"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}

            // 2. Watchlist
            try {
                val watchlistResult = mediaRepository.getTraktWatchlist(
                    traktToken = prefs.traktAccessToken,
                    clientId = traktClientId
                )
                val rawWatchlist = watchlistResult.getOrDefault(emptyList()).take(20)
                val matchedWatchlist = filterToLibraryUseCase.filterTraktItems(
                    traktItems = rawWatchlist,
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 20
                )
                if (matchedWatchlist.isNotEmpty()) {
                    shownMediaIds.addAll(matchedWatchlist.map { it.id })
                    sections.add(
                        HomeSection(
                            title = "Tu Watchlist de Trakt.tv",
                            items = matchedWatchlist.map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TRAKT_WATCHLIST)
                            },
                            badge = "TRAKT"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}
        }

        // 3. Trending Trakt
        try {
            val trendingResult = mediaRepository.getTraktTrending(clientId = traktClientId)
            val rawTrending = trendingResult.getOrDefault(emptyList()).take(20)
            val matchedTrending = filterToLibraryUseCase.filterTraktItems(
                traktItems = rawTrending,
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken,
                maxCandidates = 20
            )
            if (matchedTrending.isNotEmpty()) {
                shownMediaIds.addAll(matchedTrending.map { it.id })
                sections.add(
                    HomeSection(
                        title = "Tendencias en Trakt.tv",
                        items = matchedTrending.map {
                            it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TRAKT_RECOMMENDATION)
                        },
                        badge = "TRAKT"
                    )
                )
                emit(sections.toList())
            }
        } catch (_: Exception) {}

        // Fallback final
        if (sections.isEmpty()) {
            emit(emptyList())
        }
    }

    /**
     * Filtra las películas del servidor contra TMDB para descartar títulos con nota
     * alta pero pocos votos (ej. película de nicho con 10 votos familiares y nota 10).
     * Pondera la nota junto con el volumen de votos para dar prioridad a verdaderas producciones.
     */
    private suspend fun filterHighQualityMovies(
        movies: List<JellyfinMediaEntity>,
        tmdbApiKey: String
    ): List<JellyfinMediaEntity> {
        val validMovies = movies.filter { !it.backdropImageTag.isNullOrEmpty() || !it.overview.isNullOrBlank() }
        val candidates = validMovies.filter { it.tmdbId != null }

        if (candidates.isEmpty()) {
            return validMovies.filter {
                val r = it.communityRating ?: 0f
                r in 7.0f..9.5f
            }.take(20)
        }

        val checked = candidates.mapNotNull { movie ->
            val tmdbId = movie.tmdbId?.toLongOrNull() ?: return@mapNotNull null
            val stats = mediaRepository.getTmdbVoteStats(tmdbApiKey, tmdbId, isTv = false)
                ?: return@mapNotNull null
            Triple(movie.copy(communityRating = stats.voteAverage), stats.voteCount, stats.voteAverage)
        }

        val passed = checked
            .filter { (_, count, avg) -> count >= 150 && avg >= 6.8f }
            .sortedByDescending { (_, count, avg) -> avg * 10f + kotlin.math.min(count, 5000) / 500f }
            .map { it.first }

        return if (passed.isNotEmpty()) passed else validMovies.filter {
            val r = it.communityRating ?: 0f
            r in 7.0f..9.5f
        }.take(20)
    }

    /**
     * Filtra las series del servidor contra TMDB para descartar títulos con nota
     * adulterada o inflada por poquísimos votos (ej. documentales o telenovelas con 1 voto de 10).
     * Exige una masa crítica de votos (mínimo 50 votos en TMDB y nota >= 6.8),
     * pondera por volumen de votos para dar prioridad a series de prestigio contrastado,
     * y reemplaza la nota local adulterada por la nota real ponderada de TMDB.
     */
    private suspend fun filterHighQualitySeries(
        series: List<JellyfinMediaEntity>,
        tmdbApiKey: String
    ): List<JellyfinMediaEntity> {
        val validSeries = series.filter { !it.backdropImageTag.isNullOrEmpty() || !it.overview.isNullOrBlank() }
        val candidates = validSeries.filter { it.tmdbId != null }

        if (candidates.isEmpty()) {
            return validSeries.filter {
                val r = it.communityRating ?: 0f
                r in 7.0f..9.5f
            }.take(20)
        }

        val checked = candidates.mapNotNull { s ->
            val tmdbId = s.tmdbId?.toLongOrNull() ?: return@mapNotNull null
            val stats = mediaRepository.getTmdbVoteStats(tmdbApiKey, tmdbId, isTv = true)
                ?: return@mapNotNull null
            Triple(s.copy(communityRating = stats.voteAverage), stats.voteCount, stats.voteAverage)
        }

        val passed = checked
            .filter { (_, count, avg) -> count >= 50 && avg >= 6.8f }
            .sortedByDescending { (_, count, avg) -> avg * 10f + kotlin.math.min(count, 5000) / 500f }
            .map { it.first }

        return if (passed.isNotEmpty()) passed else validSeries.filter {
            val r = it.communityRating ?: 0f
            r in 7.0f..9.5f
        }.take(20)
    }

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "api_key=$token" else ""

        // If it's an episode belonging to a series, use the SERIES vertical poster!
        val effectivePosterId = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesId else id
        val effectivePosterTag = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesPrimaryImageTag else primaryImageTag

        val tagParam = if (!effectivePosterTag.isNullOrEmpty()) "&tag=$effectivePosterTag" else ""
        val posterUrl = "$baseUrl/Items/$effectivePosterId/Images/Primary?$authParam$tagParam"

        val backdropTagParam = if (!backdropImageTag.isNullOrEmpty()) "&tag=$backdropImageTag" else ""
        val backdropUrl = "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"

        val effectiveLogoId = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesId else id
        val logoUrl = if (baseUrl.isNotBlank()) "$baseUrl/Items/$effectiveLogoId/Images/Logo?$authParam" else null

        val isContinueWatching = source == MediaSource.JELLYFIN && playbackPositionTicks > 0
        val effectiveTitle = if (type.equals("Episode", ignoreCase = true) && !seriesName.isNullOrEmpty()) {
            if (isContinueWatching) {
                val epCode = if (seasonNumber != null && episodeNumber != null) " (T${seasonNumber}:E${episodeNumber})" else ""
                "$seriesName$epCode"
            } else {
                seriesName
            }
        } else {
            title
        }

        val effectiveType = if (type.equals("Episode", ignoreCase = true) && source != MediaSource.JELLYFIN) "Series" else type

        val total = totalItemCount
        val unplayed = unplayedItemCount
        val played = if (total != null && unplayed != null) (total - unplayed).coerceAtLeast(0) else null

        val effectivePlayed = if (effectiveType.equals("Series", ignoreCase = true)) {
            if (unplayed != null) unplayed == 0 && (total ?: 0) > 0 else isPlayed
        } else if (type.equals("Episode", ignoreCase = true) && source != MediaSource.JELLYFIN) {
            false
        } else {
            isPlayed
        }

        val effectiveRating = when {
            communityRating == null -> null
            communityRating > 9.5f -> null // Descartar notas infladas de un único voto (ej: 10.0, 9.7) no ponderadas
            communityRating <= 0f -> null
            else -> communityRating
        }

        return MediaItem(
            id = id,
            title = effectiveTitle,
            overview = overview,
            type = effectiveType,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            logoUrl = logoUrl,
            rating = effectiveRating,
            year = productionYear,
            source = source,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = effectivePlayed,
            isFavorite = isFavorite,
            totalEpisodes = total,
            playedEpisodes = played
        )
    }
}
