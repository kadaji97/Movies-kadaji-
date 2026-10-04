package com.example.data.service

import android.content.Context
import com.example.data.download.MediaDownloadManager
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.ui.screens.PlayerActivity

/**
 * MediaStreamAggregator
 *
 * Client-side aggregation and translation layer:
 * - Translates local database IDs (IMDb tt... or TMDb IDs) to active embed players and mirror streams.
 * - Formats primary embed, fallback mirror, and direct MP4 download links.
 * - Handles both standalone Movies and episodic TV series (Season & Episode routing).
 */
object MediaStreamAggregator {

    data class StreamEndpoints(
        val mediaId: String,
        val title: String,
        val primaryEmbedUrl: String,
        val fallbackMirrorUrl: String,
        val directDownloadUrl: String,
        val isTv: Boolean,
        val season: Int? = null,
        val episode: Int? = null
    )

    /**
     * Resolves stream and embed endpoints for a movie or TV episode.
     */
    fun resolveStreamEndpoints(
        mediaId: String,
        title: String,
        isTv: Boolean = false,
        season: Int = 1,
        episode: Int = 1
    ): StreamEndpoints {
        val cleanId = mediaId.removePrefix("tmdb_tv_").removePrefix("tmdb_")

        val primaryEmbed = if (isTv) {
            "https://vidsrc.to/embed/tv/$cleanId/$season/$episode"
        } else {
            "https://vidsrc.to/embed/movie/$cleanId"
        }

        val fallbackMirror = if (isTv) {
            "https://vidsrc.me/embed/tv?tmdb=$cleanId&season=$season&episode=$episode"
        } else {
            "https://vidsrc.me/embed/movie?imdb=$cleanId&tmdb=$cleanId"
        }

        // Direct high-speed CDN stream URL for offline saving
        val directDownload = if (isTv) {
            MovieStreamUrlResolver.resolvePlayableMovieUrl(
                title = title,
                mediaId = cleanId,
                category = MediaCategory.TV_SHOWS,
                season = season,
                episode = episode
            )
        } else {
            MovieStreamUrlResolver.resolvePlayableMovieUrl(
                title = title,
                mediaId = cleanId,
                category = MediaCategory.MOVIES
            )
        }

        return StreamEndpoints(
            mediaId = mediaId,
            title = title,
            primaryEmbedUrl = primaryEmbed,
            fallbackMirrorUrl = fallbackMirror,
            directDownloadUrl = directDownload,
            isTv = isTv,
            season = if (isTv) season else null,
            episode = if (isTv) episode else null
        )
    }

    /**
     * Launches the resilient PlayerActivity directly in landscape mode.
     */
    fun launchSecureStream(
        context: Context,
        mediaItem: MediaItem,
        season: Int = 1,
        episode: Int = 1
    ) {
        try {
            val isTv = mediaItem.category == MediaCategory.TV_SHOWS
            val realUrl = MovieStreamUrlResolver.resolvePlayableMovieUrl(mediaItem, season, episode)
            val streamTitle = if (isTv) "${mediaItem.title} - S${season}E${episode}" else mediaItem.title

            com.example.ui.screens.PlayerActivity.launch(
                context = context,
                videoUrl = realUrl,
                movieTitle = streamTitle,
                mediaId = mediaItem.id,
                isPremium = true
            )
        } catch (e: Exception) {
            android.util.Log.e("MediaStreamAggregator", "Failed to launch stream: ${e.message}", e)
            android.widget.Toast.makeText(context, "Stream error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Enqueues direct media download to Environment.DIRECTORY_MOVIES using system DownloadManager.
     */
    fun downloadToMoviesDir(
        context: Context,
        mediaItem: MediaItem,
        season: Int? = null,
        episode: Int? = null,
        customUrl: String? = null
    ): Long {
        return try {
            val isTv = mediaItem.category == MediaCategory.TV_SHOWS
            val targetUrl = if (!customUrl.isNullOrBlank() && !MovieStreamUrlResolver.isInvalidOrAdUrl(customUrl)) {
                customUrl
            } else {
                MovieStreamUrlResolver.resolvePlayableMovieUrl(mediaItem, season ?: 1, episode ?: 1)
            }
            val downloadTitle = if (isTv && season != null && episode != null) {
                "${mediaItem.title} - S${season}E${episode}"
            } else {
                mediaItem.title
            }

            MediaDownloadManager.enqueueMediaDownload(
                context = context,
                directVideoUrl = targetUrl,
                title = downloadTitle,
                subDirectory = "Movieskadaji"
            )
        } catch (e: Exception) {
            android.util.Log.e("MediaStreamAggregator", "Failed to download: ${e.message}", e)
            -1L
        }
    }
}
