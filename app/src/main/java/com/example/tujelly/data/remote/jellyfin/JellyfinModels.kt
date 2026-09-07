package com.example.tujelly.data.remote.jellyfin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyfinPublicInfo(
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("serverName") val serverNameLower: String? = null,
    @SerialName("Version") val version: String? = null,
    @SerialName("version") val versionLower: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("id") val idLower: String? = null
) {
    val effectiveServerName: String get() = serverName ?: serverNameLower ?: "Jellyfin"
    val effectiveVersion: String get() = version ?: versionLower ?: ""
    val effectiveId: String get() = id ?: idLower ?: ""
}

@Serializable
data class JellyfinAuthRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val pw: String = ""
)

@Serializable
data class JellyfinAuthUser(
    @SerialName("Id") val id: String? = null,
    @SerialName("id") val idLower: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("name") val nameLower: String? = null
) {
    val effectiveId: String get() = id ?: idLower ?: ""
    val effectiveName: String get() = name ?: nameLower ?: ""
}

@Serializable
data class JellyfinAuthResponse(
    @SerialName("User") val user: JellyfinAuthUser? = null,
    @SerialName("user") val userLower: JellyfinAuthUser? = null,
    @SerialName("AccessToken") val accessToken: String? = null,
    @SerialName("accessToken") val accessTokenLower: String? = null
) {
    val effectiveUser: JellyfinAuthUser get() = user ?: userLower ?: JellyfinAuthUser()
    val effectiveToken: String get() = accessToken ?: accessTokenLower ?: ""
}

@Serializable
data class JellyfinQuickConnectResult(
    @SerialName("Code") val code: String? = null,
    @SerialName("code") val codeLower: String? = null,
    @SerialName("Secret") val secret: String? = null,
    @SerialName("secret") val secretLower: String? = null,
    @SerialName("AuthenticationToken") val authenticationToken: String? = null,
    @SerialName("authenticationToken") val authenticationTokenLower: String? = null,
    @SerialName("Authenticated") val authenticated: Boolean = false,
    @SerialName("authenticated") val authenticatedLower: Boolean = false
) {
    val effectiveCode: String get() = code ?: codeLower ?: ""
    val effectiveSecret: String get() = secret ?: secretLower ?: ""
    val effectiveToken: String? get() = authenticationToken ?: authenticationTokenLower
    val isAuthed: Boolean get() = authenticated || authenticatedLower
}

@Serializable
data class JellyfinUserDataDto(
    @SerialName("Played") val played: Boolean = false,
    @SerialName("played") val playedLower: Boolean = false,
    @SerialName("IsPlayed") val isPlayedAlias: Boolean = false,
    @SerialName("isPlayed") val isPlayedAliasLower: Boolean = false,
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerialName("playbackPositionTicks") val playbackPositionTicksLower: Long = 0L,
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("playCount") val playCountLower: Int = 0,
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerialName("UnplayedItemCount") val unplayedItemCount: Int? = null,
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("isFavorite") val isFavoriteLower: Boolean = false
) {
    val rawPlayed: Boolean
        get() = played || playedLower || isPlayedAlias || isPlayedAliasLower

    fun isPlayedForType(type: String?): Boolean {
        return if (type.equals("Series", ignoreCase = true)) {
            // For Series: ONLY mark played if there are 0 unplayed episodes
            if (unplayedItemCount != null) {
                unplayedItemCount == 0
            } else {
                rawPlayed
            }
        } else {
            rawPlayed || playCount > 0 || playCountLower > 0 ||
                    (playedPercentage != null && playedPercentage >= 90.0)
        }
    }

    val isPlayed: Boolean
        get() = isPlayedForType(null)

    val effectivePositionTicks: Long
        get() = if (playbackPositionTicks > 0L) playbackPositionTicks else playbackPositionTicksLower

    val effectiveIsFavorite: Boolean
        get() = isFavorite || isFavoriteLower
}

@Serializable
data class JellyfinPlaybackProgressRequest(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long = 0L,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("EventName") val eventName: String? = null
)

@Serializable
data class JellyfinMediaStreamDto(
    @SerialName("Type") val type: String? = null, // "Audio", "Video", "Subtitle"
    @SerialName("Index") val index: Int = 0,
    @SerialName("Language") val language: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean = false,
    @SerialName("IsForced") val isForced: Boolean = false,
    @SerialName("DeliveryUrl") val deliveryUrl: String? = null,
    @SerialName("SupportsExternalStream") val supportsExternalStream: Boolean = false
)

@Serializable
data class JellyfinMediaSourceDto(
    @SerialName("Id") val id: String = "",
    @SerialName("Path") val path: String? = null,
    @SerialName("Protocol") val protocol: String? = null, // "File", "Http", "Hls"
    @SerialName("Container") val container: String? = null, // "strm", "mkv", "mp4"
    @SerialName("DirectStreamUrl") val directStreamUrl: String? = null,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("SupportsDirectStream") val supportsDirectStream: Boolean = false,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerialName("MediaStreams") val mediaStreams: List<JellyfinMediaStreamDto> = emptyList()
)

@Serializable
data class JellyfinPlaybackInfoResponse(
    @SerialName("MediaSources") val mediaSources: List<JellyfinMediaSourceDto> = emptyList(),
    @SerialName("PlaySessionId") val playSessionId: String? = null
)

@Serializable
data class JellyfinItemDto(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String? = null,
    @SerialName("OriginalTitle") val originalTitle: String? = null,
    @SerialName("Type") val type: String? = null, // "Movie", "Series", "Episode"
    @SerialName("CollectionType") val collectionType: String? = null,
    @SerialName("ProviderIds") val providerIds: Map<String, String>? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("CommunityRating") val communityRating: Float? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Genres") val genres: List<String>? = null,
    @SerialName("Tags") val tags: List<String>? = null,
    @SerialName("UserData") val userData: JellyfinUserDataDto? = null,
    @SerialName("MediaSources") val mediaSources: List<JellyfinMediaSourceDto> = emptyList(),
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @SerialName("SeasonId") val seasonId: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("RecursiveItemCount") val recursiveItemCount: Int? = null,
    @SerialName("recursiveItemCount") val recursiveItemCountLower: Int? = null,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("childCount") val childCountLower: Int? = null
) {
    val effectiveItemCount: Int?
        get() = recursiveItemCount ?: recursiveItemCountLower ?: childCount ?: childCountLower
}

@Serializable
data class JellyfinItemsResponse(
    @SerialName("Items") val items: List<JellyfinItemDto> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)
