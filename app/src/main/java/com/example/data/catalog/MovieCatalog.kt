package com.example.data.catalog

import com.example.data.model.*
import com.example.data.service.StreamingIndexService

object MovieCatalog {

    private val defaultSubtitleTracks = listOf(
        SubtitleTrack(
            id = "off",
            label = "Subtitles Off",
            cues = emptyList()
        ),
        SubtitleTrack(
            id = "en",
            label = "English Track",
            cues = listOf(
                SubtitleCue(0, 4, "[Cinematic Theme Music Playing]"),
                SubtitleCue(5, 10, "In a world of infinite stories..."),
                SubtitleCue(11, 16, "Connected to The Movie Database (TMDb) Live."),
                SubtitleCue(17, 23, "3 CDN failover server relays ready."),
                SubtitleCue(24, 30, "System status: All video streams optimal.")
            )
        ),
        SubtitleTrack(
            id = "es",
            label = "Spanish Track",
            cues = listOf(
                SubtitleCue(0, 4, "[Música Cinemática en Reproducción]"),
                SubtitleCue(5, 10, "Conectado a The Movie Database en vivo..."),
                SubtitleCue(11, 16, "Relés de servidor CDN listos."),
                SubtitleCue(17, 23, "Reproduciendo contenido de alta definición.")
            )
        )
    )

    private fun createServers(baseTitle: String, id: String): List<StreamServer> {
        return com.example.data.service.MovieStreamUrlResolver.generateStreamServers(baseTitle, id)
    }

    private fun resolveDefaultUrl(title: String, id: String): String {
        return com.example.data.service.MovieStreamUrlResolver.resolvePlayableMovieUrl(title = title, mediaId = id)
    }

    // Initial real TMDb movies (seeds if waiting for live HTTP response)
    private val fallbackBanners = listOf(
        MediaItem(
            id = "tmdb_693134",
            title = "Dune: Part Two",
            category = MediaCategory.MOVIES,
            genres = listOf("Sci-Fi", "Adventure", "Action"),
            releaseYear = 2024,
            duration = "2h 46m",
            imdbRating = 8.6f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            cast = listOf(
                CastMember("Timothée Chalamet", "Paul Atreides", 0xFF8C5B32),
                CastMember("Zendaya", "Chani", 0xFF6D4C41),
                CastMember("Rebecca Ferguson", "Lady Jessica", 0xFF455A64)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/xOMo8BRK7PfcJv9JCnx7s520b4q.jpg",
            streamServers = createServers("Dune: Part Two", "tmdb_693134"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TRENDING_NOW, MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Dune: Part Two", "tmdb_693134")
        ),
        MediaItem(
            id = "tmdb_533535",
            title = "Deadpool & Wolverine",
            category = MediaCategory.MOVIES,
            genres = listOf("Action", "Comedy", "Sci-Fi"),
            releaseYear = 2024,
            duration = "2h 08m",
            imdbRating = 7.8f,
            qualityBadge = "4K HDR",
            plotSynopsis = "A listless Wade Wilson toils away in civilian life with his days as the morally flexible mercenary, Deadpool, behind him. But when his homeworld faces an existential threat, Wade must reluctantly suit-up.",
            cast = listOf(
                CastMember("Ryan Reynolds", "Wade Wilson / Deadpool", 0xFFB71C1C),
                CastMember("Hugh Jackman", "Logan / Wolverine", 0xFFF57F17),
                CastMember("Emma Corrin", "Cassandra Nova", 0xFF4A148C)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/yDHYTjA3R0jFYba16jBB1jv82E9.jpg",
            streamServers = createServers("Deadpool & Wolverine", "tmdb_533535"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TRENDING_NOW, MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Deadpool & Wolverine", "tmdb_533535")
        ),
        MediaItem(
            id = "tmdb_1022789",
            title = "Inside Out 2",
            category = MediaCategory.MOVIES,
            genres = listOf("Animation", "Family", "Comedy"),
            releaseYear = 2024,
            duration = "1h 36m",
            imdbRating = 7.7f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Teenager Riley's mind headquarters is undergoing a sudden demolition to make room for something entirely unexpected: new Emotions! Joy, Sadness, Anger, Fear and Disgust are not sure how to feel when Anxiety shows up.",
            cast = listOf(
                CastMember("Amy Poehler", "Joy", 0xFFFFD54F),
                CastMember("Maya Hawke", "Anxiety", 0xFFFF7043),
                CastMember("Kensington Tallman", "Riley", 0xFF42A5F5)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/stKGOmvdUb7LDmi72WjhaodqwAh.jpg",
            streamServers = createServers("Inside Out 2", "tmdb_1022789"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TRENDING_NOW, MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Inside Out 2", "tmdb_1022789")
        ),
        MediaItem(
            id = "tmdb_558449",
            title = "Gladiator II",
            category = MediaCategory.MOVIES,
            genres = listOf("Action", "Adventure", "Drama"),
            releaseYear = 2024,
            duration = "2h 28m",
            imdbRating = 7.5f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Years after witnessing the death of the revered hero Maximus at the hands of his uncle, Lucius must enter the Colosseum after his home is conquered by the tyrannical Emperors who now lead Rome.",
            cast = listOf(
                CastMember("Paul Mescal", "Lucius", 0xFF8D6E63),
                CastMember("Pedro Pascal", "Marcus Acacius", 0xFF5D4037),
                CastMember("Denzel Washington", "Macrinus", 0xFF3E2723)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/euYIwmwkmz95mnXvufEmbL6ovhZ.jpg",
            streamServers = createServers("Gladiator II", "tmdb_558449"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TRENDING_NOW, MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Gladiator II", "tmdb_558449")
        ),
        MediaItem(
            id = "tmdb_tv_66732",
            title = "Stranger Things",
            category = MediaCategory.TV_SHOWS,
            genres = listOf("Sci-Fi", "Drama", "Mystery"),
            releaseYear = 2016,
            duration = "4 Seasons",
            imdbRating = 8.7f,
            qualityBadge = "4K HDR",
            plotSynopsis = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
            cast = listOf(
                CastMember("Millie Bobby Brown", "Eleven", 0xFF8E24AA),
                CastMember("Finn Wolfhard", "Mike Wheeler", 0xFF1E88E5),
                CastMember("David Harbour", "Jim Hopper", 0xFF43A047)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
            streamServers = createServers("Stranger Things", "tmdb_tv_66732"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TOP_TV_SHOWS, MediaCollectionType.TRENDING_NOW),
            defaultVideoUrl = resolveDefaultUrl("Stranger Things", "tmdb_tv_66732"),
            seasons = com.example.data.service.generateTvSeasons("Stranger Things", "tmdb_tv_66732", 4, 8, "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg"),
            episodes = com.example.data.service.generateTvEpisodes("Stranger Things", "tmdb_tv_66732", 4, 8, "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg")
        ),
        MediaItem(
            id = "tmdb_tv_100088",
            title = "The Last of Us",
            category = MediaCategory.TV_SHOWS,
            genres = listOf("Drama", "Sci-Fi", "Action"),
            releaseYear = 2023,
            duration = "2 Seasons",
            imdbRating = 8.8f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Twenty years after modern civilization has been destroyed, Joel, a hardened survivor, is hired to smuggle Ellie, a 14-year-old girl, out of an oppressive quarantine zone.",
            cast = listOf(
                CastMember("Pedro Pascal", "Joel Miller", 0xFF5D4037),
                CastMember("Bella Ramsey", "Ellie Williams", 0xFF00897B)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/uKvVjK19uKnQzdtNDtB6cG58OXS.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/uDgy6hyPd82kOHh6I95FLtLnj6p.jpg",
            streamServers = createServers("The Last of Us", "tmdb_tv_100088"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TOP_TV_SHOWS, MediaCollectionType.TRENDING_NOW),
            defaultVideoUrl = resolveDefaultUrl("The Last of Us", "tmdb_tv_100088"),
            seasons = com.example.data.service.generateTvSeasons("The Last of Us", "tmdb_tv_100088", 2, 9, "https://image.tmdb.org/t/p/w500/uKvVjK19uKnQzdtNDtB6cG58OXS.jpg"),
            episodes = com.example.data.service.generateTvEpisodes("The Last of Us", "tmdb_tv_100088", 2, 9, "https://image.tmdb.org/t/p/w500/uKvVjK19uKnQzdtNDtB6cG58OXS.jpg")
        ),
        MediaItem(
            id = "tmdb_872585",
            title = "Oppenheimer",
            category = MediaCategory.MOVIES,
            genres = listOf("Drama", "History"),
            releaseYear = 2023,
            duration = "3h 00m",
            imdbRating = 8.9f,
            qualityBadge = "4K IMAX",
            plotSynopsis = "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II.",
            cast = listOf(
                CastMember("Cillian Murphy", "J. Robert Oppenheimer", 0xFF37474F),
                CastMember("Emily Blunt", "Katherine Oppenheimer", 0xFF455A64),
                CastMember("Robert Downey Jr.", "Lewis Strauss", 0xFF263238)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/rLb2cwF3Pazuxaj0sRXQ037tGI1.jpg",
            streamServers = createServers("Oppenheimer", "tmdb_872585"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Oppenheimer", "tmdb_872585")
        ),
        MediaItem(
            id = "tmdb_157336",
            title = "Interstellar",
            category = MediaCategory.MOVIES,
            genres = listOf("Sci-Fi", "Drama", "Adventure"),
            releaseYear = 2014,
            duration = "2h 49m",
            imdbRating = 8.7f,
            qualityBadge = "4K HDR",
            plotSynopsis = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel.",
            cast = listOf(
                CastMember("Matthew McConaughey", "Cooper", 0xFF5D4037),
                CastMember("Anne Hathaway", "Brand", 0xFF37474F)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/rAiYTsqJiik70dwJ2f1CVHgx94q.jpg",
            streamServers = createServers("Interstellar", "tmdb_157336"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Interstellar", "tmdb_157336")
        ),
        MediaItem(
            id = "tmdb_569094",
            title = "Spider-Man: Across the Spider-Verse",
            category = MediaCategory.CARTOONS_ANIME,
            genres = listOf("Animation", "Action", "Sci-Fi"),
            releaseYear = 2023,
            duration = "2h 20m",
            imdbRating = 8.6f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its very existence.",
            cast = listOf(
                CastMember("Shameik Moore", "Miles Morales", 0xFFB71C1C),
                CastMember("Hailee Steinfeld", "Gwen Stacy", 0xFF1E88E5)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg",
            streamServers = createServers("Spider-Man: Across the Spider-Verse", "tmdb_569094"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.POPULAR_MOVIES),
            defaultVideoUrl = resolveDefaultUrl("Spider-Man: Across the Spider-Verse", "tmdb_569094")
        ),
        MediaItem(
            id = "tmdb_tv_1396",
            title = "Breaking Bad",
            category = MediaCategory.TV_SHOWS,
            genres = listOf("Crime", "Drama", "Thriller"),
            releaseYear = 2008,
            duration = "5 Seasons",
            imdbRating = 9.5f,
            qualityBadge = "4K HDR",
            plotSynopsis = "A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine with a former student.",
            cast = listOf(
                CastMember("Bryan Cranston", "Walter White", 0xFF33691E),
                CastMember("Aaron Paul", "Jesse Pinkman", 0xFFBF360C)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
            streamServers = createServers("Breaking Bad", "tmdb_tv_1396"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TOP_TV_SHOWS),
            defaultVideoUrl = resolveDefaultUrl("Breaking Bad", "tmdb_tv_1396"),
            seasons = com.example.data.service.generateTvSeasons("Breaking Bad", "tmdb_tv_1396", 5, 8, "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg"),
            episodes = com.example.data.service.generateTvEpisodes("Breaking Bad", "tmdb_tv_1396", 5, 8, "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg")
        ),
        MediaItem(
            id = "tmdb_tv_106379",
            title = "Fallout",
            category = MediaCategory.TV_SHOWS,
            genres = listOf("Sci-Fi", "Action", "Adventure"),
            releaseYear = 2024,
            duration = "1 Season",
            imdbRating = 8.5f,
            qualityBadge = "4K HDR",
            plotSynopsis = "In a future post-apocalyptic Los Angeles brought about by nuclear decimation, citizens must live in underground bunkers to protect themselves from radiation, mutants and bandits.",
            cast = listOf(
                CastMember("Ella Purnell", "Lucy MacLean", 0xFF1E88E5),
                CastMember("Walton Goggins", "The Ghoul", 0xFF5D4037)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/AnsZu45Wb1i0Nn6n15r6kI1qT4.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/2rmK7mnchw9Xr3XdiTFSxTTvYqi.jpg",
            streamServers = createServers("Fallout", "tmdb_tv_106379"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TOP_TV_SHOWS),
            defaultVideoUrl = resolveDefaultUrl("Fallout", "tmdb_tv_106379"),
            seasons = com.example.data.service.generateTvSeasons("Fallout", "tmdb_tv_106379", 1, 8, "https://image.tmdb.org/t/p/w500/AnsZu45Wb1i0Nn6n15r6kI1qT4.jpg"),
            episodes = com.example.data.service.generateTvEpisodes("Fallout", "tmdb_tv_106379", 1, 8, "https://image.tmdb.org/t/p/w500/AnsZu45Wb1i0Nn6n15r6kI1qT4.jpg")
        ),
        MediaItem(
            id = "tmdb_tv_94605",
            title = "Arcane",
            category = MediaCategory.CARTOONS_ANIME,
            genres = listOf("Animation", "Sci-Fi", "Action"),
            releaseYear = 2024,
            duration = "2 Seasons",
            imdbRating = 9.0f,
            qualityBadge = "4K HDR",
            plotSynopsis = "Set in Utopian Piltover and the oppressed underground of Zaun, the story follows the origins of two iconic League champions-and the power that will tear them apart.",
            cast = listOf(
                CastMember("Hailee Steinfeld", "Vi", 0xFFB71C1C),
                CastMember("Ella Purnell", "Jinx", 0xFF00ACC1)
            ),
            posterUrl = "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn397Fvd247Gs.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/kXfqcdQKsToO0OUXHcrrNCHDBzO.jpg",
            streamServers = createServers("Arcane", "tmdb_tv_94605"),
            subtitleTracks = defaultSubtitleTracks,
            collections = listOf(MediaCollectionType.TOP_TV_SHOWS),
            defaultVideoUrl = resolveDefaultUrl("Arcane", "tmdb_tv_94605"),
            seasons = com.example.data.service.generateTvSeasons("Arcane", "tmdb_tv_94605", 2, 9, "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn397Fvd247Gs.jpg"),
            episodes = com.example.data.service.generateTvEpisodes("Arcane", "tmdb_tv_94605", 2, 9, "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn397Fvd247Gs.jpg")
        )
    )

    private val fallbackItems = fallbackBanners

    @Volatile
    var tmdbBanners: List<MediaItem> = emptyList()

    @Volatile
    var tmdbItems: List<MediaItem> = emptyList()

    @Volatile
    var dynamicItems: List<MediaItem> = emptyList()

    @Volatile
    var publishedItems: List<MediaItem> = emptyList()

    @Volatile
    var bulkCategoryGroups: List<com.example.data.service.CategoryGroup> = emptyList()

    fun hasWorkingMedia(item: MediaItem): Boolean {
        if (item.posterUrl.isBlank() || item.posterUrl.contains("null")) return false
        if (item.title.isBlank() || item.title.equals("Untitled", ignoreCase = true) || item.title.equals("None", ignoreCase = true)) return false
        // Exclude unreleased content
        if (item.releaseYear > 2025) return false
        if (item.imdbRating <= 0f) return false
        if (item.plotSynopsis.isBlank() || item.plotSynopsis.length < 15) return false
        val cleanId = item.id.removePrefix("tmdb_tv_").removePrefix("tmdb_")
        if (cleanId.isBlank()) return false
        // TV Shows must have valid seasons and episodes
        if (item.category == MediaCategory.TV_SHOWS && item.seasons.isEmpty() && item.episodes.isEmpty()) return false
        if (item.defaultVideoUrl.isBlank() && item.streamServers.isEmpty()) return false
        return true
    }

    val featuredBanners: List<MediaItem>
        get() = if (tmdbBanners.isNotEmpty()) tmdbBanners.filter { hasWorkingMedia(it) } else fallbackBanners.filter { hasWorkingMedia(it) }

    val catalogItems: List<MediaItem>
        get() = if (tmdbItems.isNotEmpty()) tmdbItems.filter { hasWorkingMedia(it) } else fallbackItems.filter { hasWorkingMedia(it) }

    val effectiveCatalogItems: List<MediaItem>
        get() = (featuredBanners + catalogItems + dynamicItems + publishedItems)
            .distinctBy { it.id }
            .filter { hasWorkingMedia(it) }

    fun setTmdbCatalog(banners: List<MediaItem>, allMovies: List<MediaItem>) {
        if (banners.isNotEmpty()) {
            this.tmdbBanners = banners
        }
        if (allMovies.isNotEmpty()) {
            this.tmdbItems = allMovies
        }
    }

    /**
     * Integrates 1,000+ Movies and 1,000+ TV series grouped by genres into active catalog.
     */
    fun setBulkCatalog(
        movies: List<MediaItem>,
        tvSeries: List<MediaItem>,
        categories: List<com.example.data.service.CategoryGroup>
    ) {
        val combined = (movies + tvSeries).distinctBy { it.id }
        if (combined.isNotEmpty()) {
            this.dynamicItems = combined
        }
        if (categories.isNotEmpty()) {
            this.bulkCategoryGroups = categories
        }
    }

    /**
     * Incrementally appends newly fetched discovery batches without repeating items.
     */
    fun appendBulkItems(newItems: List<MediaItem>) {
        if (newItems.isEmpty()) return
        val validOnly = newItems.filter { hasWorkingMedia(it) }
        if (validOnly.isEmpty()) return
        val current = dynamicItems
        dynamicItems = (current + validOnly).distinctBy { it.id }
    }

    fun getItemById(id: String): MediaItem? {
        return effectiveCatalogItems.find { it.id == id }
    }

    fun getItemsForCategory(category: MediaCategory): List<MediaItem> {
        val items = effectiveCatalogItems
        if (category == MediaCategory.ALL) return items
        val matched = items.filter { it.category == category }
        if (matched.isNotEmpty()) return matched
        return when (category) {
            MediaCategory.MOVIES -> items.filter { it.category == MediaCategory.MOVIES || it.seasons.isEmpty() }
            MediaCategory.TV_SHOWS -> items.filter { it.category == MediaCategory.TV_SHOWS || it.seasons.isNotEmpty() }
            MediaCategory.LIVE_SPORTS -> items.filter { it.genres.any { g -> g.contains("Sport", ignoreCase = true) || g.contains("Documentary", ignoreCase = true) } }
            MediaCategory.CARTOONS_ANIME -> items.filter { it.genres.any { g -> g.contains("Animation", ignoreCase = true) || g.contains("Anime", ignoreCase = true) } }
            MediaCategory.REGIONAL_HITS -> items.filter { it.genres.any { g -> g.contains("Drama", ignoreCase = true) || g.contains("Action", ignoreCase = true) } }
            else -> items
        }
    }

    fun getItemsForCollection(collection: MediaCollectionType): List<MediaItem> {
        val matched = effectiveCatalogItems.filter { it.collections.contains(collection) }
        if (matched.isNotEmpty()) return matched
        return when (collection) {
            MediaCollectionType.TOP_TV_SHOWS -> effectiveCatalogItems.filter { it.category == MediaCategory.TV_SHOWS || it.seasons.isNotEmpty() }.ifEmpty { effectiveCatalogItems.take(10) }
            MediaCollectionType.LIVE_SPORTS -> effectiveCatalogItems.filter { it.category == MediaCategory.LIVE_SPORTS }.ifEmpty { effectiveCatalogItems.take(10) }
            MediaCollectionType.POPULAR_MOVIES -> effectiveCatalogItems.filter { it.category == MediaCategory.MOVIES }.ifEmpty { effectiveCatalogItems.take(10) }
            MediaCollectionType.TRENDING_NOW -> effectiveCatalogItems.take(10)
            else -> effectiveCatalogItems.take(10)
        }
    }

    fun getItemsByGenre(genre: String): List<MediaItem> {
        val allMedia = effectiveCatalogItems
        if (genre.isBlank() || genre.equals("All", ignoreCase = true) || genre.equals("All Genres", ignoreCase = true)) {
            return allMedia
        }
        val cleanGenre = genre.trim().lowercase()
        return allMedia.filter { item ->
            item.genres.any { it.trim().lowercase() == cleanGenre } ||
            item.category.label.lowercase().contains(cleanGenre) ||
            item.category.name.lowercase().contains(cleanGenre)
        }
    }

    fun search(query: String): List<MediaItem> {
        val items = effectiveCatalogItems
        if (query.isBlank()) return items
        val clean = query.trim().lowercase()
        return items.filter { item ->
            item.title.lowercase().contains(clean) ||
            item.genres.any { it.lowercase().contains(clean) } ||
            item.cast.any { it.name.lowercase().contains(clean) } ||
            item.category.label.lowercase().contains(clean)
        }
    }
}
