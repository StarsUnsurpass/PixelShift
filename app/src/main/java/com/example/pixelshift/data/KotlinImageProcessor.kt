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
                var current = original
                current = smooth(current, config)
                current = pixelate(current, config)
                current = quantize(current, config)
                current
            }

    override suspend fun smooth(bitmap: Bitmap, config: ProcessingConfig): Bitmap =
            withContext(Dispatchers.Default) {
                if (config.smoothImage) {
                    applyKuwaharaFilter(bitmap, 5) // Kernel size 5 is a good balance
                } else {
                    bitmap
                }
            }

    override suspend fun pixelate(bitmap: Bitmap, config: ProcessingConfig): Bitmap =
            withContext(Dispatchers.Default) {
                if (config.pixelSize > 1) {
                    val width = bitmap.width / config.pixelSize
                    val height = bitmap.height / config.pixelSize
                    if (width == 0 || height == 0) return@withContext bitmap
                    
                    // Downsample
                    val downscaled = Bitmap.createScaledBitmap(bitmap, width, height, false)
                    
                    // Optional: Edge Enhancement after downscaling (before quantization)
                     if (config.enhanceEdges) {
                        applySobelEdgeDetection(downscaled)
                    } else {
                        downscaled
                    }
                } else {
                    bitmap
                }
            }

    override suspend fun quantize(bitmap: Bitmap, config: ProcessingConfig): Bitmap =
            withContext(Dispatchers.Default) {
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                // Apply simple contrast/saturation first
                applyContrastSaturation(pixels, config.contrast, config.saturation)

                val paletteColors = getPaletteColors(config.palette, pixels)
                
                // Dithering & Quantization
                 val resultPixels =
                        if (config.ditherType != DitherType.None || config.palette !is Palette.None) {
                            applyDithering(pixels, width, height, config.ditherType, paletteColors)
                        } else {
                            pixels
                        }

                val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
                
                // If we pixelated, we might want to scale back up for display, 
                // BUT the requirements say "progress", and usually we want to see the pixelated version small or large?
                // The original code scaled back up if pixelSize > 1. 
                // Let's keep it consistent: IF it was downscaled, we rely on the UI to scale it up for view or 
                // we scale it back up here. The previous implementation returned a SCALED UP bitmap.
                // However, for "pixelate" step, returning a tiny bitmap is technically correct.
                // But for the final output, user might expect original logic.
                // Let's return the small bitmap here and let `process` or UI handle upscale?
                // Wait, `process` returned scaled UP bitmap in original code.
                // Re-reading original code:
                // `return@withContext if (config.pixelSize > 1) { Bitmap.createScaledBitmap(...) }`
                // AND
                // `if (width == 0 || height == 0) return@withContext original`
                
                // The task is to "optimize pixel conversion".
                // If I return small bitmap, the UI needs to handle it.
                // `EditorViewModel` has `usePixelPerfectUpscale`.
                // Let's return the actual processed bitmap (small if pixelated) from `quantize`.
                // The UI viewing it should handle scaling. 
                // BUT `EditorViewModel.exportImage` handles upscale.
                // The `process` method needs to return consistent result with old `process`.
                // Old `process` returned SCALED UP bitmap if `pixelSize > 1` at the end (lines 57-59 was inside an early return, but notice lines 77 just returned `resultBitmap` which was created from `resultPixels` at line 74.
                // Wait, line 45 `width = pixelated.width`. If `pixelated` was downscaled, `resultBitmap` is small.
                // So the OLD `process` returned a SMALL bitmap (if pixelated). 
                // UNLESS `config.palette is Palette.None && config.ditherType == DitherType.None` (Line 53), which did explicitly scale back up!
                // This means the old code was inconsistent!
                // If I have a palette, it returns small bitmap. If I don't, it returns big bitmap?
                // Let's look at `EditorViewModel.exportImage`:
                // `if (useUpscale && config.pixelSize > 1) { Bitmap.createScaledBitmap(preview, original.width, original.height, false) }`
                // This implies `preview` (output of `process`) is mostly small.
                
                resultBitmap
            }


    private fun getPaletteColors(palette: Palette, pixels: IntArray? = null): IntArray {
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
            is Palette.Auto -> {
                if (pixels != null && pixels.isNotEmpty()) {
                    generatePaletteKMeans(pixels, palette.colorCount)
                } else {
                    IntArray(0)
                }
            }
        }
    }

    private fun generatePaletteKMeans(pixels: IntArray, colorCount: Int): IntArray {
        if (pixels.isEmpty()) return IntArray(0)
        
        // 1. Initialize centroids (randomly pick from pixels)
        val centroids = IntArray(colorCount)
        val random = java.util.Random()
        for (i in 0 until colorCount) {
            centroids[i] = pixels[random.nextInt(pixels.size)]
        }
        
        val assignments = IntArray(pixels.size)
        var changed = true
        var iterations = 0
        val maxIterations = 10 // Limit iterations for performance
        
        while (changed && iterations < maxIterations) {
            changed = false
            iterations++
            
            // 2. Assign pixels to nearest centroid
            // Sampling for speed: we can just use a subset of pixels for clustering if image is huge,
            // but for now let's try full or strided.
            // Using full pixels for now 
            
            val sumsR = LongArray(colorCount)
            val sumsG = LongArray(colorCount)
            val sumsB = LongArray(colorCount)
            val counts = IntArray(colorCount)
            
            for (i in pixels.indices) {
                val p = pixels[i]
                var minDist = Double.MAX_VALUE
                var nearestIndex = 0
                
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                
                for (k in 0 until colorCount) {
                    val c = centroids[k]
                    val cr = Color.red(c)
                    val cg = Color.green(c)
                    val cb = Color.blue(c)
                    
                    val dist = ((r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)).toDouble()
                    if (dist < minDist) {
                        minDist = dist
                        nearestIndex = k
                    }
                }
                
                if (assignments[i] != nearestIndex) {
                    assignments[i] = nearestIndex
                    changed = true
                }
                
                sumsR[nearestIndex] += r.toLong()
                sumsG[nearestIndex] += g.toLong()
                sumsB[nearestIndex] += b.toLong()
                counts[nearestIndex]++
            }
            
            // 3. Update centroids
            for (k in 0 until colorCount) {
                if (counts[k] > 0) {
                    val avgR = (sumsR[k] / counts[k]).toInt()
                    val avgG = (sumsG[k] / counts[k]).toInt()
                    val avgB = (sumsB[k] / counts[k]).toInt()
                    centroids[k] = Color.rgb(avgR, avgG, avgB)
                } else {
                    // Re-init empty cluster
                     centroids[k] = pixels[random.nextInt(pixels.size)]
                }
            }
        }
        
        return centroids
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

    private fun applyMedianFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val newPixels = IntArray(width * height)

        // 3x3 Median Filter
        val window = IntArray(9)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var k = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        window[k++] = pixels[(y + ky) * width + (x + kx)]
                    }
                }
                // Manual Bubble Sort for 9 elements (faster than boxing)
                for (i in 0 until 9) {
                    for (j in 0 until 8 - i) {
                        if (Color.green(window[j]) > Color.green(window[j + 1])) {
                            val temp = window[j]
                            window[j] = window[j + 1]
                            window[j + 1] = temp
                        }
                    }
                }
                newPixels[y * width + x] = window[4]
            }
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun applySobelEdgeDetection(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val newPixels = pixels.clone()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // Horizontal Gradient (Gx)
                val p00 = Color.green(pixels[(y - 1) * width + (x - 1)])
                val p02 = Color.green(pixels[(y - 1) * width + (x + 1)])
                val p10 = Color.green(pixels[y * width + (x - 1)])
                val p12 = Color.green(pixels[y * width + (x + 1)])
                val p20 = Color.green(pixels[(y + 1) * width + (x - 1)])
                val p22 = Color.green(pixels[(y + 1) * width + (x + 1)])

                val gx = (p02 + 2 * p12 + p22) - (p00 + 2 * p10 + p20)

                // Vertical Gradient (Gy)
                val p01 = Color.green(pixels[(y - 1) * width + x])
                val p21 = Color.green(pixels[(y + 1) * width + x])

                val gy = (p20 + 2 * p21 + p22) - (p00 + 2 * p01 + p02)

                val magnitude = Math.sqrt((gx * gx + gy * gy).toDouble()).toInt()

                // Threshold for edge
                if (magnitude > 128) {
                    newPixels[y * width + x] = Color.BLACK
                }
            }
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }
    private fun applyKuwaharaFilter(bitmap: Bitmap, kernelSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val newPixels = IntArray(width * height)
        val radius = kernelSize / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Region 1: (x-r, y-r) to (x, y)
                val stats1 = calcKuwaharaStats(pixels, width, height, x - radius, y - radius, x, y)
                // Region 2: (x, y-r) to (x+r, y)
                val stats2 = calcKuwaharaStats(pixels, width, height, x, y - radius, x + radius, y)
                // Region 3: (x-r, y) to (x, y+r)
                val stats3 = calcKuwaharaStats(pixels, width, height, x - radius, y, x, y + radius)
                // Region 4: (x, y) to (x+r, y+r)
                val stats4 = calcKuwaharaStats(pixels, width, height, x, y, x + radius, y + radius)

                // Find min variance
                var minVar = stats1.first
                var resultColor = stats1.second

                if (stats2.first < minVar) { minVar = stats2.first; resultColor = stats2.second }
                if (stats3.first < minVar) { minVar = stats3.first; resultColor = stats3.second }
                if (stats4.first < minVar) { minVar = stats4.first; resultColor = stats4.second }

                newPixels[y * width + x] = resultColor
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun calcKuwaharaStats(
        pixels: IntArray, 
        width: Int, 
        height: Int, 
        startX: Int, 
        startY: Int, 
        endX: Int, 
        endY: Int
    ): Pair<Double, Int> {
        var count = 0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var sumSqR = 0.0
        var sumSqG = 0.0
        var sumSqB = 0.0

        for (qy in startY..endY) {
            val safeY = qy.coerceIn(0, height - 1)
            for (qx in startX..endX) {
                val safeX = qx.coerceIn(0, width - 1)
                val p = pixels[safeY * width + safeX]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                sumR += r
                sumG += g
                sumB += b
                sumSqR += r * r
                sumSqG += g * g
                sumSqB += b * b
                count++
            }
        }

        if (count == 0) return 0.0 to Color.BLACK

        val meanR = sumR / count
        val meanG = sumG / count
        val meanB = sumB / count

        val meanColor = Color.rgb(meanR.toInt(), meanG.toInt(), meanB.toInt())

        val varR = (sumSqR / count) - (meanR * meanR)
        val varG = (sumSqG / count) - (meanG * meanG)
        val varB = (sumSqB / count) - (meanB * meanB)

        val totalVariance = varR + varG + varB
        return totalVariance to meanColor
    }
}
