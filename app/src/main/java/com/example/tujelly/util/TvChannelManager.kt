package com.example.tujelly.util

import android.content.Context
import android.media.tv.TvContract
import androidx.core.net.toUri
import com.example.tujelly.domain.model.MediaItem

/**
 * Gestor de la fila nativa "Continuar Viendo" (Watch Next) en el Launcher de Android TV.
 */
object TvChannelManager {

    /**
     * Actualiza o añade un elemento multimedia en la lista global "Watch Next" del sistema Android TV.
     */
    fun publishToWatchNext(
        context: Context,
        mediaItem: MediaItem,
        progressMs: Long,
        durationMs: Long
    ) {
        if (progressMs <= 0 || durationMs <= 0) return

        try {
            val contentUri = "content://android.media.tv/watch_next_program".toUri()
            val values = android.content.ContentValues().apply {
                put(TvContract.WatchNextPrograms.COLUMN_TYPE, TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                put(TvContract.WatchNextPrograms.COLUMN_TITLE, mediaItem.title)
                put(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION, mediaItem.overview)
                put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, mediaItem.posterUrl)
                put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, System.currentTimeMillis())
                put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, progressMs)
                put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, durationMs)
                put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, "tujelly://player/${mediaItem.id}")
            }

            context.contentResolver.insert(contentUri, values)
        } catch (_: Exception) {
            // Ignorar suavemente en dispositivos no-TV que no soporten TvContract Provider
        }
    }
}
