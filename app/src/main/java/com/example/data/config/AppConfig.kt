package com.example.data.config

import android.content.Context
import org.json.JSONObject

/**
 * AppConfig
 *
 * Centralized dynamic resource router configuration.
 * Reads runtime routing rules from assets/app_config.json with resilient defaults.
 */
object AppConfig {
    const val MOVIE_PLAYER_BASE_URL = "https://vidlink.pro"
    const val TV_PLAYER_BASE_URL = "https://vidlink.pro"
    const val MOVIE_MIRROR_BASE_URL = "https://vidsrc.xyz"
    const val TV_MIRROR_BASE_URL = "https://vidsrc.xyz"

    const val CUSTOM_PRIMARY_COLOR = "00E5FF"
    const val CUSTOM_ICON_STYLE = "vid"
    const val AUTOPLAY_ENABLED = true
    const val NEXT_EPISODE_BUTTON_ENABLED = true

    var moviePlayerBaseUrl: String = MOVIE_PLAYER_BASE_URL
        private set

    var tvPlayerBaseUrl: String = TV_PLAYER_BASE_URL
        private set

    var movieMirrorBaseUrl: String = MOVIE_MIRROR_BASE_URL
        private set

    var tvMirrorBaseUrl: String = TV_MIRROR_BASE_URL
        private set

    var customPrimaryColor: String = CUSTOM_PRIMARY_COLOR
        private set

    var customIconStyle: String = CUSTOM_ICON_STYLE
        private set

    var autoplayEnabled: Boolean = AUTOPLAY_ENABLED
        private set

    var nextEpisodeButtonEnabled: Boolean = NEXT_EPISODE_BUTTON_ENABLED
        private set

    var defaultDownloadSubDir: String = "Movieskadaji"
        private set

    var fallbackCdnUrl: String = com.example.data.service.MovieStreamUrlResolver.REAL_FEATURE_SCI_FI
        private set

    fun initialize(context: Context) {
        try {
            val jsonString = context.assets.open("app_config.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            if (json.has("MOVIE_PLAYER_BASE_URL")) {
                moviePlayerBaseUrl = json.getString("MOVIE_PLAYER_BASE_URL")
            }
            if (json.has("TV_PLAYER_BASE_URL")) {
                tvPlayerBaseUrl = json.getString("TV_PLAYER_BASE_URL")
            }
            if (json.has("MOVIE_MIRROR_BASE_URL")) {
                movieMirrorBaseUrl = json.getString("MOVIE_MIRROR_BASE_URL")
            }
            if (json.has("TV_MIRROR_BASE_URL")) {
                tvMirrorBaseUrl = json.getString("TV_MIRROR_BASE_URL")
            }
            if (json.has("CUSTOM_PRIMARY_COLOR")) {
                customPrimaryColor = json.getString("CUSTOM_PRIMARY_COLOR")
            }
            if (json.has("CUSTOM_ICON_STYLE")) {
                customIconStyle = json.getString("CUSTOM_ICON_STYLE")
            }
            if (json.has("AUTOPLAY_ENABLED")) {
                autoplayEnabled = json.getBoolean("AUTOPLAY_ENABLED")
            }
            if (json.has("NEXT_EPISODE_BUTTON_ENABLED")) {
                nextEpisodeButtonEnabled = json.getBoolean("NEXT_EPISODE_BUTTON_ENABLED")
            }
            if (json.has("DEFAULT_DOWNLOAD_SUBDIR")) {
                defaultDownloadSubDir = json.getString("DEFAULT_DOWNLOAD_SUBDIR")
            }
            if (json.has("FALLBACK_CDN_URL")) {
                val url = json.getString("FALLBACK_CDN_URL")
                fallbackCdnUrl = if (url.contains("plyr.io") || url.contains("vidlink")) com.example.data.service.MovieStreamUrlResolver.REAL_FEATURE_SCI_FI else url
            }
        } catch (_: Exception) {
            // Keep default values
        }
    }

    fun buildMovieUrl(
        tmdbId: String,
        startAtSeconds: Long? = null,
        subFile: String? = null,
        subLabel: String? = null
    ): String {
        val cleanId = tmdbId.removePrefix("tmdb_tv_").removePrefix("tmdb_")
        val base = moviePlayerBaseUrl.trimEnd('/')
        val path = when {
            base.contains("vidlink") -> "$base/movie/$cleanId"
            base.contains("vidsrc") && !base.contains("/embed") -> "$base/embed/movie/$cleanId"
            else -> "$base/movie/$cleanId"
        }
        val queryParts = mutableListOf<String>()
        queryParts.add("primaryColor=$customPrimaryColor")
        queryParts.add("icons=$customIconStyle")
        queryParts.add("autoplay=$autoplayEnabled")
        if (startAtSeconds != null && startAtSeconds > 0) {
            queryParts.add("startAt=$startAtSeconds")
        }
        if (!subFile.isNullOrBlank()) {
            queryParts.add("sub_file=${java.net.URLEncoder.encode(subFile, "UTF-8")}")
            val label = if (!subLabel.isNullOrBlank()) subLabel else "English"
            queryParts.add("sub_label=${java.net.URLEncoder.encode(label, "UTF-8")}")
        }
        return "$path?${queryParts.joinToString("&")}"
    }

    fun buildTvUrl(
        tmdbId: String,
        season: Int = 1,
        episode: Int = 1,
        startAtSeconds: Long? = null,
        subFile: String? = null,
        subLabel: String? = null
    ): String {
        val cleanId = tmdbId.removePrefix("tmdb_tv_").removePrefix("tmdb_")
        val base = tvPlayerBaseUrl.trimEnd('/')
        val path = when {
            base.contains("vidlink") -> "$base/tv/$cleanId/$season/$episode"
            base.contains("vidsrc") && !base.contains("/embed") -> "$base/embed/tv/$cleanId/$season/$episode"
            else -> "$base/tv/$cleanId/$season/$episode"
        }
        val queryParts = mutableListOf<String>()
        queryParts.add("primaryColor=$customPrimaryColor")
        queryParts.add("icons=$customIconStyle")
        queryParts.add("autoplay=$autoplayEnabled")
        queryParts.add("nextbutton=$nextEpisodeButtonEnabled")
        if (startAtSeconds != null && startAtSeconds > 0) {
            queryParts.add("startAt=$startAtSeconds")
        }
        if (!subFile.isNullOrBlank()) {
            queryParts.add("sub_file=${java.net.URLEncoder.encode(subFile, "UTF-8")}")
            val label = if (!subLabel.isNullOrBlank()) subLabel else "English"
            queryParts.add("sub_label=${java.net.URLEncoder.encode(label, "UTF-8")}")
        }
        return "$path?${queryParts.joinToString("&")}"
    }

    fun buildMirrorMovieUrl(
        tmdbId: String,
        startAtSeconds: Long? = null,
        subFile: String? = null,
        subLabel: String? = null
    ): String {
        val cleanId = tmdbId.removePrefix("tmdb_tv_").removePrefix("tmdb_")
        val base = movieMirrorBaseUrl.trimEnd('/')
        val path = when {
            base.contains("vidsrc.xyz") -> "$base/embed/movie/$cleanId"
            base.contains("vidlink") -> "$base/movie/$cleanId"
            else -> "$base/embed/movie/$cleanId"
        }
        val queryParts = mutableListOf<String>()
        queryParts.add("primaryColor=$customPrimaryColor")
        queryParts.add("icons=$customIconStyle")
        queryParts.add("autoplay=$autoplayEnabled")
        if (startAtSeconds != null && startAtSeconds > 0) {
            queryParts.add("startAt=$startAtSeconds")
        }
        if (!subFile.isNullOrBlank()) {
            queryParts.add("sub_file=${java.net.URLEncoder.encode(subFile, "UTF-8")}")
            val label = if (!subLabel.isNullOrBlank()) subLabel else "English"
            queryParts.add("sub_label=${java.net.URLEncoder.encode(label, "UTF-8")}")
        }
        return "$path?${queryParts.joinToString("&")}"
    }

    fun buildMirrorTvUrl(
        tmdbId: String,
        season: Int = 1,
        episode: Int = 1,
        startAtSeconds: Long? = null,
        subFile: String? = null,
        subLabel: String? = null
    ): String {
        val cleanId = tmdbId.removePrefix("tmdb_tv_").removePrefix("tmdb_")
        val base = tvMirrorBaseUrl.trimEnd('/')
        val path = when {
            base.contains("vidsrc.xyz") -> "$base/embed/tv/$cleanId/$season/$episode"
            base.contains("vidlink") -> "$base/tv/$cleanId/$season/$episode"
            else -> "$base/embed/tv/$cleanId/$season/$episode"
        }
        val queryParts = mutableListOf<String>()
        queryParts.add("primaryColor=$customPrimaryColor")
        queryParts.add("icons=$customIconStyle")
        queryParts.add("autoplay=$autoplayEnabled")
        queryParts.add("nextbutton=$nextEpisodeButtonEnabled")
        if (startAtSeconds != null && startAtSeconds > 0) {
            queryParts.add("startAt=$startAtSeconds")
        }
        if (!subFile.isNullOrBlank()) {
            queryParts.add("sub_file=${java.net.URLEncoder.encode(subFile, "UTF-8")}")
            val label = if (!subLabel.isNullOrBlank()) subLabel else "English"
            queryParts.add("sub_label=${java.net.URLEncoder.encode(label, "UTF-8")}")
        }
        return "$path?${queryParts.joinToString("&")}"
    }

    fun buildMovieDownloadUrl(tmdbId: String): String {
        return com.example.data.service.MovieStreamUrlResolver.resolvePlayableMovieUrl(tmdbId, tmdbId)
    }

    fun buildTvDownloadUrl(tmdbId: String, season: Int = 1, episode: Int = 1): String {
        return com.example.data.service.MovieStreamUrlResolver.resolvePlayableMovieUrl(
            title = tmdbId,
            mediaId = tmdbId,
            category = com.example.data.model.MediaCategory.TV_SHOWS,
            season = season,
            episode = episode
        )
    }
}
