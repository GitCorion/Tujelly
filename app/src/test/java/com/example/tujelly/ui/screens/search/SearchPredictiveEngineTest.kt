package com.example.tujelly.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPredictiveEngineTest {

    private val sampleCatalog = listOf(
        "Batman Begins",
        "The Batman",
        "Spider-Man",
        "Spider-Man: No Way Home",
        "Breaking Bad",
        "Avatar",
        "Avatar: The Way of Water",
        "Avengers: Endgame",
        "El Señor de los Anillos: La Comunidad del Anillo",
        "1917",
        "2001: A Space Odyssey"
    )

    @Test
    fun testNormalizeRemovesAccentsAndPunctuation() {
        assertEquals("el senor de los anillos", SearchPredictiveEngine.normalize("El Señor de los Anillos"))
        assertEquals("spider man no way home", SearchPredictiveEngine.normalize("Spider-Man: No Way Home"))
        assertEquals("amelie", SearchPredictiveEngine.normalize("Amélie"))
    }

    @Test
    fun testEmptyQueryReturnsStartingLetters() {
        val nextChars = SearchPredictiveEngine.findValidNextCharacters("", sampleCatalog)
        assertTrue(nextChars.contains('B')) // Batman, Breaking
        assertTrue(nextChars.contains('S')) // Spider-Man, Señor
        assertTrue(nextChars.contains('A')) // Avatar, Avengers
        assertTrue(nextChars.contains('1')) // 1917
        assertTrue(nextChars.contains('2')) // 2001
        assertTrue(nextChars.contains('T')) // The
    }

    @Test
    fun testPs4KeyNarrowingAfterPrefix() {
        // As you type "BAT", in sampleCatalog we have "Batman Begins" and "The Batman"
        // After "bat", next char in "batman" is 'M'
        val nextChars = SearchPredictiveEngine.findValidNextCharacters("BAT", sampleCatalog)
        assertTrue(nextChars.contains('M'))
        assertFalse(nextChars.contains('Z'))
        assertFalse(nextChars.contains('X'))
        assertFalse(nextChars.contains('Q'))
    }

    @Test
    fun testPs4KeyNarrowingAvatarVsAvengers() {
        // Query "AV" -> "Avatar" (next char 'A'), "Avengers" (next char 'E')
        val nextChars = SearchPredictiveEngine.findValidNextCharacters("AV", sampleCatalog)
        assertTrue(nextChars.contains('A')) // Avatar
        assertTrue(nextChars.contains('E')) // Avengers
        assertFalse(nextChars.contains('B'))
        assertFalse(nextChars.contains('C'))
    }

    @Test
    fun testAutocompleteSuggestions() {
        val suggestions = SearchPredictiveEngine.generateAutocompleteSuggestions("spi", sampleCatalog, limit = 3)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("Spider-Man", suggestions[0])
    }

    @Test
    fun testAutocompleteAccentInsensitive() {
        val suggestions = SearchPredictiveEngine.generateAutocompleteSuggestions("senor", sampleCatalog, limit = 1)
        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].contains("Señor"))
    }

    @Test
    fun testDirectionResolverExactUserCaseCPL() {
        val activeChars = setOf('C', 'L', 'P')

        // 1. Desde 'C' hacia la DERECHA debe ir a 'P' (columna 3), no saltar al panel derecho ni a 'L'
        val fromC = KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.RIGHT, activeChars)
        assertEquals('P', fromC)

        // 2. Desde 'P' hacia la DERECHA debe ir a 'L' (columna 5)
        val fromP = KeyboardDirectionResolver.findNextKey('P', KeyboardDirectionResolver.Direction.RIGHT, activeChars)
        assertEquals('L', fromP)

        // 3. Desde 'L' hacia la DERECHA debe devolver null (para salir naturalmente al panel de resultados)
        val fromL = KeyboardDirectionResolver.findNextKey('L', KeyboardDirectionResolver.Direction.RIGHT, activeChars)
        assertEquals(null, fromL)

        // 4. Navegación hacia la IZQUIERDA
        assertEquals('P', KeyboardDirectionResolver.findNextKey('L', KeyboardDirectionResolver.Direction.LEFT, activeChars))
        assertEquals('C', KeyboardDirectionResolver.findNextKey('P', KeyboardDirectionResolver.Direction.LEFT, activeChars))
        assertEquals(null, KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.LEFT, activeChars))

        // 5. Navegación VERTICAL
        assertEquals('P', KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.DOWN, activeChars))
        assertEquals(null, KeyboardDirectionResolver.findNextKey('P', KeyboardDirectionResolver.Direction.DOWN, activeChars))

        // 6. Transición a botones de acción desde 'P'
        val activeActions = setOf(
            KeyboardDirectionResolver.ActionKey.BACKSPACE,
            KeyboardDirectionResolver.ActionKey.CLEAR
        )
        val actionFromP = KeyboardDirectionResolver.findNextActionKey('P', activeActions)
        assertEquals(KeyboardDirectionResolver.ActionKey.BACKSPACE, actionFromP)

        // 7. Transición desde botón de acción hacia arriba a la letra más cercana
        val letterFromBackspace = KeyboardDirectionResolver.findNextLetterFromAction(
            KeyboardDirectionResolver.ActionKey.BACKSPACE.col,
            activeChars
        )
        assertEquals('P', letterFromBackspace)
    }

    @Test
    fun testDirectionResolverFullKeyboardNormalGrid() {
        val allChars = KeyboardDirectionResolver.ALL_CHARS.toSet()

        // En cuadrícula completa normal:
        // C(0, 2) -> Derecha es D(0, 3), Izquierda es B(0, 1), Abajo es I(1, 2)
        assertEquals('D', KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.RIGHT, allChars))
        assertEquals('B', KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.LEFT, allChars))
        assertEquals('I', KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.DOWN, allChars))
        assertEquals(null, KeyboardDirectionResolver.findNextKey('C', KeyboardDirectionResolver.Direction.UP, allChars))
    }

    @Test
    fun testIndexedCatalogBuildsCorrectlyAndYieldsIdenticalResults() {
        val indexed = SearchPredictiveEngine.IndexedCatalog.build(sampleCatalog)
        assertEquals(sampleCatalog.size, indexed.titles.size)
        assertTrue(indexed.allStartingChars.contains('B'))
        assertTrue(indexed.allStartingChars.contains('A'))
        assertTrue(indexed.allStartingChars.contains('S'))

        // Verificar que con consulta vacía devuelve allStartingChars directamente
        val nextEmpty = SearchPredictiveEngine.findValidNextCharacters("", indexed)
        assertEquals(indexed.allStartingChars, nextEmpty)

        // Verificar que con "BAT" produce las mismas letras que la lista directa
        val nextBatDirect = SearchPredictiveEngine.findValidNextCharacters("BAT", sampleCatalog)
        val nextBatIndexed = SearchPredictiveEngine.findValidNextCharacters("BAT", indexed)
        assertEquals(nextBatDirect, nextBatIndexed)

        // Verificar sugerencias idénticas
        val sugDirect = SearchPredictiveEngine.generateAutocompleteSuggestions("spi", sampleCatalog, 3)
        val sugIndexed = SearchPredictiveEngine.generateAutocompleteSuggestions("spi", indexed, 3)
        assertEquals(sugDirect, sugIndexed)
    }

    @Test
    fun testStaticFullGridDirectionsMatchCalculatedDirections() {
        val allChars = KeyboardDirectionResolver.ALL_CHARS.toSet()
        for (ch in KeyboardDirectionResolver.ALL_CHARS) {
            for (dir in KeyboardDirectionResolver.Direction.values()) {
                val staticResult = KeyboardDirectionResolver.STATIC_FULL_GRID_DIRECTIONS[ch to dir]
                val resolverResult = KeyboardDirectionResolver.findNextKey(ch, dir, allChars)
                assertEquals("Mismatch for char $ch in dir $dir", staticResult, resolverResult)
            }
        }
    }
}

