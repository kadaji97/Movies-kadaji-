package com.example

import com.example.data.catalog.MovieCatalog
import com.example.data.local.DownloadItemEntity
import com.example.data.model.MediaItem
import com.example.data.model.StreamServer
import com.example.data.service.toMediaItem
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun hybridMediaRepository_resolvesMediaTargetCorrectly() = kotlinx.coroutines.runBlocking {
    val movie = MovieCatalog.catalogItems.first()
    val target = com.example.data.service.HybridMediaRepository.resolveMediaTarget(movie)
    assertNotNull(target)
    assertTrue(target.streamUrl.isNotBlank())
    assertTrue(target.streamUrl.startsWith("http"))
    assertEquals(movie.title, target.title)
    assertFalse(target.isTv)
  }

  @Test
  fun appConfig_movieUrlBuildsProperly() {
    val movieUrl = com.example.data.config.AppConfig.buildMovieUrl("693134")
    assertTrue(movieUrl.contains("693134"))
    assertTrue(movieUrl.startsWith("http"))
    val tvUrl = com.example.data.config.AppConfig.buildTvUrl("12345", 2, 4)
    assertTrue(tvUrl.contains("12345"))
    assertTrue(tvUrl.contains("2"))
    assertTrue(tvUrl.contains("4"))
  }

  @Test
  fun catalogItems_haveValidStreamServersAndVideoUrls() {
    val items = MovieCatalog.catalogItems
    assertTrue(items.isNotEmpty())
    items.forEach { item ->
      assertNotNull(item.defaultVideoUrl)
      assertTrue(item.defaultVideoUrl.startsWith("http://") || item.defaultVideoUrl.startsWith("https://"))
      assertTrue(item.streamServers.isNotEmpty())
      item.streamServers.forEach { server ->
        assertTrue(server.videoUrl.isNotBlank())
        assertTrue(server.quality.isNotBlank())
      }
    }
  }

  @Test
  fun downloadItemEntity_filePathAndProgressLogic() {
    val download = DownloadItemEntity(
      id = "dl_test_1",
      mediaId = "m1",
      title = "Cyberpunk Neo: 2099",
      posterUrl = "https://example.com/poster.jpg",
      progressPercent = 100,
      fileSizeMb = 1420.0,
      downloadedMb = 1420.0,
      isCompleted = true,
      downloadSpeed = "Offline Ready",
      quality = "1080p",
      timestamp = System.currentTimeMillis(),
      localFilePath = "/data/data/com.example/files/downloads/dl_m1_1080p.mp4",
      streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    )

    assertEquals("dl_test_1", download.id)
    assertTrue(download.isCompleted)
    assertEquals(100, download.progressPercent)
    assertNotNull(download.localFilePath)
    assertTrue(download.localFilePath!!.endsWith(".mp4"))
  }

  @Test
  fun streamServerRedundancy_failoverCandidateExists() {
    val item = MovieCatalog.catalogItems.first()
    assertTrue(item.streamServers.size >= 2)
    val server1 = item.streamServers[0]
    val server2 = item.streamServers[1]
    assertNotEquals(server1.id, server2.id)
  }

  @Test
  fun internetArchive_cleanTitleForQuery() {
    val titleWithPunctuation = "Dune: Part Two - The IMAX Edition / Final"
    val cleaned = com.example.data.service.InternetArchiveService.cleanTitleForQuery(titleWithPunctuation)
    assertFalse(cleaned.contains(":"))
    assertFalse(cleaned.contains("-"))
    assertFalse(cleaned.contains("/"))
    assertTrue(cleaned.startsWith("Dune Part Two"))
  }

  @Test
  fun internetArchive_curatedInstantPairingAndCdnUrl() = kotlinx.coroutines.runBlocking {
    val result = com.example.data.service.InternetArchiveService.searchArchiveForMovie("Night of the Living Dead", 1968)
    assertNotNull(result)
    assertTrue(result!!.isPublicDomainFound)
    assertEquals("night_of_the_living_dead_1968", result.identifier)
    assertEquals("https://archive.org/download/night_of_the_living_dead_1968/night_of_the_living_dead_1968.mp4", result.streamUrl)
    assertEquals(result.streamUrl, result.downloadUrl)
  }

  @Test
  fun internetArchive_fallbackWatchProvidersForCommercialMovie() {
    val providers = com.example.data.service.InternetArchiveService.getFallbackWatchProviders("Oppenheimer")
    assertTrue(providers.isNotEmpty())
    assertTrue(providers.any { it.providerName == "Netflix" })
    assertTrue(providers.any { it.providerName.contains("Prime") })
  }

  @Test
  fun bulkDataVolume_tvSeriesAndMovieMapping() {
    val movieJson = """{"id":101,"title":"Interstellar","overview":"Space journey","vote_average":8.7,"release_date":"2014-11-05","genre_ids":[878,18]}"""
    val tvJson = """{"id":202,"name":"Breaking Bad","overview":"Chemistry teacher turned kingpin","vote_average":9.5,"first_air_date":"2008-01-20","genre_ids":[18,80]}"""

    val gson = com.google.gson.Gson()
    val movie = gson.fromJson(movieJson, com.example.data.service.Movie::class.java)
    val tv = gson.fromJson(tvJson, com.example.data.service.Movie::class.java)

    assertEquals("Interstellar", movie.displayTitle)
    assertEquals("2014-11-05", movie.displayDate)
    assertEquals("Breaking Bad", tv.displayTitle)
    assertEquals("2008-01-20", tv.displayDate)

    val genreMap = mapOf(878 to "Sci-Fi", 18 to "Drama", 80 to "Crime")
    val movieMedia = movie.toMediaItem(category = com.example.data.model.MediaCategory.MOVIES, genreMap = genreMap)
    val tvMedia = tv.toMediaItem(category = com.example.data.model.MediaCategory.TV_SHOWS, genreMap = genreMap)

    assertEquals("tmdb_101", movieMedia.id)
    assertEquals(com.example.data.model.MediaCategory.MOVIES, movieMedia.category)
    assertTrue(movieMedia.genres.contains("Sci-Fi"))

    assertEquals("tmdb_tv_202", tvMedia.id)
    assertEquals(com.example.data.model.MediaCategory.TV_SHOWS, tvMedia.category)
    assertTrue(tvMedia.genres.contains("Drama"))
    assertTrue(tvMedia.genres.contains("Crime"))
  }

  @Test
  fun categoryGrouping_structuresOutputIntoCleanCategoryGroups() {
    val genreMap = mapOf(28 to "Action", 878 to "Sci-Fi", 35 to "Comedy")
    val items = listOf(
      com.example.data.service.Movie(id = 1, title = "Action Movie 1", genre_ids = listOf(28)).toMediaItem(category = com.example.data.model.MediaCategory.MOVIES, genreMap = genreMap),
      com.example.data.service.Movie(id = 2, title = "Action Movie 2", genre_ids = listOf(28, 878)).toMediaItem(category = com.example.data.model.MediaCategory.MOVIES, genreMap = genreMap),
      com.example.data.service.Movie(id = 3, name = "Sci-Fi TV", genre_ids = listOf(878)).toMediaItem(category = com.example.data.model.MediaCategory.TV_SHOWS, genreMap = genreMap),
      com.example.data.service.Movie(id = 4, name = "Comedy TV", genre_ids = listOf(35)).toMediaItem(category = com.example.data.model.MediaCategory.TV_SHOWS, genreMap = genreMap)
    )

    val categories = mutableListOf<com.example.data.service.CategoryGroup>()
    genreMap.values.forEach { genreName ->
      val matched = items.filter { it.genres.contains(genreName) }
      if (matched.isNotEmpty()) {
        categories.add(
          com.example.data.service.CategoryGroup(
            id = genreName.lowercase(),
            name = genreName,
            items = matched
          )
        )
      }
    }

    assertEquals(3, categories.size)
    val actionGroup = categories.find { it.id == "action" }
    assertNotNull(actionGroup)
    assertEquals(2, actionGroup!!.items.size)

    val sciFiGroup = categories.find { it.id == "sci-fi" }
    assertNotNull(sciFiGroup)
    assertEquals(2, sciFiGroup!!.items.size)

    val comedyGroup = categories.find { it.id == "comedy" }
    assertNotNull(comedyGroup)
    assertEquals(1, comedyGroup!!.items.size)
  }

  @Test
  fun bulkCatalogState_countersAndStructure() {
    val state = com.example.data.service.BulkCatalogState(
      isSyncing = false,
      progress = 1.0f,
      statusMessage = "Loaded 1,000 Movies & 1,000 TV Series",
      totalMoviesFetched = 1000,
      totalTvFetched = 1000,
      categories = listOf(
        com.example.data.service.CategoryGroup("action", "Action", items = emptyList())
      )
    )

    assertEquals(1000, state.totalMoviesFetched)
    assertEquals(1000, state.totalTvFetched)
    assertEquals(1, state.categories.size)
    assertEquals("Action", state.categories[0].name)
    assertEquals(1.0f, state.progress, 0.001f)
  }
}

