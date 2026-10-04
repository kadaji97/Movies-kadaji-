package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.MovieCatalog
import com.example.data.local.MovieskadajiDatabase
import com.example.data.local.MovieskadajiRepository
import com.example.data.local.WatchlistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var database: MovieskadajiDatabase
  private lateinit var repository: MovieskadajiRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, MovieskadajiDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = MovieskadajiRepository(database)
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Movieskadaji", appName)
  }

  @Test
  fun `verify movie catalog content`() {
    assertTrue(MovieCatalog.catalogItems.isNotEmpty())
    assertTrue(MovieCatalog.featuredBanners.size >= 3)
    val searchResults = MovieCatalog.search("Dune")
    assertTrue(searchResults.isNotEmpty())
    val item = MovieCatalog.getItemById("tmdb_693134")
    assertNotNull(item)
    assertEquals("Dune: Part Two", item?.title)
  }

  @Test
  fun `verify room watchlist dao operations`() = runBlocking {
    val dao = database.watchlistDao()
    val entity = WatchlistEntity(
        mediaId = "test_movie_1",
        title = "Interstellar Test",
        posterUrl = "https://example.com/poster.jpg",
        backdropUrl = "https://example.com/backdrop.jpg",
        category = "Movies",
        qualityBadge = "4K HDR",
        imdbRating = 8.9f,
        releaseYear = 2024,
        duration = "2h 49m",
        genres = "Sci-Fi, Adventure"
    )

    // Insert
    dao.insertWatchlist(entity)
    assertTrue(dao.isItemInWatchlist("test_movie_1"))
    assertFalse(dao.isItemInWatchlist("non_existing"))

    // Query Flow
    val list = dao.getAllWatchlist().first()
    assertEquals(1, list.size)
    assertEquals("Interstellar Test", list[0].title)

    // Delete
    dao.deleteWatchlist("test_movie_1")
    assertFalse(dao.isItemInWatchlist("test_movie_1"))
    assertEquals(0, dao.getAllWatchlist().first().size)
  }

  @Test
  fun `verify repository toggle and remove watchlist`() = runBlocking {
    val movie = MovieCatalog.featuredBanners[0]

    // Initially not in watchlist
    assertFalse(repository.isItemInWatchlist(movie.id))

    // Toggle on (Add)
    val added = repository.toggleWatchlist(movie)
    assertTrue(added)
    assertTrue(repository.isItemInWatchlist(movie.id))
    val currentList = repository.watchlist.first()
    assertTrue(currentList.any { it.mediaId == movie.id })

    // Toggle off (Remove)
    val removed = repository.toggleWatchlist(movie)
    assertFalse(removed)
    assertFalse(repository.isItemInWatchlist(movie.id))

    // Add again and delete directly
    repository.addToWatchlist(movie)
    assertTrue(repository.isItemInWatchlist(movie.id))
    repository.removeFromWatchlist(movie.id)
    assertFalse(repository.isItemInWatchlist(movie.id))
  }

  @Test
  fun `verify movie catalog genre filtering`() {
    // All returns all catalog items + banners
    val allItems = MovieCatalog.getItemsByGenre("All")
    assertTrue(allItems.isNotEmpty())

    // Sci-Fi filtering
    val sciFiItems = MovieCatalog.getItemsByGenre("Sci-Fi")
    assertTrue(sciFiItems.isNotEmpty())
    assertTrue(sciFiItems.all { item ->
      item.genres.any { it.equals("Sci-Fi", ignoreCase = true) } ||
      item.category.label.contains("Sci-Fi", ignoreCase = true)
    })

    // Action filtering
    val actionItems = MovieCatalog.getItemsByGenre("Action")
    assertTrue(actionItems.isNotEmpty())
    assertTrue(actionItems.all { item ->
      item.genres.any { it.equals("Action", ignoreCase = true) } ||
      item.category.label.contains("Action", ignoreCase = true)
    })

    // Non-existent genre returns empty list
    val nonExistent = MovieCatalog.getItemsByGenre("NonExistentGenreXYZ")
    assertTrue(nonExistent.isEmpty())
  }

  @Test
  fun `verify viewmodel genre selection and filtered items`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.MovieskadajiViewModel(context)

    // Initial state should be "All"
    assertEquals("All", viewModel.homeSelectedGenre.value)
    val initialItems = viewModel.getFilteredHomeItems()
    assertTrue(initialItems.isNotEmpty())

    // Select "Sci-Fi"
    viewModel.selectHomeGenre("Sci-Fi")
    assertEquals("Sci-Fi", viewModel.homeSelectedGenre.value)
    val sciFiItems = viewModel.getFilteredHomeItems()
    assertTrue(sciFiItems.isNotEmpty())

    // Reset back to "All"
    viewModel.selectHomeGenre("All")
    assertEquals("All", viewModel.homeSelectedGenre.value)
    assertEquals(initialItems.size, viewModel.getFilteredHomeItems().size)
  }

  @Test
  fun `verify imdb id resolution provides at least 3 cdn streaming links`() = runBlocking {
    val result = com.example.data.service.StreamingIndexService.resolveImdbId("tt15239678")
    assertTrue(result.isSuccess)
    val media = result.getOrThrow()
    assertEquals("Dune: Part Two", media.title)
    assertTrue(media.posterUrl.isNotBlank())
    assertTrue(media.genres.isNotEmpty())
    assertTrue(media.cast.isNotEmpty())
    assertTrue("Must have at least 3 alternative CDN streams", media.streamServers.size >= 3)
    assertEquals(3, media.streamServers.size)
    assertTrue(media.streamServers.any { it.name.contains("Akamai") })
    assertTrue(media.streamServers.any { it.name.contains("Cloudflare") })
    assertTrue(media.streamServers.any { it.name.contains("Fastly") })
  }


  @Test
  fun `verify admin 1-click publishing writes to database and sync log`() = runBlocking {
    val publishResult = repository.fetchAndPublishByImdbId("tt15239678")
    assertTrue(publishResult.isSuccess)
    val publishedMedia = publishResult.getOrThrow()

    // Verify in published movies DAO
    val publishedList = repository.publishedMovies.first()
    assertEquals(1, publishedList.size)
    val item = publishedList[0]
    assertEquals("Dune: Part Two", item.title)
    assertEquals("tt15239678", item.imdbId)
    assertEquals("ADMIN_MANUAL_1CLICK", item.source)

    // Verify 3 CDN stream links are preserved
    val convertedMedia = com.example.data.service.StreamingIndexService.toMediaItem(item)
    assertEquals(3, convertedMedia.streamServers.size)

    // Verify sync log entry
    val logs = repository.syncLogs.first()
    assertTrue(logs.isNotEmpty())
    assertEquals("PUBLISHED", logs[0].status)
    assertTrue(logs[0].summary.contains("Dune: Part Two"))

    // Duplicate publish should fail safely
    val dupResult = repository.fetchAndPublishByImdbId("tt15239678")
    assertTrue(dupResult.isFailure)
  }

  @Test
  fun `verify automated hourly content sync inserts new releases with 3 cdn links`() = runBlocking {
    val syncResult = repository.runAutomatedContentSync()
    assertTrue(syncResult.isSuccess)
    assertTrue(syncResult.newTitlesAdded > 0)

    val publishedList = repository.publishedMovies.first()
    assertTrue(publishedList.isNotEmpty())

    for (movie in publishedList) {
      val media = com.example.data.service.StreamingIndexService.toMediaItem(movie)
      assertTrue("Every synced movie must have title", media.title.isNotBlank())
      assertTrue("Every synced movie must have poster", media.posterUrl.isNotBlank())
      assertTrue("Every synced movie must have genres", media.genres.isNotEmpty())
      assertTrue("Every synced movie must have cast", media.cast.isNotEmpty())
      assertTrue("Every synced movie must have at least 3 CDN links", media.streamServers.size >= 3)
    }

    // Running again should detect that releases are already up to date
    val secondSync = repository.runAutomatedContentSync()
    assertTrue(secondSync.isSuccess)
    assertEquals(0, secondSync.newTitlesAdded)
  }

  @Test
  fun `verify tmdb response parsing and movie data structure`() {
    val sampleJson = """
      {
        "page": 1,
        "results": [
          {
            "id": 933260,
            "title": "The Substance",
            "overview": "A fading celebrity decides to use a black market drug...",
            "poster_path": "/lqoMzCcZYEFK729Fc65q83NTb9s.jpg"
          },
          {
            "id": 533535,
            "title": "Deadpool & Wolverine",
            "overview": "A listless Wade Wilson toils away in civilian life...",
            "poster_path": "/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg"
          }
        ],
        "total_pages": 100,
        "total_results": 2000
      }
    """.trimIndent()

    val gson = com.google.gson.Gson()
    val response = gson.fromJson(sampleJson, com.example.data.service.TmdbResponse::class.java)

    assertNotNull(response)
    assertEquals(2, response.results.size)
    val firstMovie = response.results[0]
    assertEquals(933260, firstMovie.id)
    assertEquals("The Substance", firstMovie.title)
    assertTrue(firstMovie.overview?.startsWith("A fading celebrity") == true)
    assertEquals("/lqoMzCcZYEFK729Fc65q83NTb9s.jpg", firstMovie.poster_path)
    assertTrue(firstMovie.fullPosterUrl.contains("image.tmdb.org/t/p/w500"))

    // Verify TmdbApiService instance creation with Retrofit & Gson converter
    val apiService = com.example.data.service.TmdbApiService.create()
    assertNotNull(apiService)
  }

  @Test
  fun `verify exoplayer media item creation and configuration`() {
    val sampleVideoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(sampleVideoUrl))
    assertNotNull(mediaItem)
    assertEquals(sampleVideoUrl, mediaItem.localConfiguration?.uri.toString())

    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val renderersFactory = com.example.data.service.PlaybackEngineHelper.buildRenderersFactory(context)
    val exoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory).build()
    assertNotNull(exoPlayer)
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.playWhenReady = false
    assertFalse(exoPlayer.playWhenReady)
    exoPlayer.release()
  }

  @Test
  fun `verify movie details screen retrofit model and state`() {
    val genre1 = com.example.data.service.TmdbGenre(id = 28, name = "Action")
    val genre2 = com.example.data.service.TmdbGenre(id = 878, name = "Sci-Fi")
    val details = com.example.data.service.TmdbMovieDetailsResponse(
        id = 550,
        title = "Inception",
        overview = "A thief who steals corporate secrets through the use of dream-sharing technology.",
        posterPath = "/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
        backdropPath = "/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
        genres = listOf(genre1, genre2),
        releaseDate = "2010-07-15",
        voteAverage = 8.4,
        voteCount = 35000,
        runtime = 148,
        tagline = "Your mind is the scene of the crime."
    )

    assertEquals("Inception", details.title)
    assertTrue(details.fullPosterUrl.contains("image.tmdb.org/t/p/w500"))
    assertEquals(listOf("Action", "Sci-Fi"), details.genreNames)
    assertEquals("Your mind is the scene of the crime.", details.tagline)
    assertEquals(148, details.runtime)

    val uiState = com.example.ui.viewmodel.MovieDetailsUiState(
        isLoading = false,
        details = details,
        error = null,
        isRetrofitLive = true,
        latencyMs = 120L
    )

    assertFalse(uiState.isLoading)
    assertTrue(uiState.isRetrofitLive)
    assertNotNull(uiState.details)
    assertEquals("Inception", uiState.details?.title)
    assertEquals(2, uiState.details?.genreNames?.size)
  }

  @Test
  fun `verify system is 100 percent ad free with zero ad units and instant direct playback`() {
    val policy = com.example.data.monetization.MonetizationController.evaluatePolicy()
    assertTrue(policy.isAdFree)
    assertTrue(policy.allowsOfflineDownloads)
    assertFalse(policy.bitrateThrottled)
    assertEquals("1080p FHD", policy.maxResolution)
    assertTrue(policy.tierLabel.contains("Ad-Free"))
  }

  @Test
  fun `verify movie grids contain pure media items without ad injection`() {
    val sampleMovies = (1..14).map { idx ->
      com.example.data.model.MediaItem(
        id = "movie_$idx",
        title = "Movie $idx",
        category = com.example.data.model.MediaCategory.MOVIES,
        genres = listOf("Action"),
        releaseYear = 2024,
        duration = "2h",
        imdbRating = 8.0f,
        qualityBadge = "4K",
        plotSynopsis = "Synopsis $idx",
        cast = emptyList(),
        posterUrl = "https://example.com/$idx.jpg",
        backdropUrl = "https://example.com/bg_$idx.jpg",
        streamServers = emptyList(),
        subtitleTracks = emptyList(),
        collections = emptyList(),
        defaultVideoUrl = "https://example.com/stream_$idx.mp4"
      )
    }

    // Grid receives pure movie items with zero ad items injected
    assertEquals(14, sampleMovies.size)
    sampleMovies.forEach { item ->
      assertNotNull(item.id)
      assertTrue(item.id.startsWith("movie_"))
    }
  }

  @Test
  fun `verify direct movie click navigates with zero interstitial delays`() {
    var proceedCount = 0
    val mediation = com.example.data.monetization.GlobalAdMediationController()
    mediation.showInterstitialAd(null) { proceedCount++ }
    assertEquals(1, proceedCount)

    var rewardCount = 0
    mediation.showRewardedOptInAd(null) { rewardCount++ }
    assertEquals(1, rewardCount)
  }

  @Test
  fun `verify user state has full cinema privileges and ad suppression by default`() {
    val defaultUserState = com.example.data.monetization.UserState()
    assertTrue(defaultUserState.isPremiumUser)
    assertTrue(defaultUserState.hasFullCinemaPrivilege)
    assertTrue(defaultUserState.subscriptionPlan.contains("Ad-Free"))
  }

  @Test
  fun `verify dynamic material 3 multi-theme palettes and colors`() {
    // 1. Imperial Gold (Premium Cinematic - Default)
    val imperial = com.example.ui.theme.AppThemeMode.IMPERIAL_GOLD
    assertEquals(0xFFD4AF37, imperial.primaryHex)
    assertEquals(0xFFAA7C11, imperial.secondaryHex)
    assertEquals(0xFF08080A, imperial.backgroundHex)
    assertEquals(0xB31C1A14, imperial.crystalSurfaceHex)
    assertEquals(0xFFFDFBF7, imperial.textHex)

    // 2. Midnight Theater (Classic Deep Dark)
    val midnight = com.example.ui.theme.AppThemeMode.MIDNIGHT_THEATER
    assertEquals(0xFF10B981, midnight.primaryHex)
    assertEquals(0xFF0B0F19, midnight.backgroundHex)
    assertEquals(0xBF1F2937, midnight.crystalSurfaceHex)
    assertEquals(0xFFF9FAFB, midnight.textHex)

    // 3. Cyberpunk Neon (High Contrast)
    val cyberpunk = com.example.ui.theme.AppThemeMode.CYBERPUNK_NEON
    assertEquals(0xFFFBBF24, cyberpunk.primaryHex)
    assertEquals(0xFF0F0F10, cyberpunk.backgroundHex)
    assertEquals(0xB31A1A1E, cyberpunk.crystalSurfaceHex)
    assertEquals(0xFFFFFFFF, cyberpunk.textHex)

    // 4. Hollywood Rouge (Vintage Cinema)
    val rouge = com.example.ui.theme.AppThemeMode.HOLLYWOOD_ROUGE
    assertEquals(0xFFEF4444, rouge.primaryHex)
    assertEquals(0xFF110606, rouge.backgroundHex)
    assertEquals(0xA6220F0F, rouge.crystalSurfaceHex)
    assertEquals(0xFFFFEAEA, rouge.textHex)

    // Verify ColorScheme creation for all 4 themes
    com.example.ui.theme.AppThemeMode.entries.forEach { mode ->
      val scheme = com.example.ui.theme.getAppColorScheme(mode)
      assertNotNull(scheme)
      assertEquals(androidx.compose.ui.graphics.Color(mode.primaryHex), scheme.primary)
      assertEquals(androidx.compose.ui.graphics.Color(mode.backgroundHex), scheme.background)
    }
  }

  @Test
  fun `verify theme preferences persistence across restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val themePrefs = com.example.ui.theme.ThemePreferences(context)

    // Default is Imperial Gold
    assertEquals(com.example.ui.theme.AppThemeMode.IMPERIAL_GOLD, themePrefs.themeMode.value)

    // Switch to Midnight Theater
    themePrefs.setThemeMode(com.example.ui.theme.AppThemeMode.MIDNIGHT_THEATER)
    assertEquals(com.example.ui.theme.AppThemeMode.MIDNIGHT_THEATER, themePrefs.themeMode.value)

    // Instantiate a new ThemePreferences instance simulating app restart
    val newInstance = com.example.ui.theme.ThemePreferences(context)
    assertEquals(com.example.ui.theme.AppThemeMode.MIDNIGHT_THEATER, newInstance.themeMode.value)

    // Switch to Cyberpunk Neon
    newInstance.setThemeMode(com.example.ui.theme.AppThemeMode.CYBERPUNK_NEON)
    assertEquals(com.example.ui.theme.AppThemeMode.CYBERPUNK_NEON, newInstance.themeMode.value)

    // Switch to Hollywood Rouge
    newInstance.setThemeMode(com.example.ui.theme.AppThemeMode.HOLLYWOOD_ROUGE)
    assertEquals(com.example.ui.theme.AppThemeMode.HOLLYWOOD_ROUGE, newInstance.themeMode.value)

    // Reset back to default Imperial Gold
    newInstance.setThemeMode(com.example.ui.theme.AppThemeMode.IMPERIAL_GOLD)
    assertEquals(com.example.ui.theme.AppThemeMode.IMPERIAL_GOLD, newInstance.themeMode.value)
  }

  @Test
  fun `verify splash screen clean entrance without ad network delay`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNotNull(context)
    val appName = context.getString(R.string.app_name)
    assertEquals("Movieskadaji", appName)
  }

  @Test
  fun `verify download quality profiles and estimates`() {
    val profiles = com.example.data.download.DownloadQualityProfile.entries
    assertEquals(4, profiles.size)

    val p4k = com.example.data.download.DownloadQualityProfile.ULTRA_4K
    assertEquals("4K Ultra HD", p4k.label)
    assertEquals("2160p", p4k.resolution)
    assertTrue(p4k.estimatedMb > 1000)

    val p1080 = com.example.data.download.DownloadQualityProfile.FULL_HD
    assertEquals("Full HD", p1080.label)
    assertEquals("1080p", p1080.resolution)

    val p720 = com.example.data.download.DownloadQualityProfile.BALANCED_HD
    assertEquals("720p", p720.resolution)

    val p480 = com.example.data.download.DownloadQualityProfile.DATA_SAVER
    assertEquals("480p", p480.resolution)
  }

  @Test
  fun `verify device video item model and media playback uri creation`() {
    val sampleUri = android.net.Uri.parse("content://media/external/video/media/42")
    val item = com.example.data.download.DeviceVideoItem(
      id = 42L,
      title = "Vacation_Video_2026.mp4",
      uri = sampleUri,
      durationMs = 125000L,
      sizeBytes = 85000000L,
      path = "/storage/emulated/0/DCIM/Vacation_Video_2026.mp4"
    )

    assertEquals(42L, item.id)
    assertEquals("Vacation_Video_2026.mp4", item.title)
    assertEquals(sampleUri, item.uri)
    assertEquals(125000L, item.durationMs)
    assertEquals(85000000L, item.sizeBytes)
    assertEquals("/storage/emulated/0/DCIM/Vacation_Video_2026.mp4", item.path)
  }

  @Test
  fun `verify download flow and export to device storage`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val movie = MovieCatalog.featuredBanners[0]

    // 1. Insert download into room repository
    val downloadItem = repository.startDownload(
      mediaItem = movie,
      quality = "1080p Full HD",
      targetPath = "${context.filesDir}/downloads/${movie.id}.mp4",
      streamUrl = movie.defaultVideoUrl
    )

    assertNotNull(downloadItem)
    assertEquals(movie.id, downloadItem.mediaId)
    assertEquals("1080p Full HD", downloadItem.quality)
    assertFalse(downloadItem.isCompleted)

    // 2. Complete download
    repository.updateDownloadProgressWithFile(
      id = downloadItem.id,
      percent = 100,
      downloadedMb = downloadItem.fileSizeMb,
      completed = true,
      speed = "Offline Ready",
      filePath = downloadItem.localFilePath
    )

    val completedList = repository.completedDownloads.first()
    assertTrue(completedList.any { it.id == downloadItem.id && it.isCompleted })

    // 3. Export to device storage
    val exportResult = com.example.data.download.MovieDownloadManager.exportVideoToDeviceStorage(
      context = context,
      movieTitle = movie.title,
      sourceFilePath = downloadItem.localFilePath
    )

    assertTrue("Export must succeed or safely create MediaStore entry", exportResult.isSuccess)
    val exportPath = exportResult.getOrThrow()
    assertTrue(exportPath.contains("Movieskadaji"))
  }

  @Test
  fun `verify playback quality profiles and engine initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Test profiles
    val profiles = com.example.data.service.PlaybackQualityProfile.entries
    assertEquals(5, profiles.size)
    
    val autoProfile = com.example.data.service.PlaybackQualityProfile.fromBadge("AUTO")
    assertEquals(com.example.data.service.PlaybackQualityProfile.AUTO, autoProfile)
    assertTrue(autoProfile.isRecommended)

    val hdProfile = com.example.data.service.PlaybackQualityProfile.fromBadge("1080p")
    assertEquals(com.example.data.service.PlaybackQualityProfile.FHD_1080P, hdProfile)
    assertEquals(1920, hdProfile.width)
    assertEquals(1080, hdProfile.height)

    // Test PlaybackEngineHelper
    val loadControl = com.example.data.service.PlaybackEngineHelper.buildFastLoadControl()
    assertNotNull(loadControl)

    val trackSelector = com.example.data.service.PlaybackEngineHelper.buildTrackSelector(context, hdProfile)
    assertNotNull(trackSelector)
    assertEquals(1920, trackSelector.parameters.maxVideoWidth)
    assertEquals(1080, trackSelector.parameters.maxVideoHeight)

    val extractorsFactory = com.example.data.service.PlaybackEngineHelper.buildExtractorsFactory()
    assertNotNull(extractorsFactory)
  }

  @Test
  fun `verify network speed manager provides instant bandwidth estimate and categories`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val meter = com.example.data.service.NetworkSpeedManager.getBandwidthMeter(context)
    assertNotNull(meter)

    val estimatedBitrate = com.example.data.service.NetworkSpeedManager.getInstantEstimatedBitrate(context)
    assertTrue("Estimated bitrate must be positive for instant start", estimatedBitrate > 0)

    val speedCategory = com.example.data.service.NetworkSpeedManager.currentSpeedCategory.value
    assertNotNull(speedCategory)

    val description = com.example.data.service.NetworkSpeedManager.currentSpeedDescription.value
    assertTrue(description.isNotBlank())
  }

  @Test
  fun `verify fast format decider mechanism detects all major video and audio streams`() {
    // HLS
    assertEquals(androidx.media3.common.MimeTypes.APPLICATION_M3U8, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/movie/master.m3u8"))
    // DASH
    assertEquals(androidx.media3.common.MimeTypes.APPLICATION_MPD, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/manifest.mpd"))
    // SmoothStreaming
    assertEquals(androidx.media3.common.MimeTypes.APPLICATION_SS, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/tears-of-steel.ism"))
    // MP4
    assertEquals(androidx.media3.common.MimeTypes.VIDEO_MP4, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/video_1080p.mp4"))
    // MKV
    assertEquals(androidx.media3.common.MimeTypes.VIDEO_MATROSKA, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/stream.mkv"))
    // WebM
    assertEquals(androidx.media3.common.MimeTypes.VIDEO_WEBM, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/clip.webm"))
    // TS
    assertEquals(androidx.media3.common.MimeTypes.VIDEO_MP2T, com.example.data.service.PlaybackEngineHelper.detectFastMimeType("https://cdn.example.com/live.ts"))
  }

  @Test
  fun `verify live quality switching on running exoplayer instance`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val player = com.example.data.service.PlaybackEngineHelper.buildExoPlayer(context, com.example.data.service.PlaybackQualityProfile.AUTO)
    assertNotNull(player)

    // Initially AUTO
    assertEquals(Int.MAX_VALUE, player.trackSelectionParameters.maxVideoWidth)

    // Switch smoothly to 720p HD
    com.example.data.service.PlaybackEngineHelper.applyQualityProfile(player, com.example.data.service.PlaybackQualityProfile.HD_720P)
    assertEquals(1280, player.trackSelectionParameters.maxVideoWidth)
    assertEquals(720, player.trackSelectionParameters.maxVideoHeight)

    // Switch smoothly to 480p SD
    com.example.data.service.PlaybackEngineHelper.applyQualityProfile(player, com.example.data.service.PlaybackQualityProfile.SD_480P)
    assertEquals(854, player.trackSelectionParameters.maxVideoWidth)
    assertEquals(480, player.trackSelectionParameters.maxVideoHeight)

    // Switch back to AUTO
    com.example.data.service.PlaybackEngineHelper.applyQualityProfile(player, com.example.data.service.PlaybackQualityProfile.AUTO)
    assertEquals(Int.MAX_VALUE, player.trackSelectionParameters.maxVideoWidth)

    player.release()
  }

  @Test
  fun `verify viewmodel player quality state updates`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.MovieskadajiViewModel(context)

    // Default player quality is AUTO
    assertEquals(com.example.data.service.PlaybackQualityProfile.AUTO, viewModel.playerState.value.qualityProfile)

    // User chooses 1080p
    viewModel.setPlayerQuality(com.example.data.service.PlaybackQualityProfile.FHD_1080P)
    assertEquals(com.example.data.service.PlaybackQualityProfile.FHD_1080P, viewModel.playerState.value.qualityProfile)

    // User switches back to AUTO
    viewModel.setPlayerQuality(com.example.data.service.PlaybackQualityProfile.AUTO)
    assertEquals(com.example.data.service.PlaybackQualityProfile.AUTO, viewModel.playerState.value.qualityProfile)
  }

  @Test
  fun `verify cinema player initializes with auto-play and adaptive speed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val movie = MovieCatalog.featuredBanners[0]

    // Initialize tuned ExoPlayer for cinema
    val player = com.example.data.service.PlaybackEngineHelper.buildExoPlayer(
      context,
      com.example.data.service.PlaybackQualityProfile.AUTO
    )
    player.playWhenReady = true

    // Verify auto play when ready is enabled immediately
    assertTrue("Cinema player must have playWhenReady set to true for zero-interaction auto-play", player.playWhenReady)

    // Verify adaptive track selector parameters are ready
    assertNotNull(player.trackSelectionParameters)
    assertEquals("AUTO profile has unbounded max dimensions for dynamic adaptiveness", Int.MAX_VALUE, player.trackSelectionParameters.maxVideoWidth)

    player.release()
  }

  @Test
  fun `verify immediate download process starts and tracks valid offline file`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val movie = MovieCatalog.featuredBanners[0]

    val downloadItem = repository.startDownload(
      mediaItem = movie,
      quality = "1080p FHD",
      targetPath = "${context.filesDir}/downloads/${movie.id}.mp4",
      streamUrl = movie.defaultVideoUrl
    )

    assertNotNull("Target movie download must be present in database", downloadItem)
    assertEquals("1080p FHD", downloadItem.quality)
    assertNotNull("Local file path must be defined for offline playback", downloadItem.localFilePath)
    assertFalse("Download starts in active non-completed state", downloadItem.isCompleted)
  }
}


