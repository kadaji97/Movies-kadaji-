package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.catalog.MovieCatalog
import com.example.data.local.WatchlistEntity
import com.example.data.model.MediaItem
import com.example.ui.theme.*

enum class WatchlistSortOption(val label: String) {
    RECENT("Recently Added"),
    RATING("Highest Rated"),
    TITLE("Title (A-Z)")
}

@Composable
fun WatchlistScreen(
    watchlist: List<WatchlistEntity>,
    onMediaClick: (MediaItem) -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onRemoveFromWatchlist: (String) -> Unit,
    onClearAllWatchlist: () -> Unit,
    onExploreMovies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf(WatchlistSortOption.RECENT) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Derive category filters
    val categories = remember(watchlist) {
        val unique = watchlist.map { it.category }.distinct()
        listOf("All") + unique
    }

    // Filter and sort items
    val displayedItems = remember(watchlist, searchQuery, selectedCategoryFilter, selectedSortOption) {
        var filtered = watchlist

        if (selectedCategoryFilter != "All") {
            filtered = filtered.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                        it.genres.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }

        when (selectedSortOption) {
            WatchlistSortOption.RECENT -> filtered.sortedByDescending { it.addedTimestamp }
            WatchlistSortOption.RATING -> filtered.sortedByDescending { it.imdbRating }
            WatchlistSortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Clear Watchlist?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all ${watchlist.size} movies from your watchlist? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllWatchlist()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel", color = TextTertiary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("watchlist_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyanGlow)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Watchlist Icon",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "My Watchlist",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "${watchlist.size} saved title${if (watchlist.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            }

            if (watchlist.isNotEmpty()) {
                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_watchlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clear Watchlist",
                        tint = NeonRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonRed.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (watchlist.isEmpty()) {
            // Empty State
            EmptyWatchlistState(
                onExplore = onExploreMovies,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Search and Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Filter watchlist...",
                            color = TextTertiary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("watchlist_search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                if (categories.size > 2) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategoryFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurfaceElevated,
                                    selectedContainerColor = CyberCyan,
                                    labelColor = TextSecondary,
                                    selectedLabelColor = Color(0xFF07080B)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkOutline,
                                    selectedBorderColor = CyberCyan
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sort Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sort by:",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WatchlistSortOption.entries.forEach { option ->
                            val isSelected = option == selectedSortOption
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberCyanGlow else DarkSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkOutline,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedSortOption = option }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Watchlist Items List
            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No movies match \"$searchQuery\"",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("watchlist_items_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedItems, key = { it.mediaId }) { item ->
                        WatchlistItemCard(
                            item = item,
                            onClick = {
                                val fullMedia = MovieCatalog.getItemById(item.mediaId) ?: toMediaItem(item)
                                onMediaClick(fullMedia)
                            },
                            onPlay = {
                                val fullMedia = MovieCatalog.getItemById(item.mediaId) ?: toMediaItem(item)
                                onPlayMedia(fullMedia)
                            },
                            onRemove = { onRemoveFromWatchlist(item.mediaId) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistItemCard(
    item: WatchlistEntity,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
            .clickable(onClick = onClick)
            .testTag("watchlist_item_${item.mediaId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .height(125.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Quality Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.qualityBadge.contains("4K", ignoreCase = true)) NeonAmber else CyberCyan
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.qualityBadge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF07080B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Category & Rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = NeonAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format("%.1f", item.imdbRating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Year • Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${item.releaseYear}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(text = "•", fontSize = 12.sp, color = TextTertiary)
                        Text(
                            text = item.duration,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (item.genres.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.genres,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Row: Watch Now & Remove
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF07080B)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("watchlist_play_${item.mediaId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watch",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .testTag("watchlist_remove_${item.mediaId}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Remove from Watchlist",
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWatchlistState(
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(2.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = "Empty Watchlist",
                tint = CyberCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Watchlist is Empty",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Save your favorite movies and shows to watch later by clicking the bookmark button on any title.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExplore,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan,
                contentColor = Color(0xFF07080B)
            ),
            modifier = Modifier
                .height(44.dp)
                .testTag("watchlist_explore_button")
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = "Explore",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Explore Movies",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun toMediaItem(entity: WatchlistEntity): MediaItem {
    return MediaItem(
        id = entity.mediaId,
        title = entity.title,
        category = com.example.data.model.MediaCategory.MOVIES,
        genres = entity.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        releaseYear = entity.releaseYear,
        duration = entity.duration,
        imdbRating = entity.imdbRating,
        qualityBadge = entity.qualityBadge,
        plotSynopsis = entity.plotSynopsis.ifBlank { "Available in your saved watchlist." },
        cast = emptyList(),
        posterUrl = entity.posterUrl,
        backdropUrl = entity.backdropUrl,
        streamServers = emptyList(),
        subtitleTracks = emptyList(),
        collections = emptyList(),
        defaultVideoUrl = com.example.data.service.MovieStreamUrlResolver.resolvePlayableMovieUrl(
            title = entity.title,
            mediaId = entity.mediaId,
            category = com.example.data.model.MediaCategory.MOVIES
        )
    )
}
