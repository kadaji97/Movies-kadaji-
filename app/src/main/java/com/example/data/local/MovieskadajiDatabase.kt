package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.data.model.WatchProgress

@Database(
    entities = [
        WatchHistoryEntity::class,
        DownloadItemEntity::class,
        AppSettingEntity::class,
        WatchlistEntity::class,
        PublishedMovieEntity::class,
        SyncLogEntity::class,
        WatchProgress::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MovieskadajiDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun publishedMovieDao(): PublishedMovieDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun watchProgressDao(): WatchProgressDao


    companion object {
        @Volatile
        private var INSTANCE: MovieskadajiDatabase? = null

        fun getDatabase(context: Context): MovieskadajiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieskadajiDatabase::class.java,
                    "movieskadaji_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
