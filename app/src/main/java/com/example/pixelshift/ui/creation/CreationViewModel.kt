package com.example.pixelshift.ui.creation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelshift.data.SettingsRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreationState(
        val width: String = "32",
        val height: String = "32",
        val isAspectRatioLocked: Boolean = false,
        val backgroundType: String = "Transparent", // "Transparent" or "Solid"
        val backgroundColor: Int = android.graphics.Color.WHITE,
        val widthError: String? = null,
        val heightError: String? = null
)

class CreationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(CreationState())
    val uiState: StateFlow<CreationState> = _uiState.asStateFlow()

    val recentConfigs =
            repository.recentCanvasConfigs.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
            )

    private var aspectRatio: Float = 1.0f

    fun updateWidth(newWidth: String) {
        _uiState.update { it.copy(width = newWidth) }
        validateWidth(newWidth)
        if (_uiState.value.isAspectRatioLocked &&
                        newWidth.isNotEmpty() &&
                        newWidth.toIntOrNull() != null
        ) {
            val w = newWidth.toInt()
            val newH = (w / aspectRatio).roundToInt()
            _uiState.update { it.copy(height = newH.toString()) }
            validateHeight(newH.toString())
        }
    }

    fun updateHeight(newHeight: String) {
        _uiState.update { it.copy(height = newHeight) }
        validateHeight(newHeight)
        if (_uiState.value.isAspectRatioLocked &&
                        newHeight.isNotEmpty() &&
                        newHeight.toIntOrNull() != null
        ) {
            val h = newHeight.toInt()
            val newW = (h * aspectRatio).roundToInt()
            _uiState.update { it.copy(width = newW.toString()) }
            validateWidth(newW.toString())
        }
    }

    fun toggleAspectRatioLock() {
        _uiState.update {
            val newLocked = !it.isAspectRatioLocked
            if (newLocked) {
                val w = it.width.toIntOrNull() ?: 32
                val h = it.height.toIntOrNull() ?: 32
                if (h != 0) {
                    aspectRatio = w.toFloat() / h.toFloat()
                }
            }
            it.copy(isAspectRatioLocked = newLocked)
        }
    }

    fun swapDimensions() {
        val current = _uiState.value
        _uiState.update { it.copy(width = current.height, height = current.width) }
        if (current.isAspectRatioLocked) {
            if (aspectRatio != 0f) {
                aspectRatio = 1f / aspectRatio
            }
        }
    }

    fun setBackgroundType(type: String) {
        _uiState.update { it.copy(backgroundType = type) }
    }

    fun setBackgroundColor(color: Int) {
        _uiState.update { it.copy(backgroundColor = color) }
    }

    fun applyPreset(w: Int, h: Int) {
        _uiState.update { it.copy(width = w.toString(), height = h.toString()) }
        // Recalculate aspect ratio if locked, or just let it update?
        // If locked, we should probably update the ratio to the new preset's ratio
        if (_uiState.value.isAspectRatioLocked) {
            aspectRatio = w.toFloat() / h.toFloat()
        }
        validateWidth(w.toString())
        validateHeight(h.toString())
    }

    fun validateInput(): Boolean {
        return validateWidth(_uiState.value.width) && validateHeight(_uiState.value.height)
    }

    private fun validateWidth(wStr: String): Boolean {
        val w = wStr.toIntOrNull()
        return if (w == null) {
            _uiState.update { it.copy(widthError = "Invalid") }
            false
        } else if (w > 512) {
            _uiState.update { it.copy(widthError = "Max 512px") }
            false
        } else {
            _uiState.update { it.copy(widthError = null) }
            true
        }
    }

    private fun validateHeight(hStr: String): Boolean {
        val h = hStr.toIntOrNull()
        return if (h == null) {
            _uiState.update { it.copy(heightError = "Invalid") }
            false
        } else if (h > 512) {
            _uiState.update { it.copy(heightError = "Max 512px") }
            false
        } else {
            _uiState.update { it.copy(heightError = null) }
            true
        }
    }

    fun createCanvas(
            onSuccess: (width: Int, height: Int, isTransparent: Boolean, color: Int) -> Unit
    ) {
        if (validateInput()) {
            val w = _uiState.value.width.toInt()
            val h = _uiState.value.height.toInt()
            viewModelScope.launch { repository.addRecentCanvasConfig(w, h) }
            val isTransparent = _uiState.value.backgroundType == "Transparent"
            onSuccess(w, h, isTransparent, _uiState.value.backgroundColor)
        }
    }
}
