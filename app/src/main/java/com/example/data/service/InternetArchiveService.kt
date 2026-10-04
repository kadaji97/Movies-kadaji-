package com.example.data.service

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Data model representing the result of an automated Internet Archive public-domain movie search.
 */
data class ArchiveStreamResult(
    val identifier: String,
    val streamUrl: String,
    val downloadUrl: String,
    val title: String,
    val releaseYear: Int?,
    val isPublicDomainFound: Boolean
)

/**
 * TMDB Watch Provider model to display official platforms when no public domain stream exists.
 */
data class WatchProvider(
    val providerId: Int,
    val providerName: String,
    val logoUrl: String,
    val type: String = "Subscription" // Subscription, Rent, Buy, Free
)

/**
 * AUTOMATED SEARCH BRIDGE (Retrofit / OkHttp + Internet Archive API)
 *
 * Automatically and instantly pairs TMDb movie results with real public-domain movie streams
 * and downloads using the Internet Archive API:
 * https://archive.org/advancedsearch.php?q=title:("{TITLE}")+AND+year:({YEAR})+AND+mediatype:(movies)&fl[]=identifier&output=json
 *
 * Direct CDN construct: https://archive.org/download/{identifier}/{identifier}.mp4
 */
object InternetArchiveService {

    private const val TAG = "InternetArchiveService"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // Curated high-availability public domain archive identifiers for instant pairing
    private val KNOWN_PUBLIC_DOMAIN_IDENTIFIERS = mapOf(
        "night of the living dead" to "night_of_the_living_dead_1968",
        "plan 9 from outer space" to "Plan_9_from_Outer_Space_1959",
        "carnival of souls" to "CarnivalofSouls",
        "charade" to "Charade1963",
        "nosferatu" to "Nosferatu1922",
        "metropolis" to "Metropolis1927",
        "house on haunted hill" to "house_on_haunted_hill_1959",
        "the last man on earth" to "TheLastManOnEarth_1964",
        "a trip to the moon" to "A_Trip_to_the_Moon_1902",
        "the general" to "TheGeneral1926",
        "detour" to "detour_1945",
        "d.o.a." to "D.O.A._1949",
        "his girl friday" to "HisGirlFriday",
        "little shop of horrors" to "little_shop_of_horrors",
        "the phantom of the opera" to "phantom_of_the_opera_1925"
    )

    /**
     * Cleans and sanitizes the title string for the archive.org query syntax.
     */
    fun cleanTitleForQuery(title: String): String {
        return title
            .replace("\"", "")
            .replace(":", " ")
            .replace("-", " ")
            .replace("/", " ")
            .replace("\\", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Background asynchronous function that queries the Internet Archive API dynamically.
     * Takes TMDB `title` and `releaseYear`.
     * Constructs URL:
     * https://archive.org/advancedsearch.php?q=title:("{TITLE}")+AND+year:({YEAR})+AND+mediatype:(movies)&fl[]=identifier&output=json
     *
     * Returns ArchiveStreamResult if an identifier is found, or null if no public domain stream is available.
     */
    suspend fun searchArchiveForMovie(title: String, releaseYear: Int?): ArchiveStreamResult? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitleForQuery(title)
        if (cleanTitle.isBlank()) return@withContext null

        val normalizedKey = cleanTitle.lowercase()

        // 1. Instant match check for curated public-domain gems
        val curatedId = KNOWN_PUBLIC_DOMAIN_IDENTIFIERS[normalizedKey]
        if (curatedId != null) {
            val directStreamUrl = "https://archive.org/download/$curatedId/$curatedId.mp4"
            Log.d(TAG, "Instant curated archive match for '$title' -> $curatedId")
            return@withContext ArchiveStreamResult(
                identifier = curatedId,
                streamUrl = directStreamUrl,
                downloadUrl = directStreamUrl,
                title = title,
                releaseYear = releaseYear,
                isPublicDomainFound = true
            )
        }

        // 2. Query dynamic Internet Archive API with year filter
        val resultWithYear = queryArchiveApi(cleanTitle, releaseYear)
        if (resultWithYear != null) {
            return@withContext resultWithYear
        }

        // 3. Fallback query without year filter (in case TMDb release year differs from Archive copyright year)
        if (releaseYear != null && releaseYear > 0) {
            val resultWithoutYear = queryArchiveApi(cleanTitle, null)
            if (resultWithoutYear != null) {
                return@withContext resultWithoutYear
            }
        }

        Log.d(TAG, "No public domain archive stream found for '$title' ($releaseYear)")
        null
    }

    /**
     * Executes the HTTP request against archive.org advancedsearch API.
     */
    private fun queryArchiveApi(cleanTitle: String, year: Int?): ArchiveStreamResult? {
        try {
            val queryParam = if (year != null && year > 1880) {
                """title:("$cleanTitle") AND year:($year) AND mediatype:(movies)"""
            } else {
                """title:("$cleanTitle") AND mediatype:(movies)"""
            }

            val encodedQuery = URLEncoder.encode(queryParam, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier&fl[]=title&output=json&rows=3"

            Log.d(TAG, "Executing Archive.org search: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Movieskadaji-PublicDomain-Finder/1.0 (Android)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Archive.org API responded with code: ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val respObj = root.optJSONObject("response") ?: return null
            val numFound = respObj.optInt("numFound", 0)
            if (numFound <= 0) return null

            val docsArray = respObj.optJSONArray("docs") ?: return null
            if (docsArray.length() == 0) return null

            for (i in 0 until docsArray.length()) {
                val doc = docsArray.optJSONObject(i) ?: continue
                val identifier = doc.optString("identifier", "")
                if (identifier.isNotBlank()) {
                    val directMp4Url = "https://archive.org/download/$identifier/$identifier.mp4"
                    Log.d(TAG, "Discovered public domain stream: identifier=$identifier, url=$directMp4Url")
                    return ArchiveStreamResult(
                        identifier = identifier,
                        streamUrl = directMp4Url,
                        downloadUrl = directMp4Url,
                        title = doc.optString("title", cleanTitle),
                        releaseYear = year,
                        isPublicDomainFound = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Internet Archive for $cleanTitle: ${e.message}")
        }
        return null
    }

    /**
     * Fallback Watch Providers list for licensed titles without public domain streams.
     * Modeled after TMDb watch providers.
     */
    fun getFallbackWatchProviders(title: String): List<WatchProvider> {
        return listOf(
            WatchProvider(
                providerId = 8,
                providerName = "Netflix",
                logoUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=120&auto=format&fit=crop&q=80",
                type = "Subscription"
            ),
            WatchProvider(
                providerId = 9,
                providerName = "Amazon Prime Video",
                logoUrl = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=120&auto=format&fit=crop&q=80",
                type = "Rent / Buy"
            ),
            WatchProvider(
                providerId = 337,
                providerName = "Disney+",
                logoUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=120&auto=format&fit=crop&q=80",
                type = "Subscription"
            ),
            WatchProvider(
                providerId = 350,
                providerName = "Apple TV+",
                logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=120&auto=format&fit=crop&q=80",
                type = "4K Stream"
            )
        )
    }
}
