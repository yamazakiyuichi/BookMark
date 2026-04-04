package com.yamazaki.bookmark.data.remote

import org.jsoup.Jsoup
import java.io.InputStream

data class ImportedBookmark(
    val url: String,
    val title: String
)

object BookmarkHtmlParser {

    /**
     * Parse Netscape Bookmark File Format (Chrome export).
     * Format: <DT><A HREF="url" ...>title</A>
     */
    fun parse(inputStream: InputStream): List<ImportedBookmark> {
        val html = inputStream.bufferedReader().readText()
        val doc = Jsoup.parse(html)
        val links = doc.select("DT > A[HREF]")

        return links.mapNotNull { element ->
            val url = element.attr("HREF").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val title = element.text().ifBlank { url }
            ImportedBookmark(url = url, title = title)
        }
    }
}
