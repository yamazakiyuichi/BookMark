package com.yamazaki.bookmark.ui.screen.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.domain.model.Bookmark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FeedViewModel(
    repository: BookmarkRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> =
        repository.observeBookmarksWithThumbnail()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(private val repository: BookmarkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeedViewModel(repository) as T
    }
}
