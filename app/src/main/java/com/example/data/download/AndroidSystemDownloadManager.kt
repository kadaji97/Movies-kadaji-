package com.example.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log

/**
 * Native Android System Download Manager implementation.
 * Takes the constructed .mp4 URL and runs the download as a system service with
 * progress notifications (VISIBILITY_VISIBLE_NOTIFY_COMPLETED) and graceful
 * reconnection handling across Wi-Fi and mobile data networks.
 */
object AndroidSystemDownloadManager {

    private const val TAG = "SystemDownloadMgr"

    /**
     * Enqueues a video stream download into the Android system DownloadManager service.
     *
     * @param context Application context
     * @param mp4Url Direct CDN .mp4 URL (e.g. from Internet Archive CDN)
     * @param movieTitle The title of the movie
     * @param description Notification subtitle
     * @return System download ID (Long) or -1L on failure
     */
    fun enqueueMovieDownload(
        context: Context,
        mp4Url: String,
        movieTitle: String,
        description: String? = null
    ): Long {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: run {
                    Log.e(TAG, "DownloadManager system service unavailable")
                    return -1L
                }

            val uri = Uri.parse(mp4Url)
            val cleanTitle = movieTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "$cleanTitle.mp4"

            val request = DownloadManager.Request(uri).apply {
                setTitle(movieTitle)
                setDescription(description ?: "Public domain stream: $fileName")
                // Requirement 3: Include notifications showing progress and completion
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // Requirement 3: Handle network reconnections gracefully
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                // Save to public Downloads directory
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Enqueued native DownloadManager task ID: $downloadId for '$movieTitle' from $mp4Url")
            downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue DownloadManager request for '$movieTitle'", e)
            -1L
        }
    }
}
