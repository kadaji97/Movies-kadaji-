package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.catalog.MovieCatalog
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.MediaCollectionType
import com.example.data.model.MediaItem
import com.example.ui.components.MediaCard
import com.example.ui.theme.*

private fun getGenreIcon(genre: String): ImageVector {
    return when (genre.lowercase()) {
        "all", "all genres" -> Icons.Default.Apps
        "action" -> Icons.Default.Bolt
        "sci-fi" -> Icons.Default.RocketLaunch
        "comedy" -> Icons.Default.SentimentVerySatisfied
        "thriller" -> Icons.Default.Visibility
        "drama" -> Icons.Default.TheaterComedy
        "animation" -> Icons.Default.Palette
        "sports" -> Icons.Default.SportsBasketball
        "romance" -> Icons.Default.Favorite
        "documentary" -> Icons.Default.Videocam
        else -> Icons.Default.Movie
    }
}

@Composable
fun HomeScreen(
    heroIndex: Int,
    featuredBanners: List<MediaItem>,
    watchHistory: List<WatchHistoryEntity>,
    watchlist: List<WatchlistEntity> = emptyList(),
    selectedGenre: String = "All",
    availableGenres: List<String> = emptyList(),
    filteredMediaItems: List<MediaItem> = emptyList(),
    isLoadingTmdb: Boolean = false,
    infiniteMovies: List<MediaItem> = emptyList(),
    isLoadingMoreMovies: Boolean = false,
    onLoadMoreMovies: () -> Unit = {},
    onSelectHeroIndex: (Int) -> Unit,
    onWatchHero: (MediaItem) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onResumeHistory: (WatchHistoryEntity) -> Unit,
    onSelectGenre: (String) -> Unit = {},
    onNavigateToWatchlist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (isLoadingTmdb) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonAmberGlow)
                        .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = NeonAmber
                    )
                    Text(
                        text = "Connecting to TMDb • Populating Live Movie Database...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonAmber
                    )
                }
            }
        }

        // Hero Featured Carousel
        item {
            val currentBanner = featuredBanners.getOrNull(heroIndex) ?: featuredBanners.firstOrNull()
            if (currentBanner != null) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Carousel Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .shadow(10.dp, shape = RoundedCornerShape(14.dp), spotColor = CyberCyan.copy(alpha = 0.25f))
                        .testTag("hero_carousel_card")
                ) {
                    AnimatedContent(
                        targetState = currentBanner,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "hero_banner_transition"
                    ) { banner ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Backdrop Image
                            val bannerModel: Any = if (banner.drawableResId != null) {
                                banner.drawableResId
                            } else {
                                banner.backdropUrl
                            }

                            AsyncImage(
                                model = bannerModel,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Gradient overlay for legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Black.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )

                            // Top Quality Badge
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.TopStart)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonAmber)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FEATURED • ${banner.qualityBadge}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF07080B)
                                )
                            }

                            // Bottom Content Info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = banner.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${banner.releaseYear}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyberCyan
                                    )
                                    Text(text = "•", color = TextTertiary, fontSize = 12.sp)
                                    Text(
                                        text = banner.duration,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Text(text = "•", color = TextTertiary, fontSize = 12.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = NeonAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${banner.imdbRating}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // "Watch Now" Button
                                Button(
                                    onClick = { onWatchHero(banner) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = Color(0xFF07080B)
                                    ),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("hero_watch_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Watch Now",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Carousel Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    featuredBanners.forEachIndexed { idx, _ ->
                        val isSelected = idx == heroIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 20.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyberCyan else DarkOutline)
                                .clickable { onSelectHeroIndex(idx) }
                        )
                    }
                }
            }
            }
        }

        // Genre Filtering Chip-Based Interface
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_genre_chip_row")
                ) {
                    items(availableGenres) { genre ->
                        val isSelected = (genre.equals(selectedGenre, ignoreCase = true)) ||
                                (genre == "All" && (selectedGenre.isBlank() || selectedGenre == "All" || selectedGenre == "All Genres"))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectGenre(genre) }
                                .testTag("home_genre_chip_${genre.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = getGenreIcon(genre),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = genre,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        val isFiltered = !selectedGenre.isBlank() &&
                !selectedGenre.equals("All", ignoreCase = true) &&
                !selectedGenre.equals("All Genres", ignoreCase = true)

        if (isFiltered) {
            // Filtered Catalog Header with Results Counter and Reset Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyberCyan)
                        )
                        Text(
                            text = "$selectedGenre Catalog",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredMediaItems.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }

                    // Reset Filter Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectGenre("All") }
                            .testTag("home_clear_genre_filter")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Filter",
                                tint = NeonAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Show All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonAmber
                            )
                        }
                    }
                }
            }

            // Continue Watching matching selected genre (if any)
            val genreWatchHistory = watchHistory.filter { history ->
                val full = MovieCatalog.getItemById(history.mediaId)
                full?.genres?.any { it.equals(selectedGenre, ignoreCase = true) } == true
            }
            if (genreWatchHistory.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        SectionHeader(title = "Resume $selectedGenre", accentColor = NeonAmber)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(genreWatchHistory) { history ->
                                val progressFrac = (history.progressSeconds.toFloat() / history.totalDurationSeconds.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onResumeHistory(history) }
                                        .testTag("continue_watching_${history.mediaId}")
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        ) {
                                            AsyncImage(
                                                model = history.backdropUrl,
                                                contentDescription = history.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonAmber),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Resume",
                                                    tint = Color(0xFF07080B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { progressFrac },
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                color = CyberCyan,
                                                trackColor = DarkOutline
                                            )
                                        }
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = history.title,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filtered Items Grid
            if (filteredMediaItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No titles found in $selectedGenre",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Try selecting another genre chip above",
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onSelectGenre("All") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = Color(0xFF07080B)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("home_empty_reset_filter_button")
                            ) {
                                Text("Browse All Titles", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val chunkedItems = filteredMediaItems.chunked(2)
                items(chunkedItems) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (media in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                MediaCard(
                                    mediaItem = media,
                                    onClick = { onMediaClick(media) },
                                    cardWidth = androidx.compose.ui.unit.Dp.Unspecified,
                                    cardHeight = 220.dp,
                                    showGenres = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("filtered_media_${media.id}")
                                )
                            }
                        }
                        if (pair.count() == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Continue Watching Row (if exists)
        if (watchHistory.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    SectionHeader(title = "Continue Watching", accentColor = NeonAmber)

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(watchHistory) { history ->
                            val progressFrac = (history.progressSeconds.toFloat() / history.totalDurationSeconds.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onResumeHistory(history) }
                                    .testTag("continue_watching_${history.mediaId}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    ) {
                                        AsyncImage(
                                            model = history.backdropUrl,
                                            contentDescription = history.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f))
                                        )
                                        // Center Resume play button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(NeonAmber),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Resume",
                                                tint = Color(0xFF07080B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Progress Bar overlay at bottom of poster
                                        LinearProgressIndicator(
                                            progress = { progressFrac },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            color = CyberCyan,
                                            trackColor = DarkOutline
                                        )
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = history.title,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${(progressFrac * 100).toInt()}% completed",
                                                color = CyberCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = history.quality,
                                                color = TextTertiary,
                                                fontSize = 10.sp
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

        // My Watchlist Row (if exists)
        if (watchlist.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NeonAmber)
                            )
                            Text(
                                text = "My Watchlist",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${watchlist.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }
                        }

                        Text(
                            text = "See All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onNavigateToWatchlist() }
                                .padding(4.dp)
                                .testTag("home_watchlist_see_all")
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(watchlist) { entry ->
                            val fullMedia = MovieCatalog.getItemById(entry.mediaId) ?: MediaItem(
                                id = entry.mediaId,
                                title = entry.title,
                                category = com.example.data.model.MediaCategory.MOVIES,
                                genres = entry.genres.split(",").map { it.trim() },
                                releaseYear = entry.releaseYear,
                                duration = entry.duration,
                                imdbRating = entry.imdbRating,
                                qualityBadge = entry.qualityBadge,
                                plotSynopsis = entry.plotSynopsis,
                                cast = emptyList(),
                                posterUrl = entry.posterUrl,
                                backdropUrl = entry.backdropUrl,
                                streamServers = emptyList(),
                                subtitleTracks = emptyList(),
                                collections = emptyList(),
                                defaultVideoUrl = com.example.data.service.MovieStreamUrlResolver.resolvePlayableMovieUrl(
                                    title = entry.title,
                                    mediaId = entry.mediaId,
                                    category = com.example.data.model.MediaCategory.MOVIES
                                )
                            )

                            MediaCard(
                                mediaItem = fullMedia,
                                onClick = { onMediaClick(fullMedia) },
                                showGenres = false
                            )
                        }
                    }
                }
            }
        }

        // Swiper Rows: Trending Now, Popular Movies, Top TV Shows, Live Sports Highlights
        val collections = listOf(
            Triple(MediaCollectionType.TRENDING_NOW, "Trending Now", CyberCyan),
            Triple(MediaCollectionType.POPULAR_MOVIES, "Popular Movies", NeonAmber),
            Triple(MediaCollectionType.TOP_TV_SHOWS, "Top TV Shows", CyberCyan),
            Triple(MediaCollectionType.LIVE_SPORTS, "Live Sports Highlights", NeonRed)
        )

        collections.forEach { (type, label, accent) ->
            item {
                val items = MovieCatalog.getItemsForCollection(type)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                ) {
                    SectionHeader(title = label, accentColor = accent)

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items) { media ->
                            MediaCard(
                                mediaItem = media,
                                onClick = { onMediaClick(media) },
                                showGenres = true
                            )
                        }
                    }
                }
            }
        }

        // 5. Endless Cinema Discovery Section (Infinite scrolling without repeating)
        val allDiscoveryMovies = (infiniteMovies.ifEmpty { MovieCatalog.effectiveCatalogItems.filter { it.category == com.example.data.model.MediaCategory.MOVIES } }).distinctBy { it.id }
        if (allDiscoveryMovies.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Endless Cinema Discovery", accentColor = CyberCyan)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${allDiscoveryMovies.size} Titles",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            // Staggered 2-column infinite movie cards
            val chunkedMovies = allDiscoveryMovies.chunked(2)
            items(chunkedMovies) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MediaCard(
                            mediaItem = pair[0],
                            onClick = { onMediaClick(pair[0]) },
                            showGenres = true
                        )
                    }
                    if (pair.size > 1) {
                        Box(modifier = Modifier.weight(1f)) {
                            MediaCard(
                                mediaItem = pair[1],
                                onClick = { onMediaClick(pair[1]) },
                                showGenres = true
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Infinite scrolling trigger & loading spinner
            item {
                LaunchedEffect(Unit) {
                    onLoadMoreMovies()
                }

                if (isLoadingMoreMovies) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Loading more cinema titles from TMDb...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun SectionHeader(title: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
