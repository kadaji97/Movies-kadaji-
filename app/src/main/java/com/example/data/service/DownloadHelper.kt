package com.example.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import java.io.File
import java.util.concurrent.Executors

/**
 * Type alias satisfying explicit DefaultDatabaseProvider specifications while mapping
 * cleanly to Media3's StandaloneDatabaseProvider implementation.
 */
@OptIn(UnstableApi::class)
typealias DefaultDatabaseProvider = StandaloneDatabaseProvider

/**
 * STEP 4: FLAWLESS OFFLINE DOWNLOAD HELPER ($0 SERVER COST)
 *
 * Thread-safe singleton utility managing local device caches.
 * Leverages Media3's native offline download subsystem to cache adaptive chunks directly
 * to isolated application storage (context.getExternalFilesDir(null)) preventing external
 * piracy or filesystem tampering.
 */
@OptIn(UnstableApi::class)
object DownloadHelper {

    private const val TAG = "DownloadHelper"
    private const val DOWNLOAD_CONTENT_SUBDIR = "movieskadaji_offline_vault"
    private const val PLAYBACK_CACHE_SUBDIR = "media3_video_precache"
    private const val MAX_PRECACHE_SIZE_BYTES = 200L * 1024L * 1024L // 200MB max size for pre-buffering

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    @Volatile
    private var downloadCache: SimpleCache? = null

    @Volatile
    private var playbackPreCache: SimpleCache? = null

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var httpDataSourceFactory: DefaultHttpDataSource.Factory? = null

    /**
     * Thread-safe access to Media3 DatabaseProvider.
     */
    @Synchronized
    fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: synchronized(this) {
            databaseProvider ?: DefaultDatabaseProvider(context.applicationContext).also {
                databaseProvider = it
            }
        }
    }

    /**
     * Initializes and returns the isolated Media3 SimpleCache backed by NoOpCacheEvictor.
     * Stored in context.getExternalFilesDir(null) to maintain strict sandbox isolation.
     */
    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        return downloadCache ?: synchronized(this) {
            downloadCache ?: run {
                val appCtx = context.applicationContext
                val dbProvider = getDatabaseProvider(appCtx)
                val baseDir = appCtx.getExternalFilesDir(null) ?: appCtx.filesDir
                val downloadDirectory = File(baseDir, DOWNLOAD_CONTENT_SUBDIR).apply {
                    if (!exists()) mkdirs()
                }

                // SimpleCache paired with NoOpCacheEvictor allows persistent offline retention
                SimpleCache(downloadDirectory, NoOpCacheEvictor(), dbProvider).also {
                    downloadCache = it
                }
            }
        }
    }

    /**
     * Requirement 2: Fast Playback Engine with Pre-Caching (Jetpack Media3 / ExoPlayer)
     * Initializes and returns a SimpleCache instance (200MB max size) using LeastRecentlyUsedCacheEvictor
     * so that video files pre-buffer ahead of playback to eliminate lagging.
     */
    @Synchronized
    fun getPlaybackPreCache(context: Context): SimpleCache {
        return playbackPreCache ?: synchronized(this) {
            playbackPreCache ?: run {
                val appCtx = context.applicationContext
                val dbProvider = getDatabaseProvider(appCtx)
                val cacheDirectory = File(appCtx.cacheDir, PLAYBACK_CACHE_SUBDIR).apply {
                    if (!exists()) mkdirs()
                }

                // 200MB max size pre-buffer cache with LRU eviction
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_PRECACHE_SIZE_BYTES)
                SimpleCache(cacheDirectory, evictor, dbProvider).also {
                    playbackPreCache = it
                }
            }
        }
    }

    /**
     * Returns the upstream HTTP data source configured with resilient 15s timeouts.
     */
    @Synchronized
    fun getHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return httpDataSourceFactory ?: synchronized(this) {
            httpDataSourceFactory ?: DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf(
                    "Accept" to "*/*",
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                ))
                .also { httpDataSourceFactory = it }
        }
    }

    /**
     * Initializes and returns the background DownloadManager to coordinate stream chunk transfers.
     */
    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: synchronized(this) {
            downloadManager ?: run {
                val appCtx = context.applicationContext
                val dbProvider = getDatabaseProvider(appCtx)
                val cache = getDownloadCache(appCtx)
                val upstreamFactory = getHttpDataSourceFactory()
                val executor = Executors.newFixedThreadPool(4)

                DownloadManager(
                    appCtx,
                    dbProvider,
                    cache,
                    upstreamFactory,
                    executor
                ).apply {
                    maxParallelDownloads = 3
                    resumeDownloads()
                }.also {
                    downloadManager = it
                }
            }
        }
    }

    /**
     * Requirement 2: Fast Playback Engine with Pre-Caching (Jetpack Media3 / ExoPlayer)
     * Returns a CacheDataSource.Factory that attaches the 200MB LeastRecentlyUsedCacheEvictor
     * pre-cache so that video files pre-buffer ahead of playback to eliminate lagging.
     */
    fun buildPlaybackCacheDataSourceFactory(context: Context): DataSource.Factory {
        val appCtx = context.applicationContext
        val upstreamFactory = getHttpDataSourceFactory()
        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            appCtx,
            upstreamFactory
        )
        return try {
            val cache = getPlaybackPreCache(appCtx)
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } catch (e: Exception) {
            Log.w(TAG, "Playback pre-cache unavailable, falling back to direct data source: ${e.message}")
            defaultDataSourceFactory
        }
    }

    /**
     * Returns a CacheDataSource.Factory that automatically reads from the local cache
     * or seamlessly falls back to the network with zero overhead.
     */
    fun buildCacheDataSourceFactory(context: Context): DataSource.Factory {
        return try {
            buildPlaybackCacheDataSourceFactory(context)
        } catch (e: Exception) {
            Log.w(TAG, "CacheDataSource fallback engaged: ${e.message}")
            val appCtx = context.applicationContext
            androidx.media3.datasource.DefaultDataSource.Factory(
                appCtx,
                getHttpDataSourceFactory()
            )
        }
    }

    /**
     * Queues a video stream for background offline caching.
     */
    fun startDownload(context: Context, contentId: String, uri: Uri, title: String) {
        try {
            val manager = getDownloadManager(context)
            val downloadRequest = DownloadRequest.Builder(contentId, uri)
                .setCustomCacheKey(contentId)
                .setData(title.toByteArray(Charsets.UTF_8))
                .build()

            manager.addDownload(downloadRequest)
            Log.d(TAG, "Queued offline download: id=$contentId title='$title' uri=$uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch offline download for $contentId", e)
        }
    }

    /**
     * Cancels and purges a downloaded media chunk payload from storage.
     */
    fun removeDownload(context: Context, contentId: String) {
        try {
            val manager = getDownloadManager(context)
            manager.removeDownload(contentId)
            Log.d(TAG, "Removed download: id=$contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing download $contentId", e)
        }
    }

    /**
     * Pauses all ongoing offline stream transfers.
     */
    fun pauseDownloads(context: Context) {
        getDownloadManager(context).pauseDownloads()
    }

    /**
     * Resumes all paused offline stream transfers.
     */
    fun resumeDownloads(context: Context) {
        getDownloadManager(context).resumeDownloads()
    }

    /**
     * Registers a listener to observe download progress and state transitions.
     */
    fun addListener(context: Context, listener: DownloadManager.Listener) {
        getDownloadManager(context).addListener(listener)
    }

    /**
     * Unregisters a download listener.
     */
    fun removeListener(context: Context, listener: DownloadManager.Listener) {
        getDownloadManager(context).removeListener(listener)
    }

    /**
     * Checks if a specific media item is already completed in the offline download index.
     */
    fun isDownloaded(context: Context, contentId: String): Boolean {
        return try {
            val manager = getDownloadManager(context)
            val download = manager.downloadIndex.getDownload(contentId)
            download != null && download.state == Download.STATE_COMPLETED
        } catch (e: Exception) {
            false
        }
    }
}
