package com.example.pixelshift.data

import android.graphics.Bitmap
import android.graphics.Color
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.domain.ImageProcessor
import com.example.pixelshift.domain.Palette
import com.example.pixelshift.domain.ProcessingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KotlinImageProcessor : ImageProcessor {

    override suspend fun process(original: Bitmap, config: ProcessingConfig): Bitmap =
            withContext(Dispatchers.Default) {
                // 1. Pixelate (Downsample)
                val pixelated =
                        if (config.pixelSize > 1) {
                            val width = original.width / config.pixelSize
                            val height = original.height / config.pixelSize
                            if (width == 0 || height == 0) return@withContext original
                            Bitmap.createScaledBitmap(original, width, height, false)
                        } else {
                            original.copy(Bitmap.Config.ARGB_8888, true)
                        }

                // 2. Process Pixels (Contrast -> Dither/Quantize)
                val width = pixelated.width
                val height = pixelated.height
                val pixels = IntArray(width * height)
                pixelated.getPixels(pixels, 0, width, 0, 0, width, height)

                val paletteColors = getPaletteColors(config.palette)

                // If no palette and no dither, just contrast/saturation
                if (config.palette is Palette.None && config.ditherType == DitherType.None) {
                    applyContrastSaturation(pixels, config.contrast, config.saturation)
                    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, width, 0, 0, width, height)
                    return@withContext if (config.pixelSize > 1) {
                        Bitmap.createScaledBitmap(result, original.width, original.height, false)
                    } else result
                }

                // Apply contrast/saturation first
                applyContrastSaturation(pixels, config.contrast, config.saturation)

                // Dithering & Quantization
                val resultPixels =
                        if (config.ditherType != DitherType.None || config.palette !is Palette.None
                        ) {
                            applyDithering(pixels, width, height, config.ditherType, paletteColors)
                        } else {
                            pixels
                        }

                val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)

                // 3. Upscale if needed (Nearest Neighbor)
                if (config.pixelSize > 1) {
                    Bitmap.createScaledBitmap(resultBitmap, original.width, original.height, false)
                } else {
                    resultBitmap
                }
            }

    private fun getPaletteColors(palette: Palette): IntArray {
        return when (palette) {
            is Palette.None -> IntArray(0)
            is Palette.BW -> intArrayOf(Color.BLACK, Color.WHITE)
            is Palette.GameBoy ->
                    intArrayOf(
                            0xFF0F380F.toInt(),
                            0xFF306230.toInt(),
                            0xFF8BAC0F.toInt(),
                            0xFF9BBC0F.toInt()
                    )
            is Palette.CGA ->
                    intArrayOf(
                            0xFF000000.toInt(),
                            0xFF55FFFF.toInt(),
                            0xFFFF55FF.toInt(),
                            0xFFFFFFFF.toInt()
                    )
            is Palette.NES ->
                    intArrayOf( // Simplified NES
                            0xFF7C7C7C.toInt(),
                            0xFF0000FC.toInt(),
                            0xFF0000BC.toInt(),
                            0xFF4428BC.toInt(),
                            0xFF940084.toInt(),
                            0xFFA80020.toInt(),
                            0xFFA81000.toInt(),
                            0xFF881400.toInt(),
                            0xFF503000.toInt(),
                            0xFF007800.toInt(),
                            0xFF006800.toInt(),
                            0xFF005800.toInt(),
                            0xFF004058.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt(),
                            0xFFBCBCBC.toInt(),
                            0xFF0078F8.toInt(),
                            0xFF0058F8.toInt(),
                            0xFF6844FC.toInt(),
                            0xFFD800CC.toInt(),
                            0xFFE40058.toInt(),
                            0xFFF83800.toInt(),
                            0xFFE45C10.toInt(),
                            0xFFAC7C00.toInt(),
                            0xFF00B800.toInt(),
                            0xFF00A800.toInt(),
                            0xFF00A844.toInt(),
                            0xFF008888.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt(),
                            0xFFF8F8F8.toInt(),
                            0xFF3CBCFC.toInt(),
                            0xFF6888FC.toInt(),
                            0xFF9878F8.toInt(),
                            0xFFF878F8.toInt(),
                            0xFFF85898.toInt(),
                            0xFFF87858.toInt(),
                            0xFFFCA044.toInt(),
                            0xFFF8B800.toInt(),
                            0xFFB8F818.toInt(),
                            0xFF58D854.toInt(),
                            0xFF58F898.toInt(),
                            0xFF00E8D8.toInt(),
                            0xFF787878.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt(),
                            0xFFFCFCFC.toInt(),
                            0xFFA4E4FC.toInt(),
                            0xFFB8B8F8.toInt(),
                            0xFFD8B8F8.toInt(),
                            0xFFF8B8F8.toInt(),
                            0xFFF8A4C0.toInt(),
                            0xFFF0D0B0.toInt(),
                            0xFFFCE0A8.toInt(),
                            0xFFF8D878.toInt(),
                            0xFFD8F878.toInt(),
                            0xFFB8F8B8.toInt(),
                            0xFFB8F8D8.toInt(),
                            0xFF00FCFC.toInt(),
                            0xFFF8D8F8.toInt(),
                            0xFF000000.toInt(),
                            0xFF000000.toInt()
                    )
            is Palette.Auto -> IntArray(0) // TODO: Implement K-Means
        }
    }

    private fun applyContrastSaturation(pixels: IntArray, contrast: Float, saturation: Float) {
        // Simple implementation
        for (i in pixels.indices) {
            val c = pixels[i]
            // Contrast/Saturation logic (omitted for brevity, just passthrough for now to focus on
            // dither structure)
            // Ideally convert to HSL or RGB manipulation
        }
    }

    // Very naive implementation for structure. Real one needs error diffusion.
    private fun applyDithering(
            pixels: IntArray,
            width: Int,
            height: Int,
            type: DitherType,
            palette: IntArray
    ): IntArray {
        if (palette.isEmpty()) return pixels

        // Clone for error diffusion
        val newPixels = pixels.clone()

        // Floyd-Steinberg
        if (type == DitherType.FloydSteinberg) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    val oldColor = newPixels[index]
                    val newColor = findNearestColor(oldColor, palette)
                    newPixels[index] = newColor

                    val quantErrorR = Color.red(oldColor) - Color.red(newColor)
                    val quantErrorG = Color.green(oldColor) - Color.green(newColor)
                    val quantErrorB = Color.blue(oldColor) - Color.blue(newColor)

                    distributeError(
                            newPixels,
                            x + 1,
                            y,
                            width,
                            height,
                            quantErrorR,
                            quantErrorG,
                            quantErrorB,
                            7.0 / 16.0
                    )
                    distributeError(
                            newPixels,
                            x - 1,
                            y + 1,
                            width,
                            height,
                            quantErrorR,
                            quantErrorG,
                            quantErrorB,
                            3.0 / 16.0
                    )
                    distributeError(
                            newPixels,
                            x,
                            y + 1,
                            width,
                            height,
                            quantErrorR,
                            quantErrorG,
                            quantErrorB,
                            5.0 / 16.0
                    )
                    distributeError(
                            newPixels,
                            x + 1,
                            y + 1,
                            width,
                            height,
                            quantErrorR,
                            quantErrorG,
                            quantErrorB,
                            1.0 / 16.0
                    )
                }
            }
        } else if (type == DitherType.None) {
            for (i in newPixels.indices) {
                newPixels[i] = findNearestColor(newPixels[i], palette)
            }
        }

        return newPixels
    }

    private fun distributeError(
            pixels: IntArray,
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            er: Int,
            eg: Int,
            eb: Int,
            factor: Double
    ) {
        if (x < 0 || x >= w || y < 0 || y >= h) return
        val index = y * w + x
        val c = pixels[index]
        val r = (Color.red(c) + er * factor).coerceIn(0.0, 255.0).toInt()
        val g = (Color.green(c) + eg * factor).coerceIn(0.0, 255.0).toInt()
        val b = (Color.blue(c) + eb * factor).coerceIn(0.0, 255.0).toInt()
        pixels[index] = Color.rgb(r, g, b)
    }

    private fun findNearestColor(color: Int, palette: IntArray): Int {
        var minDistance = Double.MAX_VALUE
        var nearest = palette[0]
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        for (p in palette) {
            val pr = Color.red(p)
            val pg = Color.green(p)
            val pb = Color.blue(p)
            // Euclidean distance
            val distance =
                    Math.sqrt(
                            ((r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb))
                                    .toDouble()
                    )
            if (distance < minDistance) {
                minDistance = distance
                nearest = p
            }
        }
        return nearest
    }
}
