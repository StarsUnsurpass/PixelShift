package com.example.pixelshift.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelshift.di.AppModule
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.domain.ImageProcessor
import com.example.pixelshift.domain.Palette
import com.example.pixelshift.domain.ProcessingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
        val original: Bitmap? = null,
        val preview: Bitmap? = null,
        val config: ProcessingConfig = ProcessingConfig(),
        val isLoading: Boolean = false,
        val error: String? = null,
        // Output settings
        val usePixelPerfectUpscale: Boolean = true,
        val outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
)

class EditorViewModel : ViewModel() {

    private val imageProcessor: ImageProcessor = AppModule.imageProcessor

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _configFlow = MutableStateFlow(ProcessingConfig())
    private var processingJob: Job? = null

    init {
        setupProcessingPipeline()
    }

    @OptIn(FlowPreview::class)
    private fun setupProcessingPipeline() {
        _configFlow
                .debounce(200L)
                .onEach { config -> processImage(config) }
                .launchIn(viewModelScope)
    }

    fun loadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bitmap =
                        withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = ImageDecoder.createSource(context.contentResolver, uri)
                                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                    decoder.isMutableRequired = true
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        }
                _uiState.update { it.copy(original = bitmap, preview = bitmap, isLoading = false) }
                _configFlow.value = _uiState.value.config
                processImage(_uiState.value.config)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "无法加载图片: ${e.message}", isLoading = false) }
            }
        }
    }

    fun updatePixelSize(size: Int) {
        updateConfig(_uiState.value.config.copy(pixelSize = size))
    }

    fun updatePalette(palette: Palette) {
        updateConfig(_uiState.value.config.copy(palette = palette))
    }

    fun updateDither(ditherType: DitherType) {
        updateConfig(_uiState.value.config.copy(ditherType = ditherType))
    }

    fun updateContrast(contrast: Float) {
        updateConfig(_uiState.value.config.copy(contrast = contrast))
    }

    fun togglePixelPerfectUpscale(enabled: Boolean) {
        _uiState.update { it.copy(usePixelPerfectUpscale = enabled) }
    }

    fun setOutputFormat(format: Bitmap.CompressFormat) {
        _uiState.update { it.copy(outputFormat = format) }
    }

    private fun updateConfig(config: ProcessingConfig) {
        _uiState.update { it.copy(config = config) }
        _configFlow.value = config
    }

    private fun processImage(config: ProcessingConfig) {
        val original = _uiState.value.original ?: return
        processingJob?.cancel()
        processingJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        val processed = imageProcessor.process(original, config)
                        _uiState.update { it.copy(preview = processed, isLoading = false) }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
    }

    fun exportImage(context: Context, filenamePrefix: String = "pixelshift"): Uri? {
        val original = uiState.value.original ?: return null
        val preview = uiState.value.preview ?: return null
        val config = uiState.value.config
        val useUpscale = uiState.value.usePixelPerfectUpscale
        val format = uiState.value.outputFormat

        val extension =
                when (format) {
                    Bitmap.CompressFormat.PNG -> "png"
                    Bitmap.CompressFormat.JPEG -> "jpg"
                    Bitmap.CompressFormat.WEBP -> "webp"
                    else -> "png"
                }
        val filename = "${filenamePrefix}_${System.currentTimeMillis()}.$extension"

        // Upscale logic
        val finalBitmap =
                if (useUpscale && config.pixelSize > 1) {
                    Bitmap.createScaledBitmap(preview, original.width, original.height, false)
                } else {
                    preview
                }

        val contentValues =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    val mimeType =
                            when (format) {
                                Bitmap.CompressFormat.JPEG -> "image/jpeg"
                                Bitmap.CompressFormat.WEBP -> "image/webp"
                                else -> "image/png"
                            }
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PixelShift")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

        var uri: Uri? = null
        try {
            val resolver = context.contentResolver
            val collection =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

            uri = resolver.insert(collection, contentValues)
            uri?.let { it ->
                resolver.openOutputStream(it)?.use { outputStream ->
                    finalBitmap.compress(format, 100, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return uri
    }
}
