package com.example.tujelly.util

import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource

/**
 * Utilidades para construir URLs de imágenes optimizadas para Jellyfin y TMDb.
 *
 * Jellyfin soporta redimensionado y compresión al vuelo en el servidor:
 * - fillWidth / fillHeight: ajusta exactamente a las dimensiones de la tarjeta TV.
 * - maxWidth / maxHeight: limita el tamaño máximo de backdrops/fanart.
 * - quality: compresión JPEG/WebP (80-85% produce una reducción de ~95% en peso sin pérdida visual perceptible en TV).
 * - tag: permite validación ETag / 304 Not Modified y caché inmutable en Coil.
 */
object JellyfinImageUtils {

    /**
     * Construye la URL de póster optimizada para tarjetas verticales (150x225 dp en TV ≈ 360x540 px).
     */
    fun getPosterUrl(
        baseUrl: String,
        itemId: String,
        imageTag: String? = null,
        token: String = "",
        width: Int = 360,
        height: Int = 540,
        quality: Int = 85
    ): String? {
        if (!imageTag.isNullOrBlank() && imageTag.startsWith("tmdb:")) {
            val path = imageTag.removePrefix("tmdb:")
            return "https://image.tmdb.org/t/p/w500$path"
        }
        if (baseUrl.isBlank() || itemId.isBlank()) return null

        val cleanBase = baseUrl.trimEnd('/')
        val params = mutableListOf<String>()
        if (token.isNotBlank()) params.add("api_key=$token")
        if (!imageTag.isNullOrBlank()) params.add("tag=$imageTag")
        params.add("fillWidth=$width")
        params.add("fillHeight=$height")
        params.add("quality=$quality")

        val query = params.joinToString("&")
        return "$cleanBase/Items/$itemId/Images/Primary?$query"
    }

    /**
     * Construye la URL de backdrop optimizada para fondos de tarjetas, ambient backlight y HeroBanner
     * (1280 px de ancho máximo en calidad 80 reduce el peso de 5-10 MB a < 90 KB).
     */
    fun getBackdropUrl(
        baseUrl: String,
        itemId: String,
        imageTag: String? = null,
        token: String = "",
        maxWidth: Int = 1280,
        quality: Int = 80
    ): String? {
        if (!imageTag.isNullOrBlank() && imageTag.startsWith("tmdb:")) {
            val path = imageTag.removePrefix("tmdb:")
            return "https://image.tmdb.org/t/p/w1280$path"
        }
        if (baseUrl.isBlank() || itemId.isBlank()) return null

        val cleanBase = baseUrl.trimEnd('/')
        val params = mutableListOf<String>()
        if (token.isNotBlank()) params.add("api_key=$token")
        if (!imageTag.isNullOrBlank()) params.add("tag=$imageTag")
        params.add("maxWidth=$maxWidth")
        params.add("quality=$quality")

        val query = params.joinToString("&")
        return "$cleanBase/Items/$itemId/Images/Backdrop/0?$query"
    }

    /**
     * Construye la URL de backdrop en alta definición (1080p) para la pantalla de detalle.
     */
    fun getDetailBackdropUrl(
        baseUrl: String,
        itemId: String,
        imageTag: String? = null,
        token: String = "",
        maxWidth: Int = 1920,
        quality: Int = 85
    ): String? {
        return getBackdropUrl(baseUrl, itemId, imageTag, token, maxWidth = maxWidth, quality = quality)
    }

    /**
     * Construye la URL optimizada para miniaturas de episodios (16:9, ~480x270 px).
     */
    fun getEpisodeThumbnailUrl(
        baseUrl: String,
        itemId: String,
        imageTag: String? = null,
        token: String = "",
        width: Int = 480,
        height: Int = 270,
        quality: Int = 80
    ): String? {
        if (baseUrl.isBlank() || itemId.isBlank()) return null

        val cleanBase = baseUrl.trimEnd('/')
        val params = mutableListOf<String>()
        if (token.isNotBlank()) params.add("api_key=$token")
        if (!imageTag.isNullOrBlank()) params.add("tag=$imageTag")
        params.add("fillWidth=$width")
        params.add("fillHeight=$height")
        params.add("quality=$quality")

        val query = params.joinToString("&")
        return "$cleanBase/Items/$itemId/Images/Primary?$query"
    }

    /**
     * Construye la URL para ClearLogos optimizados (PNG transparente, máx 400 px ancho).
     */
    fun getLogoUrl(
        baseUrl: String,
        itemId: String,
        token: String = "",
        maxWidth: Int = 400,
        quality: Int = 85
    ): String? {
        if (baseUrl.isBlank() || itemId.isBlank()) return null
        val cleanBase = baseUrl.trimEnd('/')
        val params = mutableListOf<String>()
        if (token.isNotBlank()) params.add("api_key=$token")
        params.add("maxWidth=$maxWidth")
        params.add("quality=$quality")

        val query = params.joinToString("&")
        return "$cleanBase/Items/$itemId/Images/Logo?$query"
    }
}

/**
 * Convierte un [JellyfinMediaEntity] en un [MediaItem] de dominio aplicando URLs
 * de imágenes optimizadas con escalado del lado del servidor.
 */
fun JellyfinMediaEntity.toOptimizedMediaItem(
    baseUrl: String,
    token: String,
    source: MediaSource = MediaSource.JELLYFIN
): MediaItem {
    val isEpisode = type.equals("Episode", ignoreCase = true)
    val hasSeriesParent = isEpisode && !seriesId.isNullOrEmpty()

    // Si es episodio en una fila general, mostrar el póster vertical de la serie
    val effectivePosterId = if (hasSeriesParent) seriesId!! else id
    val effectivePosterTag = if (hasSeriesParent) seriesPrimaryImageTag else primaryImageTag

    val posterUrl = JellyfinImageUtils.getPosterUrl(
        baseUrl = baseUrl,
        itemId = effectivePosterId,
        imageTag = effectivePosterTag,
        token = token
    )

    val backdropUrl = JellyfinImageUtils.getBackdropUrl(
        baseUrl = baseUrl,
        itemId = id,
        imageTag = backdropImageTag,
        token = token
    )

    val effectiveLogoId = if (hasSeriesParent) seriesId!! else id
    val logoUrl = JellyfinImageUtils.getLogoUrl(
        baseUrl = baseUrl,
        itemId = effectiveLogoId,
        token = token
    )

    val isContinueWatching = source == MediaSource.JELLYFIN && playbackPositionTicks > 0
    val effectiveTitle = if (isEpisode && !seriesName.isNullOrEmpty()) {
        if (isContinueWatching) {
            val epCode = if (seasonNumber != null && episodeNumber != null) " (T${seasonNumber}:E${episodeNumber})" else ""
            "$seriesName$epCode"
        } else {
            seriesName
        }
    } else {
        title
    }

    val effectiveType = if (isEpisode && source != MediaSource.JELLYFIN) "Series" else type

    val total = totalItemCount
    val unplayed = unplayedItemCount
    val played = if (total != null && unplayed != null) (total - unplayed).coerceAtLeast(0) else null

    val effectivePlayed = if (effectiveType.equals("Series", ignoreCase = true)) {
        if (unplayed != null) unplayed == 0 && (total ?: 0) > 0 else isPlayed
    } else if (isEpisode && source != MediaSource.JELLYFIN) {
        false
    } else {
        isPlayed
    }

    val effectiveRating = when {
        communityRating == null -> null
        communityRating > 9.5f -> null
        communityRating <= 0f -> null
        else -> communityRating
    }

    return MediaItem(
        id = id,
        title = effectiveTitle,
        overview = overview,
        type = effectiveType,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        logoUrl = logoUrl,
        rating = effectiveRating,
        year = productionYear,
        source = source,
        playbackPositionTicks = playbackPositionTicks,
        isPlayed = effectivePlayed,
        isFavorite = isFavorite,
        totalEpisodes = total,
        playedEpisodes = played
    )
}
