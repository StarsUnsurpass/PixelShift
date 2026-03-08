package com.example.pixelshift.ui.editor.common

import android.graphics.Bitmap
import kotlin.math.abs
import java.util.Arrays

object DrawingAlgorithms {

    data class PixelChange(val x: Int, val y: Int, val oldColor: Int, val newColor: Int)

    /**
     * Standard Bresenham Line Algorithm.
     * Pure integer arithmetic (no division, no floats) using Grid Decision.
     * Ensures perfect 8-connectivity for gap-less lines.
     */
    fun drawLine(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            brushSize: Int,
            plot: (x: Int, y: Int) -> Unit
    ) {
        var x = x0
        var y = y0
        
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        
        if (dx >= dy) {
            var p = (dy shl 1) - dx
            val inc2dy = dy shl 1
            val inc2dydx = (dy - dx) shl 1
            
            for (i in 0..dx) {
                plotBrush(x, y, brushSize, plot)
                if (p < 0) {
                    p += inc2dy
                } else {
                    y += sy
                    p += inc2dydx
                }
                x += sx
            }
        } else {
            var p = (dx shl 1) - dy
            val inc2dx = dx shl 1
            val inc2dxdy = (dx - dy) shl 1
            
            for (i in 0..dy) {
                plotBrush(x, y, brushSize, plot)
                if (p < 0) {
                    p += inc2dx
                } else {
                    x += sx
                    p += inc2dxdy
                }
                y += sy
            }
        }
    }

    private fun plotBrush(cx: Int, cy: Int, size: Int, plot: (x: Int, y: Int) -> Unit) {
        if (size <= 1) {
            plot(cx, cy)
            return
        }

        val offset = size / 2
        val startX = if (size % 2 != 0) cx - offset else cx
        val startY = if (size % 2 != 0) cy - offset else cy

        for (x in startX until startX + size) {
            for (y in startY until startY + size) {
                plot(x, y)
            }
        }
    }

    /**
     * Midpoint Circle Algorithm with 8-way symmetry.
     * Supports both hollow and solid (scanline-optimized) circles.
     */
    fun drawCircle(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        brushSize: Int,
        filled: Boolean,
        plot: (x: Int, y: Int) -> Unit
    ) {
        val radius = kotlin.math.sqrt(((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)).toDouble()).toInt()
        if (radius == 0) {
            plotBrush(x0, y0, brushSize, plot)
            return
        }

        var x = 0
        var y = radius
        var p = 1 - radius

        while (x <= y) {
            if (filled) {
                drawHorizontalLine(x0 - x, x0 + x, y0 + y, plot)
                drawHorizontalLine(x0 - x, x0 + x, y0 - y, plot)
                drawHorizontalLine(x0 - y, x0 + y, y0 + x, plot)
                drawHorizontalLine(x0 - y, x0 + y, y0 - x, plot)
            } else {
                plotBrush(x0 + x, y0 + y, brushSize, plot)
                plotBrush(x0 - x, y0 + y, brushSize, plot)
                plotBrush(x0 + x, y0 - y, brushSize, plot)
                plotBrush(x0 - x, y0 - y, brushSize, plot)
                plotBrush(x0 + y, y0 + x, brushSize, plot)
                plotBrush(x0 - y, y0 + x, brushSize, plot)
                plotBrush(x0 + y, y0 - x, brushSize, plot)
                plotBrush(x0 - y, y0 - x, brushSize, plot)
            }

            x++
            if (p < 0) {
                p += (x shl 1) + 1
            } else {
                y--
                p += (x shl 1) - (y shl 1) + 1
            }
        }
    }

    private fun drawHorizontalLine(startX: Int, endX: Int, y: Int, plot: (x: Int, y: Int) -> Unit) {
        for (x in startX..endX) {
            plot(x, y)
        }
    }

    fun scanlineFloodFill(
        bitmap: Bitmap, 
        startX: Int, 
        startY: Int, 
        targetColor: Int, 
        replacementColor: Int
    ): List<PixelChange> {
        if (targetColor == replacementColor) return emptyList()
        val width = bitmap.width
        val height = bitmap.height
        if (startX !in 0 until width || startY !in 0 until height) return emptyList()

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        if (pixels[startY * width + startX] != targetColor) return emptyList()

        val changes = mutableListOf<PixelChange>()
        val xStack = IntArray(width * height / 2)
        val yStack = IntArray(width * height / 2)
        var top = 0

        xStack[top] = startX
        yStack[top] = startY
        top++

        while (top > 0) {
            top--
            val cx = xStack[top]
            val cy = yStack[top]

            var lx = cx
            while (lx > 0 && pixels[cy * width + (lx - 1)] == targetColor) {
                lx--
            }

            var rx = cx
            while (rx < width - 1 && pixels[cy * width + (rx + 1)] == targetColor) {
                rx++
            }

            for (i in lx..rx) {
                val idx = cy * width + i
                changes.add(PixelChange(i, cy, pixels[idx], replacementColor))
                pixels[idx] = replacementColor
            }

            if (cy > 0) {
                var i = lx
                while (i <= rx) {
                    if (pixels[(cy - 1) * width + i] == targetColor) {
                        xStack[top] = i
                        yStack[top] = cy - 1
                        top++
                        while (i <= rx && pixels[(cy - 1) * width + i] == targetColor) i++
                    } else i++
                }
            }
            if (cy < height - 1) {
                var i = lx
                while (i <= rx) {
                    if (pixels[(cy + 1) * width + i] == targetColor) {
                        xStack[top] = i
                        yStack[top] = cy + 1
                        top++
                        while (i <= rx && pixels[(cy + 1) * width + i] == targetColor) i++
                    } else i++
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return changes
    }

    fun globalReplace(
        bitmap: Bitmap, 
        targetColor: Int, 
        replacementColor: Int
    ): List<PixelChange> {
        if (targetColor == replacementColor) return emptyList()

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val changes = mutableListOf<PixelChange>()
        var modified = false
        for (i in pixels.indices) {
            if (pixels[i] == targetColor) {
                changes.add(PixelChange(i % width, i / width, pixels[i], replacementColor))
                pixels[i] = replacementColor
                modified = true
            }
        }

        if (modified) {
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        return changes
    }

    /**
     * Perfect Loop Rectangle drawing.
     * Ensures each corner pixel is drawn exactly once to avoid alpha overlap artifacts.
     */
    fun drawRectangle(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            brushSize: Int,
            filled: Boolean,
            plot: (x: Int, y: Int) -> Unit
    ) {
        val left = minOf(x0, x1)
        val right = maxOf(x0, x1)
        val top = minOf(y0, y1)
        val bottom = maxOf(y0, y1)

        if (filled) {
            for (y in top..bottom) {
                for (x in left..right) {
                    plot(x, y)
                }
            }
        } else {
            // "The Perfect Loop" implementation
            // 1. Top Edge: [left, right - 1]
            for (x in left until right) plotBrush(x, top, brushSize, plot)
            // 2. Right Edge: [top, bottom - 1]
            for (y in top until bottom) plotBrush(right, y, brushSize, plot)
            // 3. Bottom Edge: [right, left + 1] (stepping down)
            for (x in right downTo left + 1) plotBrush(x, bottom, brushSize, plot)
            // 4. Left Edge: [bottom, top + 1] (stepping up)
            for (y in bottom downTo top + 1) plotBrush(left, y, brushSize, plot)
            
            // Handle 1x1 rectangle case
            if (left == right && top == bottom) plotBrush(left, top, brushSize, plot)
        }
    }

    /**
     * SIMD-optimized memory batch filling for rectangles.
     * Uses getPixels/setPixels and Arrays.fill() for maximum throughput.
     */
    fun fillRectangle(bitmap: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val width = bitmap.width
        val height = bitmap.height
        
        val left = minOf(x0, x1).coerceIn(0, width - 1)
        val right = maxOf(x0, x1).coerceIn(0, width - 1)
        val top = minOf(y0, y1).coerceIn(0, height - 1)
        val bottom = maxOf(y0, y1).coerceIn(0, height - 1)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in top..bottom) {
            val startIndex = y * width + left
            val endIndex = y * width + right
            Arrays.fill(pixels, startIndex, endIndex + 1, color)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    fun createRectMask(width: Int, height: Int, rect: android.graphics.Rect): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = android.graphics.Canvas(mask)
        val paint =
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = false
                }
        canvas.drawRect(rect, paint)
        return mask
    }

    fun createMagicWandMask(bitmap: Bitmap, startX: Int, startY: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

        if (startX < 0 || startX >= width || startY < 0 || startY >= height) return mask

        val targetColor = bitmap.getPixel(startX, startY)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val maskPixels = ByteArray(width * height)

        val xStack = IntArray(width * height / 2)
        val yStack = IntArray(width * height / 2)
        var top = 0

        xStack[top] = startX
        yStack[top] = startY
        top++

        while (top > 0) {
            top--
            val cx = xStack[top]
            val cy = yStack[top]

            var lx = cx
            while (lx > 0 && pixels[cy * width + (lx - 1)] == targetColor && maskPixels[cy * width + (lx - 1)] == 0.toByte()) lx--

            var rx = cx
            while (rx < width - 1 && pixels[cy * width + (rx + 1)] == targetColor && maskPixels[cy * width + (rx + 1)] == 0.toByte()) rx++

            for (i in lx..rx) {
                maskPixels[cy * width + i] = 255.toByte()
            }

            val checkRow = { rowY: Int ->
                var i = lx
                while (i <= rx) {
                    if (pixels[rowY * width + i] == targetColor && maskPixels[rowY * width + i] == 0.toByte()) {
                        xStack[top] = i
                        yStack[top] = rowY
                        top++
                        while (i <= rx && pixels[rowY * width + i] == targetColor) i++
                    } else i++
                }
            }

            if (cy > 0) checkRow(cy - 1)
            if (cy < height - 1) checkRow(cy + 1)
        }

        val buffer = java.nio.ByteBuffer.wrap(maskPixels)
        mask.copyPixelsFromBuffer(buffer)
        return mask
    }

    fun extractSelection(source: Bitmap, mask: Bitmap): Pair<Bitmap, android.graphics.Rect>? {
        val width = source.width
        val height = source.height
        
        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        
        val maskPixels = ByteArray(width * height)
        val maskBuffer = java.nio.ByteBuffer.wrap(maskPixels)
        mask.copyPixelsToBuffer(maskBuffer)

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var found = false

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                if (maskPixels[offset + x] != 0.toByte()) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    found = true
                }
            }
        }

        if (!found) return null

        val rect = android.graphics.Rect(minX, minY, maxX + 1, maxY + 1)
        val sw = rect.width()
        val sh = rect.height()
        val floating = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val floatPixels = IntArray(sw * sh)

        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val idx = (minY + y) * width + (minX + x)
                if (maskPixels[idx] != 0.toByte()) {
                    floatPixels[y * sw + x] = srcPixels[idx]
                    srcPixels[idx] = android.graphics.Color.TRANSPARENT
                }
            }
        }

        floating.setPixels(floatPixels, 0, sw, 0, 0, sw, sh)
        source.setPixels(srcPixels, 0, width, 0, 0, width, height)

        return floating to rect
    }

    fun mergeSelection(source: Bitmap, selectionBitmap: Bitmap, startX: Int, startY: Int) {
        val width = source.width
        val height = source.height
        val sw = selectionBitmap.width
        val sh = selectionBitmap.height

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val selPixels = IntArray(sw * sh)
        selectionBitmap.getPixels(selPixels, 0, sw, 0, 0, sw, sh)

        for (y in 0 until sh) {
            val dy = startY + y
            if (dy !in 0 until height) continue
            for (x in 0 until sw) {
                val dx = startX + x
                if (dx !in 0 until width) continue
                
                val selCol = selPixels[y * sw + x]
                if ((selCol ushr 24) > 0) {
                    srcPixels[dy * width + dx] = selCol
                }
            }
        }
        source.setPixels(srcPixels, 0, width, 0, 0, width, height)
    }

    fun rotateBitmap90(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = android.graphics.Matrix()
        if (horizontal) matrix.postScale(-1f, 1f) else matrix.postScale(1f, -1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }
}
