package com.example.tujelly.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "jellyfin_media",
    indices = [
        Index(value = ["tmdbId"]),
        Index(value = ["imdbId"]),
        Index(value = ["title"]),
        Index(value = ["type"]),
        Index(value = ["isFavorite"])
    ]
)
data class JellyfinMediaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val type: String, // "Movie", "Series", "Episode"
    val tmdbId: String? = null,
    val imdbId: String? = null,
    val tvdbId: String? = null,
    val overview: String? = null,
    val primaryImageTag: String? = null,
    val backdropImageTag: String? = null,
    val communityRating: Float? = null,
    val productionYear: Int? = null,
    val genres: String? = null,
    val isPlayed: Boolean = false,
    val playbackPositionTicks: Long = 0L,
    val isFavorite: Boolean = false,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seriesPrimaryImageTag: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val totalItemCount: Int? = null,
    val unplayedItemCount: Int? = null
)
