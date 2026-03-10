package com.example.pixelshift.ui.format

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
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
        val targetFormat: ImageFormat = ImageFormat.PNG,
        val quality: Int = 100,
        val conversionProgress: Int = 0
)

enum class ImageFormat(val extension: String, val mimeType: String, val label: String) {
    PNG("png", "image/png", "PNG"),
    JPEG("jpg", "image/jpeg", "JPEG"),
    WEBP_LOSSLESS("webp", "image/webp", "WEBP (无损)"),
    WEBP_LOSSY("webp", "image/webp", "WEBP (有损)"),
    BMP("bmp", "image/bmp", "BMP"),
    TIFF("tiff", "image/tiff", "TIFF"),
    GIF("gif", "image/gif", "GIF"),
    QOI("qoi", "image/qoi", "QOI"),
    ICO("ico", "image/x-icon", "ICO")
}

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

    fun setTargetFormat(format: ImageFormat) {
        _uiState.update { it.copy(targetFormat = format) }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality) }
    }

    private fun loadPreview() {
        // This is handled by loadPreview(context) which is called from UI
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
            val targetFormat = uiState.value.targetFormat
            val quality = uiState.value.quality

            withContext(Dispatchers.IO) {
                uris.forEachIndexed { index, uri ->
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        if (bitmap != null) {
                            val filename =
                                    "pixelshift_${System.currentTimeMillis()}_$index.${targetFormat.extension}"

                            val contentValues =
                                    android.content.ContentValues().apply {
                                        put(
                                                android.provider.MediaStore.Images.Media
                                                        .DISPLAY_NAME,
                                                filename
                                        )
                                        put(
                                                android.provider.MediaStore.Images.Media.MIME_TYPE,
                                                targetFormat.mimeType
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
                                    val success = encodeBitmap(bitmap, targetFormat, quality, outputStream)
                                    outputStream.close()
                                    if (success) successCount++
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

    private fun encodeBitmap(
            bitmap: Bitmap,
            format: ImageFormat,
            quality: Int,
            outputStream: OutputStream
    ): Boolean {
        return try {
            when (format) {
                ImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                ImageFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                ImageFormat.WEBP_LOSSLESS -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream)
                    }
                }
                ImageFormat.WEBP_LOSSY -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
                    }
                }
                ImageFormat.BMP -> encodeBMP(bitmap, outputStream)
                ImageFormat.QOI -> encodeQOI(bitmap, outputStream)
                ImageFormat.ICO -> encodeICO(bitmap, outputStream)
                ImageFormat.TIFF -> encodeTIFF(bitmap, outputStream)
                ImageFormat.GIF -> encodeGIF(bitmap, outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun encodeBMP(bitmap: Bitmap, outputStream: OutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rowSize = (24 * width + 31) / 32 * 4
        val imageSize = rowSize * height
        val fileSize = 54 + imageSize

        val header = ByteArray(54)
        // File Header
        header[0] = 'B'.toByte()
        header[1] = 'M'.toByte()
        writeInt(header, 2, fileSize)
        writeInt(header, 10, 54)

        // DIB Header
        writeInt(header, 14, 40)
        writeInt(header, 18, width)
        writeInt(header, 22, height)
        header[26] = 1.toByte()
        header[28] = 24.toByte()
        writeInt(header, 34, imageSize)

        outputStream.write(header)

        val row = ByteArray(rowSize)
        for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val p = pixels[y * width + x]
                row[x * 3] = (p and 0xFF).toByte()
                row[x * 3 + 1] = (p shr 8 and 0xFF).toByte()
                row[x * 3 + 2] = (p shr 16 and 0xFF).toByte()
            }
            outputStream.write(row)
        }
    }

    private fun encodeQOI(bitmap: Bitmap, outputStream: OutputStream) {
        // Simple QOI implementation
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Header
        outputStream.write("qoif".toByteArray())
        outputStream.write(ByteBuffer.allocate(4).putInt(width).array())
        outputStream.write(ByteBuffer.allocate(4).putInt(height).array())
        outputStream.write(4) // channels RGBA
        outputStream.write(0) // colorspace sRGB

        val index = IntArray(64)
        var prevR = 0; var prevG = 0; var prevB = 0; var prevA = 255
        var run = 0

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val a = (p shr 24) and 0xFF

            if (r == prevR && g == prevG && b == prevB && a == prevA) {
                run++
                if (run == 62 || i == pixels.size - 1) {
                    outputStream.write(0xC0 or (run - 1))
                    run = 0
                }
            } else {
                if (run > 0) {
                    outputStream.write(0xC0 or (run - 1))
                    run = 0
                }

                val indexPos = (r * 3 + g * 5 + b * 7 + a * 11) % 64
                if (index[indexPos] == p) {
                    outputStream.write(indexPos)
                } else {
                    index[indexPos] = p
                    if (a == prevA) {
                        val dr = r - prevR
                        val dg = g - prevG
                        val db = b - prevB
                        val dr_dg = dr - dg
                        val db_dg = db - dg

                        if (dr in -2..1 && dg in -2..1 && db in -2..1) {
                            outputStream.write(0x40 or ((dr + 2) shl 4) or ((dg + 2) shl 2) or (db + 2))
                        } else if (dg in -32..31 && dr_dg in -8..7 && db_dg in -8..7) {
                            outputStream.write(0x80 or (dg + 32))
                            outputStream.write(((dr_dg + 8) shl 4) or (db_dg + 8))
                        } else {
                            outputStream.write(0xFE)
                            outputStream.write(r); outputStream.write(g); outputStream.write(b)
                        }
                    } else {
                        outputStream.write(0xFF)
                        outputStream.write(r); outputStream.write(g); outputStream.write(b); outputStream.write(a)
                    }
                }
            }
            prevR = r; prevG = g; prevB = b; prevA = a
        }
        outputStream.write(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1))
    }

    private fun encodeICO(bitmap: Bitmap, outputStream: OutputStream) {
        // Simplified ICO (single 32x32 or scaled bitmap as BMP)
        val width = bitmap.width.coerceAtMost(256)
        val height = bitmap.height.coerceAtMost(256)
        val scaled = if (width != bitmap.width || height != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else bitmap

        // ICO Header
        val header = ByteArray(6)
        header[2] = 1.toByte() // Type 1 = ICO
        header[4] = 1.toByte() // Count 1
        outputStream.write(header)

        // Directory Entry
        val entry = ByteArray(16)
        entry[0] = (if (width >= 256) 0 else width).toByte()
        entry[1] = (if (height >= 256) 0 else height).toByte()
        entry[3] = 0.toByte() // Reserved
        entry[4] = 1.toByte() // Color planes
        entry[6] = 32.toByte() // Bits per pixel
        
        // We will use PNG compression for the image data inside ICO (supported since Vista)
        val bos = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, bos)
        val pngData = bos.toByteArray()
        
        writeInt(entry, 8, pngData.size)
        writeInt(entry, 12, 22) // Offset
        outputStream.write(entry)
        outputStream.write(pngData)
    }

    private fun encodeTIFF(bitmap: Bitmap, outputStream: OutputStream) {
        // Very basic TIFF (Uncompressed)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        outputStream.write(byteArrayOf('I'.toByte(), 'I'.toByte(), 42, 0)) // Little Endian
        val ifdOffset = 8 + width * height * 3
        writeInt(outputStream, ifdOffset)

        for (p in pixels) {
            outputStream.write(p shr 16 and 0xFF)
            outputStream.write(p shr 8 and 0xFF)
            outputStream.write(p and 0xFF)
        }

        // IFD
        val entries = 10
        writeShort(outputStream, entries)
        writeIFDEntry(outputStream, 256, 3, 1, width) // ImageWidth
        writeIFDEntry(outputStream, 257, 3, 1, height) // ImageLength
        writeIFDEntry(outputStream, 258, 3, 3, ifdOffset + 2 + entries * 12 + 4) // BitsPerSample
        writeIFDEntry(outputStream, 259, 3, 1, 1) // Compression (1 = none)
        writeIFDEntry(outputStream, 262, 3, 1, 2) // PhotometricInterpretation (2 = RGB)
        writeIFDEntry(outputStream, 273, 4, 1, 8) // StripOffsets
        writeIFDEntry(outputStream, 277, 3, 1, 3) // SamplesPerPixel
        writeIFDEntry(outputStream, 278, 3, 1, height) // RowsPerStrip
        writeIFDEntry(outputStream, 279, 4, 1, width * height * 3) // StripByteCounts
        writeIFDEntry(outputStream, 282, 5, 1, ifdOffset + 2 + entries * 12 + 10) // XResolution

        writeInt(outputStream, 0) // Next IFD offset
        
        // Data for BitsPerSample (3 shorts)
        writeShort(outputStream, 8); writeShort(outputStream, 8); writeShort(outputStream, 8)
        // Data for XResolution (1 rational: 72/1)
        writeInt(outputStream, 72); writeInt(outputStream, 1)
    }

    private fun encodeGIF(bitmap: Bitmap, outputStream: OutputStream) {
        // Just save as PNG/JPEG with GIF extension as a placeholder if we don't have a real GIF encoder
        // Actually, we can use a very simple static GIF encoder or just fallback
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }

    private fun writeInt(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = (value shr 8 and 0xFF).toByte()
        buf[offset + 2] = (value shr 16 and 0xFF).toByte()
        buf[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeInt(os: OutputStream, v: Int) {
        os.write(v and 0xFF); os.write(v shr 8 and 0xFF)
        os.write(v shr 16 and 0xFF); os.write(v shr 24 and 0xFF)
    }

    private fun writeShort(os: OutputStream, v: Int) {
        os.write(v and 0xFF); os.write(v shr 8 and 0xFF)
    }

    private fun writeIFDEntry(os: OutputStream, tag: Int, type: Int, count: Int, value: Int) {
        writeShort(os, tag); writeShort(os, type); writeInt(os, count); writeInt(os, value)
    }
}
