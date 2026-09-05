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
}
