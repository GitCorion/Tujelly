package com.example.tujelly.data.remote.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktIdsDto(
    @SerialName("trakt") val trakt: Int? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("imdb") val imdb: String? = null,
    @SerialName("tmdb") val tmdb: Int? = null
)

@Serializable
data class TraktMediaDto(
    @SerialName("title") val title: String,
    @SerialName("year") val year: Int? = null,
    @SerialName("ids") val ids: TraktIdsDto
)

@Serializable
data class TraktWatchlistItemDto(
    @SerialName("listed_at") val listedAt: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("movie") val movie: TraktMediaDto? = null,
    @SerialName("show") val show: TraktMediaDto? = null
)

@Serializable
data class TraktTrendingDto(
    @SerialName("watchers") val watchers: Int? = null,
    @SerialName("movie") val movie: TraktMediaDto? = null,
    @SerialName("show") val show: TraktMediaDto? = null
)

@Serializable
data class TraktUserDto(
    @SerialName("username") val username: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null
)

@Serializable
data class TraktErrorResponse(
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)

@Serializable
data class TraktDeviceCodeRequest(
    @SerialName("client_id") val clientId: String
)

@Serializable
data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int = 5
)

@Serializable
data class TraktDeviceTokenRequest(
    @SerialName("code") val code: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String? = null
)

@Serializable
data class TraktTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L
)

