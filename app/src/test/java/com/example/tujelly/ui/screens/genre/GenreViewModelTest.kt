package com.example.tujelly.ui.screens.genre

import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreViewModelTest {

    @Test
    fun `genre formats define correct display labels`() {
        assertEquals("Todo", GenreFormat.ALL.label)
        assertEquals("Películas", GenreFormat.MOVIES.label)
        assertEquals("Series", GenreFormat.SERIES.label)
    }

    @Test
    fun `ranked section sets isRanked true for top 10 items`() {
        val items = (1..10).map { id ->
            MediaItem(
                id = id.toString(),
                title = "Item $id",
                rating = 9.0f - (id * 0.1f)
            )
        }

        val section = HomeSection(
            title = "Top 10 Imprescindibles en Ciencia Ficción",
            items = items,
            badge = "TOP 10",
            isRanked = true
        )

        assertTrue(section.isRanked)
        assertEquals(10, section.items.size)
        assertEquals("TOP 10", section.badge)
    }
}
