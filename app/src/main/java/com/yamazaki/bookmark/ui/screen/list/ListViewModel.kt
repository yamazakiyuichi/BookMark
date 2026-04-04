package com.yamazaki.bookmark.ui.screen.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.domain.model.Bookmark
import com.yamazaki.bookmark.domain.model.LinkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(
    private val repository: BookmarkRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> = repository.observeAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCheckingAll = MutableStateFlow(false)
    val isCheckingAll: StateFlow<Boolean> = _isCheckingAll.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun checkAllLinks() {
        if (_isCheckingAll.value) return
        viewModelScope.launch {
            _isCheckingAll.value = true
            try {
                repository.checkAllLinks()
                _snackbarMessage.value = "全リンクチェック完了"
            } catch (e: Exception) {
                _snackbarMessage.value = "リンクチェックに失敗しました"
            } finally {
                _isCheckingAll.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    class Factory(private val repository: BookmarkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListViewModel(repository) as T
        }
    }
}
