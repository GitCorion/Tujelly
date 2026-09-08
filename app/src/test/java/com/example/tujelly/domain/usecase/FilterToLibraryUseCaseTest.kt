package com.example.tujelly.domain.usecase

import com.example.tujelly.data.local.db.JellyfinDao
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.data.remote.tmdb.TmdbItemDto
import com.example.tujelly.data.remote.trakt.TraktIdsDto
import com.example.tujelly.data.remote.trakt.TraktMediaDto
import com.example.tujelly.data.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeJellyfinDao : JellyfinDao {
    private val localDb = mutableListOf<JellyfinMediaEntity>()

    fun setLocalItems(items: List<JellyfinMediaEntity>) {
        localDb.clear()
        localDb.addAll(items)
    }

    override suspend fun insertOrUpdateAll(items: List<JellyfinMediaEntity>) {
        localDb.addAll(items)
    }

    override fun getAllItems(): Flow<List<JellyfinMediaEntity>> = flowOf(localDb)

    override suspend fun findByTmdbId(tmdbId: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.tmdbId == tmdbId }
    }

    override suspend fun findByImdbId(imdbId: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.imdbId == imdbId }
    }

    override suspend fun findByTmdbOrImdb(tmdbId: String?, imdbId: String?): JellyfinMediaEntity? {
        return localDb.firstOrNull {
            (tmdbId != null && it.tmdbId == tmdbId) || (imdbId != null && it.imdbId == imdbId)
        }
    }

    override suspend fun getItemById(id: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.id == id }
    }

    override suspend fun insertOrUpdate(item: JellyfinMediaEntity) {
        localDb.removeAll { it.id == item.id }
        localDb.add(item)
    }

    override suspend fun findByTitleAndYear(title: String, year: Int): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.title.equals(title, ignoreCase = true) && it.productionYear == year }
    }

    override suspend fun findByTitle(title: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.title.equals(title, ignoreCase = true) }
    }

    override suspend fun findByFuzzyTitle(title: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.title.contains(title, ignoreCase = true) }
    }

    override suspend fun getEpisodesForSeries(seriesId: String): List<JellyfinMediaEntity> {
        return localDb.filter { it.seriesId == seriesId && it.type == "Episode" }
    }

    override suspend fun getSeriesByTitle(title: String): JellyfinMediaEntity? {
        return localDb.firstOrNull { it.type == "Series" && (it.title.equals(title, ignoreCase = true) || it.originalTitle?.equals(title, ignoreCase = true) == true) }
    }

    override suspend fun searchLocalMedia(query: String, limit: Int): List<JellyfinMediaEntity> {
        return localDb.filter { it.title.contains(query, ignoreCase = true) }.take(limit)
    }

    override suspend fun getMovies(): List<JellyfinMediaEntity> = localDb.filter { it.type == "Movie" }

    override suspend fun getSeries(): List<JellyfinMediaEntity> = localDb.filter { it.type == "Series" }

    override suspend fun getTopRatedLocal(limit: Int): List<JellyfinMediaEntity> = localDb.take(limit)

    override suspend fun getTopMoviesLocal(limit: Int): List<JellyfinMediaEntity> = localDb.filter { it.type == "Movie" }.take(limit)

    override suspend fun getTopSeriesLocal(limit: Int): List<JellyfinMediaEntity> = localDb.filter { it.type == "Series" }.take(limit)

    override suspend fun getItemsByGenre(genre: String, limit: Int): List<JellyfinMediaEntity> =
        localDb.filter { it.genres?.contains(genre, ignoreCase = true) == true }.take(limit)

    override suspend fun getWatchedGenres(): List<String> = emptyList()

    override suspend fun getAllGenresRaw(): List<String> = emptyList()

    override suspend fun getCount(): Int = localDb.size

    override fun getMediaCountFlow(): Flow<Int> = flowOf(localDb.size)

    override fun getMoviesCountFlow(): Flow<Int> = flowOf(localDb.count { it.type == "Movie" })

    override fun getSeriesCountFlow(): Flow<Int> = flowOf(localDb.count { it.type == "Series" })

    override fun getEpisodesCountFlow(): Flow<Int> = flowOf(localDb.count { it.type == "Episode" })

    override fun getTotalSeriesEpisodesFlow(): Flow<Int> = flowOf(localDb.filter { it.type == "Series" }.sumOf { it.totalItemCount ?: 0 })

    override suspend fun getFavorites(): List<JellyfinMediaEntity> = localDb.filter { it.isFavorite }

    override fun getFavoritesFlow(): Flow<List<JellyfinMediaEntity>> = flowOf(localDb.filter { it.isFavorite })

    override suspend fun updateFavoriteStatus(itemId: String, isFavorite: Boolean) {
        val item = getItemById(itemId) ?: return
        insertOrUpdate(item.copy(isFavorite = isFavorite))
    }

    override suspend fun getCachedOverviews(): List<com.example.tujelly.data.local.db.CachedOverviewDto> {
        return localDb.mapNotNull {
            if (it.overview != null) com.example.tujelly.data.local.db.CachedOverviewDto(it.id, it.overview, it.backdropImageTag) else null
        }
    }

    override suspend fun clearAll() {
        localDb.clear()
    }
}

class FilterToLibraryUseCaseTest {

    @Test
    fun `filterTraktItems discards items not available in Jellyfin`() = runTest {
        val fakeDao = FakeJellyfinDao()
        val inLibraryEntity = JellyfinMediaEntity(
            id = "jf-100",
            title = "Inception",
            type = "Movie",
            tmdbId = "27205",
            imdbId = "tt1375666"
        )
        fakeDao.setLocalItems(listOf(inLibraryEntity))

        val repo = MediaRepository(fakeDao)
        val useCase = FilterToLibraryUseCase(repo)

        val traktItems = listOf(
            TraktMediaDto(
                title = "Inception",
                year = 2010,
                ids = TraktIdsDto(tmdb = 27205, imdb = "tt1375666")
            ),
            TraktMediaDto(
                title = "Avatar",
                year = 2009,
                ids = TraktIdsDto(tmdb = 19995, imdb = "tt0499549") // NOT in library!
            )
        )

        val filtered = useCase.filterTraktItems(traktItems)

        assertEquals(1, filtered.size)
        assertEquals("Inception", filtered[0].title)
        assertEquals("jf-100", filtered[0].id)
    }

    @Test
    fun `filterTmdbItems discards items not available in Jellyfin`() = runTest {
        val fakeDao = FakeJellyfinDao()
        val inLibraryEntity = JellyfinMediaEntity(
            id = "jf-200",
            title = "Interstellar",
            type = "Movie",
            tmdbId = "157336"
        )
        fakeDao.setLocalItems(listOf(inLibraryEntity))

        val repo = MediaRepository(fakeDao)
        val useCase = FilterToLibraryUseCase(repo)

        val tmdbItems = listOf(
            TmdbItemDto(id = 157336, title = "Interstellar"),
            TmdbItemDto(id = 999999, title = "Unknown Movie") // NOT in library!
        )

        val filtered = useCase.filterTmdbItems(tmdbItems)

        assertEquals(1, filtered.size)
        assertEquals("Interstellar", filtered[0].title)
        assertEquals("jf-200", filtered[0].id)
    }

    @Test
    fun `filterTmdbItems resolves episode match to parent series and keeps isPlayed false when not finished`() = runTest {
        val fakeDao = FakeJellyfinDao()
        val playedEpisode = JellyfinMediaEntity(
            id = "ep-101",
            title = "Día de la Libertad",
            type = "Episode",
            seriesId = "series-silo",
            seriesName = "Silo",
            tmdbId = "125988",
            isPlayed = true,
            seasonNumber = 1,
            episodeNumber = 1,
            totalItemCount = 10,
            unplayedItemCount = 9
        )
        fakeDao.setLocalItems(listOf(playedEpisode))

        val repo = MediaRepository(fakeDao)
        val useCase = FilterToLibraryUseCase(repo)

        val tmdbItems = listOf(
            TmdbItemDto(id = 125988, name = "Silo", firstAirDate = "2023-05-05")
        )

        val filtered = useCase.filterTmdbItems(tmdbItems)

        assertEquals(1, filtered.size)
        val item = filtered[0]
        assertEquals("Silo", item.title)
        assertEquals("Series", item.type)
        assertEquals(false, item.isPlayed) // Must NOT be marked as fully played
        assertEquals(10, item.totalItemCount)
        assertEquals(9, item.unplayedItemCount)
    }
}
