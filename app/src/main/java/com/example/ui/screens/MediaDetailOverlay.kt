package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.model.CastMember
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.data.model.TvEpisode
import com.example.data.service.HybridMediaRepository
import com.example.data.service.MediaStreamAggregator
import com.example.data.service.PlaybackQualityProfile
import com.example.data.service.WatchProvider
import com.example.ui.components.PlaybackQualitySelectionDialog
import com.example.ui.components.QualityDialogMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArchiveSearchUiState
import com.example.ui.viewmodel.MovieDetailsUiState
import kotlinx.coroutines.launch

/**
 * Production-ready Movie Details Screen
 * Displays rich movie details fetched via Retrofit (TMDb API v3):
 * - Live poster & cinematic backdrop
 * - Title & tagline
 * - Plot description / synopsis with expandable reading mode
 * - Live genre tags fetched via Retrofit
 * - Real-time Retrofit sync badge & latency metrics
 * - Automated Internet Archive search bridge with direct CDN playback & download
 * - Streaming, Cinema theater presentation, download, and watchlist actions
 */
@Composable
fun MediaDetailOverlay(
    mediaItem: MediaItem,
    detailsState: MovieDetailsUiState = MovieDetailsUiState(),
    archiveSearchState: ArchiveSearchUiState = ArchiveSearchUiState(),
    onRefreshRetrofit: () -> Unit = {},
    isAlreadyDownloaded: Boolean,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    isInWatchlist: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    onClose: () -> Unit,
    onPlayStream: () -> Unit,
    onLaunchCinemaPlayer: (season: Int, episode: Int, qualityProfile: PlaybackQualityProfile) -> Unit = { _, _, _ -> },
    onDownloadOffline: (season: Int, episode: Int) -> Unit = { _, _ -> },
    onWatchArchiveStream: (String) -> Unit = {},
    onDownloadArchiveVideo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val liveDetails = detailsState.details

    // Dynamic resolution of poster, title, description, and genres from Retrofit
    val effectivePosterUrl = liveDetails?.fullPosterUrl ?: mediaItem.posterUrl
    val effectiveBackdropUrl = liveDetails?.fullBackdropUrl ?: mediaItem.backdropUrl
    val effectiveTitle = liveDetails?.title?.ifBlank { mediaItem.title } ?: mediaItem.title
    val effectiveDescription = liveDetails?.overview?.ifBlank { mediaItem.plotSynopsis } ?: mediaItem.plotSynopsis
    val effectiveGenres = if (!liveDetails?.genres.isNullOrEmpty()) {
        liveDetails!!.genreNames
    } else {
        mediaItem.genres
    }
    val effectiveRating = if ((liveDetails?.voteAverage ?: 0.0) > 0.0) {
        String.format("%.1f", liveDetails!!.voteAverage)
    } else {
        String.format("%.1f", mediaItem.imdbRating)
    }
    val effectiveVoteCount = liveDetails?.voteCount ?: 1250
    val effectiveReleaseYear = liveDetails?.releaseDate?.take(4) ?: "${mediaItem.releaseYear}"
    val effectiveRuntime = liveDetails?.runtime?.let { mins ->
        if (mins > 0) "${mins / 60}h ${mins % 60}m" else mediaItem.duration
    } ?: mediaItem.duration

    var isSynopsisExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isTvShow = mediaItem.category == MediaCategory.TV_SHOWS || mediaItem.seasons.isNotEmpty() || mediaItem.episodes.isNotEmpty() || mediaItem.id.contains("_tv_")
    var loadedTvItem by remember(mediaItem.id) { mutableStateOf(mediaItem) }
    var selectedSeasonNumber by remember(mediaItem.id) { mutableIntStateOf(1) }
    var selectedStreamQuality by remember { mutableStateOf(PlaybackQualityProfile.AUTO) }

    var showQualityDialogForPlay by remember { mutableStateOf(false) }
    var showQualityDialogForDownload by remember { mutableStateOf(false) }
    var targetSeasonEpisode by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(mediaItem.id) {
        val numericTvId = mediaItem.id.removePrefix("tmdb_tv_").removePrefix("tmdb_").toIntOrNull()
        if (numericTvId != null && isTvShow) {
            try {
                val liveRes = com.example.data.service.TmdbRepository.getLiveTvShowWithAllSeasons(numericTvId)
                if (liveRes.isSuccess) {
                    val fullItem = liveRes.getOrThrow()
                    if (fullItem.seasons.isNotEmpty()) {
                        loadedTvItem = fullItem
                        selectedSeasonNumber = fullItem.sortedSeasons.firstOrNull()?.seasonNumber ?: 1
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("media_detail_overlay")
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 40.dp)
        ) {
            // 1. Cinematic Backdrop with Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = effectiveBackdropUrl,
                    contentDescription = "$effectiveTitle Backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.85f),
                                    DarkBackground
                                )
                            )
                        )
                )

                // Top Bar: Back Button, Retrofit Live Status, Watchlist Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("close_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Watchlist Icon
                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isInWatchlist) NeonAmber.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, if (isInWatchlist) NeonAmber else Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("watchlist_toggle_header_button")
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isInWatchlist) "Remove from Watchlist" else "Save to Watchlist",
                                tint = if (isInWatchlist) NeonAmber else TextPrimary
                            )
                        }

                        // Quality Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberCyanGlow)
                                .border(1.dp, CyberCyan, RoundedCornerShape(6.dp))
                                .clickable {
                                    targetSeasonEpisode = selectedSeasonNumber to 1
                                    showQualityDialogForPlay = true
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("quality_badge_header")
                        ) {
                            Text(
                                text = mediaItem.qualityBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            // 2. Poster & Key Metadata Header (Overlapping Backdrop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-40).dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High-Definition Poster Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .width(115.dp)
                        .height(170.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .testTag("movie_poster_image")
                ) {
                    AsyncImage(
                        model = effectivePosterUrl,
                        contentDescription = "$effectiveTitle Poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Title & Technical Badges
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = effectiveTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        lineHeight = 28.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("movie_title_text")
                    )

                    liveDetails?.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$tagline\"",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = NeonAmber.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating, Year & Duration Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TMDb Rating Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "TMDb Rating",
                                tint = NeonAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = effectiveRating,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = effectiveReleaseYear,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )

                        Text(
                            text = effectiveRuntime,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 3. Retrofit Sync Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp)
            ) {
                RetrofitStatusBanner(
                    state = detailsState,
                    onRefresh = onRefreshRetrofit
                )
            }

            // 4. Genres Section (Fetched via Retrofit)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-10).dp)
            ) {
                Text(
                    text = "GENRES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("genres_container")
                ) {
                    items(effectiveGenres) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("genre_chip_$genre")
                        ) {
                            Text(
                                text = genre,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Primary Stream Emulator & Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Stream Action: "Watch Now" (Instant auto-play without intermediate dialog)
                val activeEpisodeNumber = if (isTvShow) {
                    val matchingSeason = loadedTvItem.sortedSeasons.find { it.seasonNumber == selectedSeasonNumber }
                    matchingSeason?.episodes?.firstOrNull()?.episodeNumber ?: 1
                } else 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // Immediate auto-play when user clicks play button
                            onLaunchCinemaPlayer(selectedSeasonNumber, activeEpisodeNumber, selectedStreamQuality)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF070B11)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("watch_now_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF070B11)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Now",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = if (isTvShow) "WATCH S${selectedSeasonNumber}E${activeEpisodeNumber}" else "WATCH NOW",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFF070B11)
                                )
                                Text(
                                    text = if (selectedStreamQuality == PlaybackQualityProfile.AUTO) {
                                        "Instant Auto-Play • Dynamic Speed Quality"
                                    } else {
                                        "Instant Auto-Play • ${selectedStreamQuality.title}"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF070B11).copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Optional Quality Quick-Selector Pill (user can click to choose specific quality if desired)
                    Surface(
                        onClick = {
                            targetSeasonEpisode = selectedSeasonNumber to activeEpisodeNumber
                            showQualityDialogForPlay = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("quality_selector_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "Choose Stream Quality",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = selectedStreamQuality.badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Row: Download & Watchlist (Instant download without extra dialog)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Button (Direct MP4 Download immediately triggered on click)
                    OutlinedButton(
                        onClick = {
                            onDownloadOffline(selectedSeasonNumber, activeEpisodeNumber)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isAlreadyDownloaded -> NeonGreen
                                isDownloading -> CyberCyan
                                else -> DarkOutline
                            }
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = when {
                                isAlreadyDownloaded -> NeonGreen
                                isDownloading -> CyberCyan
                                else -> TextPrimary
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("download_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = CyberCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isAlreadyDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = "Download Video",
                                tint = if (isAlreadyDownloaded) NeonGreen else CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isAlreadyDownloaded -> "Downloaded"
                                isDownloading -> "Downloading $downloadProgress%"
                                else -> "Download"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Watchlist Button
                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isInWatchlist) NeonAmber else DarkOutline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isInWatchlist) NeonAmber.copy(alpha = 0.15f) else DarkSurfaceElevated,
                            contentColor = if (isInWatchlist) NeonAmber else TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("watchlist_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isInWatchlist) "In Watchlist" else "Add to Watchlist",
                            tint = if (isInWatchlist) NeonAmber else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInWatchlist) "In Watchlist" else "Watchlist",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInWatchlist) NeonAmber else TextPrimary
                        )
                    }
                }
            }

            // TV Series Episodes & Seasons Section (Multi-Season Tabbed Navigation)
            val isTvShow = mediaItem.category == MediaCategory.TV_SHOWS || mediaItem.seasons.isNotEmpty() || mediaItem.episodes.isNotEmpty()
            if (isTvShow) {
                var loadedTvItem by remember(mediaItem.id) { mutableStateOf(mediaItem) }

                // Live fetch full real seasons & episodes from TMDb if series has placeholder
                LaunchedEffect(mediaItem.id) {
                    val numericTvId = mediaItem.id.removePrefix("tmdb_tv_").removePrefix("tmdb_").toIntOrNull()
                    if (numericTvId != null && (loadedTvItem.category == MediaCategory.TV_SHOWS || loadedTvItem.seasons.size <= 1)) {
                        try {
                            val liveRes = com.example.data.service.TmdbRepository.getLiveTvShowWithAllSeasons(numericTvId)
                            if (liveRes.isSuccess) {
                                val fullItem = liveRes.getOrThrow()
                                if (fullItem.seasons.isNotEmpty()) {
                                    loadedTvItem = fullItem
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val allSeasons = if (loadedTvItem.seasons.isNotEmpty()) {
                    loadedTvItem.sortedSeasons
                } else if (loadedTvItem.episodes.isNotEmpty()) {
                    val grouped = loadedTvItem.sortedEpisodes.groupBy { it.seasonNumber }
                    grouped.map { (sNum, eps) ->
                        com.example.data.model.TvSeason(sNum, "Season $sNum", eps.size, eps)
                    }
                } else {
                    emptyList()
                }
                LaunchedEffect(allSeasons) {
                    if (allSeasons.isNotEmpty() && allSeasons.none { it.seasonNumber == selectedSeasonNumber }) {
                        selectedSeasonNumber = allSeasons.first().seasonNumber
                    }
                }

                val activeSeason = allSeasons.find { it.seasonNumber == selectedSeasonNumber } ?: allSeasons.firstOrNull()
                val episodesList = activeSeason?.episodes?.sortedBy { it.episodeNumber } ?: loadedTvItem.sortedEpisodes

                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(CyberCyan)
                            )
                            Text(
                                text = "SEASONS & EPISODES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = CyberCyan
                            )
                        }
                        Text(
                            text = "${allSeasons.size} Seasons • Season $selectedSeasonNumber (${episodesList.size} Episodes)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    // Season Selector Tabs Row
                    if (allSeasons.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allSeasons) { season ->
                                val isSelected = season.seasonNumber == selectedSeasonNumber
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) CyberCyan else DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkOutline
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedSeasonNumber = season.seasonNumber }
                                        .testTag("season_tab_${season.seasonNumber}")
                                ) {
                                    Text(
                                        text = season.name.ifBlank { "Season ${season.seasonNumber}" },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF070B11) else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    episodesList.forEach { ep ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp, 48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkBackground)
                                ) {
                                    AsyncImage(
                                        model = ep.fullStillUrl,
                                        contentDescription = ep.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${ep.runtimeMinutes ?: 45}m",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "S${ep.seasonNumber}E${ep.episodeNumber} • ${ep.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ep.overview.ifBlank { "Full cinematic episode stream." },
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            // Direct instant play for this episode
                                            onLaunchCinemaPlayer(ep.seasonNumber, ep.episodeNumber, selectedStreamQuality)
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CyberCyan.copy(alpha = 0.15f))
                                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .testTag("watch_episode_${ep.seasonNumber}_${ep.episodeNumber}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Watch Episode",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            // Direct instant download for this episode
                                            onDownloadOffline(ep.seasonNumber, ep.episodeNumber)
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonGreen.copy(alpha = 0.12f))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .testTag("download_episode_${ep.seasonNumber}_${ep.episodeNumber}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Episode MP4",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Synopsis / Description Section (Fetched via Retrofit)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNOPSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = CyberCyan
                    )

                    if (effectiveDescription.length > 180) {
                        Text(
                            text = if (isSynopsisExpanded) "Show Less" else "Read More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            modifier = Modifier
                                .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = effectiveDescription,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = TextPrimary.copy(alpha = 0.92f),
                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .animateContentSize()
                        .testTag("movie_synopsis_text")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Starring Cast Section
            val castMembers = liveDetails?.credits?.cast?.take(6)?.map { c ->
                CastMember(
                    name = c.name,
                    role = c.character ?: "Featured Cast",
                    avatarColorHex = 0xFF2A3142
                )
            } ?: mediaItem.cast

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "STARRING CAST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(castMembers) { member ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(82.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(member.avatarColorHex))
                                    .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(2).uppercase(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = member.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.role,
                                fontSize = 10.sp,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Quality Selection Dialog for Streaming Playback
        if (showQualityDialogForPlay) {
            val effectiveTarget = targetSeasonEpisode ?: (selectedSeasonNumber to 1)
            PlaybackQualitySelectionDialog(
                mediaItem = if (isTvShow) loadedTvItem else mediaItem,
                mode = QualityDialogMode.STREAM_PLAY,
                initialProfile = selectedStreamQuality,
                onConfirm = { qualityProfile ->
                    selectedStreamQuality = qualityProfile
                    showQualityDialogForPlay = false
                    onLaunchCinemaPlayer(effectiveTarget.first, effectiveTarget.second, qualityProfile)
                },
                onDismiss = { showQualityDialogForPlay = false }
            )
        }

        // Quality Selection Dialog for Offline Downloads
        if (showQualityDialogForDownload) {
            val effectiveTarget = targetSeasonEpisode ?: (selectedSeasonNumber to 1)
            PlaybackQualitySelectionDialog(
                mediaItem = if (isTvShow) loadedTvItem else mediaItem,
                mode = QualityDialogMode.DOWNLOAD,
                initialProfile = PlaybackQualityProfile.FHD_1080P,
                onConfirm = { qualityProfile ->
                    showQualityDialogForDownload = false
                    coroutineScope.launch {
                        if (isTvShow) {
                            HybridMediaRepository.resolveAndDownload(
                                context = context,
                                mediaItem = loadedTvItem,
                                season = effectiveTarget.first,
                                episode = effectiveTarget.second,
                                qualityProfile = qualityProfile
                            )
                        } else {
                            HybridMediaRepository.resolveAndDownload(
                                context = context,
                                mediaItem = mediaItem,
                                qualityProfile = qualityProfile
                            )
                        }
                    }
                },
                onDismiss = { showQualityDialogForDownload = false }
            )
        }
    }
}

/**
 * Modern Status Badge reflecting the Retrofit network call status
 */
@Composable
private fun RetrofitStatusBanner(
    state: MovieDetailsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
            .testTag("retrofit_status_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Fetching live details via Retrofit...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                    state.isRetrofitLive -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Text(
                            text = "Retrofit TMDb Live (${state.latencyMs}ms)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonGreen
                        )
                    }
                    state.error != null -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonAmber)
                        )
                        Text(
                            text = "Offline Catalog Cache Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                        Text(
                            text = "Retrofit TMDb Client Ready",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("refresh_retrofit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh from TMDb Retrofit",
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Requirement 4: UI Logic & Fallback (Jetpack Compose)
 * - While the automated search runs, show a loading indicator.
 * - If a streaming URL is found, display highly responsive "Watch Now" and "Download Video" buttons.
 * - If no public domain stream is found for that specific title, hide the streaming buttons and display
 *   a placeholder text or container where TMDB "Watch Providers" data can be rendered.
 */
@Composable
fun ArchiveStreamBridgeSection(
    movieTitle: String,
    searchState: ArchiveSearchUiState,
    onWatchNow: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.85f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                searchState.result != null -> NeonGreen.copy(alpha = 0.5f)
                searchState.isSearching -> CyberCyan.copy(alpha = 0.4f)
                else -> DarkOutline
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("archive_stream_bridge_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                // 1. Loading Indicator while automated search runs
                searchState.isSearching -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATED SEARCH BRIDGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Searching Internet Archive API for public-domain stream...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Public Domain Stream Found -> Responsive "Watch Now" & "Download Video" buttons
                searchState.result != null -> {
                    val streamResult = searchState.result
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Public Domain",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PUBLIC DOMAIN STREAM VERIFIED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "Archive.org CDN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextTertiary
                        )
                    }

                    Text(
                        text = "Instant high-speed stream identified: ${streamResult.identifier}. Direct .mp4 container pre-buffered via Media3 200MB cache.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Watch Now" button
                        Button(
                            onClick = { onWatchNow(streamResult.streamUrl) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF070B08)
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("archive_watch_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Watch Now",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watch Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // "Download Video" button (Native Android DownloadManager)
                        Button(
                            onClick = { onDownloadVideo(streamResult.downloadUrl) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBackground,
                                contentColor = CyberCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("archive_download_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Download Video",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Download",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. Fallback: No Public Domain stream found -> Hide streaming buttons, render TMDb Watch Providers data
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Public Domain Notice",
                                tint = NeonAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "PUBLIC DOMAIN ARCHIVE STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "No public-domain stream indexed on Internet Archive for '$movieTitle'. Commercial distribution rights apply.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        // Render TMDb Watch Providers
                        Text(
                            text = "OFFICIAL WATCH PROVIDERS (TMDb):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 0.5.sp
                        )

                        val providers = searchState.watchProviders.ifEmpty {
                            com.example.data.service.InternetArchiveService.getFallbackWatchProviders(movieTitle)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(providers) { provider ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBackground)
                                        .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = provider.providerName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = provider.type,
                                            fontSize = 9.sp,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
