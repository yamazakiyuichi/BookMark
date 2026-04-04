package com.yamazaki.bookmark.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class OgpResult(
    val title: String?,
    val thumbnailUrl: String?
)

class OgpFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val youtubePatterns = listOf(
        Regex("""(?:https?://)?(?:www\.)?youtube\.com/watch\?.*v=([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.)?youtube\.com/embed/([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})""")
    )

    suspend fun fetch(url: String): OgpResult = withContext(Dispatchers.IO) {
        // YouTube URL check
        val videoId = extractYouTubeVideoId(url)
        if (videoId != null) {
            return@withContext OgpResult(
                title = null, // will be fetched from OGP if needed
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
            ).let { youtubeResult ->
                // Try to also get title from OGP
                try {
                    val ogp = fetchOgp(url)
                    OgpResult(
                        title = ogp.title ?: youtubeResult.title,
                        thumbnailUrl = youtubeResult.thumbnailUrl
                    )
                } catch (_: Exception) {
                    youtubeResult
                }
            }
        }

        // General OGP fetch
        try {
            fetchOgp(url)
        } catch (_: Exception) {
            OgpResult(title = null, thumbnailUrl = null)
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        for (pattern in youtubePatterns) {
            pattern.find(url)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun fetchOgp(url: String): OgpResult {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return OgpResult(null, null)

        val doc = Jsoup.parse(body, url)

        val ogTitle = doc.select("meta[property=og:title]").attr("abs:content").ifBlank {
            doc.title().ifBlank { null }
        }

        val ogImage = doc.select("meta[property=og:image]").attr("abs:content").ifBlank { null }

        return OgpResult(
            title = ogTitle,
            thumbnailUrl = ogImage
        )
    }
}
