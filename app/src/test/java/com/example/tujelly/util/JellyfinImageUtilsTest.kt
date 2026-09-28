package com.example.tujelly.util

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinImageUtilsTest {

    @Test
    fun `getPosterUrl produces server-side resized query parameters`() {
        val url = JellyfinImageUtils.getPosterUrl(
            baseUrl = "http://192.168.1.50:8096",
            itemId = "item-123",
            imageTag = "tagABC",
            token = "secretToken"
        )

        assertNotNull(url)
        assertTrue(url!!.contains("fillWidth=360"))
        assertTrue(url.contains("fillHeight=540"))
        assertTrue(url.contains("quality=85"))
        assertTrue(url.contains("tag=tagABC"))
        assertTrue(url.contains("api_key=secretToken"))
        assertTrue(url.startsWith("http://192.168.1.50:8096/Items/item-123/Images/Primary?"))
    }

    @Test
    fun `getBackdropUrl produces server-side resized backdrop parameters`() {
        val url = JellyfinImageUtils.getBackdropUrl(
            baseUrl = "http://192.168.1.50:8096",
            itemId = "item-456",
            imageTag = "tagXYZ",
            token = "secretToken"
        )

        assertNotNull(url)
        assertTrue(url!!.contains("maxWidth=1280"))
        assertTrue(url.contains("quality=80"))
        assertTrue(url.contains("tag=tagXYZ"))
        assertTrue(url.contains("api_key=secretToken"))
    }

    @Test
    fun `getEpisodeThumbnailUrl produces 16 to 9 scaled thumbnail`() {
        val url = JellyfinImageUtils.getEpisodeThumbnailUrl(
            baseUrl = "http://192.168.1.50:8096",
            itemId = "ep-789",
            imageTag = "thumbTag",
            token = "token1"
        )

        assertNotNull(url)
        assertTrue(url!!.contains("fillWidth=480"))
        assertTrue(url.contains("fillHeight=270"))
        assertTrue(url.contains("quality=80"))
    }

    @Test
    fun `toOptimizedMediaItem maps series episode correctly`() {
        val episodeEntity = JellyfinMediaEntity(
            id = "ep-10",
            title = "Pilot",
            seriesId = "series-99",
            seriesName = "Breaking Good",
            seriesPrimaryImageTag = "seriesPosterTag",
            type = "Episode",
            seasonNumber = 1,
            episodeNumber = 1,
            playbackPositionTicks = 500000000L // 50s
        )

        val mediaItem = episodeEntity.toOptimizedMediaItem(
            baseUrl = "http://server:8096",
            token = "tok",
            source = MediaSource.JELLYFIN
        )

        // Poster URL should use the parent series ID and seriesPrimaryImageTag
        assertTrue(mediaItem.posterUrl!!.contains("/Items/series-99/Images/Primary"))
        assertTrue(mediaItem.posterUrl!!.contains("tag=seriesPosterTag"))
        // Title in continue watching should show series name + code
        assertEquals("Breaking Good (T1:E1)", mediaItem.title)
    }
}
