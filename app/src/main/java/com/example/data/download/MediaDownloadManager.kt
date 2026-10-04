package com.example.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast

/**
 * MediaDownloadManager
 *
 * Utilizes the native Android DownloadManager system service.
 * - Downloads proceed completely in the background (even if the app process is closed).
 * - Shows persistent progress bar notification in the system drawer.
 * - Allows data transfer across both Wi-Fi and mobile networks.
 * - Writes file directly as an .mp4 container inside the device's public Environment.DIRECTORY_MOVIES directory.
 */
object MediaDownloadManager {

    private const val TAG = "MediaDownloadManager"

    fun enqueueMediaDownload(
        context: Context,
        directVideoUrl: String,
        title: String,
        subDirectory: String = "Movieskadaji"
    ): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: run {
                    Log.e(TAG, "DownloadManager service unavailable")
                    Toast.makeText(context, "System DownloadManager is unavailable", Toast.LENGTH_SHORT).show()
                    return -1L
                }

            val sanitizedTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val fileName = if (sanitizedTitle.endsWith(".mp4", ignoreCase = true)) sanitizedTitle else "$sanitizedTitle.mp4"
            val realMovieUrl = com.example.data.service.MovieStreamUrlResolver.resolveUrlFromInput(directVideoUrl, title)
            val targetUri = Uri.parse(realMovieUrl)

            val request = DownloadManager.Request(targetUri).apply {
                setTitle(title)
                setDescription("Downloading high-definition video...")
                setMimeType("video/mp4")

                // Allow transfer over Wi-Fi and mobile networks
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                setAllowedOverRoaming(true)
                setAllowedOverMetered(true)

                // Enable persistent progress bar notification in system drawer
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Write file to Environment.DIRECTORY_MOVIES
                try {
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_MOVIES,
                        "$subDirectory/$fileName"
                    )
                } catch (e: Exception) {
                    // Fallback to DIRECTORY_DOWNLOADS if DIRECTORY_MOVIES is restricted
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        fileName
                    )
                }
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "📥 Download started: $title", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Enqueued download ID: $downloadId for '$title' to DIRECTORY_MOVIES")
            return downloadId

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download for $title: ${e.message}", e)
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    /**
     * Checks current progress and status of an active download.
     */
    fun queryDownloadStatus(context: Context, downloadId: Long): DownloadStatus {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return DownloadStatus.Failed

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytesIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                val status = it.getInt(statusIndex)
                val downloadedBytes = it.getLong(bytesDownloadedIndex)
                val totalBytes = it.getLong(totalBytesIndex)

                val progressPercent = if (totalBytes > 0) {
                    ((downloadedBytes * 100) / totalBytes).toInt()
                } else 0

                return when (status) {
                    DownloadManager.STATUS_RUNNING -> DownloadStatus.Running(progressPercent)
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed
                    DownloadManager.STATUS_PENDING -> DownloadStatus.Pending
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.Paused
                    else -> DownloadStatus.Failed
                }
            }
        }
        return DownloadStatus.Failed
    }

    /**
     * Cancels and deletes an active download by ID.
     */
    fun cancelDownload(context: Context, downloadId: Long): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return false
        return downloadManager.remove(downloadId) > 0
    }

    /**
     * Helper BroadcastReceiver to monitor when downloads complete.
     */
    fun registerDownloadCompletionReceiver(
        context: Context,
        onDownloadComplete: (downloadId: Long) -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != -1L) {
                        onDownloadComplete(id)
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        return receiver
    }

    sealed class DownloadStatus {
        data object Pending : DownloadStatus()
        data class Running(val progressPercentage: Int) : DownloadStatus()
        data object Paused : DownloadStatus()
        data object Completed : DownloadStatus()
        data object Failed : DownloadStatus()
    }
}
