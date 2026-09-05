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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val context = LocalContext.current

    LaunchedEffect(itemId) {
        viewModel.loadStreamUrl(itemId)
    }

    val streamInfo by viewModel.streamInfo.collectAsState()
    val buttonStyle by viewModel.buttonStyle.collectAsState()

    var showOverlayControls by remember { mutableStateOf(true) }
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }
    var activeModalTab by remember { mutableStateOf(PlayerModalTab.NONE) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }

    val activity = remember(context) { context.findActivity() }

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
    val playPauseFocusRequester = remember { FocusRequester() }

    // Auto-focus the play/pause button when controls overlay is visible so remote D-pad works immediately!
    LaunchedEffect(showOverlayControls, streamInfo) {
        if (showOverlayControls && streamInfo != null) {
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
        }
    }

    // Auto-hide controls overlay after 5s if activeModalTab is NONE
    LaunchedEffect(showOverlayControls, activeModalTab) {
        if (showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
            delay(5000L)
            showOverlayControls = false
        }
    }

    BackHandler {
        if (activeModalTab != PlayerModalTab.NONE) {
            activeModalTab = PlayerModalTab.NONE
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                showOverlayControls = true

                when (keyEvent.key) {
                    Key.DirectionLeft, Key.MediaRewind -> {
                        if (!showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
                            seekIndicatorText = "⏪ -10s"
                        }
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        if (!showOverlayControls && activeModalTab == PlayerModalTab.NONE) {
                            seekIndicatorText = "⏩ +10s"
                        }
                    }
                }
                false
            }
            .clickable {
                showOverlayControls = !showOverlayControls
            }
    ) {
        if (streamInfo != null) {
            val info = streamInfo!!
            val candidates = info.candidateUrls.ifEmpty { listOf(info.primaryStreamUrl) }

            val exoPlayer = remember(info) {
                var candidateIdx = 0

                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(30_000)
                    .setUserAgent("Mozilla/5.0 (Linux; Android 14; Google TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 60_000, 2_500, 5_000)
                    .build()

                val renderersFactory = DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

                ExoPlayer.Builder(context)
                    .setRenderersFactory(renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(loadControl)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build().apply {

                        val subtitleConfigs = info.subtitles.map { sub ->
                            val mimeType = when {
                                sub.codec.contains("vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                                sub.codec.contains("ass", ignoreCase = true) || sub.codec.contains("ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                                else -> MimeTypes.APPLICATION_SUBRIP
                            }
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                .setMimeType(mimeType)
                                .setLanguage(sub.language)
                                .setLabel(sub.title)
                                .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                                .build()
                        }

                        val mediaItem = MediaItem.Builder()
                            .setUri(candidates.first())
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(info.title)
                                    .setDisplayTitle(info.title)
                                    .build()
                            )
                            .setSubtitleConfigurations(subtitleConfigs)
                            .build()

                        setMediaItem(mediaItem)

                        if (info.startPositionMs > 0) {
                            seekTo(info.startPositionMs)
                        }

                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isBuffering = (playbackState == Player.STATE_BUFFERING)
                            }

                            override fun onTracksChanged(tracks: Tracks) {
                                val (audios, subs) = extractTracksFromExoPlayer(this@apply)
                                audioTracks = audios
                                subtitleTracks = subs
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                if (candidateIdx < candidates.size - 1) {
                                    candidateIdx++
                                    val nextUrl = candidates[candidateIdx]
                                    val pos = currentPosition
                                    val fallbackItem = MediaItem.Builder()
                                        .setUri(nextUrl)
                                        .setMediaMetadata(
                                            androidx.media3.common.MediaMetadata.Builder()
                                                .setTitle(info.title)
                                                .setDisplayTitle(info.title)
                                                .build()
                                        )
                                        .setSubtitleConfigurations(subtitleConfigs)
                                        .build()
                                    setMediaItem(fallbackItem)
                                    if (pos > 0) seekTo(pos)
                                    prepare()
                                    playWhenReady = true
                                }
                            }
                        })

                        prepare()
                        playWhenReady = true
                    }
            }

            // Sync position & Report progress
            LaunchedEffect(exoPlayer) {
                viewModel.reportStart(info.startPositionMs)
                while (isActive) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    currentPosition = pos
                    duration = dur
                    bufferedPosition = buf

                    delay(250L)
                    if (pos > 0) {
                        viewModel.reportProgress(pos, !exoPlayer.isPlaying)
                    }
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    val finalPos = exoPlayer.currentPosition
                    viewModel.reportStopped(finalPos)
                    exoPlayer.release()
                }
            }

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

            // Buffering / Loading Indicator Overlay
            if (isBuffering) {
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
                            message = null
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

            // Seek Indicator Overlay (Center Top Toast for D-Pad Left/Right)
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
                        .border(1.dp, Color.White, RoundedCornerShape(24.dp))
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
                visible = showOverlayControls,
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
                        TvCircularIconButton(
                            onClick = onBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            size = 44.dp,
                            iconSize = 20.dp
                        )

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
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Time & Unified Glass Controls Row (Play & Seek on Left, Options on Right)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left Group: Play / Pause, Rewind 10s, Forward 10s, and Time
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Play / Pause
                                    TvCircularIconButton(
                                        onClick = {
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.play()
                                            }
                                        },
                                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                        size = 46.dp,
                                        iconSize = 24.dp,
                                        containerColor = Color.White,
                                        focusedContainerColor = Color.White,
                                        contentColor = Color(0xFF0F172A),
                                        focusedContentColor = Color(0xFF0F172A),
                                        focusRequester = playPauseFocusRequester
                                    )

                                    // 2. Rewind 10s
                                    TvCircularIconButton(
                                        onClick = {
                                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                            exoPlayer.seekTo(newPos)
                                            seekIndicatorText = "⏪ -10s"
                                        },
                                        icon = Icons.Default.FastRewind,
                                        contentDescription = "Retroceder 10 segundos",
                                        size = 42.dp,
                                        iconSize = 20.dp,
                                        containerColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color.White,
                                        contentColor = Color.White,
                                        focusedContentColor = Color(0xFF0F172A)
                                    )

                                    // 3. Fast Forward 10s
                                    TvCircularIconButton(
                                        onClick = {
                                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                            exoPlayer.seekTo(newPos)
                                            seekIndicatorText = "⏩ +10s"
                                        },
                                        icon = Icons.Default.FastForward,
                                        contentDescription = "Adelantar 10 segundos",
                                        size = 42.dp,
                                        iconSize = 20.dp,
                                        containerColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color.White,
                                        contentColor = Color.White,
                                        focusedContentColor = Color(0xFF0F172A)
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
                                            activeModalTab = PlayerModalTab.AUDIO
                                        },
                                        icon = Icons.Default.Audiotrack,
                                        label = "Audio",
                                        isSelected = activeModalTab == PlayerModalTab.AUDIO,
                                        buttonStyle = buttonStyle
                                    )

                                    // 5. Subtitles Selector Button
                                    TvPillButton(
                                        onClick = {
                                            activeModalTab = PlayerModalTab.SUBTITLES
                                        },
                                        icon = Icons.Default.Subtitles,
                                        label = "Subtítulos",
                                        isSelected = activeModalTab == PlayerModalTab.SUBTITLES,
                                        buttonStyle = buttonStyle
                                    )

                                    // 6. Settings Selector Button
                                    TvPillButton(
                                        onClick = {
                                            activeModalTab = PlayerModalTab.SETTINGS
                                        },
                                        icon = Icons.Default.Tune,
                                        label = "Ajustes",
                                        isSelected = activeModalTab == PlayerModalTab.SETTINGS,
                                        buttonStyle = buttonStyle
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
                        selectAudioTrackInExoPlayer(exoPlayer, track)
                    },
                    onSelectSubtitleTrack = { track ->
                        selectSubtitleTrackInExoPlayer(exoPlayer, track)
                    },
                    onSelectSpeed = { speed ->
                        currentPlaybackSpeed = speed
                        exoPlayer.setPlaybackSpeed(speed)
                    },
                    onSelectResizeMode = { mode ->
                        currentResizeMode = mode
                    },
                    onClose = {
                        activeModalTab = PlayerModalTab.NONE
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
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
            focusedBorder = Border(border = BorderStroke(1.5.dp, Color.White), shape = CircleShape)
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
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val showIcons = buttonStyle != BUTTON_STYLE_TEXT_ONLY
    val showText = buttonStyle != BUTTON_STYLE_ICONS_ONLY

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x35FFFFFF) else Color(0x18FFFFFF),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color(0xFF0F172A)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0x20FFFFFF)),
                shape = RoundedCornerShape(20.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(1.5.dp, Color.White),
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
            val currentContentColor = if (isFocused) Color(0xFF0F172A) else Color.White
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
    onClose: () -> Unit
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
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
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
                            tint = Color.White,
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
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color(0xFF0F172A)
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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) Color(0x35FFFFFF) else Color(0x14FFFFFF),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF0F172A)
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
                        tint = if (isSelected) Color.White else Color(0xFF94A3B8)
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
                tint = if (isSelected) Color.White else Color(0x33FFFFFF),
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
                .background(Color.White)
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
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0x44000000), CircleShape)
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
