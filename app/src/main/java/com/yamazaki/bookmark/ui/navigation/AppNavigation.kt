package com.yamazaki.bookmark.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ListRoute

@Serializable
data class AddRoute(val prefillUrl: String? = null)

@Serializable
data class DetailRoute(val bookmarkId: Long)
