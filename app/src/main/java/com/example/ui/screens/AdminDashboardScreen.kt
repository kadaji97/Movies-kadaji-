package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PublishedMovieEntity
import com.example.data.local.SyncLogEntity
import com.example.data.model.MediaItem
import com.example.data.service.StreamingIndexService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    publishedMovies: List<PublishedMovieEntity>,
    syncLogs: List<SyncLogEntity>,
    isAutoSyncEnabled: Boolean,
    isSyncingNow: Boolean,
    isPublishingImdb: Boolean,
    lastPublishedMedia: MediaItem?,
    secondsUntilNextHourlySync: Int,
    lastSyncTimestamp: Long,
    onBack: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onTriggerSyncNow: () -> Unit,
    onPublishByImdbId: (String) -> Unit,
    onDeletePublishedMovie: (String) -> Unit,
    onClearAllPublished: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onOpenDetail: (MediaItem) -> Unit,
    onDismissPublishedPreview: () -> Unit,
    onSyncTmdbNow: () -> Unit = onTriggerSyncNow,
    userState: com.example.data.monetization.UserState = com.example.data.monetization.UserState(),
    onTogglePremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var imdbInputText by remember { mutableStateOf("tt15239678") }
    var showConfirmDeleteId by remember { mutableStateOf<String?>(null) }
    var showConfirmClearAll by remember { mutableStateOf(false) }

    val presetImdbIds = listOf(
        "tt15239678" to "Dune: Part Two",
        "tt6263850" to "Deadpool & Wolverine",
        "tt16366886" to "Gladiator II",
        "tt18411490" to "Alien: Romulus",
        "tt15398776" to "Oppenheimer",
        "tt9362722" to "Spider-Verse"
    )

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val remainingMinutes = secondsUntilNextHourlySync / 60
    val remainingSeconds = secondsUntilNextHourlySync % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Admin & Content Engine",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAutoSyncEnabled) CyberCyanGlow else DarkSurfaceElevated)
                                    .border(1.dp, if (isAutoSyncEnabled) CyberCyan else DarkOutline, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAutoSyncEnabled) "CRON: 1H ACTIVE" else "CRON: PAUSED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoSyncEnabled) CyberCyan else TextTertiary
                                )
                            }
                        }
                        Text(
                            text = "Automated Streaming Index & 1-Click Publisher",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION: VIP Access & Monetization Privileges (Admin Exclusive)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (userState.isPremiumUser) NeonAmber else CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_monetization_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (userState.isPremiumUser) NeonAmber.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (userState.isPremiumUser) Icons.Default.WorkspacePremium else Icons.Default.VpnKey,
                                        contentDescription = "VIP Admin Control",
                                        tint = if (userState.isPremiumUser) NeonAmber else CyberCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "VIP Cinema Access & Privileges",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (userState.isPremiumUser) "100% Ad-Free 1080p / 4K Cinema Active" else "Standard Stream Tier",
                                        fontSize = 12.sp,
                                        color = if (userState.isPremiumUser) NeonGreen else TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = userState.isPremiumUser,
                                onCheckedChange = { onTogglePremium() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonAmber,
                                    checkedTrackColor = NeonAmber.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("admin_vip_toggle_switch")
                            )
                        }

                        Text(
                            text = "👑 Administrator Privilege: All streaming and downloads operate in unrestricted ad-free cinema fidelity with no advertisements, commercial breaks, or countdown delays across the application.",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // SECTION 0: TMDb Live API Cloud Status & Sync
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonAmber.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().testTag("tmdb_status_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeonAmberGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "CloudSync",
                                    tint = NeonAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TMDb API v3/v4 Engine",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonGreen.copy(alpha = 0.2f))
                                            .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CONNECTED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGreen
                                        )
                                    }
                                }
                                Text(
                                    text = "Live Metadata, Cast Credits & Multi-Search Active",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Status indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "API Key: efad...ee62 • Read Token: Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Endpoints: /popular • /trending • /find/{imdb_id} • /search",
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }
                        }

                        Button(
                            onClick = onSyncTmdbNow,
                            enabled = !isSyncingNow,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                            modifier = Modifier.fillMaxWidth().testTag("sync_tmdb_button")
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing TMDb Releases...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Sync",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch Popular Releases from TMDb", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 1: 1-Click IMDb Instant Publisher
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyanGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Bolt",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "1-Click IMDb Instant Publisher",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Auto-populates title, poster, genres, cast & 3 CDN streams",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Presets Quick-Chips Row
                        Text(
                            text = "Quick Presets (Tap to select):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetImdbIds.forEach { (id, label) ->
                                val isSelected = imdbInputText.trim().equals(id, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { imdbInputText = id },
                                    label = {
                                        Text(
                                            text = "$label ($id)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyanGlow,
                                        selectedLabelColor = CyberCyan,
                                        containerColor = DarkSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = DarkOutline,
                                        selectedBorderColor = CyberCyan
                                    )
                                )
                            }
                        }

                        // Text Field for IMDb ID
                        OutlinedTextField(
                            value = imdbInputText,
                            onValueChange = { imdbInputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("imdb_id_input"),
                            label = { Text("IMDb ID (Format: tt1234567)") },
                            placeholder = { Text("e.g. tt15239678") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            },
                            trailingIcon = {
                                if (imdbInputText.isNotBlank()) {
                                    IconButton(onClick = { imdbInputText = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextTertiary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkOutline,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Big 1-Click Publish Button
                        Button(
                            onClick = { onPublishByImdbId(imdbInputText) },
                            enabled = !isPublishingImdb && imdbInputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_publish_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF07080B)
                            )
                        ) {
                            if (isPublishingImdb) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color(0xFF07080B),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Indexing & Linking 3 CDN Streams...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-Populate & Publish (1-Click)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Live Preview of newly published item
                        AnimatedVisibility(visible = lastPublishedMedia != null) {
                            lastPublishedMedia?.let { media ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(NeonGreen)
                                                )
                                                Text(
                                                    text = "JUST PUBLISHED TO DATABASE",
                                                    color = NeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            IconButton(
                                                onClick = onDismissPublishedPreview,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close Preview",
                                                    tint = TextTertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = media.posterUrl,
                                                contentDescription = media.title,
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(105.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = media.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${media.releaseYear} • ${media.duration}",
                                                        fontSize = 12.sp,
                                                        color = TextSecondary
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE5A00D).copy(alpha = 0.2f))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "IMDb ${media.imdbRating}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF5C518)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "Genres: ${media.genres.joinToString(", ")}",
                                                    fontSize = 11.sp,
                                                    color = CyberCyan,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = "Cast: ${media.cast.take(3).joinToString { it.name }}",
                                                    fontSize = 11.sp,
                                                    color = TextTertiary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // 3 Alternative CDN Server links
                                        Text(
                                            text = "Active Alternative CDN Streaming Links (${media.streamServers.size} Nodes):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            media.streamServers.forEachIndexed { idx, srv ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DarkSurfaceElevated)
                                                        .border(0.5.dp, DarkOutline, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(NeonGreen)
                                                        )
                                                        Text(
                                                            text = srv.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Text(
                                                        text = "${srv.quality} • ${srv.pingMs}ms • ${srv.bitrate}",
                                                        fontSize = 10.sp,
                                                        color = CyberCyan
                                                    )
                                                }
                                            }
                                        }

                                        // Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onPlayMedia(media) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = NeonGreen,
                                                    contentColor = Color(0xFF07080B)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Watch Now", fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { onOpenDetail(media) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = CyberCyan
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View Details")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Automated Hourly Content Updater Monitor
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF880E4F).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = Color(0xFFFF4081),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Automated Hourly Content Worker",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Continuous cron check against streaming feed",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = onToggleAutoSync,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberCyan,
                                    checkedTrackColor = CyberCyanGlow
                                )
                            )
                        }

                        // Schedule & Status Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "NEXT HOURLY CHECK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAutoSyncEnabled) "${remainingMinutes}m ${remainingSeconds}s" else "Paused",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isAutoSyncEnabled) CyberCyan else TextTertiary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "LAST SYNC EXECUTION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = timeFormat.format(Date(lastSyncTimestamp)),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Trigger Manual Check Button
                        OutlinedButton(
                            onClick = onTriggerSyncNow,
                            enabled = !isSyncingNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_trigger_sync_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberCyan
                            )
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CyberCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking External Streaming Feeds...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Auto-Sync Worker Now", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Recent Sync Logs (Collapsible preview)
                        if (syncLogs.isNotEmpty()) {
                            Text(
                                text = "Recent Sync Activity Logs:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                syncLogs.take(3).forEach { log ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurface)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = log.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (log.status) {
                                                        "NEW_RELEASES_ADDED", "PUBLISHED" -> NeonGreen
                                                        "ERROR" -> Color(0xFFFF5252)
                                                        else -> CyberCyan
                                                    }
                                                )
                                                Text(
                                                    text = timeFormat.format(Date(log.timestamp)),
                                                    fontSize = 10.sp,
                                                    color = TextTertiary
                                                )
                                            }
                                            Text(
                                                text = log.summary,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 2,
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

            // SECTION 3: Database Content Inventory
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Database Content Inventory",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "${publishedMovies.size} custom published title(s) in Room database",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (publishedMovies.isNotEmpty()) {
                        TextButton(
                            onClick = { showConfirmClearAll = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (publishedMovies.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No custom published movies yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "Use the 1-Click IMDb Publisher above or tap 'Run Auto-Sync Worker Now' to index new releases.",
                                fontSize = 12.sp,
                                color = TextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(publishedMovies, key = { it.mediaId }) { entity ->
                    val mediaItem = remember(entity) { StreamingIndexService.toMediaItem(entity) }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("published_item_${entity.mediaId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = entity.posterUrl,
                                contentDescription = entity.title,
                                modifier = Modifier
                                    .width(55.dp)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = entity.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (entity.source == "ADMIN_MANUAL_1CLICK") CyberCyanGlow else Color(0xFF4A148C).copy(alpha = 0.3f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (entity.source == "ADMIN_MANUAL_1CLICK") "1-CLICK" else "AUTO-CRON",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entity.source == "ADMIN_MANUAL_1CLICK") CyberCyan else Color(0xFFCE93D8)
                                        )
                                    }
                                }

                                Text(
                                    text = "IMDb: ${entity.imdbId} • Year: ${entity.releaseYear} • Rating: ${entity.imdbRating}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "3 Alternative CDN Streams Active (Akamai, Cloudflare, Fastly)",
                                    fontSize = 10.sp,
                                    color = NeonGreen
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onPlayMedia(mediaItem) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyanGlow)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onOpenDetail(mediaItem) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurface)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Detail",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showConfirmDeleteId = entity.mediaId },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurface)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
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

    // Confirmation dialog for deleting single movie
    showConfirmDeleteId?.let { delId ->
        AlertDialog(
            onDismissRequest = { showConfirmDeleteId = null },
            title = { Text("Unpublish Movie?") },
            text = { Text("Are you sure you want to remove [$delId] from the local database and catalog?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePublishedMovie(delId)
                        showConfirmDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Unpublish & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteId = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    // Confirmation dialog for clearing all
    if (showConfirmClearAll) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAll = false },
            title = { Text("Clear All Published Titles?") },
            text = { Text("This will remove all custom movies from the Room database. Bundled catalog titles will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllPublished()
                        showConfirmClearAll = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAll = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
