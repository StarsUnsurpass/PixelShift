package com.example.pixelshift.ui.format

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormatConversionUiState(
        val uris: List<Uri> = emptyList(),
        val selectedUri: Uri? = null,
        val previewBitmap: Bitmap? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val targetFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        val quality: Int = 100,
        val conversionProgress: Int = 0
)

class FormatConversionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FormatConversionUiState())
    val uiState: StateFlow<FormatConversionUiState> = _uiState.asStateFlow()

    fun updateUris(uris: List<Uri>) {
        _uiState.update { it.copy(uris = uris, selectedUri = uris.firstOrNull()) }
        loadPreview()
    }

    fun selectUri(uri: Uri) {
        _uiState.update { it.copy(selectedUri = uri) }
        loadPreview()
    }

    fun setTargetFormat(format: Bitmap.CompressFormat) {
        _uiState.update { it.copy(targetFormat = format) }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality) }
    }

    private fun loadPreview() {
        val uri = uiState.value.selectedUri ?: return
        // In a real app, use dependency injection for context or a repository
        // For now, we will rely on the UI to pass context or load the bitmap
        // Since ViewModel shouldn't hold context, we'll expose a function to load
    }

    fun loadPreview(context: Context) {
        val uri = uiState.value.selectedUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bitmap =
                    withContext(Dispatchers.IO) {
                        try {
                            val inputStream: InputStream? =
                                    context.contentResolver.openInputStream(uri)
                            BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) {
                            null
                        }
                    }
            _uiState.update { it.copy(previewBitmap = bitmap, isLoading = false) }
        }
    }

    fun convertAndSaveAll(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, conversionProgress = 0) }
            var successCount = 0
            val uris = uiState.value.uris
            val total = uris.size

            withContext(Dispatchers.IO) {
                uris.forEachIndexed { index, uri ->
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        if (bitmap != null) {
                            val format = uiState.value.targetFormat
                            val extension =
                                    when (format) {
                                        Bitmap.CompressFormat.PNG -> "png"
                                        Bitmap.CompressFormat.JPEG -> "jpg"
                                        Bitmap.CompressFormat.WEBP -> "webp"
                                        else -> "png"
                                    }
                            // Save to specific folder or scoped storage
                            // Simple "save to Pictures" logic for demonstration
                            val filename =
                                    "pixelshift_converted_${System.currentTimeMillis()}_$index.$extension"

                            // Use MediaStore (simplified here, in real app use a Repository)
                            val contentValues =
                                    android.content.ContentValues().apply {
                                        put(
                                                android.provider.MediaStore.Images.Media
                                                        .DISPLAY_NAME,
                                                filename
                                        )
                                        put(
                                                android.provider.MediaStore.Images.Media.MIME_TYPE,
                                                "image/$extension"
                                        )
                                        put(
                                                android.provider.MediaStore.Images.Media
                                                        .RELATIVE_PATH,
                                                "Pictures/PixelShift/Converted"
                                        )
                                    }

                            val resolver = context.contentResolver
                            val imageUri =
                                    resolver.insert(
                                            android.provider.MediaStore.Images.Media
                                                    .EXTERNAL_CONTENT_URI,
                                            contentValues
                                    )

                            if (imageUri != null) {
                                val outputStream: OutputStream? =
                                        resolver.openOutputStream(imageUri)
                                if (outputStream != null) {
                                    bitmap.compress(format, uiState.value.quality, outputStream)
                                    outputStream.close()
                                    successCount++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    _uiState.update { it.copy(conversionProgress = index + 1) }
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            onComplete(successCount)
        }
    }
}
