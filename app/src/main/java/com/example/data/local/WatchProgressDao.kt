package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WatchProgress
import kotlinx.coroutines.flow.Flow

/**
 * WatchProgressDao
 *
 * Data Access Object providing robust CRUD operations and reactive queries for playback progress.
 */
@Dao
interface WatchProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getProgress(contentId: String, seasonNumber: Int = 1, episodeNumber: Int = 1): WatchProgress?

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId ORDER BY lastUpdatedTimestamp DESC LIMIT 1")
    suspend fun getLatestProgressForContent(contentId: String): WatchProgress?

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getProgressFlowForContent(contentId: String): Flow<List<WatchProgress>>

    @Query("SELECT * FROM watch_progress ORDER BY lastUpdatedTimestamp DESC")
    fun getAllProgress(): Flow<List<WatchProgress>>

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId")
    suspend fun deleteProgressByContentId(contentId: String)

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun deleteProgress(contentId: String, seasonNumber: Int, episodeNumber: Int)

    @Query("DELETE FROM watch_progress")
    suspend fun clearAllProgress()
}
