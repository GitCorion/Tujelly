package com.example.tujelly.domain.usecase

import com.example.tujelly.data.local.UserPreferences
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.repository.MediaRepository
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
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

        // =========================================================================
        // PASO 0: GRANDES PRODUCCIONES DE TU SERVIDOR (Garantizado 20 ítems del servidor)
        // =========================================================================
        val topMoviesServer = mediaRepository.getTopRatedMoviesServer(
            serverUrl = prefs.jellyfinServerUrl,
            userId = prefs.jellyfinUserId,
            token = prefs.jellyfinAccessToken,
            limit = 20
        )
        val topSeriesServer = mediaRepository.getTopRatedSeriesServer(
            serverUrl = prefs.jellyfinServerUrl,
            userId = prefs.jellyfinUserId,
            token = prefs.jellyfinAccessToken,
            limit = 20
        )

        if (topMoviesServer.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Grandes Producciones de tu colección",
                    items = topMoviesServer.map {
                        it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.JELLYFIN)
                    },
                    badge = "DESTACADO"
                )
            )
        }

        if (topSeriesServer.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Series Destacadas",
                    items = topSeriesServer.map {
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
        // PASO 2: RECOMENDACIÓN PERSONALIZADA ("Porque viste [Título]")
        // =========================================================================
        if (prefs.tmdbApiKey.isNotBlank()) {
            val referenceItem = resumeItems.firstOrNull { it.tmdbId != null }
                ?: recentlyPlayedItems.firstOrNull { it.tmdbId != null }
                ?: topMoviesServer.firstOrNull { it.tmdbId != null }

            if (referenceItem != null) {
                try {
                    val tmdbIdLong = referenceItem.tmdbId?.toLongOrNull()
                    if (tmdbIdLong != null) {
                        val isTv = referenceItem.type.equals("Series", ignoreCase = true) ||
                                referenceItem.type.equals("Episode", ignoreCase = true)
                        val recsResult = mediaRepository.getTmdbRecommendations(
                            apiKey = prefs.tmdbApiKey,
                            tmdbId = tmdbIdLong,
                            isTv = isTv
                        ).getOrDefault(emptyList())

                        if (recsResult.isNotEmpty()) {
                            val matchedRecs = filterToLibraryUseCase.filterTmdbItems(
                                tmdbItems = recsResult.take(15),
                                serverUrl = prefs.jellyfinServerUrl,
                                userId = prefs.jellyfinUserId,
                                token = prefs.jellyfinAccessToken,
                                maxCandidates = 20
                            ).filter { it.id != referenceItem.id }

                            if (matchedRecs.isNotEmpty()) {
                                sections.add(
                                    HomeSection(
                                        title = "Porque viste ${referenceItem.title}",
                                        items = matchedRecs.map {
                                            it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_RECOMMENDATION)
                                        },
                                        badge = "RECOMENDADO"
                                    )
                                )
                                emit(sections.toList())
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // =====================================================================
            // PASO 3: TOP 10 DE HOY EN ESPAÑA / REGIÓN
            // =====================================================================
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
            // PASO 4: PLATAFORMAS OFICIALES DE STREAMING (CRUZADAS CON TU JELLYFIN)
            // =====================================================================

            // A. Populares en Netflix (Provider 8)
            try {
                val netflixResult = mediaRepository.getTmdbByProvider(
                    apiKey = prefs.tmdbApiKey,
                    providerId = "8",
                    region = prefs.watchRegion
                )
                val matchedNetflix = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = netflixResult.getOrDefault(emptyList()),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 50
                )
                if (matchedNetflix.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Populares en Netflix",
                            items = matchedNetflix.take(20).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "NETFLIX"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}

            // B. Éxitos de Disney+ (Provider 337)
            try {
                val disneyResult = mediaRepository.getTmdbByProvider(
                    apiKey = prefs.tmdbApiKey,
                    providerId = "337",
                    region = prefs.watchRegion
                )
                val matchedDisney = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = disneyResult.getOrDefault(emptyList()),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 50
                )
                if (matchedDisney.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Éxitos de Disney+",
                            items = matchedDisney.take(20).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "DISNEY+"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}

            // C. Destacados de Max (Provider 1899|384)
            try {
                val maxResult = mediaRepository.getTmdbByProvider(
                    apiKey = prefs.tmdbApiKey,
                    providerId = "1899|384",
                    region = prefs.watchRegion
                )
                val matchedMax = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = maxResult.getOrDefault(emptyList()),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 50
                )
                if (matchedMax.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Destacados de Max",
                            items = matchedMax.take(20).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "MAX"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}

            // D. Éxitos de Amazon Prime Video (Provider 119)
            try {
                val primeResult = mediaRepository.getTmdbByProvider(
                    apiKey = prefs.tmdbApiKey,
                    providerId = "119",
                    region = prefs.watchRegion
                )
                val matchedPrime = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = primeResult.getOrDefault(emptyList()),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 50
                )
                if (matchedPrime.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Éxitos de Prime Video",
                            items = matchedPrime.take(20).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "PRIME"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}

            // E. Aclamadas de Apple TV+ (Provider 350)
            try {
                val appleResult = mediaRepository.getTmdbByProvider(
                    apiKey = prefs.tmdbApiKey,
                    providerId = "350",
                    region = prefs.watchRegion
                )
                val matchedApple = filterToLibraryUseCase.filterTmdbItems(
                    tmdbItems = appleResult.getOrDefault(emptyList()),
                    serverUrl = prefs.jellyfinServerUrl,
                    userId = prefs.jellyfinUserId,
                    token = prefs.jellyfinAccessToken,
                    maxCandidates = 50
                )
                if (matchedApple.isNotEmpty()) {
                    sections.add(
                        HomeSection(
                            title = "Aclamadas de Apple TV+",
                            items = matchedApple.take(20).map {
                                it.toMediaItem(prefs.jellyfinServerUrl, prefs.jellyfinAccessToken, MediaSource.TMDB_TRENDING)
                            },
                            badge = "APPLE TV+"
                        )
                    )
                    emit(sections.toList())
                }
            } catch (_: Exception) {}
        }

        // =========================================================================
        // PASO 5: CINE RECOMENDADO
        // =========================================================================
        if (topMoviesServer.isNotEmpty()) {
            sections.add(
                HomeSection(
                    title = "Películas Recomendadas",
                    items = topMoviesServer.map {
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

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String, source: MediaSource): MediaItem {
        val authParam = if (token.isNotBlank()) "api_key=$token" else ""

        // If it's an episode belonging to a series, use the SERIES vertical poster!
        val effectivePosterId = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesId else id
        val effectivePosterTag = if (type.equals("Episode", ignoreCase = true) && !seriesId.isNullOrEmpty()) seriesPrimaryImageTag else primaryImageTag

        val tagParam = if (!effectivePosterTag.isNullOrEmpty()) "&tag=$effectivePosterTag" else ""
        val posterUrl = "$baseUrl/Items/$effectivePosterId/Images/Primary?$authParam$tagParam"

        val backdropTagParam = if (!backdropImageTag.isNullOrEmpty()) "&tag=$backdropImageTag" else ""
        val backdropUrl = "$baseUrl/Items/$id/Images/Backdrop/0?$authParam$backdropTagParam"

        val effectiveTitle = if (type.equals("Episode", ignoreCase = true) && !seriesName.isNullOrEmpty()) {
            val epCode = if (seasonNumber != null && episodeNumber != null) " (T${seasonNumber}:E${episodeNumber})" else ""
            "$seriesName$epCode"
        } else {
            title
        }

        return MediaItem(
            id = id,
            title = effectiveTitle,
            overview = overview,
            type = type,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = communityRating,
            year = productionYear,
            source = source,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite
        )
    }
}
