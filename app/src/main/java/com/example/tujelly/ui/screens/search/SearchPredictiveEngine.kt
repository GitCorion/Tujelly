package com.example.tujelly.ui.screens.search

import java.text.Normalizer
import java.util.Locale

object SearchPredictiveEngine {

    val ALL_KEYBOARD_CHARS: Set<Char> = (('A'..'Z') + ('0'..'9')).toSet()

    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val SPACES_REGEX = Regex("\\s+")

    /**
     * Normaliza el texto de forma ultrarrápida: pasa a minúsculas, elimina diacríticos/tildes
     * (á->a, é->e, etc.), reemplaza caracteres especiales por espacios y comprime espacios múltiples.
     * Reutiliza regex precompilados y evita asignaciones intermedias para no saturar el Garbage Collector.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS_REGEX.replace(nfd, "")
        val sb = StringBuilder(withoutDiacritics.length)
        for (i in 0 until withoutDiacritics.length) {
            val ch = withoutDiacritics[i].lowercaseChar()
            if (ch.isLetterOrDigit() || ch == ' ') {
                sb.append(ch)
            } else {
                sb.append(' ')
            }
        }
        return SPACES_REGEX.replace(sb.toString(), " ").trim()
    }

    data class IndexedTitle(
        val rawTitle: String,
        val normalizedTitle: String,
        val words: List<String>,
        val startingChars: Set<Char>
    )

    data class IndexedCatalog(
        val titles: List<IndexedTitle>,
        val allStartingChars: Set<Char>
    ) {
        companion object {
            val EMPTY = IndexedCatalog(emptyList(), ALL_KEYBOARD_CHARS)

            fun build(rawTitles: List<String>): IndexedCatalog {
                if (rawTitles.isEmpty()) return EMPTY

                val allStarting = mutableSetOf<Char>()
                val indexedList = ArrayList<IndexedTitle>(rawTitles.size)

                for (raw in rawTitles) {
                    val norm = normalize(raw)
                    if (norm.isEmpty()) continue

                    val words = norm.split(' ').filter { it.isNotEmpty() }
                    val starting = mutableSetOf<Char>()

                    val first = norm.first().uppercaseChar()
                    if (first in ALL_KEYBOARD_CHARS) {
                        starting.add(first)
                        allStarting.add(first)
                    }

                    for (w in words) {
                        val wFirst = w.first().uppercaseChar()
                        if (wFirst in ALL_KEYBOARD_CHARS) {
                            starting.add(wFirst)
                            allStarting.add(wFirst)
                        }
                    }

                    indexedList.add(IndexedTitle(raw, norm, words, starting))
                }

                val finalStarting = if (allStarting.isNotEmpty()) allStarting else ALL_KEYBOARD_CHARS
                return IndexedCatalog(indexedList, finalStarting)
            }
        }
    }

    /**
     * Versión de alto rendimiento de findValidNextCharacters usando el catálogo pre-indexado.
     * Tiempo O(1) si query está vacío, O(N) directo sin re-normalizaciones en cada pulsación.
     */
    fun findValidNextCharacters(query: String, catalog: IndexedCatalog): Set<Char> {
        val q = normalize(query)

        if (q.isEmpty()) {
            return catalog.allStartingChars
        }

        val nextChars = mutableSetOf<Char>()

        for (item in catalog.titles) {
            val norm = item.normalizedTitle

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
            for (word in item.words) {
                if (word.startsWith(q)) {
                    val nextIdx = q.length
                    if (nextIdx < word.length) {
                        val upper = word[nextIdx].uppercaseChar()
                        if (upper in ALL_KEYBOARD_CHARS) {
                            nextChars.add(upper)
                        }
                    } else if (nextIdx == word.length) {
                        nextChars.add(' ')
                    }
                }
            }

            // 3. Coincidencias dentro de la cadena (subcadena tras espacio o inicio)
            var startIndex = 0
            while (true) {
                val foundIndex = norm.indexOf(q, startIndex)
                if (foundIndex == -1) break
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
     * Sobrecarga retrocompatible para listas sin indexar (usado en tests unitarios rápidos).
     */
    fun findValidNextCharacters(query: String, catalogTitles: List<String>): Set<Char> {
        val catalog = IndexedCatalog.build(catalogTitles)
        return findValidNextCharacters(query, catalog)
    }

    /**
     * Genera sugerencias de autocompletado de alto rendimiento sobre el catálogo indexado.
     */
    fun generateAutocompleteSuggestions(
        query: String,
        catalog: IndexedCatalog,
        limit: Int = 5
    ): List<String> {
        val q = normalize(query)
        if (q.isEmpty() || catalog.titles.isEmpty()) return emptyList()

        val scored = catalog.titles.mapNotNull { item ->
            val norm = item.normalizedTitle
            val score = when {
                norm == q -> 100
                norm.startsWith(q) -> 80
                item.words.any { it.startsWith(q) } -> 60
                norm.contains(q) -> 40
                else -> -1
            }
            if (score > 0) {
                ScoredTitle(item.rawTitle, score, norm.length)
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

    /**
     * Sobrecarga retrocompatible para listas sin indexar (usado en tests unitarios).
     */
    fun generateAutocompleteSuggestions(
        query: String,
        catalogTitles: List<String>,
        limit: Int = 5
    ): List<String> {
        val catalog = IndexedCatalog.build(catalogTitles)
        return generateAutocompleteSuggestions(query, catalog, limit)
    }

    private data class ScoredTitle(
        val title: String,
        val score: Int,
        val length: Int
    )
}

