package com.example.tujelly.data.repository

import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.local.db.JellyfinDao
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.jellyfin.JellyfinApiService
import com.example.tujelly.data.remote.jellyfin.JellyfinItemDto
import com.example.tujelly.data.remote.jellyfin.JellyfinItemsResponse
import com.example.tujelly.data.remote.tmdb.TmdbApiService
import com.example.tujelly.data.remote.tmdb.TmdbItemDto
import com.example.tujelly.data.remote.trakt.TraktApiService
import com.example.tujelly.data.remote.trakt.TraktMediaDto
import com.example.tujelly.domain.model.EpisodeItem
import com.example.tujelly.domain.model.SeasonItem
import com.example.tujelly.domain.model.SeriesStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class SyncProgress(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val message: String = ""
)

class MediaRepository(
    private val jellyfinDao: JellyfinDao,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {
    companion object {
        private val _syncProgress = MutableStateFlow(SyncProgress())
        val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
        private val syncMutex = Mutex()
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun startBackgroundSync(
        serverUrl: String,
        userId: String,
        token: String,
        lastSyncTimestamp: String? = null,
        forceFullSync: Boolean = false,
        onSyncCompleted: ((String) -> Unit)? = null
    ) {
        if (serverUrl.isBlank() || userId.isBlank() || token.isBlank()) return
        if (_syncProgress.value.isSyncing || syncMutex.isLocked) return
        syncScope.launch {
            syncJellyfinLibrary(
                serverUrl = serverUrl,
                userId = userId,
                token = token,
                lastSyncTimestamp = lastSyncTimestamp,
                forceFullSync = forceFullSync,
                onSyncCompleted = { newTs ->
                    onSyncCompleted?.invoke(newTs)
                }
            )
        }
    }
    private fun buildJellyfinAuthHeader(clientName: String = "Tujelly", deviceId: String = "AndroidTV", version: String = "1.0.0", token: String = ""): String {
        return "MediaBrowser Client=\"$clientName\", Device=\"AndroidTV\", DeviceId=\"$deviceId\", Version=\"$version\"" +
                if (token.isNotEmpty()) ", Token=\"$token\"" else ""
    }

    suspend fun searchAndCache(serverUrl: String, userId: String, token: String, title: String, year: Int? = null, tmdbId: String? = null, imdbId: String? = null): JellyfinMediaEntity? {
        // 1. Check local Room database first
        val local = findLocal(tmdbId = tmdbId, imdbId = imdbId, title = title, year = year)
        if (local != null) return local

        // 2. Query remote Jellyfin with fast indexed search
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val cleanTitle = title.trim()
                .substringBefore(":")
                .substringBefore("-")
                .trim()
            if (cleanTitle.isEmpty()) return null

            val response = api.searchLibraryItems(
                authHeader = authHeader,
                userId = userId,
                searchTerm = cleanTitle,
                limit = 5
            )

            val matches = response.items.map { it.toEntity() }
            if (matches.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(matches)
            }

            matches.firstOrNull { entity ->
                val titleMatch = entity.title.contains(cleanTitle, ignoreCase = true) ||
                        cleanTitle.contains(entity.title, ignoreCase = true)
                val yearMatch = year == null || entity.productionYear == null || Math.abs(entity.productionYear - year) <= 1
                val idMatch = (tmdbId != null && entity.tmdbId == tmdbId) || (imdbId != null && entity.imdbId == imdbId)
                idMatch || (titleMatch && yearMatch)
            } ?: matches.firstOrNull()
        }.getOrNull()
    }

    suspend fun searchRemoteAndCache(
        serverUrl: String,
        userId: String,
        token: String,
        query: String,
        limit: Int = 30
    ): Result<List<JellyfinMediaEntity>> {
        val cleanQuery = query.trim()
        if (serverUrl.isBlank() || userId.isBlank() || token.isBlank() || cleanQuery.isBlank()) {
            return Result.success(emptyList())
        }
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.searchLibraryItems(
                authHeader = authHeader,
                userId = userId,
                searchTerm = cleanQuery,
                limit = limit
            )
            val matches = response.items.map { it.toEntity() }
            if (matches.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(matches)
            }
            matches
        }
    }

    suspend fun getUserViews(serverUrl: String, userId: String, token: String): Result<List<JellyfinItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            api.getUserViews(authHeader = authHeader, userId = userId).items
        }
    }

    suspend fun syncQuickInit(serverUrl: String, userId: String, token: String): Result<Unit> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)

            // 1. Sync Continue Watching (Resume Items)
            try {
                val resume = api.getResumeItems(authHeader = authHeader, userId = userId, limit = 30)
                val resumeEntities = resume.items.map { it.toEntity() }
                if (resumeEntities.isNotEmpty()) {
                    jellyfinDao.insertOrUpdateAll(resumeEntities)
                }
            } catch (_: Exception) {}

            // 2. Sync Recent Items (Fast, immediately populates UI with up to 50 items)
            try {
                val latest = api.getLatestItems(authHeader = authHeader, userId = userId, limit = 50)
                val latestEntities = latest.map { it.toEntity() }
                if (latestEntities.isNotEmpty()) {
                    jellyfinDao.insertOrUpdateAll(latestEntities)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaRepository", "Error syncing latest items: ${e.message}", e)
            }

            // 3. Sync Favorites (Fast, critical)
            try {
                val favResponse = api.getFavoriteItems(authHeader = authHeader, userId = userId)
                val favEntities = favResponse.items.map { it.toEntity().copy(isFavorite = true) }
                if (favEntities.isNotEmpty()) {
                    jellyfinDao.insertOrUpdateAll(favEntities)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaRepository", "Error syncing favorites: ${e.message}", e)
            }

            // 4. Sync Recently Played items (Fast, marks watched/progress in local DB)
            var playedEntities: List<JellyfinMediaEntity> = emptyList()
            try {
                val playedResponse = api.getRecentlyPlayedItems(authHeader = authHeader, userId = userId, limit = 100)
                playedEntities = playedResponse.items.map { it.toEntity() }
                if (playedEntities.isNotEmpty()) {
                    jellyfinDao.insertOrUpdateAll(playedEntities)
                }
            } catch (_: Exception) {}

            // 5. Pre-cache parent series for any synced episodes so full series details exist in Room
            try {
                val seriesIds = playedEntities
                    .filter { it.type.equals("Episode", ignoreCase = true) && !it.seriesId.isNullOrBlank() }
                    .mapNotNull { it.seriesId }
                    .distinct()
                for (sId in seriesIds) {
                    try {
                        val seriesDto = api.getItemDetail(authHeader = authHeader, userId = userId, itemId = sId)
                        jellyfinDao.insertOrUpdate(seriesDto.toEntity())
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun syncJellyfinLibrary(
        serverUrl: String,
        userId: String,
        token: String,
        lastSyncTimestamp: String? = null,
        forceFullSync: Boolean = false,
        onSyncCompleted: (suspend (String) -> Unit)? = null
    ): Result<Unit> {
        if (serverUrl.isBlank() || userId.isBlank() || token.isBlank()) {
            return Result.success(Unit)
        }
        if (!syncMutex.tryLock()) {
            // Sincronización ya en curso: evitar duplicar peticiones y saturar el servidor
            return Result.success(Unit)
        }
        return try {
            runCatching {
                val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
                val authHeader = buildJellyfinAuthHeader(token = token)

                val effectiveLastSync = if (forceFullSync) null else {
                    lastSyncTimestamp?.ifBlank { null }
                        ?: userPreferencesRepository?.userPreferencesFlow?.firstOrNull()?.jellyfinLastSync?.ifBlank { null }
                }

                val currentLocal = getLocalCount()

                // 1. Prioridad: refrescar de inmediato los elementos de portada (< 500ms)
                syncQuickInit(serverUrl, userId, token)

                // 2. Obtener vistas del usuario (Películas, Series, etc.)
                val userViews = runCatching {
                    api.getUserViews(authHeader = authHeader, userId = userId).items
                }.getOrDefault(emptyList())

                val mediaViews = userViews.filter { view ->
                    val cType = view.collectionType?.lowercase()
                    cType == "movies" || cType == "tvshows" || (cType == null && !view.name.orEmpty().contains("playlist", ignoreCase = true) && !view.name.orEmpty().contains("coleccion", ignoreCase = true))
                }

                // =========================================================================
                // CAMINO 1: SINCRONIZACIÓN INCREMENTAL (DELTA SYNC)
                // =========================================================================
                // Solo si el catálogo local ya tiene un volumen significativo (> 15.000 títulos)
                // y una fecha de sincronización previa completa
                if (!forceFullSync && currentLocal > 15000 && !effectiveLastSync.isNullOrBlank()) {
                    _syncProgress.value = SyncProgress(
                        isSyncing = true,
                        current = currentLocal,
                        total = currentLocal,
                        message = "Buscando novedades en Jellyfin..."
                    )

                    var deltaCount = 0
                    val fields = "ProviderIds,PrimaryImageTag,CommunityRating,Genres,UserData,ItemCounts,RecursiveItemCount"
                    for (view in mediaViews.ifEmpty { listOf(null) }) {
                        val isSeries = view?.collectionType.equals("tvshows", ignoreCase = true) || view?.name.equals("Series", ignoreCase = true)
                        val deltaResponse = runCatching {
                            api.getLibraryItems(
                                authHeader = authHeader,
                                userId = userId,
                                parentId = view?.id,
                                includeItemTypes = if (isSeries) "Series" else "Movie",
                                fields = fields,
                                recursive = !isSeries,
                                limit = 200,
                                minDateLastSaved = effectiveLastSync
                            )
                        }.getOrNull()

                        val deltaItems = deltaResponse?.items ?: emptyList()
                        if (deltaItems.isNotEmpty()) {
                            val cachedOverviews = jellyfinDao.getCachedOverviews().associateBy { it.id }
                            val entities = deltaItems.map { dto ->
                                val entity = dto.toEntity()
                                val cached = cachedOverviews[entity.id]
                                if (cached != null && entity.overview.isNullOrBlank()) {
                                    entity.copy(
                                        overview = cached.overview,
                                        backdropImageTag = entity.backdropImageTag ?: cached.backdropImageTag
                                    )
                                } else {
                                    entity
                                }
                            }
                            jellyfinDao.insertOrUpdateAll(entities)
                            deltaCount += entities.size
                        }
                    }

                    val nowTimestamp = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
                    userPreferencesRepository?.updateJellyfinLastSync(nowTimestamp)
                    onSyncCompleted?.invoke(nowTimestamp)

                    val updatedLocal = getLocalCount()
                    _syncProgress.value = SyncProgress(
                        isSyncing = false,
                        current = updatedLocal,
                        total = updatedLocal,
                        message = if (deltaCount > 0) "Catálogo actualizado (+$deltaCount novedades)" else "Catálogo al día ($updatedLocal títulos)"
                    )
                    return@runCatching
                }

                // =========================================================================
                // CAMINO 2: SINCRONIZACIÓN COMPLETA GLOBAL
                // =========================================================================
                val fields = "ProviderIds,PrimaryImageTag,CommunityRating,UserData,ItemCounts,RecursiveItemCount"
                val pageSize = 200
                var totalSynced = 0
                // Estimación inicial del catálogo: 35.000 títulos
                var grandTotal = 34966

                _syncProgress.value = SyncProgress(
                    isSyncing = true,
                    current = totalSynced,
                    total = grandTotal,
                    message = "Sincronizando: 0 / $grandTotal"
                )

                val viewsToSync = if (mediaViews.isNotEmpty()) mediaViews else listOf(null)
                var cumulativeViewTotal = 0

                for (view in viewsToSync) {
                    val viewId = view?.id
                    val isSeries = view?.collectionType.equals("tvshows", ignoreCase = true) || view?.name.equals("Series", ignoreCase = true)
                    val itemType = if (isSeries) "Series" else if (view != null) "Movie" else "Movie,Series"
                    val isRecursive = !isSeries // Series son hijas directas del CollectionFolder, Movies pueden estar en subcarpetas

                    var startIndex = 0
                    var viewTotal = 0

                    do {
                        var response: JellyfinItemsResponse? = null
                        var attempt = 0
                        var lastError: Exception? = null

                        while (attempt < 4 && response == null) {
                            try {
                                response = api.getLibraryItems(
                                    authHeader = authHeader,
                                    userId = userId,
                                    parentId = viewId,
                                    includeItemTypes = itemType,
                                    fields = fields,
                                    recursive = isRecursive,
                                    limit = pageSize,
                                    startIndex = startIndex,
                                    sortBy = "SortName",
                                    sortOrder = "Ascending",
                                    enableTotalRecordCount = (startIndex == 0)
                                )
                            } catch (e: Exception) {
                                lastError = e
                                attempt++
                                if (e is retrofit2.HttpException && e.code() == 401) throw e
                                if (attempt < 4) delay(1000L * attempt)
                            }
                        }

                        if (response == null) {
                            android.util.Log.w("MediaRepository", "Failed page $startIndex for view ${view?.name ?: "root"}: ${lastError?.message}")
                            // Avanzamos al siguiente bloque para no romper toda la sincronización
                            startIndex += pageSize
                            continue
                        }

                        val items = response.items
                        if (startIndex == 0 && response.totalRecordCount > 0) {
                            viewTotal = response.totalRecordCount
                            cumulativeViewTotal += viewTotal
                            if (cumulativeViewTotal > 0) {
                                grandTotal = maxOf(grandTotal, cumulativeViewTotal)
                            }
                        }

                        if (items.isEmpty()) break

                        val entities = items.map { it.toEntity() }
                        jellyfinDao.insertOrUpdateAll(entities)

                        startIndex += items.size
                        totalSynced += items.size

                        val effectiveTotal = maxOf(grandTotal, totalSynced)
                        _syncProgress.value = SyncProgress(
                            isSyncing = true,
                            current = totalSynced,
                            total = effectiveTotal,
                            message = "Sincronizando: $totalSynced / $effectiveTotal"
                        )

                        if (viewTotal > 0 && startIndex >= viewTotal) break
                        if (items.size < pageSize) break

                        // Pausa breve para mantener el servidor fluido
                        delay(60L)
                    } while (viewTotal == 0 || startIndex < viewTotal)
                }

                val nowTimestamp = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
                userPreferencesRepository?.updateJellyfinLastSync(nowTimestamp)
                onSyncCompleted?.invoke(nowTimestamp)

                val finalCount = getLocalCount()
                _syncProgress.value = SyncProgress(
                    isSyncing = false,
                    current = finalCount,
                    total = finalCount,
                    message = "Catálogo actualizado ($finalCount títulos)"
                )
            }.onFailure { e ->
                android.util.Log.e("MediaRepository", "Error during Jellyfin library sync: ${e.message}", e)
                val is401 = (e as? retrofit2.HttpException)?.code() == 401
                val errorMessage = if (is401) {
                    "Sesión caducada (401). Reconecta en Ajustes"
                } else {
                    "Error al sincronizar: ${e.localizedMessage ?: "Error de conexión"}"
                }
                _syncProgress.value = SyncProgress(
                    isSyncing = false,
                    current = getLocalCount(),
                    total = getLocalCount(),
                    message = errorMessage
                )
            }
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun getContinueWatching(serverUrl: String, userId: String, token: String): Result<List<JellyfinMediaEntity>> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getResumeItems(authHeader = authHeader, userId = userId)
            val entities = response.items.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            entities
        }
    }

    suspend fun getNextUp(serverUrl: String, userId: String, token: String): Result<List<JellyfinMediaEntity>> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getNextUpItems(authHeader = authHeader, userId = userId)
            val entities = response.items.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            entities
        }
    }

    suspend fun getLatest(serverUrl: String, userId: String, token: String): Result<List<JellyfinMediaEntity>> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getLatestItems(authHeader = authHeader, userId = userId)
            val entities = response.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            entities
        }
    }

    suspend fun getItemDetail(serverUrl: String, userId: String, token: String, itemId: String): Result<JellyfinMediaEntity> {
        return runCatching {
            val local = jellyfinDao.getItemById(itemId)
            if (local != null && !local.overview.isNullOrBlank()) return@runCatching local
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val dto = api.getItemDetail(authHeader = authHeader, userId = userId, itemId = itemId)
            val entity = dto.toEntity()
            if (entity.title.isNotBlank()) {
                val mergedEntity = if (local != null && entity.overview.isNullOrBlank()) local else entity
                jellyfinDao.insertOrUpdate(mergedEntity)
                return@runCatching mergedEntity
            }
            local ?: entity
        }
    }

    suspend fun getTraktRecommendations(traktToken: String, clientId: String): Result<List<TraktMediaDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.trakt.tv/", TraktApiService::class.java)
            val authHeader = "Bearer $traktToken"
            val movies = runCatching { api.getRecommendedMovies(authHeader, clientId) }.getOrDefault(emptyList())
            val shows = runCatching { api.getRecommendedShows(authHeader, clientId) }.getOrDefault(emptyList())
            movies + shows
        }
    }

    suspend fun getTraktTrending(clientId: String): Result<List<TraktMediaDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.trakt.tv/", TraktApiService::class.java)
            val movies = runCatching { api.getTrendingMovies(clientId) }.getOrDefault(emptyList()).mapNotNull { it.movie }
            val shows = runCatching { api.getTrendingShows(clientId) }.getOrDefault(emptyList()).mapNotNull { it.show }
            movies + shows
        }
    }

    suspend fun getTraktWatchlist(traktToken: String, clientId: String): Result<List<TraktMediaDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.trakt.tv/", TraktApiService::class.java)
            val authHeader = "Bearer $traktToken"
            val items = api.getWatchlist(authHeader, clientId)
            items.mapNotNull { it.movie ?: it.show }
        }
    }

    suspend fun getTraktUserProfile(traktToken: String, clientId: String): Result<com.example.tujelly.data.remote.trakt.TraktUserDto> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.trakt.tv/", TraktApiService::class.java)
            val authHeader = "Bearer $traktToken"
            api.getUserProfile(authHeader = authHeader, clientId = clientId)
        }
    }

    suspend fun getRecentlyPlayed(serverUrl: String, userId: String, token: String): Result<List<JellyfinMediaEntity>> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getRecentlyPlayedItems(authHeader = authHeader, userId = userId)
            val entities = response.items.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            entities
        }
    }

    suspend fun getTmdbTrending(apiKey: String): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            val response = api.getTrending(apiKey = apiKey)
            response.results
        }
    }

    suspend fun getTmdbTrendingDay(apiKey: String): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            val response = api.getTrendingDay(apiKey = apiKey)
            response.results
        }
    }

    suspend fun getTmdbRecommendations(apiKey: String, tmdbId: Long, isTv: Boolean = false): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            if (isTv) {
                api.getTvRecommendations(seriesId = tmdbId, apiKey = apiKey).results
            } else {
                api.getMovieRecommendations(movieId = tmdbId, apiKey = apiKey).results
            }
        }
    }

    suspend fun getTmdbTrailerUrl(apiKey: String, tmdbId: Long, isTv: Boolean = false): String? {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            val response = if (isTv) {
                api.getTvVideos(seriesId = tmdbId, apiKey = apiKey)
            } else {
                api.getMovieVideos(movieId = tmdbId, apiKey = apiKey)
            }
            val trailer = response.results.firstOrNull { it.type.equals("Trailer", ignoreCase = true) && it.site.equals("YouTube", ignoreCase = true) }
                ?: response.results.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
            trailer?.key?.let { key -> "https://www.youtube.com/watch?v=$key" }
        }.getOrNull()
    }

    suspend fun getTmdbByGenre(apiKey: String, genreId: String, isTv: Boolean = false): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            if (isTv) {
                api.discoverTvByGenre(apiKey = apiKey, genreId = genreId).results
            } else {
                api.discoverMoviesByGenre(apiKey = apiKey, genreId = genreId).results
            }
        }
    }

    suspend fun getTmdbByProvider(apiKey: String, providerId: String, region: String = "ES"): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            val movies = runCatching { api.discoverMoviesByProvider(apiKey = apiKey, providerId = providerId, watchRegion = region) }
                .getOrNull()?.results ?: emptyList()
            val tv = runCatching { api.discoverTvByProvider(apiKey = apiKey, providerId = providerId, watchRegion = region) }
                .getOrNull()?.results ?: emptyList()
            movies + tv
        }
    }

    suspend fun getTmdbTopRated(apiKey: String): Result<List<TmdbItemDto>> {
        return runCatching {
            val api = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
            val movies = runCatching { api.getTopRatedMovies(apiKey = apiKey) }.getOrNull()?.results ?: emptyList()
            val tv = runCatching { api.getTopRatedTv(apiKey = apiKey) }.getOrNull()?.results ?: emptyList()
            movies + tv
        }
    }

    suspend fun getTopRatedMoviesServer(serverUrl: String, userId: String, token: String, limit: Int = 20): List<JellyfinMediaEntity> {
        val local = jellyfinDao.getTopMoviesLocal(limit)
        if (local.size >= limit) return local
        if (serverUrl.isBlank() || token.isBlank()) return local

        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getLibraryItems(
                authHeader = authHeader,
                userId = userId,
                includeItemTypes = "Movie",
                fields = "Overview,ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,Genres",
                recursive = true,
                limit = limit,
                sortBy = "CommunityRating",
                sortOrder = "Descending"
            )
            val entities = response.items.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            if (entities.isNotEmpty()) entities else local
        }.getOrDefault(local)
    }

    suspend fun getTopRatedSeriesServer(serverUrl: String, userId: String, token: String, limit: Int = 20): List<JellyfinMediaEntity> {
        val local = jellyfinDao.getTopSeriesLocal(limit)
        if (local.size >= limit) return local
        if (serverUrl.isBlank() || token.isBlank()) return local

        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getLibraryItems(
                authHeader = authHeader,
                userId = userId,
                includeItemTypes = "Series",
                fields = "Overview,ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,Genres",
                recursive = true,
                limit = limit,
                sortBy = "CommunityRating",
                sortOrder = "Descending"
            )
            val entities = response.items.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                jellyfinDao.insertOrUpdateAll(entities)
            }
            if (entities.isNotEmpty()) entities else local
        }.getOrDefault(local)
    }

    fun getAllLocalLibrary(): Flow<List<JellyfinMediaEntity>> = jellyfinDao.getAllItems()


    suspend fun getLocalMovies(): List<JellyfinMediaEntity> = jellyfinDao.getMovies()
    suspend fun getLocalSeries(): List<JellyfinMediaEntity> = jellyfinDao.getSeries()
    suspend fun getTopMoviesLocal(limit: Int = 20): List<JellyfinMediaEntity> = jellyfinDao.getTopMoviesLocal(limit)
    suspend fun getTopSeriesLocal(limit: Int = 20): List<JellyfinMediaEntity> = jellyfinDao.getTopSeriesLocal(limit)
    suspend fun getTopRatedLocal(limit: Int = 20): List<JellyfinMediaEntity> = jellyfinDao.getTopRatedLocal(limit)
    suspend fun searchLocalMedia(query: String, limit: Int = 40): List<JellyfinMediaEntity> = jellyfinDao.searchLocalMedia(query.trim(), limit)
    suspend fun getLocalCount(): Int = jellyfinDao.getCount()
    fun getMediaCountFlow(): Flow<Int> = jellyfinDao.getMediaCountFlow()
    suspend fun getFavoritesLocal(): List<JellyfinMediaEntity> = jellyfinDao.getFavorites()
    fun getFavoritesFlow(): Flow<List<JellyfinMediaEntity>> = jellyfinDao.getFavoritesFlow()

    suspend fun getFavorites(serverUrl: String, userId: String, token: String): Result<List<JellyfinMediaEntity>> {
        return runCatching {
            val local = jellyfinDao.getFavorites()
            if (serverUrl.isBlank() || token.isBlank()) return@runCatching local
            try {
                val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
                val authHeader = buildJellyfinAuthHeader(token = token)
                val response = api.getFavoriteItems(authHeader = authHeader, userId = userId)
                val entities = response.items.map { it.toEntity().copy(isFavorite = true) }
                if (entities.isNotEmpty()) {
                    jellyfinDao.insertOrUpdateAll(entities)
                    entities
                } else {
                    local
                }
            } catch (e: Exception) {
                local
            }
        }
    }

    suspend fun toggleFavorite(serverUrl: String, userId: String, token: String, itemId: String, makeFavorite: Boolean): Result<Boolean> {
        return runCatching {
            jellyfinDao.updateFavoriteStatus(itemId, makeFavorite)
            if (serverUrl.isNotBlank() && token.isNotBlank() && userId.isNotBlank()) {
                val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
                val authHeader = buildJellyfinAuthHeader(token = token)
                if (makeFavorite) {
                    api.markFavorite(authHeader, userId, itemId)
                } else {
                    api.unmarkFavorite(authHeader, userId, itemId)
                }
            }
            makeFavorite
        }
    }

    suspend fun findLocal(tmdbId: String?, imdbId: String?, title: String? = null, year: Int? = null): JellyfinMediaEntity? {
        val matched = findLocalInternal(tmdbId, imdbId, title, year) ?: return null
        return resolveToSeriesIfEpisode(matched)
    }

    private suspend fun findLocalInternal(tmdbId: String?, imdbId: String?, title: String? = null, year: Int? = null): JellyfinMediaEntity? {
        // 1. Try matching by TMDB or IMDB ID
        if (tmdbId != null || imdbId != null) {
            val matchedById = jellyfinDao.findByTmdbOrImdb(tmdbId, imdbId)
            if (matchedById != null) return matchedById
        }

        // 2. Fallback: match by title and year
        if (!title.isNullOrBlank()) {
            val cleanTitle = title.trim().substringBefore(":").substringBefore("-").trim()
            if (year != null && year > 1900) {
                val matchedByTitleAndYear = jellyfinDao.findByTitleAndYear(cleanTitle, year)
                    ?: jellyfinDao.findByTitleAndYear(title.trim(), year)
                if (matchedByTitleAndYear != null) return matchedByTitleAndYear
            }
            val matchedByTitle = jellyfinDao.findByTitle(cleanTitle)
                ?: jellyfinDao.findByTitle(title.trim())
                ?: jellyfinDao.findByFuzzyTitle(cleanTitle)
            if (matchedByTitle != null) return matchedByTitle
        }

        return null
    }

    private suspend fun resolveToSeriesIfEpisode(entity: JellyfinMediaEntity): JellyfinMediaEntity {
        if (!entity.type.equals("Episode", ignoreCase = true)) {
            return entity
        }

        // 1. Look up parent series in local Room DB by seriesId
        if (!entity.seriesId.isNullOrBlank()) {
            val parent = jellyfinDao.getItemById(entity.seriesId)
            if (parent != null) return parent
        }

        // 2. Look up parent series in local Room DB by seriesName or title
        val sName = entity.seriesName ?: entity.title.substringBefore("(").substringBefore("-").trim()
        val parentByName = jellyfinDao.getSeriesByTitle(sName)
        if (parentByName != null) return parentByName

        // 3. Synthesize Series entity from Episode data
        val eps = if (!entity.seriesId.isNullOrBlank()) {
            jellyfinDao.getEpisodesForSeries(entity.seriesId)
        } else emptyList()

        val playedCount = if (eps.isNotEmpty()) {
            eps.count { it.isPlayed }
        } else {
            if (entity.isPlayed) 1 else 0
        }
        val totalCount = entity.totalItemCount ?: if (playedCount > 0) maxOf(playedCount + 1, 10) else 10
        val unplayedCount = (totalCount - playedCount).coerceAtLeast(0)

        return JellyfinMediaEntity(
            id = entity.seriesId ?: entity.id,
            title = sName,
            originalTitle = entity.originalTitle,
            type = "Series",
            tmdbId = entity.tmdbId,
            imdbId = entity.imdbId,
            tvdbId = entity.tvdbId,
            overview = entity.overview,
            primaryImageTag = entity.seriesPrimaryImageTag ?: entity.primaryImageTag,
            backdropImageTag = entity.backdropImageTag,
            communityRating = entity.communityRating,
            productionYear = entity.productionYear,
            genres = entity.genres,
            isPlayed = unplayedCount == 0 && playedCount > 0,
            playbackPositionTicks = 0L,
            isFavorite = entity.isFavorite,
            seriesId = null,
            seriesName = null,
            seriesPrimaryImageTag = null,
            seasonNumber = null,
            episodeNumber = null,
            totalItemCount = totalCount,
            unplayedItemCount = unplayedCount
        )
    }

    suspend fun getMostWatchedGenres(): List<String> {
        val watchedRaw = runCatching { jellyfinDao.getWatchedGenres() }.getOrDefault(emptyList())
        val allRaw = if (watchedRaw.size < 5) runCatching { jellyfinDao.getAllGenresRaw() }.getOrDefault(emptyList()) else emptyList()

        val rawList = watchedRaw.ifEmpty { allRaw }
        val counts = mutableMapOf<String, Int>()

        for (raw in rawList) {
            if (raw.isNullOrBlank()) continue
            val parts = raw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
            for (part in parts) {
                val clean = part.replaceFirstChar { it.uppercase() }
                counts[clean] = (counts[clean] ?: 0) + 1
            }
        }

        val defaultGenres = listOf("Acción", "Comedia", "Drama", "Ciencia ficción", "Terror", "Aventura", "Animación", "Thriller", "Romance", "Fantasía", "Documental")
        for (g in defaultGenres) {
            if (!counts.containsKey(g)) {
                counts[g] = 1
            }
        }

        return counts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(12)
    }

    suspend fun getItemsByGenre(genre: String): List<JellyfinMediaEntity> {
        return jellyfinDao.getItemsByGenre(genre)
    }

    suspend fun getSeasons(serverUrl: String, userId: String, token: String, seriesId: String): List<SeasonItem> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getSeasonsForSeries(authHeader = authHeader, seriesId = seriesId, userId = userId)
            response.items.mapNotNull { item ->
                val seasonNum = item.indexNumber ?: 1
                SeasonItem(
                    id = item.id,
                    name = item.name ?: "Temporada $seasonNum",
                    seasonNumber = seasonNum
                )
            }.sortedBy { it.seasonNumber }
        }.getOrDefault(emptyList())
    }

    suspend fun getEpisodes(
        serverUrl: String,
        userId: String,
        token: String,
        seriesId: String,
        seasonId: String? = null
    ): List<EpisodeItem> {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val response = api.getEpisodesForSeries(
                authHeader = authHeader,
                seriesId = seriesId,
                userId = userId,
                seasonId = seasonId
            )
            response.items.map { item ->
                val epNum = item.indexNumber ?: 1
                val seasonNum = item.parentIndexNumber ?: 1
                val durationMin = item.runTimeTicks?.let { (it / 10_000_000L / 60L).toInt() }
                val imageTag = item.imageTags?.get("Primary")
                val imageUrl = if (imageTag != null) {
                    val authParam = if (token.isNotBlank()) "&api_key=$token" else ""
                    "$serverUrl/Items/${item.id}/Images/Primary?tag=$imageTag$authParam"
                } else null

                EpisodeItem(
                    id = item.id,
                    name = item.name ?: "Episodio $epNum",
                    episodeNumber = epNum,
                    seasonNumber = seasonNum,
                    overview = item.overview,
                    imageUrl = imageUrl,
                    durationMinutes = durationMin,
                    isPlayed = item.userData?.isPlayed ?: false,
                    playbackPositionTicks = item.userData?.playbackPositionTicks ?: 0L
                )
            }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        }.getOrDefault(emptyList())
    }

    suspend fun getSeriesStatus(
        tmdbApiKey: String,
        tmdbId: Long?,
        jellyfinStatus: String?
    ): SeriesStatus {
        if (tmdbId != null && tmdbApiKey.isNotBlank()) {
            val tmdbStatus = runCatching {
                val tmdbApi = NetworkClientFactory.createService("https://api.themoviedb.org/3/", TmdbApiService::class.java)
                val detail = tmdbApi.getTvDetails(seriesId = tmdbId, apiKey = tmdbApiKey)
                detail.status
            }.getOrNull()
            if (!tmdbStatus.isNullOrBlank()) {
                return SeriesStatus.from(tmdbStatus = tmdbStatus, jellyfinStatus = jellyfinStatus)
            }
        }
        return SeriesStatus.from(tmdbStatus = null, jellyfinStatus = jellyfinStatus)
    }

    suspend fun getNextUpEpisode(
        serverUrl: String,
        userId: String,
        token: String,
        seriesId: String
    ): EpisodeItem? {
        return runCatching {
            val api = NetworkClientFactory.createService(serverUrl, JellyfinApiService::class.java)
            val authHeader = buildJellyfinAuthHeader(token = token)
            val nextUpResponse = api.getNextUpItems(
                authHeader = authHeader,
                userId = userId,
                seriesId = seriesId,
                limit = 1
            )
            val nextItem = nextUpResponse.items.firstOrNull()
            if (nextItem != null) {
                val epNum = nextItem.indexNumber ?: 1
                val seasonNum = nextItem.parentIndexNumber ?: 1
                EpisodeItem(
                    id = nextItem.id,
                    name = nextItem.name ?: "Episodio $epNum",
                    episodeNumber = epNum,
                    seasonNumber = seasonNum,
                    overview = nextItem.overview,
                    isPlayed = nextItem.userData?.isPlayed ?: false,
                    playbackPositionTicks = nextItem.userData?.playbackPositionTicks ?: 0L
                )
            } else {
                null
            }
        }.getOrNull()
    }

    private fun JellyfinItemDto.toEntity(): JellyfinMediaEntity {
        val totalCount = effectiveItemCount
        val unplayedCount = userData?.unplayedItemCount
        val itemType = type ?: "Unknown"
        val playedStatus = userData?.isPlayedForType(itemType) ?: false

        return JellyfinMediaEntity(
            id = id,
            title = name ?: "",
            originalTitle = originalTitle,
            type = itemType,
            tmdbId = providerIds?.get("Tmdb") ?: providerIds?.get("tmdb"),
            imdbId = providerIds?.get("Imdb") ?: providerIds?.get("imdb"),
            tvdbId = providerIds?.get("Tvdb") ?: providerIds?.get("tvdb"),
            overview = overview,
            primaryImageTag = imageTags?.get("Primary"),
            backdropImageTag = backdropImageTags?.firstOrNull(),
            communityRating = communityRating,
            productionYear = productionYear,
            genres = genres?.joinToString(", "),
            isPlayed = playedStatus,
            playbackPositionTicks = userData?.effectivePositionTicks ?: 0L,
            isFavorite = userData?.effectiveIsFavorite ?: false,
            seriesId = seriesId,
            seriesName = seriesName,
            seriesPrimaryImageTag = seriesPrimaryImageTag,
            seasonNumber = parentIndexNumber,
            episodeNumber = indexNumber,
            totalItemCount = totalCount,
            unplayedItemCount = unplayedCount
        )
    }
}
