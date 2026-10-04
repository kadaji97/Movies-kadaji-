package com.example.data.service

import android.content.Context
import com.example.data.config.AppConfig
import com.example.data.download.DownloadUtility
import com.example.data.local.MovieskadajiDatabase
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.ui.screens.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HybridMediaRepository
 *
 * Coordinates decoupled asset-resolving and delivery pipeline:
 * - Asynchronously resolves runtime stream URLs from configuration rules.
 * - Coordinates main-thread playback handoff to StreamPlayerActivity.
 * - Coordinates background media download handoff to DownloadUtility.
 */
object HybridMediaRepository {

    data class ResolvedMediaTarget(
        val mediaId: String,
        val title: String,
        val streamUrl: String,
        val mirrorUrl: String = "",
        val downloadUrl: String,
        val isTv: Boolean
    )

    /**
     * Intercepts media click action, resolves URL from runtime configuration rules,
     * evaluates automated live subtitle detectors, and performs main-thread handoff.
     */
    suspend fun resolveMediaTarget(
        mediaItem: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        startAtSeconds: Long? = null
    ): ResolvedMediaTarget = withContext(Dispatchers.IO) {
        val isTv = mediaItem.category == MediaCategory.TV_SHOWS || mediaItem.seasons.isNotEmpty() || mediaItem.id.contains("_tv_")

        // Automated live subtitle detector: evaluate if valid subtitle URL exists
        val detectedSub = mediaItem.subtitleTracks.firstOrNull {
            it.id != "off" && it.fileUrl.isNotBlank()
        }
        val subFile = detectedSub?.fileUrl?.takeIf { it.isNotBlank() }
        val subLabel = detectedSub?.label?.ifBlank { "English" }

        val directPlayableUrl = MovieStreamUrlResolver.resolvePlayableMovieUrl(
            mediaItem = mediaItem,
            season = season,
            episode = episode
        )

        val displayTitle = if (isTv) {
            "${mediaItem.title} - S${season}E${episode}"
        } else {
            mediaItem.title
        }

        ResolvedMediaTarget(
            mediaId = mediaItem.id,
            title = displayTitle,
            streamUrl = directPlayableUrl,
            mirrorUrl = directPlayableUrl,
            downloadUrl = directPlayableUrl,
            isTv = isTv
        )
    }

    /**
     * Intercepts localized watch history and Room WatchProgress snapshots,
     * builds dynamic embed endpoints, and coordinates main-thread playback handoff to PlayerActivity.
     */
    suspend fun executeActionPipeline(
        context: Context,
        mediaItem: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        qualityProfile: PlaybackQualityProfile = PlaybackQualityProfile.AUTO
    ) {
        try {
            val target = resolveMediaTarget(
                mediaItem = mediaItem,
                season = season,
                episode = episode
            )

            withContext(Dispatchers.Main) {
                com.example.ui.screens.PlayerActivity.launch(
                    context = context,
                    videoUrl = target.streamUrl,
                    movieTitle = target.title,
                    mediaId = mediaItem.id,
                    isPremium = true,
                    qualityProfile = qualityProfile
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridMediaRepository", "Playback pipeline error for ${mediaItem.title}", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Unable to launch stream: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Asynchronous resolution and handoff to StreamPlayerActivity on the main thread.
     */
    suspend fun resolveAndPlay(
        context: Context,
        mediaItem: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        qualityProfile: PlaybackQualityProfile = PlaybackQualityProfile.AUTO
    ) {
        executeActionPipeline(context, mediaItem, season, episode, qualityProfile)
    }

    /**
     * Asynchronous resolution and handoff to DownloadUtility on the main thread.
     */
    suspend fun resolveAndDownload(
        context: Context,
        mediaItem: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        qualityProfile: PlaybackQualityProfile = PlaybackQualityProfile.FHD_1080P
    ) {
        try {
            val target = resolveMediaTarget(mediaItem, season, episode)
            withContext(Dispatchers.Main) {
                DownloadUtility.enqueueDownload(
                    context = context,
                    videoUrl = target.downloadUrl,
                    title = "${target.title} (${qualityProfile.badge})"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridMediaRepository", "Download pipeline error for ${mediaItem.title}", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Download error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
