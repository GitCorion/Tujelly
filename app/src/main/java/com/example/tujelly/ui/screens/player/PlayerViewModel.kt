package com.example.tujelly.ui.screens.player

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.jellyfin.JellyfinApiService
import com.example.tujelly.data.remote.jellyfin.JellyfinPlaybackProgressRequest
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

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
    val startPositionMs: Long = 0L,
    val token: String = "",
    val authHeader: String = "",
    val availableVersions: List<com.example.tujelly.data.remote.jellyfin.JellyfinMediaSourceDto> = emptyList(),
    val currentVersionName: String = ""
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

    private suspend fun resolveStrmTargetUrl(
        baseUrl: String,
        token: String,
        authHeader: String,
        playableId: String,
        mediaSourceId: String
    ): String? {
        val downloadEndpoints = listOfNotNull(
            "$baseUrl/Items/$playableId/Download?api_key=$token",
            if (mediaSourceId.isNotBlank()) "$baseUrl/Videos/$playableId/stream?static=true&MediaSourceId=$mediaSourceId&api_key=$token" else null,
            "$baseUrl/Videos/$playableId/stream?static=true&api_key=$token"
        )

        for (targetUrl in downloadEndpoints) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("X-Emby-Authorization", authHeader)
                    .build()

                val content = withContext(Dispatchers.IO) {
                    NetworkClientFactory.okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.string()?.trim()
                        } else null
                    }
                }

                if (!content.isNullOrBlank()) {
                    val lineWithUrl = content.lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }

                    if (lineWithUrl != null) {
                        Log.i("PlayerViewModel", "Extracted target URL from .strm file: $lineWithUrl")
                        return lineWithUrl
                    }
                }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Failed to resolve .strm from $targetUrl: ${e.message}")
            }
        }
        return null
    }

    private fun isDockerOrLocalhostHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") return true
        val parts = host.split(".")
        if (parts.size == 4) {
            val p0 = parts[0].toIntOrNull() ?: return false
            val p1 = parts[1].toIntOrNull() ?: return false
            // Docker default / bridge subnets: 172.16.0.0 - 172.31.255.255
            if (p0 == 172 && p1 in 16..31) return true
            // Also 10.0.0.0 - 10.255.255.255 if in a docker overlay
            if (p0 == 10 && p1 in 0..1) return true
        }
        return false
    }

    /**
     * Builds DeviceProfile dynamically from MediaCodecList (A.2 requirement from backend dev).
     * - On emulator / no HW HEVC: declares only h264 + aac → Jellyfin will transcode HEVC content
     * - On real TV with HEVC HW decoder: declares hevc + ac3/eac3 → Jellyfin allows DirectPlay
     * This ensures PlayMethod == DirectPlay when the device truly supports the codecs.
     */
    private fun createAndroidTvDeviceProfile(): com.example.tujelly.data.remote.jellyfin.JellyfinDeviceProfile {
        val isEmu = com.example.tujelly.util.DeviceUtils.isEmulator()
        val hwVideoCodecs = mutableSetOf("h264") // h264 always supported
        val hwAudioCodecs = mutableSetOf("aac", "mp3") // baseline audio
        var maxVideoWidth = 1920 // default to 1080p

        if (!isEmu) {
            try {
                val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
                for (info in codecList.codecInfos) {
                    if (info.isEncoder) continue
                    val name = info.name.lowercase()
                    val isSoftware = name.startsWith("omx.google.") ||
                            name.startsWith("c2.android.") ||
                            name.startsWith("c2.goldfish.")
                    if (isSoftware) continue

                    for (type in info.supportedTypes) {
                        when {
                            type.equals("video/hevc", ignoreCase = true) -> {
                                val caps = info.getCapabilitiesForType(type)
                                hwVideoCodecs.add("hevc")
                                if (caps.videoCapabilities?.isSizeSupported(3840, 2160) == true) {
                                    maxVideoWidth = maxOf(maxVideoWidth, 3840)
                                }
                            }
                            type.equals("video/vp9", ignoreCase = true) -> hwVideoCodecs.add("vp9")
                            type.equals("video/av01", ignoreCase = true) -> hwVideoCodecs.add("av1")
                        }
                    }
                }
                // If device has HW HEVC, it almost certainly has AC3/EAC3 passthrough (Android TV)
                if ("hevc" in hwVideoCodecs) {
                    hwAudioCodecs.addAll(listOf("ac3", "eac3"))
                }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Error probing MediaCodecList: ${e.message}")
            }
        }

        val videoCodecStr = hwVideoCodecs.joinToString(",")
        val audioCodecStr = (hwAudioCodecs + setOf("aac", "mp3")).joinToString(",")

        Log.i("PlayerViewModel", "DeviceProfile: isEmulator=$isEmu videoCodecs=$videoCodecStr audioCodecs=$audioCodecStr maxWidth=$maxVideoWidth")

        // Build CodecProfiles with width constraints per codec (A.1 spec)
        val codecProfiles = mutableListOf<com.example.tujelly.data.remote.jellyfin.JellyfinCodecProfile>()
        codecProfiles.add(
            com.example.tujelly.data.remote.jellyfin.JellyfinCodecProfile(
                type = "Video",
                codec = "h264",
                conditions = listOf(
                    com.example.tujelly.data.remote.jellyfin.JellyfinProfileCondition(
                        condition = "LessThanEqual",
                        property = "Width",
                        value = "1920"
                    )
                )
            )
        )
        if ("hevc" in hwVideoCodecs) {
            codecProfiles.add(
                com.example.tujelly.data.remote.jellyfin.JellyfinCodecProfile(
                    type = "Video",
                    codec = "hevc",
                    conditions = listOf(
                        com.example.tujelly.data.remote.jellyfin.JellyfinProfileCondition(
                            condition = "LessThanEqual",
                            property = "Width",
                            value = maxVideoWidth.toString()
                        )
                    )
                )
            )
        }
        // VideoAudio codec profile (no conditions = allow all declared audio codecs)
        codecProfiles.add(
            com.example.tujelly.data.remote.jellyfin.JellyfinCodecProfile(
                type = "VideoAudio",
                codec = audioCodecStr
            )
        )

        return com.example.tujelly.data.remote.jellyfin.JellyfinDeviceProfile(
            name = "Tujelly Android TV",
            maxStreamingBitrate = 120_000_000L,
            maxStaticBitrate = 120_000_000L,
            musicStreamingTranscodingBitrate = 128_000L,
            directPlayProfiles = listOf(
                com.example.tujelly.data.remote.jellyfin.JellyfinDirectPlayProfile(
                    container = "mkv,mp4,mov",
                    type = "Video",
                    videoCodec = videoCodecStr,
                    audioCodec = audioCodecStr
                )
            ),
            transcodingProfiles = listOf(
                com.example.tujelly.data.remote.jellyfin.JellyfinTranscodingProfile(
                    container = "mkv",
                    type = "Video",
                    videoCodec = "h264",
                    audioCodec = "aac,mp3,ac3",
                    protocol = "hls"
                )
            ),
            subtitleProfiles = listOf(
                com.example.tujelly.data.remote.jellyfin.JellyfinSubtitleProfile(format = "srt", method = "External")
            ),
            codecProfiles = codecProfiles
        )
    }

    private fun canDeviceDecode4kHevc(): Boolean {
        if (com.example.tujelly.util.DeviceUtils.isEmulator()) {
            return false
        }
        return try {
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            codecList.codecInfos.any { info ->
                if (!info.isEncoder) {
                    val name = info.name.lowercase()
                    val isSoftware = name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.startsWith("c2.goldfish.")
                    if (!isSoftware) {
                        info.supportedTypes.any { type ->
                            if (type.equals("video/hevc", ignoreCase = true)) {
                                val caps = info.getCapabilitiesForType(type)
                                caps.videoCapabilities?.isSizeSupported(3840, 2160) == true
                            } else false
                        }
                    } else false
                } else false
            }
        } catch (e: Exception) {
            Log.w("PlayerViewModel", "Error checking 4K HEVC capabilities: ${e.message}")
            false
        }
    }

    fun selectVersion(mediaSourceId: String) {
        if (currentPlayableId.isNotBlank()) {
            loadStreamUrl(currentPlayableId, preferredMediaSourceId = mediaSourceId)
        }
    }

    fun loadStreamUrl(itemId: String, preferredMediaSourceId: String? = null) {
        viewModelScope.launch {
            Log.i("PlayerViewModel", "loadStreamUrl START for itemId=$itemId preferredMediaSourceId=$preferredMediaSourceId")
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

            var effectiveToken = token
            var effectiveUserId = userId
            var effectiveAuthHeader = jellyfinAuthHeader

            val androidTvProfile = createAndroidTvDeviceProfile()

            val pbRequest = com.example.tujelly.data.remote.jellyfin.JellyfinPlaybackInfoRequest(
                userId = effectiveUserId,
                enableDirectPlay = true,
                enableDirectStream = true,
                enableTranscoding = true,
                enableSubtitleExtraction = false,
                maxStreamingBitrate = 120_000_000L,
                deviceProfile = androidTvProfile
            )

            var itemDto = runCatching { api.getItemDetail(authHeader = effectiveAuthHeader, userId = effectiveUserId, itemId = playableId) }.getOrNull()
            var playbackInfo = runCatching { api.getPlaybackInfo(authHeader = effectiveAuthHeader, itemId = playableId, userId = effectiveUserId, request = pbRequest) }.getOrNull()

            if (itemDto == null && playbackInfo == null && prefs.jellyfinUsername.isNotBlank() && prefs.jellyfinPassword.isNotBlank()) {
                Log.i("PlayerViewModel", "Playback metadata failed (possible 401). Attempting re-authentication with Jellyfin...")
                try {
                    val authResp = api.authenticateByName(
                        authHeader = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"TujellyApp\", Version=\"1.0.0\"",
                        request = com.example.tujelly.data.remote.jellyfin.JellyfinAuthRequest(
                            username = prefs.jellyfinUsername,
                            pw = prefs.jellyfinPassword
                        )
                    )
                    effectiveToken = authResp.effectiveToken
                    effectiveUserId = authResp.effectiveUser.effectiveId
                    effectiveAuthHeader = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"AndroidTV\", Version=\"1.0.0\", Token=\"$effectiveToken\""
                    jellyfinAuthHeader = effectiveAuthHeader
                    userPreferencesRepository.updateJellyfinConfig(
                        serverUrl = baseUrl,
                        userId = effectiveUserId,
                        accessToken = effectiveToken,
                        username = prefs.jellyfinUsername,
                        password = prefs.jellyfinPassword
                    )
                    val updatedPbRequest = pbRequest.copy(userId = effectiveUserId)
                    itemDto = runCatching { api.getItemDetail(authHeader = effectiveAuthHeader, userId = effectiveUserId, itemId = playableId) }.getOrNull()
                    playbackInfo = runCatching { api.getPlaybackInfo(authHeader = effectiveAuthHeader, itemId = playableId, userId = effectiveUserId, request = updatedPbRequest) }.getOrNull()
                    Log.i("PlayerViewModel", "Re-authentication succeeded! New token obtained: ${effectiveToken.take(6)}...")
                } catch (e: Exception) {
                    Log.w("PlayerViewModel", "Re-authentication attempt failed: ${e.message}")
                }
            }

            val mediaSources = playbackInfo?.mediaSources?.ifEmpty { null } ?: itemDto?.mediaSources

            // A.2 verification: log PlayMethod for each MediaSource (DirectPlay = success, Transcode = profile mismatch)
            mediaSources?.forEachIndexed { idx, ms ->
                Log.i("PlayerViewModel", "MS[$idx]: Name='${ms.name}' Container='${ms.container}' " +
                        "SupportsDirectPlay=${ms.supportsDirectPlay} SupportsDirectStream=${ms.supportsDirectStream} " +
                        "SupportsTranscoding=${ms.supportsTranscoding} " +
                        "DirectStreamUrl='${ms.directStreamUrl}' TranscodingUrl='${ms.transcodingUrl}'")
                // Log the definitive PlayMethod check
                val playMethod = when {
                    ms.supportsDirectPlay -> "DirectPlay"
                    ms.supportsDirectStream -> "DirectStream"
                    ms.supportsTranscoding -> "Transcode"
                    else -> "Unknown"
                }
                Log.i("PlayerViewModel", "MS[$idx] PlayMethod=$playMethod ← ${if (playMethod == "DirectPlay") "✓ SUCCESS" else "⚠ Profile mismatch — check DeviceProfile codecs"}")
            }

            val supports4K = canDeviceDecode4kHevc()
            Log.i("PlayerViewModel", "Device 4K HEVC hardware decode support: $supports4K")

            val chosenMediaSource = if (!preferredMediaSourceId.isNullOrBlank()) {
                mediaSources?.firstOrNull { it.id == preferredMediaSourceId } ?: mediaSources?.firstOrNull()
            } else if (!supports4K && mediaSources != null && mediaSources.size > 1) {
                // If device cannot decode 4K HEVC (e.g. emulator, 1080p stick), pick 1080p H264 version first, then any 1080p
                val pH264 = mediaSources.firstOrNull { ms ->
                    val name = ms.name?.lowercase() ?: ""
                    (name.contains("1080") || name.contains("fhd")) && (name.contains("h264") || name.contains("eng+esp") || name.contains("multi"))
                }
                val p1080 = mediaSources.firstOrNull { ms ->
                    val name = ms.name?.lowercase() ?: ""
                    name.contains("1080") || name.contains("fhd")
                }
                pH264 ?: p1080 ?: mediaSources.firstOrNull()
            } else {
                mediaSources?.firstOrNull()
            }

            val candidateUrls = mutableListOf<String>()
            val subtitles = mutableListOf<SubtitleTrackInfo>()
            val audioTracks = mutableListOf<AudioTrackInfo>()

            var isStrmMedia = false

            chosenMediaSource?.let { mediaSource ->
                val mediaSourceId = mediaSource.id
                val path = mediaSource.path ?: ""
                val container = mediaSource.container?.lowercase() ?: ""
                val isStrm = container == "strm" ||
                        path.endsWith(".strm", ignoreCase = true) ||
                        mediaSource.protocol?.equals("Http", ignoreCase = true) == true
                isStrmMedia = isStrm

                Log.d("PlayerViewModel", "Using MediaSource id=$mediaSourceId name='${mediaSource.name}' container=$container isStrm=$isStrm path=$path")

                // For .strm files:
                // 1. Direct static stream from Jellyfin (static=true forces Jellyfin to stream raw bytes without FFmpeg)
                if (isStrm) {
                    if (mediaSourceId.isNotBlank()) {
                        val staticStream = "$baseUrl/Videos/$playableId/stream?static=true&MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(staticStream)) {
                            candidateUrls.add(staticStream)
                        }
                    }

                    // 2. DirectStreamUrl provided by Jellyfin API if available
                    val dsUrl = mediaSource.directStreamUrl
                    if (!dsUrl.isNullOrBlank()) {
                        val fullDsUrl = if (dsUrl.startsWith("http", ignoreCase = true)) dsUrl else "$baseUrl$dsUrl"
                        val authDsUrl = if (!fullDsUrl.contains("api_key=")) "$fullDsUrl&api_key=$effectiveToken" else fullDsUrl
                        if (!candidateUrls.contains(authDsUrl)) {
                            candidateUrls.add(authDsUrl)
                        }
                    }

                    // 3. Alternative versions (e.g. 1080p SDR versions) as direct static candidates
                    // Allows seamless fallback if 4K HDR HEVC hardware decoder fails
                    mediaSources?.filter { it.id != mediaSourceId }?.forEach { altSource ->
                        if (altSource.id.isNotBlank()) {
                            val altStatic = "$baseUrl/Videos/$playableId/stream?static=true&MediaSourceId=${altSource.id}&api_key=$effectiveToken"
                            if (!candidateUrls.contains(altStatic)) {
                                candidateUrls.add(altStatic)
                            }
                        }
                    }

                    // 4. Direct path from MediaSourceInfo.Path ONLY if LAN/routable IP (not docker bridge)
                    if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
                        val pathUri = runCatching { Uri.parse(path) }.getOrNull()
                        val pathHost = pathUri?.host

                        if (!isDockerOrLocalhostHost(pathHost) && !pathHost.isNullOrBlank()) {
                            if (!candidateUrls.contains(path)) candidateUrls.add(path)
                        }
                    }

                    // 5. Transcoding / HLS fallbacks from Jellyfin (ONLY as last resort if direct play and static fails)
                    if (mediaSourceId.isNotBlank()) {
                        val transcode1080 = "$baseUrl/Videos/$playableId/master.m3u8?MediaSourceId=$mediaSourceId&VideoCodec=h264&AudioCodec=aac,mp3,ac3&MaxHeight=1080&MaxWidth=1920&api_key=$effectiveToken"
                        if (!candidateUrls.contains(transcode1080)) {
                            candidateUrls.add(transcode1080)
                        }
                        val hlsWithMs = "$baseUrl/Videos/$playableId/master.m3u8?MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(hlsWithMs)) {
                            candidateUrls.add(hlsWithMs)
                        }
                        val mainHlsWithMs = "$baseUrl/Videos/$playableId/main.m3u8?MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(mainHlsWithMs)) {
                            candidateUrls.add(mainHlsWithMs)
                        }
                    }

                    // 4. Optional: Direct proxy on server host if port is exposed
                    val extractedStrmUrl = resolveStrmTargetUrl(
                        baseUrl = baseUrl,
                        token = effectiveToken,
                        authHeader = effectiveAuthHeader,
                        playableId = playableId,
                        mediaSourceId = mediaSourceId
                    )
                    if (extractedStrmUrl != null) {
                        val strmUri = runCatching { Uri.parse(extractedStrmUrl) }.getOrNull()
                        val strmHost = strmUri?.host
                        val serverHost = runCatching { Uri.parse(baseUrl).host }.getOrNull()
                        if (!serverHost.isNullOrBlank() && isDockerOrLocalhostHost(strmHost)) {
                            val transformed = extractedStrmUrl.replaceFirst(strmHost!!, serverHost)
                            if (!candidateUrls.contains(transformed)) candidateUrls.add(transformed)
                        } else if (!isDockerOrLocalhostHost(strmHost)) {
                            if (!candidateUrls.contains(extractedStrmUrl)) candidateUrls.add(extractedStrmUrl)
                        }
                    }
                } else {
                    // For regular non-strm media files (MKV, MP4 on rclone or local disk):
                    // 1. Direct container stream with static=true
                    if (container.isNotBlank()) {
                        val containerUrl = "$baseUrl/Videos/$playableId/stream.$container?Static=true&MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(containerUrl)) {
                            candidateUrls.add(containerUrl)
                        }
                    }

                    // 2. Direct static file stream with MediaSourceId
                    if (mediaSourceId.isNotBlank()) {
                        val staticWithMs = "$baseUrl/Videos/$playableId/stream?static=true&MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(staticWithMs)) {
                            candidateUrls.add(staticWithMs)
                        }
                    }

                    // 3. Dynamic stream through Jellyfin (without static=true)
                    if (mediaSourceId.isNotBlank()) {
                        val dynamicWithMs = "$baseUrl/Videos/$playableId/stream?MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(dynamicWithMs)) {
                            candidateUrls.add(dynamicWithMs)
                        }

                        // 4. Master & Main HLS Playlists with MediaSourceId
                        val transcode1080 = "$baseUrl/Videos/$playableId/master.m3u8?MediaSourceId=$mediaSourceId&VideoCodec=h264&AudioCodec=aac,mp3,ac3&MaxHeight=1080&MaxWidth=1920&api_key=$effectiveToken"
                        if (!candidateUrls.contains(transcode1080)) {
                            candidateUrls.add(transcode1080)
                        }
                        val hlsWithMs = "$baseUrl/Videos/$playableId/master.m3u8?MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(hlsWithMs)) {
                            candidateUrls.add(hlsWithMs)
                        }
                        val mainHlsWithMs = "$baseUrl/Videos/$playableId/main.m3u8?MediaSourceId=$mediaSourceId&api_key=$effectiveToken"
                        if (!candidateUrls.contains(mainHlsWithMs)) {
                            candidateUrls.add(mainHlsWithMs)
                        }
                    }
                }

                // Parse Subtitles & Audio streams
                for (stream in mediaSource.mediaStreams) {
                    if (stream.type.equals("Subtitle", ignoreCase = true)) {
                        val subUrl = if (!stream.deliveryUrl.isNullOrBlank()) {
                            if (stream.deliveryUrl.startsWith("http", ignoreCase = true)) stream.deliveryUrl else "$baseUrl${stream.deliveryUrl}"
                        } else {
                            "$baseUrl/Videos/$playableId/${mediaSource.id}/Subtitles/${stream.index}/Stream.srt?api_key=$effectiveToken"
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

            // Fallback URLs if candidateUrls is still empty
            if (!isStrmMedia) {
                val defaultStatic = "$baseUrl/Videos/$playableId/stream?static=true&api_key=$effectiveToken"
                if (!candidateUrls.contains(defaultStatic)) {
                    candidateUrls.add(defaultStatic)
                }
            }

            val defaultStandard = "$baseUrl/Videos/$playableId/stream?api_key=$effectiveToken"
            if (!candidateUrls.contains(defaultStandard)) {
                candidateUrls.add(defaultStandard)
            }

            val defaultHls = "$baseUrl/Videos/$playableId/master.m3u8?api_key=$effectiveToken"
            if (!candidateUrls.contains(defaultHls)) {
                candidateUrls.add(defaultHls)
            }

            val primaryStreamUrl = candidateUrls.firstOrNull() ?: "$baseUrl/Videos/$playableId/stream?api_key=$effectiveToken"

            val rawTicks = itemDto?.userData?.playbackPositionTicks ?: entity?.playbackPositionTicks ?: 0L
            val startPositionMs = (rawTicks / 10_000L).coerceAtLeast(0L)

            Log.i("PlayerViewModel", "Resolved ${candidateUrls.size} candidate URLs for $playableId. Primary: $primaryStreamUrl")

            if (primaryStreamUrl.isNotBlank()) {
                prewarmStream(primaryStreamUrl, effectiveToken, effectiveAuthHeader)
            }

            _streamInfo.value = PlayerStreamInfo(
                playableId = playableId,
                primaryStreamUrl = primaryStreamUrl,
                candidateUrls = candidateUrls,
                subtitles = subtitles,
                audioTracks = audioTracks,
                title = itemDto?.name ?: entity?.title ?: "",
                startPositionMs = startPositionMs,
                token = effectiveToken,
                authHeader = effectiveAuthHeader,
                availableVersions = mediaSources ?: emptyList(),
                currentVersionName = chosenMediaSource?.name ?: ""
            )
        }
    }

    private fun prewarmStream(url: String, token: String, authHeader: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("PlayerViewModel", "Pre-warming stream: $url")
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-0")
                    .apply {
                        if (token.isNotBlank()) {
                            header("X-Emby-Token", token)
                        }
                        if (authHeader.isNotBlank()) {
                            header("X-Emby-Authorization", authHeader)
                        }
                    }
                    .build()
                NetworkClientFactory.okHttpClient.newBuilder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                    .newCall(request)
                    .execute()
                    .use { response ->
                        Log.i("PlayerViewModel", "Pre-warm response: code=${response.code}")
                    }
            } catch (e: Exception) {
                Log.d("PlayerViewModel", "Pre-warm completed/timed out (non-blocking): ${e.message}")
            }
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
