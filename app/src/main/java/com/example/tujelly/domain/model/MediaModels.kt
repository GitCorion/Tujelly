package com.example.tujelly.domain.model

enum class MediaSource {
    JELLYFIN,
    TRAKT_RECOMMENDATION,
    TMDB_TRENDING,
    TMDB_RECOMMENDATION,
    TRAKT_WATCHLIST
}

data class MediaItem(
    val id: String, // Jellyfin ID for playback
    val title: String,
    val overview: String? = null,
    val type: String = "Movie",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Float? = null,
    val year: Int? = null,
    val source: MediaSource = MediaSource.JELLYFIN,
    val playbackPositionTicks: Long = 0L,
    val rank: Int? = null,
    val isPlayed: Boolean = false,
    val isFavorite: Boolean = false,
    val totalEpisodes: Int? = null,
    val playedEpisodes: Int? = null
)

data class HomeSection(
    val title: String,
    val items: List<MediaItem>,
    val badge: String? = null,
    val isRanked: Boolean = false
)

