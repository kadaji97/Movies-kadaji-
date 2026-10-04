package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.catalog.MovieCatalog
import com.example.ui.components.BottomNavBar
import com.example.ui.components.HeaderBar
import com.example.data.model.MediaItem
import com.example.ui.components.DownloadQualityDialog
import com.example.ui.components.MultiServerModal
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.MovieskadajiViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MovieskadajiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Exception) {}

        try {
            // Guarantee Chromium cache directory structures exist to avoid opendir errors
            com.example.MovieskadajiApplication.ensureWebViewDirectories(applicationContext)
        } catch (_: Exception) {}

        try {
            // Initialize dynamic resource router configuration
            com.example.data.config.AppConfig.initialize(applicationContext)
        } catch (_: Exception) {}

        setContent {
            val currentThemeMode by viewModel.currentThemeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = currentThemeMode) {
                // Splash Screen & 2.5-second race-condition timeout management
                var isSplashScreenVisible by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    // Smooth branded cinematic splash entrance
                    delay(1200L)
                    isSplashScreenVisible = false
                }

                // Storage permissions and media scanning
                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }

                var hasStoragePermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            storagePermission
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasStoragePermission = isGranted
                    if (isGranted) {
                        viewModel.queryLocalPhoneVideos(this@MainActivity)
                        viewModel.showToastNotice("Storage access granted! Scanned device videos.")
                    } else {
                        viewModel.showToastNotice("Storage permission not granted. You can still pick files.")
                    }
                }

                // System file picker to select and play any video file directly
                val videoFilePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {}

                        val displayName = try {
                            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                            } ?: uri.lastPathSegment ?: "Device Video"
                        } catch (_: Exception) {
                            uri.lastPathSegment ?: "Device Video"
                        }

                        viewModel.playLocalVideoUri(this@MainActivity, uri, displayName)
                    }
                }

                val deviceVideos by viewModel.deviceVideos.collectAsStateWithLifecycle()
                val pendingDownloadMediaItem by viewModel.pendingDownloadMediaItem.collectAsStateWithLifecycle()
                val pendingExportItem by viewModel.pendingExportItem.collectAsStateWithLifecycle()

                LaunchedEffect(hasStoragePermission) {
                    if (hasStoragePermission) {
                        viewModel.queryLocalPhoneVideos(this@MainActivity)
                    }
                }

                // Direct media detail navigation without ad interruption
                val onMediaClickWithAd: (MediaItem) -> Unit = { media ->
                    viewModel.openMediaDetail(media)
                }

                val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
                val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

                val heroIndex by viewModel.heroIndex.collectAsStateWithLifecycle()
                val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()

                val detailMediaItem by viewModel.detailMediaItem.collectAsStateWithLifecycle()
                val movieDetailsState by viewModel.movieDetailsState.collectAsStateWithLifecycle()
                val archiveSearchState by viewModel.archiveSearchState.collectAsStateWithLifecycle()
                val serverModalMedia by viewModel.serverModalMedia.collectAsStateWithLifecycle()
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()

                val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
                val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
                val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
                val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
                val homeSelectedGenre by viewModel.homeSelectedGenre.collectAsStateWithLifecycle()
                val featuredBanners by viewModel.featuredBanners.collectAsStateWithLifecycle()
                val isLoadingTmdb by viewModel.isLoadingTmdb.collectAsStateWithLifecycle()
                val infiniteMovies by viewModel.infiniteMovies.collectAsStateWithLifecycle()
                val isLoadingMoreMovies by viewModel.isLoadingMoreMovies.collectAsStateWithLifecycle()
                val bulkCatalogState by viewModel.bulkCatalogState.collectAsStateWithLifecycle()

                val streamingQuality by viewModel.streamingQuality.collectAsStateWithLifecycle()
                val diagnosticStatus by viewModel.diagnosticStatus.collectAsStateWithLifecycle()
                val isFixingConnection by viewModel.isFixingConnection.collectAsStateWithLifecycle()
                val toastNotice by viewModel.toastNotice.collectAsStateWithLifecycle()
                val userState by viewModel.userState.collectAsStateWithLifecycle()

                // Admin & Content Engine State
                val publishedMovies by viewModel.publishedMovies.collectAsStateWithLifecycle()
                val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
                val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
                val isSyncingNow by viewModel.isSyncingNow.collectAsStateWithLifecycle()
                val isPublishingImdb by viewModel.isPublishingImdb.collectAsStateWithLifecycle()
                val lastPublishedMedia by viewModel.lastPublishedMedia.collectAsStateWithLifecycle()
                val secondsUntilNextHourlySync by viewModel.secondsUntilNextHourlySync.collectAsStateWithLifecycle()
                val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                // Trigger snackbar when toastNotice updates
                LaunchedEffect(toastNotice) {
                    toastNotice?.let { msg ->
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                        viewModel.clearToastNotice()
                    }
                }

                // Graceful Back Navigation Handling
                BackHandler(
                    enabled = pendingDownloadMediaItem != null ||
                            pendingExportItem != null ||
                            playerState.isVisible ||
                            serverModalMedia != null ||
                            detailMediaItem != null ||
                            isSearchOpen ||
                            currentDestination != AppDestination.HOME ||
                            homeSelectedGenre != "All"
                ) {
                    when {
                        pendingDownloadMediaItem != null -> viewModel.dismissDownloadDialog()
                        pendingExportItem != null -> viewModel.dismissExportDialog()
                        playerState.isVisible -> viewModel.closePlayer()
                        serverModalMedia != null -> viewModel.closeServerSelection()
                        detailMediaItem != null -> viewModel.closeMediaDetail()
                        isSearchOpen -> viewModel.closeSearch()
                        homeSelectedGenre != "All" -> viewModel.selectHomeGenre("All")
                        currentDestination == AppDestination.ADMIN -> viewModel.navigateTo(AppDestination.SETTINGS)
                        currentDestination != AppDestination.HOME -> viewModel.navigateTo(AppDestination.HOME)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DarkBackground,
                        topBar = {
                            if (!playerState.isVisible && currentDestination != AppDestination.ADMIN) {
                                HeaderBar(
                                    isSearchOpen = isSearchOpen,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                    onOpenSearch = { viewModel.openSearch() },
                                    onCloseSearch = { viewModel.closeSearch() },
                                    onNavigateToAdmin = { viewModel.navigateTo(AppDestination.ADMIN) }
                                )
                            }
                        },
                        bottomBar = {
                            if (!playerState.isVisible && currentDestination != AppDestination.ADMIN) {
                                BottomNavBar(
                                    currentDestination = currentDestination,
                                    onNavigate = {
                                        if (isSearchOpen) viewModel.closeSearch()
                                        viewModel.navigateTo(it)
                                    }
                                )
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = 70.dp)
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isSearchOpen) {
                                SearchScreen(
                                    query = searchQuery,
                                    results = searchResults,
                                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                                    onMediaClick = onMediaClickWithAd
                                )
                            } else {
                                when (currentDestination) {
                                    AppDestination.HOME -> {
                                        HomeScreen(
                                            heroIndex = heroIndex,
                                            featuredBanners = featuredBanners,
                                            watchHistory = watchHistory,
                                            watchlist = watchlist,
                                            selectedGenre = homeSelectedGenre,
                                            availableGenres = viewModel.homeAvailableGenres,
                                            filteredMediaItems = viewModel.getFilteredHomeItems(),
                                            isLoadingTmdb = isLoadingTmdb,
                                            infiniteMovies = infiniteMovies,
                                            isLoadingMoreMovies = isLoadingMoreMovies,
                                            onLoadMoreMovies = { viewModel.loadNextInfiniteMoviePage() },
                                            onSelectHeroIndex = { viewModel.setHeroIndex(it) },
                                            onWatchHero = { media ->
                                                viewModel.launchCinemaActivity(this@MainActivity, media)
                                            },
                                            onMediaClick = onMediaClickWithAd,
                                            onResumeHistory = { history ->
                                                val media = MovieCatalog.catalogItems.find { it.id == history.mediaId } ?: MovieCatalog.featuredBanners[0]
                                                viewModel.launchCinemaActivity(this@MainActivity, media)
                                            },
                                            onSelectGenre = { viewModel.selectHomeGenre(it) },
                                            onNavigateToWatchlist = { viewModel.navigateTo(AppDestination.WATCHLIST) }
                                        )
                                    }
                                    AppDestination.CATEGORIES -> {
                                        CategoriesScreen(
                                            selectedCategory = selectedCategory,
                                            selectedGenre = selectedGenre,
                                            availableGenres = viewModel.availableGenres,
                                            items = viewModel.getFilteredCategoryItems(),
                                            categoryGroups = bulkCatalogState.categories,
                                            bulkCatalogState = bulkCatalogState,
                                            onTriggerBulkSync = { viewModel.startBulkCatalogSync(50, 50) },
                                            onSelectCategory = { viewModel.selectCategory(it) },
                                            onSelectGenre = { viewModel.selectGenre(it) },
                                            onMediaClick = onMediaClickWithAd
                                        )
                                    }
                                    AppDestination.WATCHLIST -> {
                                        WatchlistScreen(
                                            watchlist = watchlist,
                                            onMediaClick = onMediaClickWithAd,
                                            onPlayMedia = { media ->
                                                viewModel.launchCinemaActivity(this@MainActivity, media)
                                            },
                                            onRemoveFromWatchlist = { viewModel.removeFromWatchlist(it) },
                                            onClearAllWatchlist = { viewModel.clearWatchlist() },
                                            onExploreMovies = { viewModel.navigateTo(AppDestination.CATEGORIES) }
                                        )
                                    }
                                    AppDestination.DOWNLOADS -> {
                                        DownloadsScreen(
                                            activeDownloads = activeDownloads,
                                            completedDownloads = completedDownloads,
                                            deviceVideos = deviceVideos,
                                            hasStoragePermission = hasStoragePermission,
                                            onPlayOffline = { dl ->
                                                val localFile = dl.localFilePath?.let { java.io.File(it) }
                                                val effectiveSource = if (localFile != null && localFile.exists() && localFile.length() > 65536L) {
                                                    localFile.absolutePath
                                                } else {
                                                    com.example.data.service.MovieStreamUrlResolver.resolveUrlFromInput(dl.streamUrl, dl.title, dl.mediaId)
                                                }
                                                PlayerActivity.launch(
                                                    context = this@MainActivity,
                                                    videoUrl = effectiveSource,
                                                    movieTitle = dl.title,
                                                    mediaId = dl.mediaId
                                                )
                                            },
                                            onDeleteDownload = { viewModel.deleteDownload(it) },
                                            onPromptSaveToDevice = { item ->
                                                viewModel.exportDownloadedItemToPhoneStorage(this@MainActivity, item)
                                            },
                                            onPlayDeviceVideo = { devVideo ->
                                                viewModel.playLocalVideoUri(
                                                    this@MainActivity,
                                                    devVideo.uri,
                                                    devVideo.title
                                                )
                                            },
                                            onOpenLocalVideoPicker = {
                                                videoFilePickerLauncher.launch(arrayOf("video/*"))
                                            },
                                            onRequestStoragePermission = {
                                                storagePermissionLauncher.launch(storagePermission)
                                            },
                                            onRefreshDeviceVideos = {
                                                viewModel.queryLocalPhoneVideos(this@MainActivity)
                                            }
                                        )
                                    }
                                    AppDestination.SETTINGS -> {
                                        SettingsScreen(
                                            streamingQuality = streamingQuality,
                                            diagnosticStatus = diagnosticStatus,
                                            isFixingConnection = isFixingConnection,
                                            userState = userState,
                                            currentThemeMode = currentThemeMode,
                                            onSelectThemeMode = { viewModel.setThemeMode(it) },
                                            onSetStreamingQuality = { viewModel.setStreamingQuality(it) },
                                            onClearCacheAndHistory = { viewModel.clearCacheAndHistory() },
                                            onFixConnection = { viewModel.fixConnection() },
                                            onTogglePremium = { viewModel.togglePremiumStatus() },
                                            onTestResilientPlayer = {
                                                val testItem = featuredBanners.firstOrNull() ?: MovieCatalog.featuredBanners[0]
                                                viewModel.launchCinemaActivity(this@MainActivity, testItem)
                                            },
                                            onNavigateToAdmin = { viewModel.navigateTo(AppDestination.ADMIN) }
                                        )
                                    }
                                    AppDestination.ADMIN -> {
                                        AdminDashboardScreen(
                                            publishedMovies = publishedMovies,
                                            syncLogs = syncLogs,
                                            isAutoSyncEnabled = isAutoSyncEnabled,
                                            isSyncingNow = isSyncingNow,
                                            isPublishingImdb = isPublishingImdb,
                                            lastPublishedMedia = lastPublishedMedia,
                                            secondsUntilNextHourlySync = secondsUntilNextHourlySync,
                                            lastSyncTimestamp = lastSyncTimestamp,
                                            onBack = { viewModel.navigateTo(AppDestination.SETTINGS) },
                                            onToggleAutoSync = { viewModel.toggleAutoSync(it) },
                                            onTriggerSyncNow = { viewModel.triggerAutomatedSyncNow() },
                                            onPublishByImdbId = { viewModel.publishByImdbId(it) },
                                            onDeletePublishedMovie = { viewModel.deletePublishedMovie(it) },
                                            onClearAllPublished = { viewModel.clearAllPublishedMovies() },
                                            onPlayMedia = { media ->
                                                viewModel.launchCinemaActivity(this@MainActivity, media)
                                            },
                                            onOpenDetail = { viewModel.openMediaDetail(it) },
                                            onDismissPublishedPreview = { viewModel.clearLastPublishedMedia() },
                                            onSyncTmdbNow = { viewModel.syncFromTmdbNow() },
                                            userState = userState,
                                            onTogglePremium = { viewModel.togglePremiumStatus() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Media Detail Overlay
                    AnimatedVisibility(
                        visible = detailMediaItem != null && !playerState.isVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        detailMediaItem?.let { media ->
                            val completedDl = completedDownloads.find { it.mediaId == media.id }
                            val activeDl = activeDownloads.find { it.mediaId == media.id }
                            val isDownloaded = completedDl != null
                            val isDownloading = activeDl != null
                            val dlProgress = activeDl?.progressPercent ?: 0
                            val isInWatchlist = watchlist.any { it.mediaId == media.id }

                            MediaDetailOverlay(
                                mediaItem = media,
                                detailsState = movieDetailsState,
                                archiveSearchState = archiveSearchState,
                                onRefreshRetrofit = { viewModel.refreshCurrentMovieDetails() },
                                isAlreadyDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                downloadProgress = dlProgress,
                                isInWatchlist = isInWatchlist,
                                onToggleWatchlist = { viewModel.toggleWatchlist(media) },
                                onClose = { viewModel.closeMediaDetail() },
                                onPlayStream = { viewModel.launchCinemaActivity(this@MainActivity, media) },
                                onLaunchCinemaPlayer = { season, episode, qualityProfile ->
                                    viewModel.launchCinemaActivity(
                                        context = this@MainActivity,
                                        mediaItem = media,
                                        season = season,
                                        episode = episode,
                                        qualityProfile = qualityProfile
                                    )
                                },
                                onDownloadOffline = { season, episode ->
                                    if (isDownloaded && completedDl != null) {
                                        val localFile = completedDl.localFilePath?.let { java.io.File(it) }
                                        val effectiveSource = if (localFile != null && localFile.exists() && localFile.length() > 65536L) {
                                            localFile.absolutePath
                                        } else {
                                            com.example.data.service.MovieStreamUrlResolver.resolveUrlFromInput(completedDl.streamUrl, completedDl.title, completedDl.mediaId)
                                        }
                                        PlayerActivity.launch(
                                            context = this@MainActivity,
                                            videoUrl = effectiveSource,
                                            movieTitle = completedDl.title,
                                            mediaId = completedDl.mediaId
                                        )
                                    } else if (!isDownloading) {
                                        viewModel.startOfflineDownload(media, season = season, episode = episode)
                                    }
                                },
                                onWatchArchiveStream = { streamUrl ->
                                    viewModel.watchArchiveStream(this@MainActivity, media, streamUrl)
                                },
                                onDownloadArchiveVideo = { downloadUrl ->
                                    viewModel.startNativeDownload(this@MainActivity, downloadUrl, media.title)
                                }
                            )
                        }
                    }

                    // Multi-Server Redundancy Modal (Direct Instant Playback)
                    if (serverModalMedia != null) {
                        MultiServerModal(
                            mediaItem = serverModalMedia!!,
                            onServerSelected = { server ->
                                val targetMedia = serverModalMedia!!
                                viewModel.closeServerSelection()
                                viewModel.launchCinemaActivity(this@MainActivity, targetMedia, server = server)
                            },
                            onDismiss = { viewModel.closeServerSelection() }
                        )
                    }

                    // Download Quality Selection Dialog
                    if (pendingDownloadMediaItem != null) {
                        DownloadQualityDialog(
                            mediaItem = pendingDownloadMediaItem!!,
                            onConfirmDownload = { qualityProfile ->
                                val targetMedia = pendingDownloadMediaItem!!
                                viewModel.dismissDownloadDialog()
                                viewModel.startOfflineDownloadWithQuality(targetMedia, qualityProfile)
                            },
                            onDismiss = { viewModel.dismissDownloadDialog() }
                        )
                    }

                    // Integrated Video Player Screen
                    AnimatedVisibility(
                        visible = playerState.isVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IntegratedPlayerScreen(
                            state = playerState,
                            onPlayPause = { viewModel.togglePlayerPlayPause() },
                            onSeekTo = { viewModel.playerSeekTo(it) },
                            onSkip10Forward = { viewModel.playerSkip10Forward() },
                            onSkip10Rewind = { viewModel.playerSkip10Rewind() },
                            onVolumeChange = { viewModel.setPlayerVolume(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onToggleOrientation = { viewModel.togglePlayerOrientation() },
                            onSelectSubtitle = { viewModel.selectSubtitleTrack(it) },
                            onSwitchServer = { viewModel.switchServer(it) },
                            onSimulateFailover = { viewModel.simulateServerFailureAndFallback() },
                            onClose = { viewModel.closePlayer() },
                            onSelectQuality = { viewModel.setPlayerQuality(it) },
                            onPositionUpdate = { viewModel.onPlayerPositionUpdate(it) },
                            onDurationDiscovered = { viewModel.onPlayerDurationDiscovered(it) },
                            onBuffering = { viewModel.onPlayerBuffering(it) },
                            onError = { viewModel.onPlayerError(it) },
                            onSeekConsumed = { viewModel.onSeekConsumed() }
                        )
                    }

                    // Loading Splash Screen with 2.5s Timeout & Preloading
                    SplashScreen(
                        visible = isSplashScreenVisible,
                        statusText = if (isLoadingTmdb) "Fetching TMDb Data..." else "Welcome to Movieskadaji"
                    )
                }
            }
        }
    }
}
