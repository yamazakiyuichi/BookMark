package com.yamazaki.bookmark.domain.model

import java.time.Instant

data class Bookmark(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val status: LinkStatus = LinkStatus.UNKNOWN,
    val lastCheckedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
