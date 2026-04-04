package com.yamazaki.bookmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yamazaki.bookmark.domain.model.Bookmark
import com.yamazaki.bookmark.domain.model.LinkStatus
import java.time.Instant

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,

    @ColumnInfo(name = "status")
    val status: String = LinkStatus.UNKNOWN.name,

    @ColumnInfo(name = "last_checked_at")
    val lastCheckedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = Instant.now().toEpochMilli(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = Instant.now().toEpochMilli()
) {
    fun toDomain(): Bookmark = Bookmark(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        status = try { LinkStatus.valueOf(status) } catch (_: Exception) { LinkStatus.UNKNOWN },
        lastCheckedAt = lastCheckedAt?.let { Instant.ofEpochMilli(it) },
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )

    companion object {
        fun fromDomain(bookmark: Bookmark): BookmarkEntity = BookmarkEntity(
            id = bookmark.id,
            url = bookmark.url,
            title = bookmark.title,
            thumbnailUrl = bookmark.thumbnailUrl,
            status = bookmark.status.name,
            lastCheckedAt = bookmark.lastCheckedAt?.toEpochMilli(),
            createdAt = bookmark.createdAt.toEpochMilli(),
            updatedAt = bookmark.updatedAt.toEpochMilli()
        )
    }
}
