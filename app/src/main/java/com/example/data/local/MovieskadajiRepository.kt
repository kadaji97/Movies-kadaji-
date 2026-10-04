package com.example.data.local

import com.example.data.catalog.MovieCatalog
import com.example.data.model.MediaItem
import com.example.data.model.WatchProgress
import com.example.data.service.StreamingIndexService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class ContentSyncResult(
    val isSuccess: Boolean,
    val newTitlesAdded: Int,
    val titles: List<String>,
    val message: String
)

class MovieskadajiRepository(private val database: MovieskadajiDatabase) {

    private val watchHistoryDao = database.watchHistoryDao()
    private val downloadDao = database.downloadDao()
    private val appSettingDao = database.appSettingDao()
    private val watchlistDao = database.watchlistDao()
    private val publishedMovieDao = database.publishedMovieDao()
    private val syncLogDao = database.syncLogDao()
    private val watchProgressDao = database.watchProgressDao()

    val watchHistory: Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllWatchHistory()
    val allDownloads: Flow<List<DownloadItemEntity>> = downloadDao.getAllDownloads()
    val completedDownloads: Flow<List<DownloadItemEntity>> = downloadDao.getCompletedDownloads()
    val activeDownloads: Flow<List<DownloadItemEntity>> = downloadDao.getActiveDownloads()
    val watchlist: Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlist()
    val publishedMovies: Flow<List<PublishedMovieEntity>> = publishedMovieDao.getAllPublishedMovies()
    val syncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentLogs()
    val allWatchProgress: Flow<List<WatchProgress>> = watchProgressDao.getAllProgress()

    suspend fun saveWatchProgress(progress: WatchProgress) = watchProgressDao.saveProgress(progress)

    suspend fun getWatchProgress(contentId: String, seasonNumber: Int = 1, episodeNumber: Int = 1): WatchProgress? =
        watchProgressDao.getProgress(contentId, seasonNumber, episodeNumber)

    suspend fun getLatestWatchProgress(contentId: String): WatchProgress? =
        watchProgressDao.getLatestProgressForContent(contentId)

    fun getWatchProgressFlow(contentId: String): Flow<List<WatchProgress>> =
        watchProgressDao.getProgressFlowForContent(contentId)

    suspend fun deleteWatchProgress(contentId: String) =
        watchProgressDao.deleteProgressByContentId(contentId)


    fun isItemInWatchlistFlow(mediaId: String): Flow<Boolean> = watchlistDao.isItemInWatchlistFlow(mediaId)

    suspend fun isItemInWatchlist(mediaId: String): Boolean = watchlistDao.isItemInWatchlist(mediaId)

    suspend fun addToWatchlist(mediaItem: MediaItem) {
        val entity = WatchlistEntity(
            mediaId = mediaItem.id,
            title = mediaItem.title,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            category = mediaItem.category.label,
            qualityBadge = mediaItem.qualityBadge,
            imdbRating = mediaItem.imdbRating,
            releaseYear = mediaItem.releaseYear,
            duration = mediaItem.duration,
            genres = mediaItem.genres.joinToString(", "),
            plotSynopsis = mediaItem.plotSynopsis,
            addedTimestamp = System.currentTimeMillis()
        )
        watchlistDao.insertWatchlist(entity)
    }

    suspend fun removeFromWatchlist(mediaId: String) {
        watchlistDao.deleteWatchlist(mediaId)
    }

    suspend fun toggleWatchlist(mediaItem: MediaItem): Boolean {
        val exists = watchlistDao.isItemInWatchlist(mediaItem.id)
        return if (exists) {
            watchlistDao.deleteWatchlist(mediaItem.id)
            false
        } else {
            addToWatchlist(mediaItem)
            true
        }
    }

    suspend fun clearWatchlist() {
        watchlistDao.clearAllWatchlist()
    }

    suspend fun recordWatchProgress(
        mediaItem: MediaItem,
        progressSeconds: Long,
        totalDurationSeconds: Long,
        quality: String = "1080p HD"
    ) {
        val entity = WatchHistoryEntity(
            mediaId = mediaItem.id,
            title = mediaItem.title,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            progressSeconds = progressSeconds,
            totalDurationSeconds = totalDurationSeconds,
            lastWatchedTimestamp = System.currentTimeMillis(),
            quality = quality,
            category = mediaItem.category.label
        )
        watchHistoryDao.insertWatchHistory(entity)
    }

    suspend fun removeHistory(mediaId: String) {
        watchHistoryDao.deleteWatchHistory(mediaId)
    }

    suspend fun clearHistory() {
        watchHistoryDao.clearAllHistory()
    }

    suspend fun startDownload(
        mediaItem: MediaItem,
        quality: String = "1080p FHD",
        targetPath: String? = null,
        streamUrl: String = ""
    ): DownloadItemEntity {
        val existing = downloadDao.getDownloadByMediaId(mediaItem.id)
        if (existing != null) {
            return existing
        }
        val sizeMb = when (quality) {
            "4K HDR" -> 3200.0
            "1080p FHD" -> 1850.0
            "720p HD" -> 850.0
            else -> 420.0
        }
        val item = DownloadItemEntity(
            id = UUID.randomUUID().toString(),
            mediaId = mediaItem.id,
            title = mediaItem.title,
            posterUrl = mediaItem.posterUrl,
            quality = quality,
            fileSizeMb = sizeMb,
            downloadedMb = 0.0,
            progressPercent = 0,
            isCompleted = false,
            downloadSpeed = "Connecting...",
            timestamp = System.currentTimeMillis(),
            localFilePath = targetPath,
            streamUrl = streamUrl
        )
        downloadDao.insertOrUpdateDownload(item)
        return item
    }

    suspend fun updateDownloadProgress(
        id: String,
        percent: Int,
        downloadedMb: Double,
        completed: Boolean,
        speed: String
    ) {
        downloadDao.updateProgress(id, percent, downloadedMb, completed, speed)
    }

    suspend fun updateDownloadProgressWithFile(
        id: String,
        percent: Int,
        downloadedMb: Double,
        completed: Boolean,
        speed: String,
        filePath: String?
    ) {
        downloadDao.updateProgressWithFilePath(id, percent, downloadedMb, completed, speed, filePath)
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownload(id)
    }

    suspend fun clearAllDownloads() {
        downloadDao.clearAllDownloads()
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return appSettingDao.getSetting(key) ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSettingEntity(key, value))
    }

    suspend fun seedInitialDataIfEmpty() {
        try {
            // Clean up any legacy demo entries if they existed from older installs
            watchHistoryDao.deleteWatchHistory("banner_1")
            watchHistoryDao.deleteWatchHistory("banner_2")
            watchHistoryDao.deleteWatchHistory("banner_3")
            watchHistoryDao.deleteWatchHistory("demo_1")
            watchHistoryDao.deleteWatchHistory("demo_2")
            downloadDao.deleteDownload("seeded_dl_dune")
            val watchlistSeed = MovieCatalog.featuredBanners.getOrNull(2) ?: MovieCatalog.featuredBanners.firstOrNull()
            if (watchlistSeed != null && !watchlistDao.isItemInWatchlist(watchlistSeed.id)) {
                addToWatchlist(watchlistSeed)
            }
        } catch (_: Exception) {}
    }

    suspend fun publishMovie(mediaItem: MediaItem, source: String = "ADMIN_MANUAL") {
        val entity = StreamingIndexService.toEntity(mediaItem, source)
        publishedMovieDao.insertPublishedMovie(entity)
        syncLogDao.insertSyncLog(
            SyncLogEntity(
                timestamp = System.currentTimeMillis(),
                status = "PUBLISHED",
                moviesFoundCount = 1,
                summary = "Published '${mediaItem.title}' (${mediaItem.id}) via $source with 3 CDN stream links."
            )
        )
        refreshCatalogDynamicItems()
    }

    suspend fun deletePublishedMovie(mediaId: String) {
        publishedMovieDao.deletePublishedMovie(mediaId)
        syncLogDao.insertSyncLog(
            SyncLogEntity(
                timestamp = System.currentTimeMillis(),
                status = "DELETED",
                moviesFoundCount = 1,
                summary = "Unpublished title with ID $mediaId."
            )
        )
        refreshCatalogDynamicItems()
    }

    suspend fun clearAllPublishedMovies() {
        publishedMovieDao.clearAllPublishedMovies()
        refreshCatalogDynamicItems()
    }

    suspend fun fetchAndPublishByImdbId(imdbId: String): Result<MediaItem> {
        val resolveResult = StreamingIndexService.resolveImdbId(imdbId)
        if (resolveResult.isFailure) {
            return resolveResult
        }
        val mediaItem = resolveResult.getOrThrow()
        val existing = publishedMovieDao.getPublishedMovieById(mediaItem.id)
        if (existing != null) {
            return Result.failure(IllegalStateException("Title '${mediaItem.title}' [${mediaItem.id}] is already published in database."))
        }

        publishMovie(mediaItem, source = "ADMIN_MANUAL_1CLICK")
        return Result.success(mediaItem)
    }

    suspend fun runAutomatedContentSync(): ContentSyncResult {
        return try {
            val feedItems = StreamingIndexService.fetchHourlyNewReleases()
            val newlyAdded = mutableListOf<String>()

            for (item in feedItems) {
                val existing = publishedMovieDao.getPublishedMovieById(item.id)
                if (existing == null) {
                    val entity = StreamingIndexService.toEntity(item, source = "HOURLY_CRON_SYNC")
                    publishedMovieDao.insertPublishedMovie(entity)
                    newlyAdded.add(item.title)
                }
            }

            refreshCatalogDynamicItems()

            val summaryMsg = if (newlyAdded.isNotEmpty()) {
                "Automated hourly check discovered ${newlyAdded.size} new releases: ${newlyAdded.joinToString(", ")} with 3 CDN streams each."
            } else {
                "Automated hourly check completed. All indexed releases are up to date."
            }

            syncLogDao.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    status = if (newlyAdded.isNotEmpty()) "NEW_RELEASES_ADDED" else "UP_TO_DATE",
                    moviesFoundCount = newlyAdded.size,
                    summary = summaryMsg
                )
            )

            ContentSyncResult(
                isSuccess = true,
                newTitlesAdded = newlyAdded.size,
                titles = newlyAdded,
                message = summaryMsg
            )
        } catch (e: Exception) {
            syncLogDao.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    status = "ERROR",
                    moviesFoundCount = 0,
                    summary = "Hourly sync error: ${e.message ?: "Unknown error"}"
                )
            )
            ContentSyncResult(
                isSuccess = false,
                newTitlesAdded = 0,
                titles = emptyList(),
                message = "Content sync failed: ${e.message}"
            )
        }
    }

    private suspend fun refreshCatalogDynamicItems() {
        // We will collect or update MovieCatalog.dynamicItems from ViewModel
    }
}

