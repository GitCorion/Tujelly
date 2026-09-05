package com.example.tujelly.data.remote.trakt

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TraktApiService {

    @GET("recommendations/movies")
    suspend fun getRecommendedMovies(
        @Header("Authorization") authHeader: String,
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2",
        @Query("limit") limit: Int = 20
    ): List<TraktMediaDto>

    @GET("recommendations/shows")
    suspend fun getRecommendedShows(
        @Header("Authorization") authHeader: String,
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2",
        @Query("limit") limit: Int = 20
    ): List<TraktMediaDto>

    @GET("sync/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") authHeader: String,
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2"
    ): List<TraktWatchlistItemDto>

    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2",
        @Query("limit") limit: Int = 30
    ): List<TraktTrendingDto>

    @GET("shows/trending")
    suspend fun getTrendingShows(
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2",
        @Query("limit") limit: Int = 30
    ): List<TraktTrendingDto>

    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") authHeader: String,
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") apiVersion: String = "2"
    ): TraktUserDto

    @retrofit2.http.POST("oauth/device/code")
    suspend fun getDeviceCode(
        @retrofit2.http.Header("trakt-api-version") apiVersion: String = "2",
        @retrofit2.http.Header("trakt-api-key") clientIdHeader: String,
        @retrofit2.http.Body request: TraktDeviceCodeRequest
    ): TraktDeviceCodeResponse

    @retrofit2.http.POST("oauth/device/token")
    suspend fun pollDeviceToken(
        @retrofit2.http.Header("trakt-api-version") apiVersion: String = "2",
        @retrofit2.http.Header("trakt-api-key") clientIdHeader: String,
        @retrofit2.http.Body request: TraktDeviceTokenRequest
    ): TraktTokenResponse
}

