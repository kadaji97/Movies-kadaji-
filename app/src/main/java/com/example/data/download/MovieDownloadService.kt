package com.example.data.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.R

/**
 * Background Media3 DownloadService to allow users to save movies for offline viewing.
 * Runs as a foreground service with continuous notification updates and system lifecycle resilience.
 */
@OptIn(UnstableApi::class)
class MovieDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download_notification_channel_name,
    R.string.download_notification_channel_description
) {

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "movie_downloads_channel"
        private const val JOB_ID = 2001
    }

    override fun getDownloadManager(): DownloadManager {
        return MovieDownloadManager.getDownloadManager(this)
    }

    override fun getScheduler(): Scheduler? {
        return if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val helper = MovieDownloadManager.getNotificationHelper(this)
        return helper.buildProgressNotification(
            this,
            R.mipmap.ic_launcher,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }
}
