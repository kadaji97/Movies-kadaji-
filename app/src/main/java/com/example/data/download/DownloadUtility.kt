package com.example.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.data.config.AppConfig

/**
 * DownloadUtility
 *
 * System-level DownloadManager queue pipeline:
 * - Accepts compiled video string asset URLs.
 * - Builds native progress and completion notifications.
 * - Schedules background downloading directly to device's public Movies directory as a standard .mp4 media container.
 */
object DownloadUtility {

    private const val TAG = "DownloadUtility"

    fun enqueueDownload(
        context: Context,
        videoUrl: String,
        title: String,
        subDir: String = AppConfig.defaultDownloadSubDir
    ): Long {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: run {
                    Log.e(TAG, "DownloadManager service unavailable on device")
                    Toast.makeText(context, "System download service unavailable", Toast.LENGTH_SHORT).show()
                    return -1L
                }

            val sanitizedTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val fileName = if (sanitizedTitle.endsWith(".mp4", ignoreCase = true)) sanitizedTitle else "$sanitizedTitle.mp4"
            
            val realMovieUrl = com.example.data.service.MovieStreamUrlResolver.resolveUrlFromInput(videoUrl, title)
            val uri = Uri.parse(realMovieUrl)

            val request = DownloadManager.Request(uri).apply {
                setTitle(title)
                setDescription("Downloading $fileName to Movies folder")
                setMimeType("video/mp4")
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                setAllowedOverRoaming(true)
                setAllowedOverMetered(true)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                try {
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_MOVIES,
                        "$subDir/$fileName"
                    )
                } catch (_: Exception) {
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        fileName
                    )
                }
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "📥 Download queued: $title", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Enqueued download with ID $downloadId for $title")
            downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing download for $title: ${e.message}", e)
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            -1L
        }
    }
}
