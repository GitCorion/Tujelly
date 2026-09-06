package com.example.tujelly.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbItemDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("release_date") val releaseDate: String? = null
)

@Serializable
data class TmdbTrendingResponse(
    @SerialName("results") val results: List<TmdbItemDto> = emptyList()
)

@Serializable
data class TmdbVideoDto(
    @SerialName("key") val key: String,
    @SerialName("site") val site: String = "YouTube",
    @SerialName("type") val type: String = "Trailer"
)

@Serializable
data class TmdbVideosResponse(
    @SerialName("results") val results: List<TmdbVideoDto> = emptyList()
)

@Serializable
data class TmdbTvDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("overview") val overview: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("in_production") val inProduction: Boolean? = null
)

@Serializable
data class TmdbMovieDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("overview") val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Float? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null
)
