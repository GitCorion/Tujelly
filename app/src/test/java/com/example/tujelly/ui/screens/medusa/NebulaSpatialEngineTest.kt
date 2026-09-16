package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NebulaSpatialEngineTest {

    private val testCatalog = listOf(
        JellyfinMediaEntity(
            id = "m1",
            title = "Blade Runner 2049",
            type = "Movie",
            genres = "Sci-Fi, Mystery",
            tags = "cyberpunk, noir, ai, dystopia",
            communityRating = 8.0f
        ),
        JellyfinMediaEntity(
            id = "m2",
            title = "Ghost in the Shell",
            type = "Movie",
            genres = "Animation, Sci-Fi",
            tags = "cyberpunk, ai, cyborg, dystopia",
            communityRating = 8.1f
        ),
        JellyfinMediaEntity(
            id = "m3",
            title = "The Godfather",
            type = "Movie",
            genres = "Crime, Drama",
            tags = "mafia, family, classic",
            communityRating = 9.2f
        ),
        JellyfinMediaEntity(
            id = "m4",
            title = "Casino",
            type = "Movie",
            genres = "Crime, Drama",
            tags = "mafia, heist, money",
            communityRating = 8.2f
        ),
        JellyfinMediaEntity(
            id = "m5",
            title = "Interstellar",
            type = "Movie",
            genres = "Sci-Fi, Adventure",
            tags = "space, time travel, wormhole, survival",
            communityRating = 8.7f
        ),
        JellyfinMediaEntity(
            id = "m6",
            title = "Sin City",
            type = "Movie",
            genres = "Crime, Thriller",
            tags = "crime, noir, neo-noir",
            communityRating = 8.0f
        )
    )

    @Test
    fun testSunNodeAndDynamicUniverseGeneration() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            tmdbTopRated = listOf(testCatalog[2]), // The Godfather
            tmdbTrending = listOf(testCatalog[0]), // Blade Runner
            recentWatched = listOf(testCatalog[4]), // Interstellar
            favorites = listOf(testCatalog[1]), // Ghost in the Shell
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val allNodes = engine.getAllNodes()
        assertTrue("Universe should have generated tag nodes", allNodes.isNotEmpty())

        // Verify Sun Core Node exists at center (0.5, 0.5) with 0 movies selected initially
        val sunNode = engine.getNode(SUN_CORE_ID)
        assertNotNull("Sun core node must exist at (0.5, 0.5)", sunNode)
        assertTrue("Node must be identified as Sun", sunNode!!.isSun)
        assertEquals(0.50f, sunNode.worldX, 0.001f)
        assertEquals(0.50f, sunNode.worldY, 0.001f)
        assertEquals(0, sunNode.movieCount)
        assertEquals("SOL CENTRAL", sunNode.label)

        val captains = allNodes.filter { it.isCaptain }
        assertTrue("Deben existir estrellas capitanas en los sectores", captains.isNotEmpty())
    }

    @Test
    fun testCaptainNodesIdentifiedForGalaxySectors() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val allNodes = engine.getAllNodes().filter { !it.isSun }
        val captains = allNodes.filter { it.isCaptain }
        assertTrue("Debe existir al menos una estrella capitana en la galaxia", captains.isNotEmpty())
        captains.forEach { captain ->
            assertTrue("La importancia de una capitana debe ser alta (>= 2.0)", captain.importance >= 2.0f)
            assertFalse("El nombre de la capitana no debe incluir caracteres extraños", captain.label.contains("★"))
        }
    }

    @Test
    fun testStrictAndMatchingWithoutDegradation() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        // 0. Cadena vacía: debe retornar 0 obras porque no se han seleccionado etiquetas aún
        val emptyMatches = engine.getMatchingMoviesForChain(
            chain = emptyList(),
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )
        assertEquals(0, emptyMatches.size)

        val cyberpunkNode = engine.getAllNodes().firstOrNull { it.rawTag == "cyberpunk" || it.label == "Cyberpunk" }
        assertNotNull("Debe existir el nodo 'cyberpunk'", cyberpunkNode)

        val noirNode = engine.getAllNodes().firstOrNull { it.rawTag == "cine negro" || it.rawTag == "noir" || it.label == "Cine Negro" }
        assertNotNull("Debe existir el nodo 'noir' / 'Cine Negro'", noirNode)

        // 1. Un solo tag: 'cyberpunk' (coincide con Blade Runner 2049 y Ghost in the Shell)
        val singleMatches = engine.getMatchingMoviesForChain(
            chain = listOf(cyberpunkNode!!),
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )
        assertEquals(2, singleMatches.size)

        // 2. Intersección estricta AND: 'cyberpunk' AND 'noir' (solo Blade Runner 2049)
        val doubleMatches = engine.getMatchingMoviesForChain(
            chain = listOf(cyberpunkNode, noirNode!!),
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )
        assertEquals(1, doubleMatches.size)
        assertEquals("Blade Runner 2049", doubleMatches.first().title)

        // 3. Intersección sin coincidencias: 'cyberpunk' AND 'mafia' (0 obras)
        val mafiaNode = engine.getAllNodes().firstOrNull { it.rawTag == "mafia" || it.label == "Mafia" }
        assertNotNull("Debe existir el nodo 'mafia'", mafiaNode)

        val zeroMatches = engine.getMatchingMoviesForChain(
            chain = listOf(cyberpunkNode, mafiaNode!!),
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )
        assertEquals("En AND estricto, si no hay coincidencias el resultado debe ser 0 (cero degradación)", 0, zeroMatches.size)
    }

    @Test
    fun testLivePruningEliminatesIncompatibleNodes() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val allNodeIds = engine.getAllNodes().map { it.id }.toSet()

        // 1. Cadena vacía: todos los nodos son visibles
        val initialVisible = engine.getVisibleNodeIds(emptyList())
        assertEquals(allNodeIds, initialVisible)

        // 2. Seleccionar 'mafia': los nodos de ciencia ficción y espacio deben ser podados
        val mafiaNode = engine.getAllNodes().first { it.rawTag == "mafia" || it.label == "Mafia" }
        val spaceNode = engine.getAllNodes().firstOrNull { it.rawTag == "espacio" || it.rawTag == "space" || it.label == "Espacio" }
        val heistNode = engine.getAllNodes().firstOrNull { it.rawTag == "atraco" || it.rawTag == "heist" || it.label == "Atraco" }

        val visibleAfterMafia = engine.getVisibleNodeIds(listOf(mafiaNode))

        assertTrue("El Sol Central siempre sobrevive a la poda", visibleAfterMafia.contains(SUN_CORE_ID))
        assertTrue("El nodo de mafia activo sobrevive a la poda", visibleAfterMafia.contains(mafiaNode.id))
        if (heistNode != null) {
            assertTrue("'heist' / 'atraco' tiene películas con 'mafia' (Casino), debe sobrevivir", visibleAfterMafia.contains(heistNode.id))
        }
        if (spaceNode != null) {
            assertFalse("'space' / 'espacio' NO tiene películas con 'mafia', debe ser podado", visibleAfterMafia.contains(spaceNode.id))
        }
    }

    @Test
    fun testSunNodeReceivesActiveRays() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val mafiaNode = engine.getAllNodes().first { it.rawTag == "mafia" }
        engine.updateSunNode(
            chain = listOf(mafiaNode),
            movieCount = 2,
            previewPosters = listOf("http://poster1.jpg")
        )

        val sun = engine.getNode(SUN_CORE_ID)
        assertNotNull(sun)
        assertEquals(2, sun!!.movieCount)

        val filaments = engine.getAllFilaments()
        val raysToSun = filaments.filter { it.toNodeId == SUN_CORE_ID && it.fromNodeId == mafiaNode.id }
        assertTrue("Debe existir un rayo de energía activo conectado hacia el Sol", raysToSun.isNotEmpty())
        assertTrue("El filamento debe estar marcado como rayo activo", raysToSun.first().isActiveRay)
    }

    @Test
    fun testDPadNavigatesOnlyVisibleNodesAndReachesSun() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val sun = engine.getNode(SUN_CORE_ID)!!
        val neighborFromSun = engine.findNextNeighbor(
            currentId = sun.id,
            direction = DPadDirection.RIGHT,
            currentZoom = 1.0f
        ) ?: engine.findNextNeighbor(
            currentId = sun.id,
            direction = DPadDirection.UP,
            currentZoom = 1.0f
        ) ?: engine.findNextNeighbor(
            currentId = sun.id,
            direction = DPadDirection.LEFT,
            currentZoom = 1.0f
        )

        assertNotNull("Desde el Sol debe poderse navegar hacia el firmamento", neighborFromSun)
    }

    @Test
    fun testAffinityProximityToCaptains() {
        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123",
            sessionSeed = 42L
        )

        val captains = engine.getAllNodes().filter { it.isCaptain }
        assertTrue("Deben existir capitanas", captains.isNotEmpty())

        val filaments = engine.getAllFilaments()
        // Buscar un filamento satélite -> capitana
        val satFilament = filaments.firstOrNull { fil ->
            val toNode = engine.getNode(fil.toNodeId)
            val fromNode = engine.getNode(fil.fromNodeId)
            toNode?.isCaptain == true && fromNode?.isCaptain == false && fromNode.isSun == false
        }
        assertNotNull("Debe haber filamentos conectando satélites con su Capitana", satFilament)

        val sat = engine.getNode(satFilament!!.fromNodeId)!!
        val cap = engine.getNode(satFilament.toNodeId)!!

        val dist = kotlin.math.hypot(sat.worldX - cap.worldX, sat.worldY - cap.worldY)
        assertTrue("El satélite debe estar a una distancia de constelación cercana de su Capitana (< 0.25f)", dist < 0.25f)
    }

    @Test
    fun testSessionSeedRotatesUniverse() {
        val engine1 = NebulaSpatialEngine()
        engine1.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123",
            sessionSeed = 100L
        )

        val engine2 = NebulaSpatialEngine()
        engine2.buildDynamicUniverse(
            catalog = testCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123",
            sessionSeed = 280L
        )

        val cap1 = engine1.getAllNodes().first { it.isCaptain }
        val cap2 = engine2.getAllNodes().first { it.isCaptain }

        // Las posiciones no deben ser idénticas cuando el seed de rotación es significativamente distinto
        val dx = kotlin.math.abs(cap1.worldX - cap2.worldX)
        val dy = kotlin.math.abs(cap1.worldY - cap2.worldY)
        assertTrue("Diferentes semillas deben rotar las constelaciones en el espacio", dx > 0.01f || dy > 0.01f)
    }

    @Test
    fun testNoDuplicateLabelsInUniverse() {
        val duplicateProneCatalog = listOf(
            JellyfinMediaEntity(id = "d1", title = "Peli 1", type = "Movie", genres = "Animation, Family", tags = "animación, familia"),
            JellyfinMediaEntity(id = "d2", title = "Peli 2", type = "Movie", genres = "Crime, Thriller", tags = "crimen, suspense"),
            JellyfinMediaEntity(id = "d3", title = "Peli 3", type = "Movie", genres = "Animation, Comedy", tags = "animacion, comedia"),
            JellyfinMediaEntity(id = "d4", title = "Peli 4", type = "Movie", genres = "Family, Comedy", tags = "familia, comedia"),
            JellyfinMediaEntity(id = "d5", title = "Peli 5", type = "Movie", genres = "Crime, Mystery", tags = "crimen, misterio"),
            JellyfinMediaEntity(id = "d6", title = "Peli 6", type = "Movie", genres = "Sci-Fi, Action", tags = "ciencia ficcion, accion")
        )

        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = duplicateProneCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val tagNodes = engine.getAllNodes().filter { !it.isSun }
        val labels = tagNodes.map { it.label }
        val uniqueLabels = labels.toSet()

        assertEquals("No debe haber etiquetas duplicadas en el firmamento: $labels", uniqueLabels.size, labels.size)
    }

    @Test
    fun testCompositeGenresDecomposedAndNoDoubleSciFi() {
        val seriesAndMoviesCatalog = listOf(
            JellyfinMediaEntity(id = "s1", title = "Serie 1", type = "Series", genres = "Sci-Fi & Fantasy, Drama", tags = "space, alien"),
            JellyfinMediaEntity(id = "s2", title = "Serie 2", type = "Series", genres = "Sci-Fi & Fantasy, Action & Adventure", tags = "space, superhero"),
            JellyfinMediaEntity(id = "m1", title = "Peli 1", type = "Movie", genres = "Science Fiction", tags = "space, alien"),
            JellyfinMediaEntity(id = "m2", title = "Peli 2", type = "Movie", genres = "Fantasy", tags = "magic, dragon"),
            JellyfinMediaEntity(id = "m3", title = "Peli 3", type = "Movie", genres = "Action", tags = "superhero, car chase")
        )

        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = seriesAndMoviesCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123"
        )

        val labels = engine.getAllNodes().filter { !it.isSun }.map { it.label }

        assertFalse(
            "No debe existir la etiqueta compuesta 'Ciencia Ficción y Fantasía' en el firmamento",
            labels.any { it.contains("Fantasía", ignoreCase = true) && it.contains("Ficción", ignoreCase = true) }
        )
        assertFalse(
            "No debe existir 'Acción y Aventura' en el firmamento",
            labels.any { it.contains("Acción", ignoreCase = true) && it.contains("Aventura", ignoreCase = true) }
        )

        val sciFiCount = labels.count { it.contains("Ciencia Ficción", ignoreCase = true) }
        assertEquals("Debe existir a lo sumo una etiqueta de Ciencia Ficción, nunca duplicada: $labels", 1, sciFiCount)
    }

    @Test
    fun testOnlyPillarGenresCanBeCaptainsAndNoRightWallClamping() {
        val richCatalog = listOf(
            JellyfinMediaEntity(id = "1", title = "Peli 1", type = "Movie", genres = "Action, Sci-Fi", tags = "superhero, lgbtq+, aliens"),
            JellyfinMediaEntity(id = "2", title = "Peli 2", type = "Movie", genres = "Action, Adventure", tags = "superhero, amistad, artes marciales"),
            JellyfinMediaEntity(id = "3", title = "Peli 3", type = "Movie", genres = "Comedy, Romance", tags = "romcom, lgbtq+, amistad"),
            JellyfinMediaEntity(id = "4", title = "Peli 4", type = "Movie", genres = "Drama, History", tags = "biografico, western, guerra"),
            JellyfinMediaEntity(id = "5", title = "Peli 5", type = "Movie", genres = "Horror, Mystery", tags = "asesinato, slasher, monstruos"),
            JellyfinMediaEntity(id = "6", title = "Peli 6", type = "Movie", genres = "Crime, Thriller", tags = "asesinato, mafia, neo-noir"),
            JellyfinMediaEntity(id = "7", title = "Peli 7", type = "Movie", genres = "Fantasy, Animation", tags = "magia, anime, amistad"),
            JellyfinMediaEntity(id = "8", title = "Peli 8", type = "Movie", genres = "Sci-Fi, Comedy", tags = "robots, distopia, humor"),
            JellyfinMediaEntity(id = "9", title = "Peli 9", type = "Movie", genres = "Drama, Crime", tags = "biografico, asesinato, carcel")
        )

        val engine = NebulaSpatialEngine()
        engine.buildDynamicUniverse(
            catalog = richCatalog,
            serverUrl = "http://localhost:8096",
            accessToken = "token123",
            sessionSeed = 12345L
        )

        val allNodes = engine.getAllNodes()
        val captains = allNodes.filter { it.isCaptain }

        assertTrue("Deben generarse estrellas capitanas", captains.isNotEmpty())

        // 1. Verificar que ninguna capitana sea de nicho (LGBTQ+, Biográfico, Asesinato, Western, etc.)
        val forbiddenCaptainTags = setOf("lgbtq+", "asesinato", "biografico", "western", "amistad", "musica", "deportes")
        for (captain in captains) {
            assertFalse(
                "La etiqueta de nicho '${captain.label}' no debe ser Capitana principal",
                captain.rawTag in forbiddenCaptainTags
            )
            assertTrue(
                "La capitana '${captain.label}' (${captain.rawTag}) debe pertenecer a los géneros pilares",
                captain.rawTag in PILLAR_GENRES
            )
        }

        // 2. Verificar que las etiquetas de nicho sigan existiendo pero como SATÉLITES
        val lgbtqNode = allNodes.firstOrNull { it.rawTag == "lgbtq+" || it.label.contains("LGBT", ignoreCase = true) }
        if (lgbtqNode != null) {
            assertFalse("LGBTQ+ debe ser un satélite, nunca una Capitana", lgbtqNode.isCaptain)
        }

        // 3. Verificar que ningún nodo esté estampado contra el margen derecho (evitar pared en X >= 0.93)
        for (node in allNodes) {
            assertTrue("El nodo '${node.label}' no debe rebasar el margen derecho (worldX=${node.worldX})", node.worldX <= 0.93f)
            assertTrue("El nodo '${node.label}' no debe rebasar el margen izquierdo (worldX=${node.worldX})", node.worldX >= 0.07f)
            assertTrue("El nodo '${node.label}' no debe rebasar el margen inferior (worldY=${node.worldY})", node.worldY <= 0.91f)
            assertTrue("El nodo '${node.label}' no debe rebasar el margen superior (worldY=${node.worldY})", node.worldY >= 0.09f)
        }
    }
}
