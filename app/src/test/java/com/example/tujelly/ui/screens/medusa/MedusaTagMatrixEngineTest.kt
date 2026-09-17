package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MedusaTagMatrixEngineTest {

    private lateinit var engine: MedusaTagMatrixEngine

    @Before
    fun setup() {
        engine = MedusaTagMatrixEngine()
    }

    private fun createMovie(id: String, title: String, genres: String, tags: String = "", rating: Float = 8.0f): JellyfinMediaEntity {
        return JellyfinMediaEntity(
            id = id,
            title = title,
            type = "Movie",
            genres = genres,
            tags = tags,
            communityRating = rating,
            productionYear = 2022
        )
    }

    @Test
    fun testInitialMosaicContainsDiversePillarsAndThemes() {
        val catalog = listOf(
            createMovie("1", "Alien", "Ciencia Ficción, Terror", "Alienígenas, Espacio, Monstruos"),
            createMovie("2", "The Thing", "Terror, Ciencia Ficción", "Monstruos, Aislamiento"),
            createMovie("3", "The Conjuring", "Terror", "Casas Encantadas, Posesión, Sobrenatural"),
            createMovie("4", "Interstellar", "Ciencia Ficción, Drama", "Espacio, Agujero Negro"),
            createMovie("5", "Die Hard", "Acción, Suspense", "Secuestro, Rascacielos"),
            createMovie("6", "Toy Story", "Animación, Aventura, Familia", "Juguetes, Amistad"),
            createMovie("7", "La La Land", "Romance, Comedia, Musical", "Música, Amor"),
            createMovie("8", "Blade Runner", "Ciencia Ficción", "Cyberpunk, Distopía, Detectives")
        )

        engine.initialize(catalog)
        val initialTags = engine.generateMosaicTags(emptyList(), MediaFormat.ALL, targetCapacity = 20)

        assertTrue(initialTags.isNotEmpty())
        assertTrue("Debe contener varios tags", initialTags.size >= 8)
        assertTrue(initialTags.any { it.label.equals("Terror", ignoreCase = true) })
        assertTrue(initialTags.any { it.label.equals("Ciencia Ficción", ignoreCase = true) })
    }

    @Test
    fun testSelectingTagPrunesIncompatibleAndPopulatesWithCooccurringMicroTags() {
        val catalog = listOf(
            createMovie("1", "Alien", "Ciencia Ficción, Terror", "Alienígenas, Espacio, Monstruos"),
            createMovie("2", "The Thing", "Terror, Ciencia Ficción", "Monstruos, Aislamiento"),
            createMovie("3", "The Conjuring", "Terror", "Casas Encantadas, Posesión, Sobrenatural"),
            createMovie("4", "Die Hard", "Acción", "Rascacielos"),
            createMovie("5", "Toy Story", "Animación", "Juguetes")
        )

        engine.initialize(catalog)

        val terrorTag = MedusaMosaicTag(
            id = "test_terror",
            label = "Terror",
            rawTag = "terror",
            movieCount = 3,
            category = "Terror",
            isCaptain = true
        )

        val activeChain = listOf(terrorTag)
        val filteredTags = engine.generateMosaicTags(activeChain, MediaFormat.ALL)

        // Verificación 1: Tags que no tienen películas con Terror (como "Juguetes" o "Rascacielos") no deben estar
        assertFalse(filteredTags.any { it.rawTag == "juguetes" })
        assertFalse(filteredTags.any { it.rawTag == "rascacielos" })

        // Verificación 2: Micro-tags que SÍ están en las películas de terror deben aparecer
        assertTrue(filteredTags.any { it.rawTag == "monstruos" })
        assertTrue(filteredTags.any { it.rawTag == "alienigenas" || it.rawTag == "espacio" || it.rawTag == "casas encantadas" })

        // Verificación 3: Películas filtradas estrictamente
        val movies = engine.getMatchingMovies(activeChain, MediaFormat.ALL)
        assertEquals(3, movies.size)
        assertTrue(movies.any { it.title == "Alien" })
        assertTrue(movies.any { it.title == "The Thing" })
        assertTrue(movies.any { it.title == "The Conjuring" })
    }

    @Test
    fun testTwoTagsStrictAndRefinement() {
        val catalog = listOf(
            createMovie("1", "Alien", "Ciencia Ficción, Terror", "Alienígenas, Espacio"),
            createMovie("2", "The Thing", "Terror, Ciencia Ficción", "Monstruos, Aislamiento"),
            createMovie("3", "The Conjuring", "Terror", "Casas Encantadas, Posesión"),
            createMovie("4", "Interstellar", "Ciencia Ficción", "Espacio")
        )

        engine.initialize(catalog)

        val terrorTag = MedusaMosaicTag("1", "Terror", "terror", 3, "Terror", true)
        val scifiTag = MedusaMosaicTag("2", "Ciencia Ficción", "ciencia ficcion", 3, "Ciencia Ficción", true)

        val chain = listOf(terrorTag, scifiTag)
        val movies = engine.getMatchingMovies(chain, MediaFormat.ALL)

        // Solo Alien y The Thing tienen AMBOS géneros (Terror AND Ciencia Ficción)
        assertEquals(2, movies.size)
        assertTrue(movies.any { it.title == "Alien" })
        assertTrue(movies.any { it.title == "The Thing" })
        assertFalse(movies.any { it.title == "The Conjuring" })
        assertFalse(movies.any { it.title == "Interstellar" })
    }

    @Test
    fun testTagIdsAreStableAcrossSelections() {
        val catalog = listOf(
            createMovie("1", "Alien", "Ciencia Ficción, Terror", "Alienígenas, Espacio"),
            createMovie("2", "The Thing", "Terror, Ciencia Ficción", "Monstruos, Aislamiento"),
            createMovie("3", "Toy Story", "Animación, Comedia", "Juguetes, Familia")
        )

        engine.initialize(catalog)

        val tagsInitial = engine.generateMosaicTags(emptyList(), MediaFormat.ALL)
        val terrorTag1 = tagsInitial.first { it.rawTag == "terror" }
        assertEquals("tag_terror", terrorTag1.id)

        // Simular primer click (seleccionar Terror)
        val tagsAfterFirstClick = engine.generateMosaicTags(listOf(terrorTag1), MediaFormat.ALL)
        val terrorTagActive = tagsAfterFirstClick.first { it.rawTag == "terror" }
        val scifiTagCandidate = tagsAfterFirstClick.first { it.rawTag == "ciencia ficcion" }

        assertEquals("tag_terror", terrorTagActive.id)
        assertEquals("tag_ciencia ficcion", scifiTagCandidate.id)

        // Simular segundo click (seleccionar Ciencia Ficción junto con Terror)
        val tagsAfterSecondClick = engine.generateMosaicTags(listOf(terrorTagActive, scifiTagCandidate), MediaFormat.ALL)
        val scifiTagActive = tagsAfterSecondClick.first { it.rawTag == "ciencia ficcion" }

        // El ID no debe variar con el orden o el estado de selección
        assertEquals("tag_ciencia ficcion", scifiTagActive.id)
        assertEquals("tag_terror", tagsAfterSecondClick.first { it.rawTag == "terror" }.id)
    }

    @Test
    fun testOnlySpanishTagsAreDisplayedAndEnglishScrapersAreFiltered() {
        val catalog = listOf(
            createMovie(
                "1",
                "Anime Movie",
                "Animación",
                "Fantasy World, Japanese Mythology, Urban Fantasy, Original Net Animation (ona), UnrecognizedEnglishJunkKeyword123"
            )
        )

        engine.initialize(catalog)

        val tags = engine.generateMosaicTags(emptyList(), MediaFormat.ALL)

        // 1. Scraper técnico y keywords en inglés sin traducción deben ser filtradas
        assertFalse(tags.any { it.label.contains("ona", ignoreCase = true) })
        assertFalse(tags.any { it.label.contains("Original Net Animation", ignoreCase = true) })
        assertFalse(tags.any { it.label.contains("UnrecognizedEnglishJunkKeyword123", ignoreCase = true) })

        // 2. Las etiquetas en inglés reconocidas deben mostrarse estrictamente traducidas al español
        assertTrue("Debe contener Mundo Fantástico", tags.any { it.label == "Mundo Fantástico" })
        assertTrue("Debe contener Mitología Japonesa", tags.any { it.label == "Mitología Japonesa" })
        assertTrue("Debe contener Fantasía Urbana", tags.any { it.label == "Fantasía Urbana" })

        // 3. Ninguna etiqueta debe mostrarse en inglés sin traducir
        assertFalse(tags.any { it.label == "Fantasy World" })
        assertFalse(tags.any { it.label == "Japanese Mythology" })
        assertFalse(tags.any { it.label == "Urban Fantasy" })
    }
}
