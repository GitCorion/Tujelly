@file:OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)

package com.example.tujelly.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Tune
import com.example.tujelly.ui.theme.TvAccent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_AND_TEXT
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.BUTTON_STYLE_TEXT_ONLY
import com.example.tujelly.data.remote.jellyfin.NextEpisodeInfo
import com.example.tujelly.data.remote.jellyfin.SkipMarker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class PlayerModalTab {
    NONE,
    AUDIO,
    SUBTITLES,
    SETTINGS
}

private data class TrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@ExperimentalTvMaterial3Api
@Composable
fun PlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    LaunchedEffect(itemId) {
        android.util.Log.i("PlayerScreen", "PlayerScreen LaunchedEffect for itemId=$itemId")
        viewModel.loadStreamUrl(itemId)
    }

    val streamInfo by viewModel.streamInfo.collectAsState()
    val buttonStyle by viewModel.buttonStyle.collectAsState()
    val isMonochrome by viewModel.isMonochrome.collectAsState()
    val accentColorKey by viewModel.accentColor.collectAsState()
    val focusColor = if (isMonochrome) Color.White else TvAccent.getColor(accentColorKey)
    val focusContent = if (isMonochrome) Color(0xFF0F172A) else TvAccent.getFocusedContentColor(accentColorKey)

    // Load intro markers + next episode once stream is ready
    val introMarker by viewModel.introMarker.collectAsState()
    val creditsMarker by viewModel.creditsMarker.collectAsState()
    val nextEpisode by viewModel.nextEpisode.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(streamInfo) {
        val info = streamInfo ?: return@LaunchedEffect
        // Look up seriesId from local DB
        val db = com.example.tujelly.data.local.db.AppDatabase.getDatabase(context)
        val entity = db.jellyfinDao().getItemById(info.playableId)
        val seriesId = entity?.seriesId
        viewModel.loadIntroAndNextEpisode(itemId = info.playableId, seriesId = seriesId)
    }

    val currentInfo = streamInfo
    if (currentInfo != null) {
        PlayerContent(
            info = currentInfo,
            buttonStyle = buttonStyle,
            isMonochrome = isMonochrome,
            focusColor = focusColor,
            focusContent = focusContent,
            viewModel = viewModel,
            introMarker = introMarker,
            creditsMarker = creditsMarker,
            nextEpisode = nextEpisode,
            onBack = onBack,
            onPlayNextEpisode = { nextEp ->
                viewModel.loadStreamUrl(nextEp.itemId)
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = if (isMonochrome) Color.White else focusColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Cargando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@ExperimentalTvMaterial3Api
@Composable
private fun PlayerContent(
    info: PlayerStreamInfo,
    buttonStyle: String,
    isMonochrome: Boolean,
    focusColor: Color,
    focusContent: Color,
    viewModel: PlayerViewModel,
    introMarker: SkipMarker? = null,
    creditsMarker: SkipMarker? = null,
    nextEpisode: NextEpisodeInfo? = null,
    onBack: () -> Unit,
    onPlayNextEpisode: (NextEpisodeInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var showOverlayControls by remember { mutableStateOf(true) }
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }
    var activeModalTab by remember { mutableStateOf(PlayerModalTab.NONE) }
    var lastInteractionTrigger by remember { mutableLongStateOf(0L) }

    fun notifyInteraction() {
        lastInteractionTrigger = System.currentTimeMillis()
    }

    // Intro / Credits skip banner visibility
    var showSkipIntroBanner by remember { mutableStateOf(false) }
    var showSkipCreditsBanner by remember { mutableStateOf(false) }

    // Next-episode overlay
    var showNextEpisodeOverlay by remember { mutableStateOf(false) }
    // countdown calculado en vivo desde la posición del reproductor, no un timer fijo
    // -1 = sin cuenta atrás (episodio terminó, mostrar sin contador)
    var nextEpisodeCountdown by remember { mutableIntStateOf(-1) }

    // Resume dialog state: null = not yet decided, true = show dialog, false = decision made
    var showResumeDialog by remember { mutableStateOf(false) }
    var resumeDecisionMade by remember { mutableStateOf(false) }
    var chosenStartPositionMs by remember { mutableLongStateOf(0L) }

    // Show resume dialog immediately when streamInfo is loaded if user has saved progress (>=10s)
    LaunchedEffect(info) {
        if (info.startPositionMs >= 10_000L && !resumeDecisionMade) {
            showResumeDialog = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }

    // Prevent Android TV from going to sleep / ambient mode while playing or viewing controls
    DisposableEffect(activity, isPlaying, showOverlayControls) {
        val keepOn = isPlaying || showOverlayControls
        if (keepOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var audioTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }

    var currentPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var isBuffering by remember { mutableStateOf(true) }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    val candidates = info.candidateUrls.ifEmpty { listOf(info.primaryStreamUrl) }

    val exoPlayer = remember(info, retryTrigger) {
        var candidateIdx = 0

        val defaultHeaders = buildMap {
            if (info.token.isNotBlank()) {
                put("X-Emby-Token", info.token)
                put("X-MediaBrowser-Token", info.token)
            }
            if (info.authHeader.isNotBlank()) {
                put("X-Emby-Authorization", info.authHeader)
            }
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(90_000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Google TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(defaultHeaders)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                addAnalyticsListener(androidx.media3.exoplayer.util.EventLogger())

                val mediaItem = MediaItem.Builder()
                    .setUri(candidates.first())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(info.title)
                            .setDisplayTitle(info.title)
                            .build()
                    )
                    .build()

                setMediaItem(mediaItem)

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) {
                            viewModel.reportStart(currentPosition.coerceAtLeast(0L))
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                        if (playbackState == Player.STATE_READY) {
                            playerErrorMessage = null
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val (audios, subs) = extractTracksFromExoPlayer(this@apply)
                        audioTracks = audios
                        subtitleTracks = subs
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        android.util.Log.e("PlayerScreen", "ExoPlayer error on candidate $candidateIdx (${candidates.getOrNull(candidateIdx)}): ${error.errorCodeName}", error)

                        candidateIdx++
                        if (candidateIdx < candidates.size) {
                            val nextUrl = candidates[candidateIdx]
                            android.util.Log.i("PlayerScreen", "Attempting fallback candidate $candidateIdx: $nextUrl")
                            val pos = currentPosition.coerceAtLeast(0L)

                            val fallbackItem = MediaItem.Builder()
                                .setUri(nextUrl)
                                .setMediaMetadata(
                                    androidx.media3.common.MediaMetadata.Builder()
                                        .setTitle(info.title)
                                        .setDisplayTitle(info.title)
                                        .build()
                                )
                                .build()
                            setMediaItem(fallbackItem)
                            if (pos > 0 && isCurrentMediaItemSeekable) seekTo(pos)
                            prepare()
                            playWhenReady = true
                        } else {
                            android.util.Log.e("PlayerScreen", "All ${candidates.size} playback candidates failed!")
                            val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: 0
                            val humanMsg = when {
                                httpCode in 500..599 -> "El proxy Real-Debrid devolvió HTTP $httpCode al resolver los mirrors. Pulsa Reintentar."
                                httpCode == 401 -> "Error de autenticación con el servidor (HTTP 401). Pulsa Reintentar."
                                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Error del servidor o proxy al obtener el vídeo (HTTP)"
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Tiempo de espera agotado al conectar con Real-Debrid o el proxy"
                                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato no compatible o enlace de streaming no disponible"
                                error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "El dispositivo no soporta este formato de vídeo o audio (HEVC/HDR)"
                                else -> error.message ?: error.errorCodeName
                            }
                            playerErrorMessage = humanMsg
                            isBuffering = false
                        }
                    }
                })

                // Only prepare ExoPlayer immediately if no resume decision is required
                if (resumeDecisionMade || info.startPositionMs < 10_000L) {
                    val startAt = if (chosenStartPositionMs > 0) chosenStartPositionMs else info.startPositionMs
                    if (startAt > 0) {
                        seekTo(startAt)
                    }
                    prepare()
                    playWhenReady = true
                }
            }
    }

    var accumulatedSeekOffset by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    fun seekRelative(deltaMs: Long) {
        notifyInteraction()
        val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = (cur + deltaMs).coerceIn(0L, dur)
        exoPlayer.seekTo(target)
        currentPosition = target

        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 1200L || (accumulatedSeekOffset > 0 && deltaMs < 0) || (accumulatedSeekOffset < 0 && deltaMs > 0)) {
            accumulatedSeekOffset = deltaMs
        } else {
            accumulatedSeekOffset += deltaMs
        }
        lastSeekTime = now

        val absSec = kotlin.math.abs(accumulatedSeekOffset) / 1000
        val sign = if (accumulatedSeekOffset < 0) "⏪ -" else "⏩ +"
        seekIndicatorText = "$sign${absSec}s (${formatTime(target)})"
    }

    // Auto-focus retry button if an error occurs
    LaunchedEffect(playerErrorMessage) {
        if (playerErrorMessage != null) {
            delay(150L)
            try {
                retryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-focus the play/pause button when controls overlay is visible so remote D-pad works immediately!
    LaunchedEffect(showOverlayControls) {
        if (showOverlayControls && playerErrorMessage == null && !showResumeDialog) {
            delay(150L)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-hide seek indicator banner after 1.2s
    LaunchedEffect(seekIndicatorText) {
        if (seekIndicatorText != null) {
            delay(1200L)
            seekIndicatorText = null
            accumulatedSeekOffset = 0L
        }
    }

    // Auto-hide controls overlay after 5s if activeModalTab is NONE (resets on user interaction)
    LaunchedEffect(showOverlayControls, activeModalTab, lastInteractionTrigger) {
        if (showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
            delay(5000L)
            showOverlayControls = false
        }
    }

    BackHandler {
        if (showResumeDialog) {
            onBack()
        } else if (activeModalTab != PlayerModalTab.NONE) {
            activeModalTab = PlayerModalTab.NONE
        } else if (showOverlayControls) {
            showOverlayControls = false
        } else {
            onBack()
        }
    }

    // 75-second timeout for initial slow proxy resolution (resolving RD / rescue can take 10-40s)
    LaunchedEffect(info, retryTrigger) {
        delay(75_000L)
        if (exoPlayer.playbackState == Player.STATE_BUFFERING && currentPosition == 0L) {
            playerErrorMessage = "La resolución en Real-Debrid tardó demasiado tiempo. Pulsa Reintentar."
            isBuffering = false
        }
    }

    // Sync position & Report progress (Only during active playback in STATE_READY, never while buffering/stalled)
    LaunchedEffect(exoPlayer) {
        var lastReportedMs = 0L
        var lastWasPlaying = false

        while (isActive) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            val playing = exoPlayer.isPlaying
            val state = exoPlayer.playbackState

            currentPosition = pos
            duration = dur
            bufferedPosition = buf

            val isActivelyPlaying = exoPlayer.playerError == null && state == Player.STATE_READY && playing
            if (isActivelyPlaying && pos > 0) {
                val now = System.currentTimeMillis()
                val stateChanged = playing != lastWasPlaying
                val intervalPassed = (now - lastReportedMs) >= 5000L

                if (stateChanged || intervalPassed) {
                    viewModel.reportProgress(pos, isPaused = false)
                    lastReportedMs = now
                    lastWasPlaying = playing
                }
            }

            delay(500L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            val finalPos = exoPlayer.currentPosition
            if (finalPos > 0 && exoPlayer.playerError == null) {
                viewModel.reportStopped(finalPos)
            }
            exoPlayer.release()
        }
    }

    // ─── Intro / Credits banner: usa los campos del plugin fielmente ────────────
    // introMarker.showAtMs = ShowSkipPromptAt del plugin
    // introMarker.hideAtMs = HideSkipPromptAt del plugin
    LaunchedEffect(currentPosition, introMarker) {
        val marker = introMarker
        if (marker != null) {
            // El banner es visible exactamente en el rango que el plugin especifica
            val inWindow = currentPosition in marker.showAtMs..marker.hideAtMs
            if (inWindow != showSkipIntroBanner) showSkipIntroBanner = inWindow
        } else {
            showSkipIntroBanner = false
        }
    }

    LaunchedEffect(currentPosition, creditsMarker) {
        val marker = creditsMarker
        if (marker != null) {
            val inWindow = currentPosition in marker.showAtMs..marker.hideAtMs
            if (inWindow != showSkipCreditsBanner) showSkipCreditsBanner = inWindow
        } else {
            showSkipCreditsBanner = false
        }
    }

    // ─── Next episode overlay ────────────────────────────────────────────
    // Caso A: creditsMarker presente (del plugin) → showAt=StartTicks, hideAt=EndTicks
    //         countdown = (hideAtMs - currentPosition) / 1000 en vivo
    // Caso B: sin marker → sólo cuando STATE_ENDED (ver listener abajo)
    LaunchedEffect(currentPosition, creditsMarker, nextEpisode) {
        if (nextEpisode == null) {
            if (showNextEpisodeOverlay) showNextEpisodeOverlay = false
            return@LaunchedEffect
        }

        val marker = creditsMarker
        if (marker != null) {
            // Rango del overlay = el mismo rango que el plugin indica para el outro
            val inRange = currentPosition in marker.showAtMs..marker.hideAtMs
            if (inRange) {
                // Cuenta atrás = ticks reales que quedan hasta el fin del outro
                nextEpisodeCountdown = ((marker.hideAtMs - currentPosition) / 1000L).toInt().coerceAtLeast(0)
                if (!showNextEpisodeOverlay) showNextEpisodeOverlay = true
            } else if (currentPosition > marker.hideAtMs) {
                if (showNextEpisodeOverlay) {
                    showNextEpisodeOverlay = false
                    onPlayNextEpisode(nextEpisode!!)
                }
            } else {
                if (showNextEpisodeOverlay) showNextEpisodeOverlay = false
            }
        }
    }

    // Caso B: STATE_ENDED sin marcador de créditos → mostrar overlay sin countdown
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && nextEpisode != null && creditsMarker == null) {
                    nextEpisodeCountdown = -1 // sin cuenta atrás: el episodio ya ha terminado
                    showNextEpisodeOverlay = true
                }
            }
        })
    }

    // Cuando el countdown llega a 0 (caso A: créditos terminaron), lanzar siguiente episodio
    LaunchedEffect(nextEpisodeCountdown) {
        if (nextEpisodeCountdown == 0 && showNextEpisodeOverlay && nextEpisode != null) {
            showNextEpisodeOverlay = false
            onPlayNextEpisode(nextEpisode!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (showResumeDialog) return@onKeyEvent false
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

                when (keyEvent.key) {
                    Key.MediaRewind -> {
                        seekRelative(-10_000L)
                        true
                    }
                    Key.MediaFastForward -> {
                        seekRelative(+10_000L)
                        true
                    }
                    Key.MediaPlayPause -> {
                        notifyInteraction()
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }
                    Key.MediaPlay -> {
                        notifyInteraction()
                        exoPlayer.play()
                        true
                    }
                    Key.MediaPause -> {
                        notifyInteraction()
                        exoPlayer.pause()
                        true
                    }
                    Key.DirectionLeft -> {
                        if (!showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
                            seekRelative(-10_000L)
                            true
                        } else {
                            notifyInteraction()
                            false // Let Compose focus system navigate to the Rewind button on the left
                        }
                    }
                    Key.DirectionRight -> {
                        if (!showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
                            seekRelative(+10_000L)
                            true
                        } else {
                            notifyInteraction()
                            false // Let Compose focus system navigate to the Forward button on the right
                        }
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!showOverlayControls) {
                            showOverlayControls = true
                            notifyInteraction()
                            true
                        } else {
                            notifyInteraction()
                            false
                        }
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (!showOverlayControls) {
                            showOverlayControls = true
                            notifyInteraction()
                            true
                        } else {
                            notifyInteraction()
                            false
                        }
                    }
                    else -> false
                }
            }
            .clickable(enabled = !showResumeDialog) {
                notifyInteraction()
                showOverlayControls = !showOverlayControls
            }
    ) {
        // Video Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = currentResizeMode
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.resizeMode = currentResizeMode
                playerView.keepScreenOn = isPlaying || showOverlayControls
            },
            modifier = Modifier.fillMaxSize()
        )

        // Resume Playback Dialog Overlay (Netflix-style)
        if (showResumeDialog && info.startPositionMs >= 10_000L) {
            ResumePlaybackDialog(
                savedPositionMs = info.startPositionMs,
                onResume = {
                    showResumeDialog = false
                    resumeDecisionMade = true
                    chosenStartPositionMs = info.startPositionMs
                    exoPlayer.seekTo(info.startPositionMs)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                },
                onStartOver = {
                    showResumeDialog = false
                    resumeDecisionMade = true
                    chosenStartPositionMs = 0L
                    exoPlayer.seekTo(0L)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                },
                isMonochrome = isMonochrome,
                focusColor = focusColor,
                focusContent = focusContent
            )
        }

        // Buffering / Loading Indicator Overlay
        if (isBuffering && playerErrorMessage == null && !showResumeDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xEE121320))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    com.example.tujelly.ui.components.JellyLoadingIndicator(
                        size = 28.dp,
                        message = null,
                        isMonochrome = isMonochrome
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Cargando reproducción...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Playback Error Overlay
        if (playerErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xEE1A1828))
                        .border(1.dp, Color(0x66FF5252), RoundedCornerShape(20.dp))
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error de reproducción",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerErrorMessage ?: "No se pudo reproducir este archivo",
                        color = Color(0xFFDDDDDD),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                playerErrorMessage = null
                                retryTrigger++
                            },
                            modifier = Modifier.focusRequester(retryFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF3F51B5),
                                focusedContainerColor = Color(0xFF5C6BC0)
                            )
                        ) {
                            Text("Reintentar", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF2C2D3C),
                                focusedContainerColor = Color(0xFF424458)
                            )
                        ) {
                            Text("Volver", color = Color.White)
                        }
                    }
                }
            }
        }

        // Seek Indicator Overlay (Center Toast for D-Pad Left/Right & Media Keys)
        AnimatedVisibility(
            visible = seekIndicatorText != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xEE121320))
                    .border(1.5.dp, if (isMonochrome) Color.White else focusColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = seekIndicatorText ?: "",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Apple TV+ / High-End Cinema HUD Controls Overlay
        AnimatedVisibility(
            visible = showOverlayControls && !showResumeDialog,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar: Back Button + Title + Minimal Metadata
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = info.title.ifBlank { "Reproduciendo" },
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "1080p  •  AAC 5.1  •  Jellyfin Direct",
                            color = Color(0x99FFFFFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom Floating Apple TV Style HUD Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress Scrubber
                        TvProgressBar(
                            currentPositionMs = currentPosition,
                            bufferedPositionMs = bufferedPosition,
                            durationMs = duration,
                            isMonochrome = isMonochrome,
                            focusColor = focusColor,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time & Unified Glass Controls Row (Ergonomic Playback Controls on Left, Options on Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Group: Rewind 10s, Play / Pause (Center Focus), Fast Forward 10s, and Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Rewind 10s (Retroceso)
                                TvCircularIconButton(
                                    onClick = {
                                        seekRelative(-10_000L)
                                    },
                                    icon = Icons.Default.FastRewind,
                                    contentDescription = "Retroceder 10 segundos",
                                    size = 42.dp,
                                    iconSize = 20.dp,
                                    containerColor = Color(0x22FFFFFF),
                                    focusedContainerColor = if (isMonochrome) Color.White else focusColor,
                                    contentColor = Color.White,
                                    focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor
                                )

                                // 2. Play / Pause (Primary Action - Centered & Default Focus)
                                TvCircularIconButton(
                                    onClick = {
                                        notifyInteraction()
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            exoPlayer.play()
                                        }
                                    },
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    size = 48.dp,
                                    iconSize = 26.dp,
                                    containerColor = if (isMonochrome) Color.White else focusColor,
                                    focusedContainerColor = if (isMonochrome) Color.White else focusColor,
                                    contentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                                    focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusRequester = playPauseFocusRequester
                                )

                                // 3. Fast Forward 10s (Avance)
                                TvCircularIconButton(
                                    onClick = {
                                        seekRelative(+10_000L)
                                    },
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Adelantar 10 segundos",
                                    size = 42.dp,
                                    iconSize = 20.dp,
                                    containerColor = Color(0x22FFFFFF),
                                    focusedContainerColor = if (isMonochrome) Color.White else focusColor,
                                    contentColor = Color.White,
                                    focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Time Indicator
                                Text(
                                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Right Group: Audio, Subtitles, Settings (Respects buttonStyle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 4. Audio Selector Button
                                TvPillButton(
                                    onClick = {
                                        notifyInteraction()
                                        activeModalTab = PlayerModalTab.AUDIO
                                    },
                                    icon = Icons.Default.Audiotrack,
                                    label = "Audio",
                                    isSelected = activeModalTab == PlayerModalTab.AUDIO,
                                    buttonStyle = buttonStyle,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent
                                )

                                // 5. Subtitles Selector Button
                                TvPillButton(
                                    onClick = {
                                        notifyInteraction()
                                        activeModalTab = PlayerModalTab.SUBTITLES
                                    },
                                    icon = Icons.Default.Subtitles,
                                    label = "Subtítulos",
                                    isSelected = activeModalTab == PlayerModalTab.SUBTITLES,
                                    buttonStyle = buttonStyle,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent
                                )

                                // 6. Settings Selector Button
                                TvPillButton(
                                    onClick = {
                                        notifyInteraction()
                                        activeModalTab = PlayerModalTab.SETTINGS
                                    },
                                    icon = Icons.Default.Tune,
                                    label = "Ajustes",
                                    isSelected = activeModalTab == PlayerModalTab.SETTINGS,
                                    buttonStyle = buttonStyle,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom TV Modal Sheet Overlay for Track / Settings Selection
        if (activeModalTab != PlayerModalTab.NONE) {
            PlayerCustomModalSheet(
                activeTab = activeModalTab,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                currentSpeed = currentPlaybackSpeed,
                currentResizeMode = currentResizeMode,
                onSelectAudioTrack = { track ->
                    notifyInteraction()
                    selectAudioTrackInExoPlayer(exoPlayer, track)
                },
                onSelectSubtitleTrack = { track ->
                    notifyInteraction()
                    selectSubtitleTrackInExoPlayer(exoPlayer, track)
                },
                onSelectSpeed = { speed ->
                    notifyInteraction()
                    currentPlaybackSpeed = speed
                    exoPlayer.setPlaybackSpeed(speed)
                },
                onSelectResizeMode = { mode ->
                    notifyInteraction()
                    currentResizeMode = mode
                },
                onClose = {
                    notifyInteraction()
                    activeModalTab = PlayerModalTab.NONE
                },
                isMonochrome = isMonochrome,
                focusColor = focusColor,
                focusContent = focusContent
            )
        }

        // ─── Skip Intro Banner (bottom-right, Netflix-style) ─────────────────
        AnimatedVisibility(
            visible = showSkipIntroBanner && !showResumeDialog && activeModalTab == PlayerModalTab.NONE,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 120.dp)
        ) {
            SkipIntroBanner(
                isMonochrome = isMonochrome,
                focusColor = focusColor,
                focusContent = focusContent,
                onSkip = {
                    // skipToMs = IntroEnd del plugin, el campo exacto para saltar
                    val skipTo = introMarker?.skipToMs ?: return@SkipIntroBanner
                    exoPlayer.seekTo(skipTo)
                    showSkipIntroBanner = false
                    notifyInteraction()
                }
            )
        }

        // ─── Skip Credits Banner ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSkipCreditsBanner && !showResumeDialog && activeModalTab == PlayerModalTab.NONE,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 120.dp)
        ) {
            SkipIntroBanner(
                label = "Omitir créditos",
                isMonochrome = isMonochrome,
                focusColor = focusColor,
                focusContent = focusContent,
                onSkip = {
                    // skipToMs = EndPositionTicks del segmento Outro
                    val skipTo = creditsMarker?.skipToMs ?: return@SkipIntroBanner
                    exoPlayer.seekTo(skipTo)
                    showSkipCreditsBanner = false
                    notifyInteraction()
                }
            )
        }

        // ─── Next Episode Overlay (Apple TV+ style, bottom-right) ────────────
        AnimatedVisibility(
            visible = showNextEpisodeOverlay && !showResumeDialog && nextEpisode != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 120.dp)
        ) {
            nextEpisode?.let { ep ->
                NextEpisodeCard(
                    nextEpisode = ep,
                    countdown = nextEpisodeCountdown,
                    isMonochrome = isMonochrome,
                    focusColor = focusColor,
                    focusContent = focusContent,
                    onPlayNow = {
                        showNextEpisodeOverlay = false
                        onPlayNextEpisode(ep)
                    },
                    onDismiss = {
                        showNextEpisodeOverlay = false
                        nextEpisodeCountdown = -1 // Prevent auto-play
                    }
                )
            }
        }
    }
}

@ExperimentalTvMaterial3Api
@Composable
private fun SkipIntroBanner(
    label: String = "Omitir intro",
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A),
    onSkip: () -> Unit
) {
    val skipFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120L)
        runCatching { skipFocusRequester.requestFocus() }
    }

    Surface(
        onClick = onSkip,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xEE1A1C2E),
            focusedContainerColor = if (isMonochrome) Color.White else focusColor,
            contentColor = Color.White,
            focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.5.dp, if (isMonochrome) Color(0x66FFFFFF) else focusColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (isMonochrome) Color.White else focusColor),
                shape = RoundedCornerShape(14.dp)
            )
        ),
        modifier = Modifier.focusRequester(skipFocusRequester)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@ExperimentalTvMaterial3Api
@Composable
private fun NextEpisodeCard(
    nextEpisode: NextEpisodeInfo,
    countdown: Int,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A),
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120L)
        runCatching { playFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .width(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0111422))
            .border(1.5.dp, if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    // Si hay countdown activo: "Siguiente en Xs" · Si no (STATE_ENDED): "Siguiente episodio"
                    text = if (countdown > 0) "Siguiente en ${countdown}s" else "Siguiente episodio",
                    color = if (isMonochrome) Color.White else focusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                // Círculo de countdown sólo cuando hay créditos con tiempo restante real
                if (countdown > 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.18f))
                            .border(1.5.dp, if (isMonochrome) Color.White else focusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$countdown",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Episode info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Episode code badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = nextEpisode.episodeCode,
                        color = if (isMonochrome) Color.White else focusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = nextEpisode.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play now (primary, focused by default)
                Surface(
                    onClick = onPlayNow,
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isMonochrome) Color.White else focusColor,
                        focusedContainerColor = if (isMonochrome) Color(0xFFE0E7FF) else focusColor,
                        contentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                        focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
                    ),
                    modifier = Modifier
                        .focusRequester(playFocusRequester)
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reproducir ahora",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dismiss
                Surface(
                    onClick = onDismiss,
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x22FFFFFF),
                        focusedContainerColor = Color(0x44FFFFFF),
                        contentColor = Color.White,
                        focusedContentColor = Color.White
                    )
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Cancelar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


@ExperimentalTvMaterial3Api
@Composable
private fun TvCircularIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    containerColor: Color = Color(0x22FFFFFF),
    focusedContainerColor: Color = Color.White,
    contentColor: Color = Color.White,
    focusedContentColor: Color = Color(0xFF0F172A),
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = focusedContainerColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, Color(0x18FFFFFF)), shape = CircleShape),
            focusedBorder = Border(border = BorderStroke(1.5.dp, if (isMonochrome) Color.White else focusColor), shape = CircleShape)
        ),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) focusedContentColor else contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@ExperimentalTvMaterial3Api
@Composable
private fun TvPillButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    buttonStyle: String = BUTTON_STYLE_ICONS_AND_TEXT,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val showIcons = buttonStyle != BUTTON_STYLE_TEXT_ONLY
    val showText = buttonStyle != BUTTON_STYLE_ICONS_ONLY

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) (if (isMonochrome) Color(0x35FFFFFF) else focusColor.copy(alpha = 0.22f)) else Color(0x18FFFFFF),
            focusedContainerColor = if (isMonochrome) Color.White else focusColor,
            contentColor = Color.White,
            focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) (if (isMonochrome) Color.White else focusColor) else Color(0x20FFFFFF)),
                shape = RoundedCornerShape(20.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(1.5.dp, if (isMonochrome) Color.White else focusColor),
                shape = RoundedCornerShape(20.dp)
            )
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = if (showText) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currentContentColor = if (isFocused) (if (isMonochrome) Color(0xFF0F172A) else focusContent) else Color.White
            if (showIcons) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = currentContentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (showText) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = currentContentColor
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@ExperimentalTvMaterial3Api
@Composable
private fun PlayerCustomModalSheet(
    activeTab: PlayerModalTab,
    audioTracks: List<TrackItem>,
    subtitleTracks: List<TrackItem>,
    currentSpeed: Float,
    currentResizeMode: Int,
    onSelectAudioTrack: (TrackItem) -> Unit,
    onSelectSubtitleTrack: (TrackItem?) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectResizeMode: (Int) -> Unit,
    onClose: () -> Unit,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onClose() },
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .clickable(enabled = false) {}
                .border(1.dp, if (isMonochrome) Color(0x22FFFFFF) else focusColor.copy(alpha = 0.35f), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            colors = NonInteractiveSurfaceDefaults.colors(
                containerColor = Color(0xFA11121E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Modal Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val titleIcon = when (activeTab) {
                            PlayerModalTab.AUDIO -> Icons.Default.Audiotrack
                            PlayerModalTab.SUBTITLES -> Icons.Default.Subtitles
                            PlayerModalTab.SETTINGS -> Icons.Default.Tune
                            PlayerModalTab.NONE -> Icons.Default.Settings
                        }
                        val titleText = when (activeTab) {
                            PlayerModalTab.AUDIO -> "Pistas de Audio"
                            PlayerModalTab.SUBTITLES -> "Subtítulos"
                            PlayerModalTab.SETTINGS -> "Ajustes de Reproducción"
                            PlayerModalTab.NONE -> ""
                        }

                        Icon(
                            imageVector = titleIcon,
                            contentDescription = null,
                            tint = if (isMonochrome) Color.White else focusColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = titleText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x22FFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = if (isMonochrome) Color.White else focusColor,
                            focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Modal Content Body
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (activeTab) {
                        PlayerModalTab.AUDIO -> {
                            if (audioTracks.isEmpty()) {
                                item {
                                    Text(
                                        text = "No se detectaron pistas de audio adicionales.",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                itemsIndexed(audioTracks) { _, track ->
                                    TrackOptionItem(
                                        label = track.label,
                                        isSelected = track.isSelected,
                                        isMonochrome = isMonochrome,
                                        focusColor = focusColor,
                                        focusContent = focusContent,
                                        onClick = {
                                            onSelectAudioTrack(track)
                                            onClose()
                                        }
                                    )
                                }
                            }
                        }

                        PlayerModalTab.SUBTITLES -> {
                            item {
                                val isSubDisabled = subtitleTracks.none { it.isSelected }
                                TrackOptionItem(
                                    label = "Desactivados",
                                    isSelected = isSubDisabled,
                                    icon = Icons.Default.SubtitlesOff,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent,
                                    onClick = {
                                        onSelectSubtitleTrack(null)
                                        onClose()
                                    }
                                )
                            }

                            itemsIndexed(subtitleTracks) { _, track ->
                                TrackOptionItem(
                                    label = track.label,
                                    isSelected = track.isSelected,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent,
                                    onClick = {
                                        onSelectSubtitleTrack(track)
                                        onClose()
                                    }
                                )
                            }
                        }

                        PlayerModalTab.SETTINGS -> {
                            item {
                                Text(
                                    text = "Velocidad de Reproducción",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            itemsIndexed(speeds) { _, speed ->
                                val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                                TrackOptionItem(
                                    label = label,
                                    isSelected = currentSpeed == speed,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent,
                                    onClick = {
                                        onSelectSpeed(speed)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Relación de Aspecto",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            val modes = listOf(
                                Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Ajustar a Pantalla (Proporcional)"),
                                Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Recortar / Zoom"),
                                Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Estirar / Pantalla Completa")
                            )

                            itemsIndexed(modes) { _, (mode, label) ->
                                TrackOptionItem(
                                    label = label,
                                    isSelected = currentResizeMode == mode,
                                    isMonochrome = isMonochrome,
                                    focusColor = focusColor,
                                    focusContent = focusContent,
                                    onClick = {
                                        onSelectResizeMode(mode)
                                    }
                                )
                            }
                        }

                        PlayerModalTab.NONE -> {}
                    }
                }
            }
        }
    }
}

@ExperimentalTvMaterial3Api
@Composable
private fun TrackOptionItem(
    label: String,
    isSelected: Boolean,
    icon: ImageVector? = null,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) (if (isMonochrome) Color(0x35FFFFFF) else focusColor.copy(alpha = 0.25f)) else Color(0x14FFFFFF),
            contentColor = Color.White,
            focusedContainerColor = if (isMonochrome) Color.White else focusColor,
            focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) (if (isMonochrome) Color.White else focusColor) else Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                contentDescription = null,
                tint = if (isSelected) (if (isMonochrome) Color.White else focusColor) else Color(0x33FFFFFF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TvProgressBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val bufferProgress = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x33FFFFFF))
        )

        // Buffer Track
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x55FFFFFF))
        )

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isMonochrome) {
                        Brush.horizontalGradient(listOf(Color.White, Color.White))
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF22D3EE), focusColor)
                        )
                    }
                )
        )

        // Thumb Dot
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(14.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(if (isMonochrome) Color.White else focusColor)
                    .border(1.dp, Color(0x66000000), CircleShape)
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun extractTracksFromExoPlayer(exoPlayer: ExoPlayer): Pair<List<TrackItem>, List<TrackItem>> {
    val audioTracks = mutableListOf<TrackItem>()
    val subtitleTracks = mutableListOf<TrackItem>()

    val currentTracks = exoPlayer.currentTracks
    for (group in currentTracks.groups) {
        val trackType = group.type
        val mediaTrackGroup = group.mediaTrackGroup
        for (i in 0 until mediaTrackGroup.length) {
            val format = mediaTrackGroup.getFormat(i)
            val isSelected = group.isTrackSelected(i)
            val lang = format.language?.takeIf { it.isNotBlank() && it != "und" } ?: "Desconocido"
            val rawLabel = format.label?.takeIf { it.isNotBlank() } ?: "Pista ${i + 1}"

            if (trackType == C.TRACK_TYPE_AUDIO) {
                val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                val channels = if (format.channelCount > 0) "${format.channelCount} ch" else ""
                val displayParts = listOfNotNull(
                    rawLabel.ifBlank { null },
                    lang.uppercase(),
                    codec.ifBlank { null },
                    channels.ifBlank { null }
                )
                val displayLabel = displayParts.joinToString(" • ")

                audioTracks.add(
                    TrackItem(
                        group = group,
                        trackIndex = i,
                        label = displayLabel,
                        language = lang,
                        isSelected = isSelected
                    )
                )
            } else if (trackType == C.TRACK_TYPE_TEXT) {
                val displayLabel = if (!rawLabel.contains(lang, ignoreCase = true)) {
                    "$rawLabel (${lang.uppercase()})"
                } else {
                    rawLabel
                }

                subtitleTracks.add(
                    TrackItem(
                        group = group,
                        trackIndex = i,
                        label = displayLabel,
                        language = lang,
                        isSelected = isSelected
                    )
                )
            }
        }
    }

    return Pair(audioTracks, subtitleTracks)
}

@OptIn(UnstableApi::class)
private fun selectAudioTrackInExoPlayer(exoPlayer: ExoPlayer, trackItem: TrackItem) {
    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(
            TrackSelectionOverride(
                trackItem.group.mediaTrackGroup,
                trackItem.trackIndex
            )
        )
        .build()
}

@OptIn(UnstableApi::class)
private fun selectSubtitleTrackInExoPlayer(exoPlayer: ExoPlayer, trackItem: TrackItem?) {
    if (trackItem == null) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
    } else {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(
                TrackSelectionOverride(
                    trackItem.group.mediaTrackGroup,
                    trackItem.trackIndex
                )
            )
            .build()
    }
}

private fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        java.lang.String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        java.lang.String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@ExperimentalTvMaterial3Api
@Composable
private fun ResumePlaybackDialog(
    savedPositionMs: Long,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    isMonochrome: Boolean = false,
    focusColor: Color = Color.White,
    focusContent: Color = Color(0xFF0F172A)
) {
    val resumeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150L)
        try {
            resumeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE121424))
                .border(1.dp, if (isMonochrome) Color(0x33FFFFFF) else focusColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Resume button (focused by default) with position included
            Button(
                onClick = onResume,
                modifier = Modifier.focusRequester(resumeFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = if (isMonochrome) Color.White else focusColor,
                    contentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent,
                    focusedContainerColor = if (isMonochrome) Color(0xFFE0E7FF) else focusColor,
                    focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
                ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Reanudar (${formatTime(savedPositionMs)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Start from beginning button
            Button(
                onClick = onStartOver,
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x22FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = if (isMonochrome) Color.White else focusColor,
                    focusedContentColor = if (isMonochrome) Color(0xFF0F172A) else focusContent
                ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Desde el principio",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
