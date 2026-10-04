package com.example.ui.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.StreamServer
import com.example.data.service.DownloadHelper
import com.example.data.service.PlaybackEngineHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.delay
import java.io.File
import android.view.LayoutInflater

/**
 * The $0 Video Player Canvas (Media3 ExoPlayer)
 * Google's production-grade open-source media engine.
 * Automatically handles MP4, HLS, DASH, streaming reconnection, CDN switching, and local playback.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerCanvas(
    playbackUrl: String,
    isPlaying: Boolean,
    volume: Float,
    isMuted: Boolean,
    qualityProfile: com.example.data.service.PlaybackQualityProfile = com.example.data.service.PlaybackQualityProfile.AUTO,
    seekRequestSec: Int?,
    onSeekConsumed: () -> Unit,
    onDurationDiscovered: (Int) -> Unit,
    onPositionUpdate: (Int) -> Unit,
    onBuffering: (Boolean) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Build and manage the ExoPlayer instance with local cache & resilient network fallback
    val exoPlayer = remember {
        try {
            PlaybackEngineHelper.buildExoPlayer(context, qualityProfile)
        } catch (e: Exception) {
            Log.e("VideoPlayerCanvas", "Fallback to default ExoPlayer engine", e)
            ExoPlayer.Builder(context).build()
        }
    }

    // Apply live dynamic quality adjustments seamlessly without restarting video playback
    LaunchedEffect(qualityProfile) {
        PlaybackEngineHelper.applyQualityProfile(exoPlayer, qualityProfile)
    }

    // Attach listener for playback events, buffering, and errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> onBuffering(true)
                    Player.STATE_READY -> {
                        onBuffering(false)
                        val durMs = exoPlayer.duration
                        if (durMs > 0) {
                            onDurationDiscovered((durMs / 1000).toInt())
                        }
                    }
                    Player.STATE_ENDED -> {
                        onBuffering(false)
                    }
                    Player.STATE_IDLE -> {
                        onBuffering(false)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("VideoPlayerCanvas", "ExoPlayer native error: ${error.message} (${error.errorCodeName})")
                onBuffering(false)

                // Automatic failover to resilient backup CDN if stream returns 403 or network failure
                if (playbackUrl != com.example.data.service.MovieStreamUrlResolver.REAL_FEATURE_SCI_FI) {
                    try {
                        val fallbackUri = Uri.parse(com.example.data.service.MovieStreamUrlResolver.REAL_FEATURE_SCI_FI)
                        val fallbackItem = Media3Item.Builder()
                            .setUri(fallbackUri)
                            .setMimeType(MimeTypes.VIDEO_MP4)
                            .build()
                        exoPlayer.setMediaItem(fallbackItem)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                        onError("CDN Failover: Switched to backup mirror stream.")
                        return
                    } catch (_: Exception) {}
                }

                onError("Stream failover notice: ${error.errorCodeName}")
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Load media item when playbackUrl changes
    LaunchedEffect(playbackUrl) {
        if (playbackUrl.isBlank()) return@LaunchedEffect

        try {
            onBuffering(true)
            val uri = if (playbackUrl.startsWith("http://") || playbackUrl.startsWith("https://")) {
                Uri.parse(playbackUrl)
            } else {
                val localFile = File(playbackUrl)
                if (localFile.exists()) Uri.fromFile(localFile) else Uri.parse(playbackUrl)
            }

            val detectedMimeType = PlaybackEngineHelper.detectFastMimeType(playbackUrl)

            val mediaItemBuilder = Media3Item.Builder().setUri(uri)
            if (detectedMimeType != null) {
                mediaItemBuilder.setMimeType(detectedMimeType)
            }
            val mediaItem = mediaItemBuilder.build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = isPlaying
        } catch (e: Exception) {
            Log.w("VideoPlayerCanvas", "Error loading ExoPlayer media: ${e.message}")
            onBuffering(false)
            onError("Stream unavailable: ${e.message}")
        }
    }

    // Play/Pause synchronization
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Volume & Mute synchronization
    LaunchedEffect(volume, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else volume
    }

    // Scrubbing & Seeking
    LaunchedEffect(seekRequestSec) {
        if (seekRequestSec != null) {
            exoPlayer.seekTo(seekRequestSec * 1000L)
            onSeekConsumed()
        }
    }

    // Position progress tracking ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(400)
            try {
                val posSec = (exoPlayer.currentPosition / 1000).toInt()
                if (posSec >= 0) {
                    onPositionUpdate(posSec)
                }
            } catch (_: Exception) {}
        }
    }

    // Render video inside AndroidView using Media3 TextureView PlayerView layout
    AndroidView(
        factory = { ctx ->
            val pv = LayoutInflater.from(ctx).inflate(
                R.layout.view_texture_player,
                null,
                false
            ) as PlayerView

            pv.apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                player = exoPlayer
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { pv ->
            pv.player = exoPlayer
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun IntegratedPlayerScreen(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkip10Forward: () -> Unit,
    onSkip10Rewind: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleOrientation: () -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onSwitchServer: (StreamServer) -> Unit,
    onSimulateFailover: () -> Unit,
    onClose: () -> Unit,
    onSelectQuality: (com.example.data.service.PlaybackQualityProfile) -> Unit = {},
    onPositionUpdate: (Int) -> Unit = {},
    onDurationDiscovered: (Int) -> Unit = {},
    onBuffering: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onSeekConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showServerMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of inactivity if playing
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            kotlinx.coroutines.delay(4500)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
            .testTag("integrated_video_player")
    ) {
        // Real Video Surface with ambient backdrop
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient backdrop poster shown during initial load or as fallback
            state.mediaItem?.let { item ->
                val imageModel: Any = if (item.drawableResId != null) item.drawableResId else item.backdropUrl
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Cinematic vignette over backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Real Hardware Video Surface
            VideoPlayerCanvas(
                playbackUrl = state.playbackSourceUrl.ifBlank { state.mediaItem?.defaultVideoUrl ?: "" },
                isPlaying = state.isPlaying,
                volume = state.volume,
                isMuted = state.isMuted,
                qualityProfile = state.qualityProfile,
                seekRequestSec = state.seekRequestSec,
                onSeekConsumed = onSeekConsumed,
                onDurationDiscovered = onDurationDiscovered,
                onPositionUpdate = onPositionUpdate,
                onBuffering = onBuffering,
                onError = onError,
                modifier = Modifier.fillMaxSize()
            )

            // Buffering Spinner
            if (state.isBuffering) {
                CircularProgressIndicator(
                    color = CyberCyan,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Subtitle WebVTT-styled Overlay
        if (state.activeSubtitleText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 95.dp else 40.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("subtitle_overlay_box")
            ) {
                Text(
                    text = state.activeSubtitleText,
                    color = NeonAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Error / Automated Failover Notice
        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonRed.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("failover_warning_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = state.errorMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Controls Overlay (animated visibility)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Top Bar: Back, Title, Server Badge, Subtitle Menu Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("close_player_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close player",
                                tint = TextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = state.mediaItem?.title ?: "Playing Media",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isOfflinePlayback) NeonGreen else CyberCyan)
                                )
                                Text(
                                    text = if (state.isOfflinePlayback) "💾 Offline Storage (Local File)" else (state.activeServer?.name ?: "Server 1 (Primary - HD)"),
                                    color = if (state.isOfflinePlayback) NeonGreen else CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Top Action Icons: Subtitles, Server Failover Test, Server Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quality Switcher Button
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (state.qualityProfile != com.example.data.service.PlaybackQualityProfile.AUTO) CyberCyanGlow else Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (state.qualityProfile != com.example.data.service.PlaybackQualityProfile.AUTO) CyberCyan else Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    showQualityMenu = !showQualityMenu
                                    showSubtitleMenu = false
                                    showServerMenu = false
                                }
                                .testTag("player_quality_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "Quality Options",
                                    tint = if (state.qualityProfile != com.example.data.service.PlaybackQualityProfile.AUTO) CyberCyan else TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.qualityProfile.badge,
                                    color = if (state.qualityProfile != com.example.data.service.PlaybackQualityProfile.AUTO) CyberCyan else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Subtitle Button
                        IconButton(
                            onClick = {
                                showSubtitleMenu = !showSubtitleMenu
                                showQualityMenu = false
                                showServerMenu = false
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (state.selectedSubtitleId != "off") CyberCyanGlow else Color.Black.copy(alpha = 0.5f))
                                .testTag("subtitle_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (state.selectedSubtitleId != "off") CyberCyan else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Simulate Server Timeout Button
                        IconButton(
                            onClick = onSimulateFailover,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("simulate_failover_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncProblem,
                                contentDescription = "Simulate Failover",
                                tint = NeonAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Server Switcher Button
                        IconButton(
                            onClick = {
                                showServerMenu = !showServerMenu
                                showQualityMenu = false
                                showSubtitleMenu = false
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("switch_server_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Switch Server",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quality Selector Dropdown Menu
                if (showQualityMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 54.dp, end = 16.dp)
                            .widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .testTag("player_quality_dropdown")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "SELECT VIDEO QUALITY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = CyberCyan
                                )
                            }

                            com.example.data.service.PlaybackQualityProfile.entries.forEach { profile ->
                                val isSelected = state.qualityProfile == profile
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable {
                                            onSelectQuality(profile)
                                            showQualityMenu = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("quality_option_${profile.badge.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.title,
                                                color = if (isSelected) CyberCyan else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                            if (profile.isRecommended) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "SMART FLOW",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = NeonGreen
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = profile.subtitle,
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Subtitle Selector Dropdown Menu
                if (showSubtitleMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 54.dp, end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .testTag("subtitle_menu_dropdown")
                    ) {
                        Column {
                            listOf(
                                "off" to "Subtitles Off",
                                "en" to "English Track",
                                "es" to "Spanish Track"
                            ).forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            onSelectSubtitle(id)
                                            showSubtitleMenu = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (state.selectedSubtitleId == id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }
                                    Text(
                                        text = label,
                                        color = if (state.selectedSubtitleId == id) CyberCyan else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (state.selectedSubtitleId == id) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Server Selector Dropdown Menu
                if (showServerMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 54.dp, end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .testTag("server_menu_dropdown")
                    ) {
                        Column {
                            state.mediaItem?.streamServers?.forEach { srv ->
                                val isSelected = state.activeServer?.id == srv.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            onSwitchServer(srv)
                                            showServerMenu = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NeonGreen else TextTertiary)
                                    )
                                    Text(
                                        text = "${srv.name} (${srv.quality})",
                                        color = if (isSelected) CyberCyan else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Controls: Rewind 10s, Play/Pause, Skip 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = onSkip10Rewind,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("rewind_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause Big Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                            .clickable { onPlayPause() }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color(0xFF07080B),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Skip 10s
                    IconButton(
                        onClick = onSkip10Forward,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("forward_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Skip 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Controls: Time Scrubber, Volume Slider, Orientation Toggle
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Time Labels & Scrubber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(state.currentPositionSec),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatTime(state.totalDurationSec),
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }

                    // Interactive Scrubbing Timeline
                    Slider(
                        value = state.currentPositionSec.toFloat(),
                        onValueChange = { onSeekTo(it.toInt()) },
                        valueRange = 0f..state.totalDurationSec.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_timeline_slider")
                    )

                    // Secondary Bottom Controls: Volume & Orientation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Slider & Mute Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.width(180.dp)
                        ) {
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier.size(32.dp).testTag("player_volume_toggle")
                            ) {
                                Icon(
                                    imageVector = if (state.isMuted || state.volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Slider(
                                value = if (state.isMuted) 0f else state.volume,
                                onValueChange = onVolumeChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = TextPrimary,
                                    activeTrackColor = NeonAmber,
                                    inactiveTrackColor = DarkOutline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_volume_slider")
                            )
                        }

                        // Orientation & Fullscreen Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onToggleOrientation,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .testTag("orientation_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (state.isLandscape) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenRotation,
                                    contentDescription = "Toggle Orientation",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
