package com.example.data.model

enum class MediaCategory(val label: String, val icon: String) {
    ALL("🔥 All", "🔥"),
    MOVIES("🎬 Movies", "🎬"),
    TV_SHOWS("📺 TV Shows", "📺"),
    LIVE_SPORTS("🔴 Live Sports", "🔴"),
    CARTOONS_ANIME("🧸 Cartoons & Anime", "🧸"),
    REGIONAL_HITS("🌍 Regional Hits", "🌍")
}

enum class MediaCollectionType(val title: String) {
    CONTINUE_WATCHING("Continue Watching"),
    TRENDING_NOW("Trending Now"),
    POPULAR_MOVIES("Popular Movies"),
    TOP_TV_SHOWS("Top TV Shows"),
    LIVE_SPORTS("Live Sports Highlights")
}

data class CastMember(
    val name: String,
    val role: String,
    val avatarColorHex: Long = 0xFF2A3142
)

data class StreamServer(
    val id: String,
    val name: String,
    val quality: String,
    val pingMs: Int,
    val status: String,
    val bitrate: String,
    val videoUrl: String
)

data class SubtitleCue(
    val startSec: Int,
    val endSec: Int,
    val text: String
)

data class SubtitleTrack(
    val id: String,
    val label: String,
    val cues: List<SubtitleCue> = emptyList(),
    val fileUrl: String = ""
)

data class TvEpisode(
    val id: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    val airDate: String? = null,
    val stillPath: String? = null,
    val voteAverage: Double = 0.0,
    val runtimeMinutes: Int? = null,
    val streamServers: List<StreamServer> = emptyList(),
    val videoUrl: String = ""
) {
    val fullStillUrl: String
        get() = if (!stillPath.isNullOrBlank()) {
            if (stillPath.startsWith("http")) stillPath else "https://image.tmdb.org/t/p/w500$stillPath"
        } else {
            "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=800&auto=format&fit=crop&q=80"
        }
}

data class TvSeason(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val episodes: List<TvEpisode> = emptyList()
)

data class MediaItem(
    val id: String,
    val title: String,
    val category: MediaCategory,
    val genres: List<String>,
    val releaseYear: Int,
    val duration: String,
    val imdbRating: Float,
    val qualityBadge: String,
    val plotSynopsis: String,
    val cast: List<CastMember>,
    val posterUrl: String,
    val backdropUrl: String,
    val drawableResId: Int? = null,
    val streamServers: List<StreamServer>,
    val subtitleTracks: List<SubtitleTrack>,
    val collections: List<MediaCollectionType>,
    val defaultVideoUrl: String,
    val seasons: List<TvSeason> = emptyList(),
    val episodes: List<TvEpisode> = emptyList()
) {
    /**
     * Programmatically arranges all episodes in strict ascending numerical sequence
     * by season number first, followed immediately by episode number.
     */
    val sortedEpisodes: List<TvEpisode>
        get() = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))

    /**
     * Programmatically arranges seasons in strict ascending numerical sequence
     * by season number, with each season's episodes sorted by episode number.
     */
    val sortedSeasons: List<TvSeason>
        get() = seasons.sortedBy { it.seasonNumber }.map { season ->
            season.copy(episodes = season.episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })))
        }
}
