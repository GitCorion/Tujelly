package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedusaGravitationalEngineTest {

    @Test
    fun testConceptSignature_Dark_Intense_NoGore_WithFamily() {
        val familyTeens = MedusaAttractorCatalog.getById("CTX_FAMILY_TEENS")!!
        val unsettling = MedusaAttractorCatalog.getById("TON_UNSETTLING")!!
        val highOctane = MedusaAttractorCatalog.getById("ENG_HIGH_OCTANE")!!

        val activeAttractors = listOf(familyTeens, unsettling, highOctane)

        val testCatalog = listOf(
            JellyfinMediaEntity(
                id = "1",
                title = "Jurassic Park",
                type = "Movie",
                genres = "Action, Sci-Fi, Adventure",
                tags = "suspense, fast-paced, dinosaurs, tension",
                overview = "A pragmatic paleontologist touring an almost complete theme park is tasked with protecting a couple of kids.",
                communityRating = 8.2f
            ),
            JellyfinMediaEntity(
                id = "2",
                title = "Saw",
                type = "Movie",
                genres = "Horror, Mystery",
                tags = "gore, slasher, blood, torture",
                overview = "Two strangers awaken in a room with no recollection of how they got there.",
                communityRating = 7.6f
            ),
            JellyfinMediaEntity(
                id = "3",
                title = "Spider-Man: Into the Spider-Verse",
                type = "Movie",
                genres = "Animation, Action, Sci-Fi",
                tags = "coming-of-age, superhero, fast-paced",
                overview = "Teen Miles Morales becomes the new Spider-Man.",
                communityRating = 8.4f
            )
        )

        val results = MedusaGravitationalEngine.evaluateCatalog(
            catalog = testCatalog,
            activeAttractors = activeAttractors,
            limit = 10
        )

        // Saw must be excluded due to gore / slasher prohibited in PG-13 / Family teens context
        assertFalse("Saw debe estar excluido por contener gore", results.any { it.entity.title == "Saw" })

        // Jurassic Park fits family teens + tension + action
        assertTrue("Jurassic Park debe estar presente en los resultados", results.any { it.entity.title == "Jurassic Park" })
    }

    @Test
    fun testCooccurrenceMatrixBuildAndAffinity() {
        val catalog = listOf(
            JellyfinMediaEntity(
                id = "1",
                title = "Toy Story",
                type = "Movie",
                genres = "Animation, Family, Comedy",
                tags = "family-friendly, disney, pixar",
                overview = "A cowboy doll is profoundly threatened and jealous when a new spaceman action figure supplants him.",
                communityRating = 8.3f
            ),
            JellyfinMediaEntity(
                id = "2",
                title = "Mad Max: Fury Road",
                type = "Movie",
                genres = "Action, Adventure, Sci-Fi",
                tags = "fast-paced, car-chase, non-stop, post-apocalyptic",
                overview = "In a post-apocalyptic wasteland, a woman rebels against a tyrannical ruler in search for her homeland.",
                communityRating = 8.1f
            )
        )

        val matrix = CooccurrenceMatrix.buildFromCatalog(catalog, MedusaAttractorCatalog.allAttractors)
        assertEquals(MedusaAttractorCatalog.allAttractors.size, matrix.size)

        val kidsSolo = MedusaAttractorCatalog.allAttractors.indexOfFirst { it.id == "CTX_KIDS_SOLO" }
        val familyAll = MedusaAttractorCatalog.allAttractors.indexOfFirst { it.id == "CTX_FAMILY_ALL" }
        val frenetic = MedusaAttractorCatalog.allAttractors.indexOfFirst { it.id == "ENG_FRENETIC" }

        assertTrue(kidsSolo >= 0 && familyAll >= 0 && frenetic >= 0)

        // Affinities must be between 0 and 1
        val affinity = matrix.getAffinity(kidsSolo, familyAll)
        assertTrue(affinity in 0.0f..1.0f)
    }
}
