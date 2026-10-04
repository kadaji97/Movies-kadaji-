package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getWatchHistoryById(mediaId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteWatchHistory(mediaId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE isCompleted = 1 ORDER BY timestamp DESC")
    fun getCompletedDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE isCompleted = 0 ORDER BY timestamp DESC")
    fun getActiveDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getDownloadByMediaId(mediaId: String): DownloadItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDownload(item: DownloadItemEntity)

    @Query("UPDATE downloads SET progressPercent = :percent, downloadedMb = :downloadedMb, isCompleted = :completed, downloadSpeed = :speed, localFilePath = :filePath WHERE id = :id")
    suspend fun updateProgressWithFilePath(id: String, percent: Int, downloadedMb: Double, completed: Boolean, speed: String, filePath: String?)

    @Query("UPDATE downloads SET progressPercent = :percent, downloadedMb = :downloadedMb, isCompleted = :completed, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, percent: Int, downloadedMb: Double, completed: Boolean, speed: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAllSettings()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedTimestamp DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE mediaId = :mediaId)")
    fun isItemInWatchlistFlow(mediaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE mediaId = :mediaId)")
    suspend fun isItemInWatchlist(mediaId: String): Boolean

    @Query("SELECT * FROM watchlist WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getWatchlistItemById(mediaId: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE mediaId = :mediaId")
    suspend fun deleteWatchlist(mediaId: String)

    @Query("DELETE FROM watchlist")
    suspend fun clearAllWatchlist()
}

@Dao
interface PublishedMovieDao {
    @Query("SELECT * FROM published_movies ORDER BY publishedTimestamp DESC")
    fun getAllPublishedMovies(): Flow<List<PublishedMovieEntity>>

    @Query("SELECT * FROM published_movies WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPublishedMovieById(mediaId: String): PublishedMovieEntity?

    @Query("SELECT * FROM published_movies WHERE imdbId = :imdbId LIMIT 1")
    suspend fun getPublishedMovieByImdbId(imdbId: String): PublishedMovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublishedMovie(movie: PublishedMovieEntity)

    @Query("DELETE FROM published_movies WHERE mediaId = :mediaId")
    suspend fun deletePublishedMovie(mediaId: String)

    @Query("DELETE FROM published_movies")
    suspend fun clearAllPublishedMovies()

    @Query("SELECT COUNT(*) FROM published_movies")
    fun getPublishedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM published_movies")
    suspend fun getPublishedCount(): Int
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 20): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllLogs()
}


