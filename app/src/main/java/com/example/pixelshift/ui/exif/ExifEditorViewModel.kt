package com.example.pixelshift.ui.exif

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixelshift.data.ExifRepository
import com.example.pixelshift.data.ExifTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExifUiState(
    val tags: List<ExifTag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class ExifEditorViewModel(
    private val repository: ExifRepository,
    private val uri: Uri
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExifUiState())
    val uiState: StateFlow<ExifUiState> = _uiState.asStateFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tags = repository.getExifMetadata(uri)
                _uiState.update { it.copy(tags = tags, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load metadata: ${e.message}", isLoading = false) }
            }
        }
    }

    fun updateTag(tag: String, newValue: String) {
        _uiState.update { state ->
            val updatedTags = state.tags.map {
                if (it.tag == tag) it.copy(value = newValue) else it
            }
            state.copy(tags = updatedTags)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updatedMap = _uiState.value.tags.associate { it.tag to (it.value ?: "") }
            val success = repository.updateExifMetadata(uri, updatedMap)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save metadata") }
            }
        }
    }

    fun clearAllMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = repository.clearExifMetadata(uri)
            if (success) {
                loadMetadata()
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Failed to clear metadata") }
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    class Factory(private val repository: ExifRepository, private val uri: Uri) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExifEditorViewModel(repository, uri) as T
        }
    }
}
