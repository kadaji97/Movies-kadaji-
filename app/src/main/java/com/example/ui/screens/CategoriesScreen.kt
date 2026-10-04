package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.data.service.BulkCatalogState
import com.example.data.service.CategoryGroup
import com.example.ui.components.MediaCard
import com.example.ui.theme.*

enum class CategoryDisplayMode {
    CAROUSELS,
    GRID
}

@Composable
fun CategoriesScreen(
    selectedCategory: MediaCategory,
    selectedGenre: String,
    availableGenres: List<String>,
    items: List<MediaItem>,
    categoryGroups: List<CategoryGroup> = emptyList(),
    bulkCatalogState: BulkCatalogState = BulkCatalogState(),
    onTriggerBulkSync: () -> Unit = {},
    onSelectCategory: (MediaCategory) -> Unit,
    onSelectGenre: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var genreDropdownExpanded by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(CategoryDisplayMode.CAROUSELS) }

    val categories = listOf(
        MediaCategory.MOVIES,
        MediaCategory.TV_SHOWS,
        MediaCategory.LIVE_SPORTS,
        MediaCategory.CARTOONS_ANIME,
        MediaCategory.REGIONAL_HITS
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("categories_screen")
    ) {
        // 1. Horizontally-scrollable Tab Bar with Pill Buttons
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CyberCyan else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyberCyan else DarkOutline
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectCategory(cat) }
                        .testTag("category_pill_${cat.name}")
                ) {
                    Text(
                        text = cat.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF07080B) else TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // 2. Bulk Volume Sync Status Banner (50 Pages Movies + 50 Pages TV Series)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (bulkCatalogState.isSyncing) CyberCyan else DarkOutline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("bulk_sync_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        Icon(
                            imageVector = if (bulkCatalogState.isSyncing) Icons.Default.CloudSync else Icons.Default.Dataset,
                            contentDescription = "Bulk Sync",
                            tint = if (bulkCatalogState.isSyncing) CyberCyan else NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "BULK TMDb DATA ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (bulkCatalogState.isSyncing) {
                                    bulkCatalogState.statusMessage
                                } else if (bulkCatalogState.totalMoviesFetched > 0 || bulkCatalogState.totalTvFetched > 0) {
                                    "${bulkCatalogState.totalMoviesFetched} Movies & ${bulkCatalogState.totalTvFetched} TV Series Indexed"
                                } else {
                                    "Fetch 1,000+ Movies & 1,000+ TV Series (50 Pages Each)"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!bulkCatalogState.isSyncing) {
                        Button(
                            onClick = onTriggerBulkSync,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF07080B)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("start_bulk_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (bulkCatalogState.totalMoviesFetched > 0) "Refresh 2,000+" else "Sync 2,000+",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (bulkCatalogState.isSyncing) {
                    LinearProgressIndicator(
                        progress = { bulkCatalogState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberCyan,
                        trackColor = DarkOutline
                    )
                }
            }
        }

        // 3. Filter Bar & View Mode Toggle (Carousels vs Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Genre Dropdown Selector
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { genreDropdownExpanded = true }
                        .testTag("genre_dropdown_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedGenre,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = genreDropdownExpanded,
                    onDismissRequest = { genreDropdownExpanded = false },
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                ) {
                    availableGenres.forEach { genre ->
                        val isSelected = selectedGenre == genre
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }
                                    Text(
                                        text = genre,
                                        color = if (isSelected) CyberCyan else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                onSelectGenre(genre)
                                genreDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Mode Selector: Carousels vs Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { displayMode = CategoryDisplayMode.CAROUSELS },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (displayMode == CategoryDisplayMode.CAROUSELS) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                        .border(1.dp, if (displayMode == CategoryDisplayMode.CAROUSELS) CyberCyan else DarkOutline, RoundedCornerShape(6.dp))
                        .testTag("mode_carousels_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewCarousel,
                        contentDescription = "Carousels View",
                        tint = if (displayMode == CategoryDisplayMode.CAROUSELS) CyberCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { displayMode = CategoryDisplayMode.GRID },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (displayMode == CategoryDisplayMode.GRID) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                        .border(1.dp, if (displayMode == CategoryDisplayMode.GRID) CyberCyan else DarkOutline, RoundedCornerShape(6.dp))
                        .testTag("mode_grid_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = if (displayMode == CategoryDisplayMode.GRID) CyberCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${items.size} titles",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ensure only items with verified working media are shown
        val verifiedItems = remember(items) {
            items.filter { com.example.data.catalog.MovieCatalog.hasWorkingMedia(it) }
        }

        // Dynamically organize current category content into genres & categories
        val activeCategoryGroups = remember(selectedCategory, categoryGroups, verifiedItems) {
            val matchedFromBulk = categoryGroups.mapNotNull { group ->
                val matching = group.items.filter { item ->
                    com.example.data.catalog.MovieCatalog.hasWorkingMedia(item) && when (selectedCategory) {
                        MediaCategory.MOVIES -> item.category == MediaCategory.MOVIES || item.seasons.isEmpty()
                        MediaCategory.TV_SHOWS -> item.category == MediaCategory.TV_SHOWS || item.seasons.isNotEmpty()
                        MediaCategory.LIVE_SPORTS -> item.category == MediaCategory.LIVE_SPORTS || item.genres.any { it.contains("Sport", ignoreCase = true) }
                        MediaCategory.CARTOONS_ANIME -> item.category == MediaCategory.CARTOONS_ANIME || item.genres.any { it.contains("Animation", ignoreCase = true) || it.contains("Anime", ignoreCase = true) }
                        MediaCategory.REGIONAL_HITS -> item.category == MediaCategory.REGIONAL_HITS
                        else -> true
                    }
                }
                if (matching.isNotEmpty()) group.copy(items = matching) else null
            }

            if (matchedFromBulk.isNotEmpty()) {
                matchedFromBulk
            } else {
                // Dynamically build category groups from verified items for the selected category
                val extractedGenres = verifiedItems.flatMap { it.genres }.distinct().filter { it.isNotBlank() }
                val targetGenres = if (extractedGenres.isNotEmpty()) {
                    extractedGenres
                } else {
                    listOf("Action", "Drama", "Sci-Fi", "Comedy", "Animation", "Crime", "Thriller")
                }
                targetGenres.mapNotNull { genreName ->
                    val matched = verifiedItems.filter { item ->
                        item.genres.any { it.equals(genreName, ignoreCase = true) }
                    }
                    if (matched.isNotEmpty()) {
                        CategoryGroup(
                            id = genreName.lowercase().replace(" ", "_"),
                            name = genreName,
                            categoryType = selectedCategory,
                            items = matched
                        )
                    } else null
                }
            }
        }

        // 4. Content Layout: Carousels View OR Grid View
        if (displayMode == CategoryDisplayMode.CAROUSELS && activeCategoryGroups.isNotEmpty() && selectedGenre == "All Genres") {
            // Feed horizontal scrolling carousels grouped by categories
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeCategoryGroups) { group ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp, 16.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(CyberCyan)
                                )
                                Text(
                                    text = group.name.uppercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "(${group.items.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextTertiary
                                )
                            }

                            Text(
                                text = "See All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                modifier = Modifier
                                    .clickable {
                                        onSelectGenre(group.name)
                                        displayMode = CategoryDisplayMode.GRID
                                    }
                                    .padding(4.dp)
                            )
                        }

                        // Horizontal Scrolling Carousel
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(group.items) { item ->
                                MediaCard(
                                    mediaItem = item,
                                    onClick = { onMediaClick(item) },
                                    cardWidth = 140.dp,
                                    cardHeight = 210.dp,
                                    showGenres = true
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Grid View
            if (verifiedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No content matches this genre",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching genres or run Bulk TMDb Sync to fetch 2,000+ titles",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(verifiedItems) { mediaItem ->
                        MediaCard(
                            mediaItem = mediaItem,
                            onClick = { onMediaClick(mediaItem) },
                            cardWidth = 145.dp,
                            cardHeight = 220.dp,
                            showGenres = true
                        )
                    }
                }
            }
        }
    }
}
