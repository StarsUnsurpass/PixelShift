package com.example.pixelshift.domain

import android.graphics.Bitmap

interface ImageProcessor {
    suspend fun process(original: Bitmap, config: ProcessingConfig): Bitmap
    suspend fun smooth(bitmap: Bitmap, config: ProcessingConfig): Bitmap
    suspend fun pixelate(bitmap: Bitmap, config: ProcessingConfig): Bitmap
    suspend fun quantize(bitmap: Bitmap, config: ProcessingConfig): Bitmap
}

data class ProcessingConfig(
        val pixelSize: Int = 1,
        val contrast: Float = 1.0f,
        val saturation: Float = 1.0f,
        val ditherType: DitherType = DitherType.None,
        val palette: Palette = Palette.None,
        val smoothImage: Boolean = false,
        val enhanceEdges: Boolean = false
)

enum class DitherType(val displayName: String) {
    None("无"),
    FloydSteinberg("Floyd-Steinberg (扩散)"),
    Bayer("Bayer (有序)"),
    Atkinson("Atkinson")
}

sealed class Palette(val displayName: String) {
    object None : Palette("原色")
    object GameBoy : Palette("GameBoy (4色)")
    object NES : Palette("红白机 (56色)")
    object CGA : Palette("CGA (4色)")
    object BW : Palette("黑白 (2色)")
    data class Auto(val colorCount: Int) : Palette("自动 ($colorCount 色)")
}
