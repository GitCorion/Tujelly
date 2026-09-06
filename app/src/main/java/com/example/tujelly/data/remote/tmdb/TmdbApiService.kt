package com.example.tujelly.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("trending/all/week")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("trending/all/day")
    suspend fun getTrendingDay(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbMovieDetailDto

    @GET("tv/{series_id}/recommendations")
    suspend fun getTvRecommendations(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("tv/{series_id}")
    suspend fun getTvDetails(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTvDetailDto

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbVideosResponse

    @GET("tv/{series_id}/videos")
    suspend fun getTvVideos(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbVideosResponse

    @GET("discover/movie")
    suspend fun discoverMoviesByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("discover/tv")
    suspend fun discoverTvByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("discover/movie")
    suspend fun discoverMoviesByProvider(
        @Query("api_key") apiKey: String,
        @Query("with_watch_providers") providerId: String,
        @Query("watch_region") watchRegion: String = "ES",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("discover/tv")
    suspend fun discoverTvByProvider(
        @Query("api_key") apiKey: String,
        @Query("with_watch_providers") providerId: String,
        @Query("watch_region") watchRegion: String = "ES",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbTrendingResponse
}
