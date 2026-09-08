package com.example.tujelly.data.remote.jellyfin

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JellyfinApiService {

    @GET("System/Info/Public")
    suspend fun getPublicInfo(
        @Header("X-Emby-Authorization") authHeader: String = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"AndroidTV\", Version=\"1.0.0\""
    ): JellyfinPublicInfo

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") authHeader: String,
        @Body request: JellyfinAuthRequest
    ): JellyfinAuthResponse

    @POST("QuickConnect/Initiate")
    suspend fun initiateQuickConnect(
        @Header("X-Emby-Authorization") authHeader: String
    ): JellyfinQuickConnectResult

    @GET("QuickConnect/Connect")
    suspend fun checkQuickConnect(
        @Header("X-Emby-Authorization") authHeader: String,
        @Query("Secret") secret: String
    ): JellyfinQuickConnectResult

    @GET("Users/Me")
    suspend fun getCurrentUser(
        @Header("X-Emby-Authorization") authHeader: String
    ): JellyfinAuthUser

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String
    ): JellyfinItemsResponse

    @GET("Items")
    suspend fun getLibraryItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Query("UserId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = "Movie,Series,BoxSet",
        @Query("Fields") fields: String? = "ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,ItemCounts,RecursiveItemCount",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int? = 500,
        @Query("StartIndex") startIndex: Int? = 0,
        @Query("SortBy") sortBy: String? = "SortName",
        @Query("SortOrder") sortOrder: String? = "Ascending",
        @Query("MinDateLastSaved") minDateLastSaved: String? = null,
        @Query("EnableTotalRecordCount") enableTotalRecordCount: Boolean? = null
    ): JellyfinItemsResponse

    @GET("Items")
    suspend fun getCollectionItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Query("UserId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("Fields") fields: String = "Overview,ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,Genres,ProductionYear",
        @Query("SortBy") sortBy: String = "SortName,ProductionYear",
        @Query("SortOrder") sortOrder: String = "Ascending"
    ): JellyfinItemsResponse

    @GET("Items")
    suspend fun searchLibraryItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Query("UserId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series,BoxSet",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 5,
        @Query("Fields") fields: String = "Overview,ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,Genres"
    ): JellyfinItemsResponse

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "ProviderIds,Overview,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData"
    ): JellyfinItemsResponse

    @GET("Shows/NextUp")
    suspend fun getNextUpItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Query("UserId") userId: String,
        @Query("SeriesId") seriesId: String? = null,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "ProviderIds,Overview,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData"
    ): JellyfinItemsResponse

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasonsForSeries(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "ItemCounts,PrimaryImageTag,Overview"
    ): JellyfinItemsResponse

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodesForSeries(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String? = null,
        @Query("Season") seasonNumber: Int? = null,
        @Query("Fields") fields: String = "Overview,PrimaryImageTag,UserData,IndexNumber,ParentIndexNumber,RunTimeTicks",
        @Query("Limit") limit: Int? = null
    ): JellyfinItemsResponse

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "ProviderIds,Overview,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData"
    ): List<JellyfinItemDto>

    @GET("Users/{userId}/Items")
    suspend fun getRecentlyPlayedItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("Filters") filters: String = "IsPlayed",
        @Query("SortBy") sortBy: String = "DatePlayed",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series,Episode",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 10,
        @Query("Fields") fields: String = "ProviderIds,Overview,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData"
    ): JellyfinItemsResponse

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetail(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): JellyfinItemDto

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Body request: JellyfinPlaybackInfoRequest? = null
    ): JellyfinPlaybackInfoResponse

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Header("X-Emby-Authorization") authHeader: String,
        @Body request: JellyfinPlaybackProgressRequest
    )

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Header("X-Emby-Authorization") authHeader: String,
        @Body request: JellyfinPlaybackProgressRequest
    )

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Header("X-Emby-Authorization") authHeader: String,
        @Body request: JellyfinPlaybackProgressRequest
    )

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): JellyfinUserDataDto

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): JellyfinUserDataDto

    @GET("Users/{userId}/Items")
    suspend fun getFavoriteItems(
        @Header("X-Emby-Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("Filters") filters: String = "IsFavorite",
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("Fields") fields: String = "Overview,ProviderIds,PrimaryImageTag,BackdropImageTags,CommunityRating,UserData,Genres",
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending"
    ): JellyfinItemsResponse
}

