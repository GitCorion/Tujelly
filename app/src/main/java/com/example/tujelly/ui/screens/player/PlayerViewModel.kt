package com.example.tujelly.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.jellyfin.JellyfinApiService
import com.example.tujelly.data.remote.jellyfin.JellyfinPlaybackProgressRequest
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubtitleTrackInfo(
    val url: String,
    val language: String,
    val title: String,
    val codec: String,
    val isDefault: Boolean
)

data class AudioTrackInfo(
    val index: Int,
    val language: String,
    val title: String,
    val codec: String,
    val isDefault: Boolean
)

data class PlayerStreamInfo(
    val playableId: String,
    val primaryStreamUrl: String,
    val candidateUrls: List<String> = emptyList(),
    val subtitles: List<SubtitleTrackInfo> = emptyList(),
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val title: String = "",
    val startPositionMs: Long = 0L
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    private val _streamInfo = MutableStateFlow<PlayerStreamInfo?>(null)
    val streamInfo: StateFlow<PlayerStreamInfo?> = _streamInfo.asStateFlow()

    val buttonStyle: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.buttonStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BUTTON_STYLE_ICONS_ONLY)

    val isMonochrome: StateFlow<Boolean> = userPreferencesRepository.userPreferencesFlow
        .map { it.isMonochrome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var currentPlayableId: String = ""
    private var jellyfinBaseUrl: String = ""
    private var jellyfinAuthHeader: String = ""

    fun loadStreamUrl(itemId: String) {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val baseUrl = prefs.jellyfinServerUrl
            val token = prefs.jellyfinAccessToken
            val userId = prefs.jellyfinUserId

            jellyfinBaseUrl = baseUrl
            jellyfinAuthHeader = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"AndroidTV\", Version=\"1.0.0\", Token=\"$token\""

            val db = com.example.tujelly.data.local.db.AppDatabase.getDatabase(getApplication())
            val entity = db.jellyfinDao().getItemById(itemId)

            var playableId = itemId
            val api = NetworkClientFactory.createService(baseUrl, JellyfinApiService::class.java)

            if (entity?.type?.equals("Series", ignoreCase = true) == true) {
                try {
                    val nextUp = api.getNextUpItems(authHeader = jellyfinAuthHeader, userId = userId, seriesId = itemId)
                    val nextEp = nextUp.items.firstOrNull()
                    if (nextEp != null) {
                        playableId = nextEp.id
                    } else {
                        val episodes = api.getEpisodesForSeries(authHeader = jellyfinAuthHeader, seriesId = itemId, userId = userId)
                        episodes.items.firstOrNull()?.id?.let { playableId = it }
                    }
                } catch (_: Exception) {}
            }

            currentPlayableId = playableId

            // Fetch full Item Detail DTO and PlaybackInfo from Jellyfin API
            val itemDto = runCatching { api.getItemDetail(authHeader = jellyfinAuthHeader, userId = userId, itemId = playableId) }.getOrNull()
            val playbackInfo = runCatching { api.getPlaybackInfo(authHeader = jellyfinAuthHeader, itemId = playableId, userId = userId) }.getOrNull()

            val mediaSources = playbackInfo?.mediaSources?.ifEmpty { null } ?: itemDto?.mediaSources

            val candidateUrls = mutableListOf<String>()
            val subtitles = mutableListOf<SubtitleTrackInfo>()
            val audioTracks = mutableListOf<AudioTrackInfo>()

            mediaSources?.firstOrNull()?.let { mediaSource ->
                val mediaSourceId = mediaSource.id
                val path = mediaSource.path ?: ""
                val container = mediaSource.container?.lowercase() ?: ""

                // 1. Direct HTTP/HTTPS link from .strm files (RealDebrid, Premiumize, IPTV, remote links)
                if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
                    candidateUrls.add(path)
                }

                // 2. DirectStreamUrl provided by Jellyfin API
                val dsUrl = mediaSource.directStreamUrl
                if (!dsUrl.isNullOrBlank()) {
                    val fullDsUrl = if (dsUrl.startsWith("http", ignoreCase = true)) dsUrl else "$baseUrl$dsUrl"
                    val authDsUrl = if (!fullDsUrl.contains("api_key=")) "$fullDsUrl&api_key=$token" else fullDsUrl
                    if (!candidateUrls.contains(authDsUrl)) {
                        candidateUrls.add(authDsUrl)
                    }
                }

                // 3. TranscodingUrl provided by Jellyfin API
                val tcUrl = mediaSource.transcodingUrl
                if (!tcUrl.isNullOrBlank()) {
                    val fullTcUrl = if (tcUrl.startsWith("http", ignoreCase = true)) tcUrl else "$baseUrl$tcUrl"
                    val authTcUrl = if (!fullTcUrl.contains("api_key=")) "$fullTcUrl&api_key=$token" else fullTcUrl
                    if (!candidateUrls.contains(authTcUrl)) {
                        candidateUrls.add(authTcUrl)
                    }
                }

                // 4. Direct container stream with MediaSourceId
                if (container.isNotBlank() && container != "strm") {
                    val containerUrl = "$baseUrl/Videos/$playableId/stream.$container?Static=true&MediaSourceId=$mediaSourceId&api_key=$token"
                    if (!candidateUrls.contains(containerUrl)) {
                        candidateUrls.add(containerUrl)
                    }
                }

                // 5. Static stream with MediaSourceId
                if (mediaSourceId.isNotBlank()) {
                    val staticWithMs = "$baseUrl/Videos/$playableId/stream?static=true&MediaSourceId=$mediaSourceId&api_key=$token"
                    if (!candidateUrls.contains(staticWithMs)) {
                        candidateUrls.add(staticWithMs)
                    }

                    // 6. Master HLS Playlist with MediaSourceId (Universal transcode/HLS fallback)
                    val hlsWithMs = "$baseUrl/Videos/$playableId/master.m3u8?MediaSourceId=$mediaSourceId&api_key=$token"
                    if (!candidateUrls.contains(hlsWithMs)) {
                        candidateUrls.add(hlsWithMs)
                    }
                }

                // Parse Subtitles & Audio streams
                for (stream in mediaSource.mediaStreams) {
                    if (stream.type.equals("Subtitle", ignoreCase = true)) {
                        val subUrl = if (!stream.deliveryUrl.isNullOrBlank()) {
                            if (stream.deliveryUrl.startsWith("http", ignoreCase = true)) stream.deliveryUrl else "$baseUrl${stream.deliveryUrl}"
                        } else {
                            "$baseUrl/Videos/$playableId/${mediaSource.id}/Subtitles/${stream.index}/Stream.srt?api_key=$token"
                        }
                        subtitles.add(
                            SubtitleTrackInfo(
                                url = subUrl,
                                language = stream.language ?: "es",
                                title = stream.displayTitle ?: stream.language ?: "Subtítulo",
                                codec = stream.codec ?: "srt",
                                isDefault = stream.isDefault || stream.isForced
                            )
                        )
                    } else if (stream.type.equals("Audio", ignoreCase = true)) {
                        audioTracks.add(
                            AudioTrackInfo(
                                index = stream.index,
                                language = stream.language ?: "und",
                                title = stream.displayTitle ?: stream.language ?: "Audio ${stream.index}",
                                codec = stream.codec ?: "aac",
                                isDefault = stream.isDefault
                            )
                        )
                    }
                }
            }

            // Fallback URLs if candidateUrls is empty
            val defaultStatic = "$baseUrl/Videos/$playableId/stream?static=true&api_key=$token"
            if (!candidateUrls.contains(defaultStatic)) {
                candidateUrls.add(defaultStatic)
            }

            val defaultHls = "$baseUrl/Videos/$playableId/master.m3u8?api_key=$token"
            if (!candidateUrls.contains(defaultHls)) {
                candidateUrls.add(defaultHls)
            }

            val defaultStandard = "$baseUrl/Videos/$playableId/stream?api_key=$token"
            if (!candidateUrls.contains(defaultStandard)) {
                candidateUrls.add(defaultStandard)
            }

            val primaryStreamUrl = candidateUrls.first()

            val rawTicks = itemDto?.userData?.playbackPositionTicks ?: entity?.playbackPositionTicks ?: 0L
            val startPositionMs = (rawTicks / 10_000L).coerceAtLeast(0L)

            _streamInfo.value = PlayerStreamInfo(
                playableId = playableId,
                primaryStreamUrl = primaryStreamUrl,
                candidateUrls = candidateUrls,
                subtitles = subtitles,
                audioTracks = audioTracks,
                title = itemDto?.name ?: entity?.title ?: "",
                startPositionMs = startPositionMs
            )
        }
    }

    fun reportStart(positionMs: Long) {
        if (currentPlayableId.isBlank() || jellyfinBaseUrl.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val api = NetworkClientFactory.createService(jellyfinBaseUrl, JellyfinApiService::class.java)
                val ticks = positionMs * 10_000L
                api.reportPlaybackStart(
                    authHeader = jellyfinAuthHeader,
                    request = JellyfinPlaybackProgressRequest(itemId = currentPlayableId, positionTicks = ticks, isPaused = false)
                )
            }
        }
    }

    fun reportProgress(positionMs: Long, isPaused: Boolean) {
        if (currentPlayableId.isBlank() || jellyfinBaseUrl.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val api = NetworkClientFactory.createService(jellyfinBaseUrl, JellyfinApiService::class.java)
                val ticks = positionMs * 10_000L
                api.reportPlaybackProgress(
                    authHeader = jellyfinAuthHeader,
                    request = JellyfinPlaybackProgressRequest(
                        itemId = currentPlayableId,
                        positionTicks = ticks,
                        isPaused = isPaused,
                        eventName = "timeupdate"
                    )
                )
            }
        }
    }

    fun reportStopped(positionMs: Long) {
        if (currentPlayableId.isBlank() || jellyfinBaseUrl.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val api = NetworkClientFactory.createService(jellyfinBaseUrl, JellyfinApiService::class.java)
                val ticks = positionMs * 10_000L
                api.reportPlaybackStopped(
                    authHeader = jellyfinAuthHeader,
                    request = JellyfinPlaybackProgressRequest(itemId = currentPlayableId, positionTicks = ticks)
                )
            }
        }
    }
}
