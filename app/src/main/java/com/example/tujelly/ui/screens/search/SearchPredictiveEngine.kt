package com.example.tujelly.ui.screens.search

import java.text.Normalizer
import java.util.Locale

object SearchPredictiveEngine {

    val ALL_KEYBOARD_CHARS: Set<Char> = (('A'..'Z') + ('0'..'9')).toSet()

    /**
     * Normaliza el texto: pasa a minúsculas, elimina diacríticos/tildes (á->a, é->e, etc.),
     * reemplaza caracteres especiales por espacios y comprime espacios múltiples.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutDiacritics.lowercase(Locale.ROOT)
            .map { ch -> if (ch.isLetterOrDigit() || ch == ' ') ch else ' ' }
            .joinToString("")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Calcula el conjunto de siguientes caracteres válidos para el teclado en pantalla.
     * Si query está vacío, devuelve todas las letras iniciales de títulos y palabras.
     * Si query tiene texto, busca en el catálogo qué letras continúan las coincidencias (estilo PS4).
     */
    fun findValidNextCharacters(query: String, catalogTitles: List<String>): Set<Char> {
        val q = normalize(query)

        if (q.isEmpty()) {
            val startingChars = mutableSetOf<Char>()
            for (title in catalogTitles) {
                val norm = normalize(title)
                if (norm.isNotEmpty()) {
                    val first = norm.first().uppercaseChar()
                    if (first in ALL_KEYBOARD_CHARS) {
                        startingChars.add(first)
                    }
                    // También iniciales de cada palabra
                    val words = norm.split(' ')
                    for (word in words) {
                        if (word.isNotEmpty()) {
                            val wordFirst = word.first().uppercaseChar()
                            if (wordFirst in ALL_KEYBOARD_CHARS) {
                                startingChars.add(wordFirst)
                            }
                        }
                    }
                }
            }
            return if (startingChars.isNotEmpty()) startingChars else ALL_KEYBOARD_CHARS
        }

        val nextChars = mutableSetOf<Char>()

        for (title in catalogTitles) {
            val norm = normalize(title)
            if (norm.isEmpty()) continue

            // 1. Coincidencia al inicio del título
            if (norm.startsWith(q)) {
                val nextIdx = q.length
                if (nextIdx < norm.length) {
                    val nextChar = norm[nextIdx]
                    if (nextChar == ' ' && nextIdx + 1 < norm.length) {
                        nextChars.add(' ')
                        val afterSpace = norm[nextIdx + 1].uppercaseChar()
                        if (afterSpace in ALL_KEYBOARD_CHARS) {
                            nextChars.add(afterSpace)
                        }
                    } else {
                        val upper = nextChar.uppercaseChar()
                        if (upper in ALL_KEYBOARD_CHARS || upper == ' ') {
                            nextChars.add(upper)
                        }
                    }
                }
            }

            // 2. Coincidencia al inicio de alguna palabra dentro del título
            val words = norm.split(' ')
            for (word in words) {
                if (word.startsWith(q)) {
                    val nextIdx = q.length
                    if (nextIdx < word.length) {
                        val upper = word[nextIdx].uppercaseChar()
                        if (upper in ALL_KEYBOARD_CHARS) {
                            nextChars.add(upper)
                        }
                    } else if (nextIdx == word.length) {
                        // La palabra termina aquí; puede seguir un espacio
                        nextChars.add(' ')
                    }
                }
            }

            // 3. Coincidencias dentro de la cadena (subcadena tras espacio o inicio)
            var startIndex = 0
            while (true) {
                val foundIndex = norm.indexOf(q, startIndex)
                if (foundIndex == -1) break
                // Solo si es inicio de título o inicio de palabra
                val isWordStart = (foundIndex == 0 || norm[foundIndex - 1] == ' ')
                if (isWordStart) {
                    val nextIdx = foundIndex + q.length
                    if (nextIdx < norm.length) {
                        val nextChar = norm[nextIdx]
                        if (nextChar == ' ' && nextIdx + 1 < norm.length) {
                            nextChars.add(' ')
                            val afterSpace = norm[nextIdx + 1].uppercaseChar()
                            if (afterSpace in ALL_KEYBOARD_CHARS) {
                                nextChars.add(afterSpace)
                            }
                        } else {
                            val upper = nextChar.uppercaseChar()
                            if (upper in ALL_KEYBOARD_CHARS || upper == ' ') {
                                nextChars.add(upper)
                            }
                        }
                    }
                }
                startIndex = foundIndex + 1
            }
        }

        return nextChars
    }

    /**
     * Genera sugerencias de autocompletado en tiempo real basadas en la consulta actual.
     */
    fun generateAutocompleteSuggestions(
        query: String,
        catalogTitles: List<String>,
        limit: Int = 5
    ): List<String> {
        val q = normalize(query)
        if (q.isEmpty() || catalogTitles.isEmpty()) return emptyList()

        val scored = catalogTitles.mapNotNull { rawTitle ->
            val norm = normalize(rawTitle)
            val score = when {
                norm == q -> 100
                norm.startsWith(q) -> 80
                norm.split(' ').any { it.startsWith(q) } -> 60
                norm.contains(q) -> 40
                else -> -1
            }
            if (score > 0) {
                ScoredTitle(rawTitle, score, norm.length)
            } else {
                null
            }
        }

        return scored
            .sortedWith(compareByDescending<ScoredTitle> { it.score }.thenBy { it.length })
            .map { it.title }
            .distinct()
            .take(limit)
    }

    private data class ScoredTitle(
        val title: String,
        val score: Int,
        val length: Int
    )
}
