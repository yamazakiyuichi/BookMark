package com.yamazaki.bookmark.data.repository

import com.yamazaki.bookmark.data.local.BookmarkDao
import com.yamazaki.bookmark.data.local.entity.BookmarkEntity
import com.yamazaki.bookmark.data.remote.ImportedBookmark
import com.yamazaki.bookmark.data.remote.OgpFetcher
import com.yamazaki.bookmark.domain.model.Bookmark
import com.yamazaki.bookmark.domain.model.LinkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.util.concurrent.TimeUnit

class BookmarkRepository(
    private val dao: BookmarkDao,
    private val ogpFetcher: OgpFetcher
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun observeAllBookmarks(): Flow<List<Bookmark>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeBookmarksWithThumbnail(): Flow<List<Bookmark>> =
        dao.observeAllWithThumbnail().map { entities -> entities.map { it.toDomain() } }

    suspend fun getBookmarkById(id: Long): Bookmark? =
        dao.getById(id)?.toDomain()

    suspend fun isDuplicate(url: String): Boolean =
        dao.getByUrl(url) != null

    suspend fun addBookmark(url: String): Bookmark {
        val ogp = ogpFetcher.fetch(url)
        val now = Instant.now()
        val entity = BookmarkEntity(
            url = url,
            title = ogp.title ?: url,
            thumbnailUrl = ogp.thumbnailUrl,
            status = LinkStatus.UNKNOWN.name,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli()
        )
        val id = dao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        val entity = BookmarkEntity.fromDomain(
            bookmark.copy(updatedAt = Instant.now())
        )
        dao.update(entity)
    }

    suspend fun deleteBookmark(id: Long) {
        dao.deleteById(id)
    }

    suspend fun checkLink(bookmark: Bookmark): LinkStatus = withContext(Dispatchers.IO) {
        try {
            // Try HEAD first
            val headRequest = Request.Builder()
                .url(bookmark.url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val headResponse = try {
                httpClient.newCall(headRequest).execute()
            } catch (_: Exception) {
                null
            }

            if (headResponse != null) {
                val code = headResponse.code
                headResponse.close()

                if (code in 200..399) {
                    return@withContext LinkStatus.OK
                }
                if (code != 405) {
                    return@withContext LinkStatus.BROKEN
                }
            }

            // Fallback to GET
            val getRequest = Request.Builder()
                .url(bookmark.url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val getResponse = httpClient.newCall(getRequest).execute()
            val getCode = getResponse.code
            getResponse.close()

            if (getCode in 200..399) LinkStatus.OK else LinkStatus.BROKEN
        } catch (_: Exception) {
            LinkStatus.BROKEN
        }
    }

    suspend fun checkAndUpdateLink(bookmark: Bookmark): Bookmark {
        val status = checkLink(bookmark)
        val updated = bookmark.copy(
            status = status,
            lastCheckedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        updateBookmark(updated)
        return updated
    }

    suspend fun checkAllLinks() {
        val all = dao.getAll().map { it.toDomain() }
        for (bookmark in all) {
            checkAndUpdateLink(bookmark)
        }
    }

    suspend fun exportBookmarksAsHtml(): String? {
        val bookmarks = dao.getAll().map { it.toDomain() }
        if (bookmarks.isEmpty()) return null
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Bookmarks</TITLE>")
        sb.appendLine("<H1>Bookmarks</H1>")
        sb.appendLine("<DL><p>")
        for (bookmark in bookmarks) {
            val addDate = bookmark.createdAt.epochSecond
            val escapedTitle = bookmark.title
                .replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
            sb.appendLine("    <DT><A HREF=\"${bookmark.url}\" ADD_DATE=\"$addDate\">$escapedTitle</A>")
        }
        sb.appendLine("</DL><p>")
        return sb.toString()
    }

    /**
     * Import bookmarks from Chrome HTML export.
     * Skips duplicates. Fetches OGP for thumbnails in background.
     * Returns (imported count, skipped count).
     */
    suspend fun importBookmarks(bookmarks: List<ImportedBookmark>): Pair<Int, Int> {
        var imported = 0
        var skipped = 0
        for (item in bookmarks) {
            if (dao.getByUrl(item.url) != null) {
                skipped++
                continue
            }
            val thumbnailUrl = try {
                ogpFetcher.fetch(item.url).thumbnailUrl
            } catch (_: Exception) {
                null
            }
            val now = Instant.now()
            val entity = BookmarkEntity(
                url = item.url,
                title = item.title,
                thumbnailUrl = thumbnailUrl,
                status = LinkStatus.UNKNOWN.name,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli()
            )
            dao.insert(entity)
            imported++
        }
        return imported to skipped
    }
}
