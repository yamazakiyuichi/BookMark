package com.yamazaki.bookmark.ui.screen.list

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yamazaki.bookmark.data.remote.BookmarkHtmlParser
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.domain.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListViewModel(
    private val repository: BookmarkRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> = repository.observeAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCheckingAll = MutableStateFlow(false)
    val isCheckingAll: StateFlow<Boolean> = _isCheckingAll.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

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

    fun importFromHtml(context: Context, uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val bookmarks = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BookmarkHtmlParser.parse(stream)
                    } ?: emptyList()
                }

                if (bookmarks.isEmpty()) {
                    _snackbarMessage.value = "ブックマークが見つかりませんでした"
                    return@launch
                }

                val (imported, skipped) = withContext(Dispatchers.IO) {
                    repository.importBookmarks(bookmarks)
                }
                _snackbarMessage.value = "${imported}件インポート完了（${skipped}件は既に登録済み）"
            } catch (e: Exception) {
                _snackbarMessage.value = "インポートに失敗しました: ${e.message}"
            } finally {
                _isImporting.value = false
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
