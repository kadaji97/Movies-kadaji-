package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val progressSeconds: Long,
    val totalDurationSeconds: Long,
    val lastWatchedTimestamp: Long,
    val quality: String,
    val category: String
)

@Entity(tableName = "downloads")
data class DownloadItemEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val quality: String,
    val fileSizeMb: Double,
    val downloadedMb: Double,
    val progressPercent: Int,
    val isCompleted: Boolean,
    val downloadSpeed: String,
    val timestamp: Long,
    val localFilePath: String? = null,
    val streamUrl: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val category: String,
    val qualityBadge: String,
    val imdbRating: Float,
    val releaseYear: Int,
    val duration: String,
    val genres: String,
    val plotSynopsis: String = "",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "published_movies")
data class PublishedMovieEntity(
    @PrimaryKey val mediaId: String,
    val imdbId: String,
    val title: String,
    val category: String,
    val genresJson: String,
    val releaseYear: Int,
    val duration: String,
    val imdbRating: Float,
    val qualityBadge: String,
    val plotSynopsis: String,
    val castJson: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamServersJson: String,
    val publishedTimestamp: Long = System.currentTimeMillis(),
    val source: String = "ADMIN_MANUAL" // "ADMIN_MANUAL" or "HOURLY_CRON_SYNC"
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val status: String,
    val moviesFoundCount: Int,
    val summary: String
)

