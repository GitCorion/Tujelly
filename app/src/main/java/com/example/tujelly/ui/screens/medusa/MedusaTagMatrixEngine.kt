package com.example.tujelly.ui.screens.medusa

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import java.util.Locale
import kotlin.math.ln

internal fun JellyfinMediaEntity.matchesFormat(format: MediaFormat): Boolean = when (format) {
    MediaFormat.ALL -> true
    MediaFormat.MOVIES -> type.equals("Movie", ignoreCase = true)
    MediaFormat.SERIES -> type.equals("Series", ignoreCase = true)
}

internal fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
    val posterUrl = if (!primaryImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
        val cleanBase = baseUrl.trimEnd('/')
        "$cleanBase/Items/$id/Images/Primary?quality=90&fillWidth=400&fillHeight=600"
    } else null

    val backdropUrl = if (!backdropImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
        val cleanBase = baseUrl.trimEnd('/')
        "$cleanBase/Items/$id/Images/Backdrop/0?quality=90&maxWidth=1920"
    } else posterUrl

    return MediaItem(
        id = id,
        title = title,
        overview = overview,
        type = type,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        rating = communityRating,
        year = productionYear,
        source = MediaSource.JELLYFIN,
        playbackPositionTicks = playbackPositionTicks,
        isPlayed = isPlayed,
        isFavorite = isFavorite,
        totalEpisodes = totalItemCount,
        playedEpisodes = if (unplayedItemCount != null && totalItemCount != null) {
            (totalItemCount - unplayedItemCount).coerceAtLeast(0)
        } else null
    )
}

/**
 * Modelo de datos para cada cápsula / celda del Muro Mosaico de Medusa.
 */
data class MedusaMosaicTag(
    val id: String,
    val label: String,
    val rawTag: String,
    val movieCount: Int,
    val category: String,
    val isCaptain: Boolean = false,
    val isSelected: Boolean = false
)

/**
 * Motor de Matriz de Co-ocurrencia y Reemplazo Dinámico para Medusa.
 * Gestiona el catálogo de obras, indexa todas las etiquetas canónicas y calcula en < 2ms
 * las películas resultantes de una combinación estricta "AND" y el conjunto óptimo de etiquetas
 * co-ocurrentes que deben poblar el mosaico sin dejar huecos vacíos.
 */
class MedusaTagMatrixEngine {

    companion object {
        val PILLAR_GENRES = listOf(
            "accion", "ciencia ficcion", "terror", "comedia", "drama",
            "fantasia", "animacion", "crimen", "romance", "familia",
            "belica", "misterio", "aventura", "suspense", "documental", "historia", "musica", "western"
        )
        const val DEFAULT_MOSAIC_CAPACITY = 32
    }

    private var tokenizedCatalog = listOf<Pair<JellyfinMediaEntity, Set<String>>>()
    private val globalTagCounts = mutableMapOf<String, Int>()
    private val canonTags = mutableSetOf<String>()
    private val trendingTags = mutableSetOf<String>()
    private val recentTags = mutableSetOf<String>()
    private val favTags = mutableSetOf<String>()

    fun initialize(
        catalog: List<JellyfinMediaEntity>,
        tmdbTopRated: List<JellyfinMediaEntity> = emptyList(),
        tmdbTrending: List<JellyfinMediaEntity> = emptyList(),
        recentWatched: List<JellyfinMediaEntity> = emptyList(),
        favorites: List<JellyfinMediaEntity> = emptyList()
    ) {
        tokenizedCatalog = catalog.map { entity ->
            Pair(entity, extractCanonicalTags(entity))
        }

        globalTagCounts.clear()
        for ((_, tags) in tokenizedCatalog) {
            for (tag in tags) {
                globalTagCounts[tag] = (globalTagCounts[tag] ?: 0) + 1
            }
        }

        canonTags.clear()
        canonTags.addAll(tmdbTopRated.flatMap { extractCanonicalTags(it) })

        trendingTags.clear()
        trendingTags.addAll(tmdbTrending.flatMap { extractCanonicalTags(it) })

        recentTags.clear()
        recentTags.addAll(recentWatched.flatMap { extractCanonicalTags(it) })

        favTags.clear()
        favTags.addAll(favorites.flatMap { extractCanonicalTags(it) })
    }

    /**
     * Resuelve las obras que satisfacen estrictamente "AND" para la cadena dada.
     */
    fun getMatchingMovies(
        chain: List<MedusaMosaicTag>,
        format: MediaFormat = MediaFormat.ALL,
        serverUrl: String = "",
        accessToken: String = ""
    ): List<MediaItem> {
        val chainKeys = chain.map { it.rawTag.lowercase() }
        val matchingEntities = tokenizedCatalog.filter { (entity, tags) ->
            entity.matchesFormat(format) && chainKeys.all { key -> tags.contains(key) }
        }.map { it.first }

        return matchingEntities
            .sortedWith(
                compareByDescending<JellyfinMediaEntity> { !it.isPlayed }
                    .thenByDescending { it.communityRating ?: 0f }
                    .thenByDescending { it.productionYear ?: 0 }
            )
            .take(40)
            .map { it.toMediaItem(serverUrl, accessToken) }
    }

    /**
     * Genera las etiquetas dinámicamente con alta vitalidad y especificidad (Lift):
     * - Si no hay cadena: Ofrece un abanico variado que combina géneros clave y temáticas atrayentes.
     * - Si hay temáticas seleccionadas: Calcula la sobrefrecuencia relativa (Lift) para que
     *   surjan micro-temáticas verdaderamente afines (ej. magia, anime, animales, robots) en lugar
     *   de repetir monótonamente siempre los mismos 5 macrogéneros globales.
     */
    fun generateMosaicTags(
        activeChain: List<MedusaMosaicTag>,
        format: MediaFormat = MediaFormat.ALL,
        targetCapacity: Int = DEFAULT_MOSAIC_CAPACITY
    ): List<MedusaMosaicTag> {
        val chainKeys = activeChain.map { it.rawTag.lowercase() }.toSet()
        val formattedCatalog = tokenizedCatalog.filter { (entity, _) -> entity.matchesFormat(format) }
        val totalCatalogCount = formattedCatalog.size.coerceAtLeast(1)

        // 1. Obras que satisfacen la cadena activa
        val matchingPairs = if (chainKeys.isEmpty()) {
            formattedCatalog
        } else {
            formattedCatalog.filter { (_, tags) -> chainKeys.all { key -> tags.contains(key) } }
        }

        if (matchingPairs.isEmpty()) {
            return activeChain
        }

        val matchingCount = matchingPairs.size

        // 2. Conteo de co-ocurrencia para todas las etiquetas presentes en las obras supervivientes
        val cooccurrenceCounts = mutableMapOf<String, Int>()
        for ((_, tags) in matchingPairs) {
            for (tag in tags) {
                if (tag !in chainKeys) {
                    cooccurrenceCounts[tag] = (cooccurrenceCounts[tag] ?: 0) + 1
                }
            }
        }

        // 3. Puntuación de relevancia dinámica con Lift (especificidad)
        val isFirstSelection = chainKeys.isEmpty()

        val scoredCandidates = cooccurrenceCounts.mapNotNull { (tag, count) ->
            if (count < 1 || isBlacklistedTag(tag)) return@mapNotNull null

            val globalCount = (globalTagCounts[tag] ?: count).coerceAtLeast(1)
            val globalRatio = globalCount.toFloat() / totalCatalogCount.toFloat()
            val subRatio = count.toFloat() / matchingCount.toFloat()

            // Lift: cuántas veces más frecuente es este tag en este subconjunto respecto al catálogo global
            val lift = (subRatio / (globalRatio + 0.0001f)).coerceIn(0.2f, 25.0f)

            var score: Float
            if (isFirstSelection) {
                // En reposo: balancear volumen con diversidad para no saturar con solo drama/comedia
                score = ln(count.toFloat() + 1f) * 4.0f
                if (tag in PILLAR_GENRES) score += 4f
                if (tag in listOf("ciencia ficcion", "animacion", "fantasia", "misterio", "aventura", "terror")) score += 6f
            } else {
                // Con selección activa: el Lift es rey para que la lista esté viva y revele
                // micro-temáticas específicas que realmente caracterizan a la selección actual
                score = ln(count.toFloat() + 1f) * 2.2f + (lift * 4.2f)

                if (tag !in PILLAR_GENRES) {
                    // Impulsar temáticas específicas (Magia, Superhéroes, Robots, Animales, Espacio...)
                    score += 5.0f
                } else {
                    // Evitar que macrogéneros genéricos acaparen siempre los primeros puestos
                    score += 1.0f
                }
            }

            if (favTags.contains(tag)) score += 6f
            if (trendingTags.contains(tag)) score += 5f
            if (canonTags.contains(tag)) score += 4f
            if (recentTags.contains(tag)) score += 3f

            Triple(tag, count, score)
        }.sortedByDescending { it.third }

        // 4. Deduplicación por nombre legible en español
        val seenNames = mutableSetOf<String>()
        chainKeys.forEach { key ->
            seenNames.add(TagTranslations.stripAccents(TagTranslations.getDisplayName(key)).lowercase(Locale.ROOT))
        }

        val resultList = mutableListOf<MedusaMosaicTag>()

        // Las etiquetas activas siempre están presentes en primer lugar marcadas como isSelected = true
        activeChain.forEach { active ->
            resultList.add(active.copy(id = "tag_${active.rawTag.lowercase().trim()}", isSelected = true))
        }

        // Rellenar con las mejores etiquetas supervivientes
        for ((rawTag, count, _) in scoredCandidates) {
            if (resultList.size >= targetCapacity) break
            if (!isValidTag(rawTag)) continue
            val displayName = TagTranslations.getDisplayName(rawTag)
            // Doble garantía: la etiqueta visible para el usuario DEBE ser en español
            if (!TagTranslations.hasTranslation(rawTag) && !TagTranslations.isSpanishText(displayName)) continue
            val normalized = TagTranslations.stripAccents(displayName).lowercase(Locale.ROOT)
            if (seenNames.add(normalized)) {
                val isPillar = rawTag in PILLAR_GENRES
                resultList.add(
                    MedusaMosaicTag(
                        id = "tag_${rawTag.lowercase().trim()}",
                        label = displayName,
                        rawTag = rawTag,
                        movieCount = count,
                        category = determineCategory(rawTag),
                        isCaptain = isPillar,
                        isSelected = false
                    )
                )
            }
        }

        return resultList
    }

    private fun isValidTag(tag: String): Boolean {
        val clean = tag.lowercase(Locale.ROOT).trim()
        if (clean.length < 3) return false
        if (isBlacklistedTag(clean)) return false
        // Estricto: DEBE tener una traducción conocida al español O ser un término nativo reconocido en español
        return TagTranslations.hasTranslation(clean) || TagTranslations.isSpanishText(clean)
    }

    private fun determineCategory(rawTag: String): String {
        val clean = TagTranslations.stripAccents(rawTag).lowercase(Locale.ROOT)
        return when {
            clean.contains("accion") || clean.contains("action") -> "Acción"
            clean.contains("terror") || clean.contains("horror") || clean.contains("zombi") || clean.contains("slasher") || clean.contains("asesin") -> "Terror"
            clean.contains("ciencia") || clean.contains("sci-fi") || clean.contains("alien") || clean.contains("robot") || clean.contains("cyberpunk") || clean.contains("espacio") -> "Ciencia Ficción"
            clean.contains("fantasia") || clean.contains("fantasy") || clean.contains("magia") || clean.contains("monstruo") -> "Fantasía"
            clean.contains("comedia") || clean.contains("comedy") || clean.contains("humor") -> "Comedia"
            clean.contains("drama") || clean.contains("melodrama") -> "Drama"
            clean.contains("romance") || clean.contains("amor") || clean.contains("romcom") -> "Romance"
            clean.contains("animacion") || clean.contains("animation") || clean.contains("anime") || clean.contains("manga") -> "Animación"
            clean.contains("documental") || clean.contains("documentary") || clean.contains("historia") || clean.contains("biograf") -> "Documental"
            clean.contains("crimen") || clean.contains("crime") || clean.contains("misterio") || clean.contains("mystery") || clean.contains("polic") -> "Crimen"
            clean.contains("aventura") || clean.contains("adventure") || clean.contains("supervivencia") -> "Aventura"
            clean.contains("belica") || clean.contains("war") || clean.contains("guerra") -> "Bélica"
            clean.contains("musical") || clean.contains("musica") || clean.contains("music") -> "Musical"
            clean.contains("western") -> "Western"
            clean.contains("familia") || clean.contains("family") || clean.contains("infantil") -> "Familia"
            else -> "Temática"
        }
    }

    private fun extractCanonicalTags(entity: JellyfinMediaEntity): Set<String> {
        val rawTokens = mutableSetOf<String>()
        entity.tags?.split(",")?.forEach { t ->
            val clean = t.trim().lowercase(Locale.ROOT)
            if (clean.length >= 3) {
                for (decomposed in decomposeCompositeGenre(clean)) {
                    if (isValidTag(decomposed)) rawTokens.add(decomposed)
                }
            }
        }
        entity.genres?.split(",")?.forEach { g ->
            val clean = g.trim().lowercase(Locale.ROOT)
            if (clean.length >= 3) {
                for (decomposed in decomposeCompositeGenre(clean)) {
                    if (isValidTag(decomposed)) rawTokens.add(decomposed)
                }
            }
        }

        val canonicalSet = mutableSetOf<String>()
        for (raw in rawTokens) {
            val displayName = TagTranslations.getDisplayName(raw)
            val canonicalKey = TagTranslations.stripAccents(displayName).lowercase(Locale.ROOT)
            if (!isBlacklistedTag(canonicalKey) && (TagTranslations.hasTranslation(canonicalKey) || TagTranslations.isSpanishText(displayName))) {
                canonicalSet.add(canonicalKey)
            }
        }
        return canonicalSet
    }

    private fun decomposeCompositeGenre(raw: String): List<String> {
        val clean = raw.trim().lowercase(Locale.ROOT)
        val unaccented = TagTranslations.stripAccents(clean)
        return when {
            (unaccented.contains("sci-fi") || unaccented.contains("scifi") ||
             unaccented.contains("science fiction") || unaccented.contains("ciencia ficcion")) &&
            (unaccented.contains("fantasy") || unaccented.contains("fantasia")) -> {
                listOf("ciencia ficcion", "fantasia")
            }
            (unaccented.contains("action") || unaccented.contains("accion")) &&
            (unaccented.contains("adventure") || unaccented.contains("aventura")) -> {
                listOf("accion", "aventura")
            }
            (unaccented.contains("war") || unaccented.contains("guerra") || unaccented.contains("belica")) &&
            (unaccented.contains("politics") || unaccented.contains("politica")) -> {
                listOf("belica", "politica")
            }
            else -> listOf(clean)
        }
    }

    private fun isBlacklistedTag(tag: String): Boolean {
        val lower = tag.lowercase(Locale.ROOT).trim()
        val unaccented = TagTranslations.stripAccents(lower)
        if (unaccented.length < 3) return true
        if (unaccented == "pelicula" || unaccented == "movie" || unaccented == "series" || unaccented == "tv" || unaccented == "general") return true

        // Formatos y metadata de scrapers técnicos
        if (lower.contains("creditsstinger") ||
            lower.contains("aftercredits") ||
            lower.contains("duringcredits") ||
            lower.contains("short film") ||
            lower.contains("cortometraje") ||
            lower.contains("feature length") ||
            lower.contains("stand-alone") ||
            lower.contains("special") ||
            lower.contains("remake") ||
            lower.contains("reboot") ||
            lower.contains("sequel") ||
            lower.contains("prequel") ||
            lower.contains("spin-off") ||
            lower.contains("live action") ||
            lower.contains("director") ||
            lower.contains("protagonist") ||
            lower.contains("based on") ||
            lower.contains("miniseries") ||
            lower.contains("anthology") ||
            lower.contains("tv movie") ||
            lower.contains("telefilme") ||
            lower.contains("made for tv") ||
            lower.contains("original net animation") ||
            lower.contains("(ona)") ||
            lower.contains("(ova)") ||
            lower == "ona" ||
            lower == "ova" ||
            lower.contains("episode") ||
            lower.contains("season")
        ) return true

        // Contenido no apto o no temático
        if (lower in setOf("rape", "suicide", "nudity", "sexual abuse", "gore", "violence", "pedophilia", "incest")) return true

        // Descomposición de géneros compuestos redundantes
        if ((unaccented.contains("ciencia ficcion") || unaccented.contains("sci-fi") || unaccented.contains("scifi")) &&
            (unaccented.contains("fantasia") || unaccented.contains("fantasy"))) return true
        if ((unaccented.contains("accion") || unaccented.contains("action")) &&
            (unaccented.contains("aventura") || unaccented.contains("adventure"))) return true
        if ((unaccented.contains("war") || unaccented.contains("guerra") || unaccented.contains("belica")) &&
            (unaccented.contains("politics") || unaccented.contains("politica"))) return true

        return false
    }
}
