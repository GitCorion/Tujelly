package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NebulaSpatialEngineTest {

    @Test
    fun testPortalCreationAndNavigation() {
        val engine = NebulaSpatialEngine()
        val testCatalog = listOf(
            JellyfinMediaEntity(
                id = "m1",
                title = "Blade Runner 2049",
                type = "Movie",
                genres = "Sci-Fi, Mystery, Drama",
                tags = "noir, cyberpunk, detective, futuristic",
                overview = "A young blade runner discovers a long-buried secret.",
                communityRating = 8.0f
            ),
            JellyfinMediaEntity(
                id = "m2",
                title = "The Godfather",
                type = "Movie",
                genres = "Crime, Drama",
                tags = "mafia, family, classic",
                overview = "The aging patriarch of an organized crime dynasty transfers control to his reluctant son.",
                communityRating = 9.2f
            )
        )

        engine.buildUniverse(testCatalog, "http://localhost:8096", "token123")
        val allNodes = engine.getAllNodes()
        assertTrue("Universe should have generated cluster and tag nodes", allNodes.isNotEmpty())

        // When chain is empty, portal should NOT exist
        engine.updatePortalNode(chain = emptyList(), movieCount = 0)
        val initialPortal = engine.getNode(PORTAL_NODE_ID)
        org.junit.Assert.assertNull("Portal node should not exist when chain is empty", initialPortal)

        // Portal creation with active chain
        val cluster0 = engine.getNode("CLUSTER_0")!!
        val tagNode = engine.getNode("TAG_0_0") ?: allNodes.first { it.id.startsWith("TAG_") }
        val chain = listOf(cluster0, tagNode)

        engine.updatePortalNode(chain = chain, movieCount = 5)
        val updatedPortal = engine.getNode(PORTAL_NODE_ID)
        assertNotNull(updatedPortal)
        assertTrue(updatedPortal!!.isPortal)
        assertEquals(5, updatedPortal.movieCount)

        // Verify that filaments connect chain nodes to portal
        val filaments = engine.getAllFilaments()
        val portalFilaments = filaments.filter { it.fromNodeId == PORTAL_NODE_ID || it.toNodeId == PORTAL_NODE_ID }
        assertTrue("Portal should have filaments connecting it to active chain", portalFilaments.isNotEmpty())

        // Verify DPad navigation can reach the portal
        val neighbor = engine.findNextNeighbor(tagNode.id, DPadDirection.DOWN, 1.0f)
            ?: engine.findNextNeighbor(tagNode.id, DPadDirection.RIGHT, 1.0f)
            ?: engine.findNextNeighbor(tagNode.id, DPadDirection.LEFT, 1.0f)
            ?: engine.findNextNeighbor(tagNode.id, DPadDirection.UP, 1.0f)

        assertNotNull("Neighbor search should find adjacent nodes", neighbor)
    }

    @Test
    fun testHistoriasRealesAndDramaFamiliarMatching() {
        val engine = NebulaSpatialEngine()
        val catalog = listOf(
            JellyfinMediaEntity(
                id = "m1",
                title = "En busca de la felicidad",
                type = "Movie",
                genres = "Drama, Biografía",
                tags = "superación, familia, emotiva",
                overview = "Basada en una historia real de un padre que lucha junto a su hijo por salir adelante y cuidar de su familia.",
                communityRating = 8.0f
            ),
            JellyfinMediaEntity(
                id = "m2",
                title = "Alien",
                type = "Movie",
                genres = "Terror, Ciencia Ficción",
                tags = "monstruo, espacio",
                overview = "Una criatura alienígena aterroriza a la tripulación de una nave espacial en el vacío exterior.",
                communityRating = 8.5f
            )
        )

        engine.buildUniverse(catalog, "http://localhost:8096", "token123")
        val allNodes = engine.getAllNodes()

        val historiasRealesNode = allNodes.firstOrNull { it.label == "Historias Reales" }
        assertNotNull("Debe existir el nodo de Historias Reales", historiasRealesNode)

        val dramaFamiliarNode = allNodes.firstOrNull { it.label == "Drama Familiar" }
        assertNotNull("Debe existir el nodo de Drama Familiar", dramaFamiliarNode)

        val chain = listOf(historiasRealesNode!!, dramaFamiliarNode!!)
        val matches = engine.getMatchingMoviesForChain(chain, catalog, "http://localhost:8096", "token123")

        assertTrue("La combinación 'Historias Reales' + 'Drama Familiar' debe encontrar películas coincidentes", matches.isNotEmpty())
        assertEquals("En busca de la felicidad", matches.first().title)

        engine.updatePortalNode(chain, matches.size)
        val portal = engine.getNode(PORTAL_NODE_ID)
        assertNotNull("El nodo Portal debe existir y estar activo", portal)
        assertTrue("El nodo Portal debe tener conteo mayor a 0", portal!!.movieCount > 0)
    }

    @Test
    fun testIncompatibleTagsCannotBeJoined() {
        val engine = NebulaSpatialEngine()
        val catalog = listOf(
            JellyfinMediaEntity(
                id = "m1",
                title = "En busca de la felicidad",
                type = "Movie",
                genres = "Drama, Biografía",
                tags = "superación, familia, emotiva",
                overview = "Basada en una historia real de un padre que lucha junto a su hijo por salir adelante y cuidar de su familia.",
                communityRating = 8.0f
            ),
            JellyfinMediaEntity(
                id = "m2",
                title = "Mi Vecino Totoro",
                type = "Movie",
                genres = "Animación, Fantasía",
                tags = "espíritus, criaturas del bosque, viajes fantásticos",
                overview = "Dos niñas descubren criaturas mágicas y espíritus en un bosque encantado en un viaje fantástico.",
                communityRating = 8.2f
            )
        )

        engine.buildUniverse(catalog, "http://localhost:8096", "token123")
        val allNodes = engine.getAllNodes()

        val historiasRealesNode = allNodes.first { it.label == "Historias Reales" }
        val dramaFamiliarNode = allNodes.first { it.label == "Drama Familiar" }
        val criaturasBosqueNode = allNodes.first { it.label == "Criaturas del Bosque" }
        val viajesFantasticosNode = allNodes.first { it.label == "Viajes Fantásticos" }

        // 1. "Historias Reales" + "Drama Familiar" SÍ debe ser compatible (ambas aplican a m1)
        val canJoinDrama = engine.canExtendChain(listOf(historiasRealesNode), dramaFamiliarNode)
        assertTrue("Drama Familiar debe ser compatible con Historias Reales", canJoinDrama)

        // 2. "Historias Reales" + "Drama Familiar" con "Criaturas del Bosque" NO debe poder juntarse
        val chain = listOf(historiasRealesNode, dramaFamiliarNode)
        val canJoinCriaturas = engine.canExtendChain(chain, criaturasBosqueNode)
        org.junit.Assert.assertFalse("Criaturas del Bosque NO debe poder unirse a Historias Reales + Drama Familiar", canJoinCriaturas)

        val canJoinViajes = engine.canExtendChain(chain, viajesFantasticosNode)
        org.junit.Assert.assertFalse("Viajes Fantásticos NO debe poder unirse a Historias Reales + Drama Familiar", canJoinViajes)

        // 3. Los IDs compatibles deben excluir las criaturas fantásticas
        val compatibleIds = engine.getCompatibleNodeIds(chain)
        org.junit.Assert.assertFalse(compatibleIds.contains(criaturasBosqueNode.id))
        org.junit.Assert.assertFalse(compatibleIds.contains(viajesFantasticosNode.id))

        // 4. Si se forzara la cadena de 4 etiquetas, el catálogo debe dar 0 películas (intersección pura)
        val fullChain = listOf(historiasRealesNode, dramaFamiliarNode, criaturasBosqueNode, viajesFantasticosNode)
        val matches = engine.getMatchingMoviesForChain(fullChain, catalog, "http://localhost:8096", "token123")
        assertTrue("La intersección forzada de etiquetas incompatibles debe devolver 0 películas", matches.isEmpty())
    }
}
