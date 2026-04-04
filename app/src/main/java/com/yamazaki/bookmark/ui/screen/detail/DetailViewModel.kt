package com.yamazaki.bookmark.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.domain.model.Bookmark
import com.yamazaki.bookmark.domain.model.LinkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val bookmark: Bookmark? = null,
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val snackbarMessage: String? = null
)

class DetailViewModel(
    private val repository: BookmarkRepository,
    private val bookmarkId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadBookmark()
    }

    private fun loadBookmark() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val bookmark = repository.getBookmarkById(bookmarkId)
            _uiState.value = _uiState.value.copy(
                bookmark = bookmark,
                isLoading = false,
                editTitle = bookmark?.title ?: ""
            )
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editTitle = _uiState.value.bookmark?.title ?: ""
        )
    }

    fun updateEditTitle(title: String) {
        _uiState.value = _uiState.value.copy(editTitle = title)
    }

    fun saveEdit() {
        val bookmark = _uiState.value.bookmark ?: return
        viewModelScope.launch {
            val updated = bookmark.copy(title = _uiState.value.editTitle)
            repository.updateBookmark(updated)
            _uiState.value = _uiState.value.copy(
                bookmark = updated,
                isEditing = false,
                snackbarMessage = "更新しました"
            )
        }
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editTitle = _uiState.value.bookmark?.title ?: ""
        )
    }

    fun checkLink() {
        val bookmark = _uiState.value.bookmark ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                bookmark = bookmark.copy(status = LinkStatus.CHECKING)
            )
            try {
                val updated = repository.checkAndUpdateLink(bookmark)
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    bookmark = updated,
                    snackbarMessage = when (updated.status) {
                        LinkStatus.OK -> "リンクは有効です"
                        LinkStatus.BROKEN -> "リンクが切れています"
                        else -> "チェック完了"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    snackbarMessage = "チェックに失敗しました"
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    class Factory(
        private val repository: BookmarkRepository,
        private val bookmarkId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(repository, bookmarkId) as T
        }
    }
}
