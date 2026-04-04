package com.yamazaki.bookmark.ui.screen.add

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yamazaki.bookmark.data.remote.OgpResult
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.data.remote.OgpFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddUiState(
    val url: String = "",
    val preview: OgpResult? = null,
    val isLoadingPreview: Boolean = false,
    val isSaving: Boolean = false,
    val isDuplicate: Boolean = false,
    val urlError: String? = null,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

class AddViewModel(
    private val repository: BookmarkRepository,
    private val ogpFetcher: OgpFetcher,
    prefillUrl: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState(url = prefillUrl ?: ""))
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    init {
        if (!prefillUrl.isNullOrBlank()) {
            fetchPreview()
        }
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            urlError = null,
            isDuplicate = false,
            preview = null
        )
    }

    fun fetchPreview() {
        val url = _uiState.value.url.trim()
        if (!isValidUrl(url)) {
            _uiState.value = _uiState.value.copy(urlError = "有効なURLを入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPreview = true, urlError = null)

            // Check duplicate
            val isDup = repository.isDuplicate(url)
            if (isDup) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    isDuplicate = true
                )
                return@launch
            }

            try {
                val result = ogpFetcher.fetch(url)
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    preview = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    preview = OgpResult(title = null, thumbnailUrl = null)
                )
            }
        }
    }

    fun save() {
        val url = _uiState.value.url.trim()
        if (!isValidUrl(url)) {
            _uiState.value = _uiState.value.copy(urlError = "有効なURLを入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                repository.addBookmark(url)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "保存に失敗しました: ${e.message}"
                )
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
    }

    class Factory(
        private val repository: BookmarkRepository,
        private val ogpFetcher: OgpFetcher,
        private val prefillUrl: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddViewModel(repository, ogpFetcher, prefillUrl) as T
        }
    }
}
