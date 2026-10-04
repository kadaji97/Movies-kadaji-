package com.example.data.model

import androidx.room.Entity

/**
 * WatchProgress
 *
 * Local resilient storage for tracking playback progress across movies and tv episodes.
 */
@Entity(
    tableName = "watch_progress",
    primaryKeys = ["contentId", "seasonNumber", "episodeNumber"]
)
data class WatchProgress(
    val contentId: String,
    val mediaType: String, // "movie" or "tv"
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val savedPositionSeconds: Long,
    val totalDurationSeconds: Long,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
