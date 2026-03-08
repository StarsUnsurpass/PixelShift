package com.example.pixelshift.ui.editor.common

import android.graphics.Bitmap
import kotlin.math.abs

object DrawingAlgorithms {

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
        var err = dx - dy

        while (true) {
            plotBrush(x, y, brushSize, plot)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun plotBrush(cx: Int, cy: Int, size: Int, plot: (x: Int, y: Int) -> Unit) {
        if (size == 1) {
            plot(cx, cy)
            return
        }

        val offset = if (size % 2 != 0) size / 2 else 0
        val startX = cx - offset
        val startY = cy - offset

        for (x in startX until startX + size) {
            for (y in startY until startY + size) {
                plot(x, y)
            }
        }
    }

    /**
     * High-performance Scanline Flood Fill.
     * Uses getPixels/setPixels to minimize JNI overhead and processes data in a pure Java array.
     * Renamed back to scanlineFloodFill for compatibility with ViewModel.
     */
    fun scanlineFloodFill(bitmap: Bitmap, startX: Int, startY: Int, targetColor: Int, replacementColor: Int) {
        if (targetColor == replacementColor) return
        val width = bitmap.width
        val height = bitmap.height
        if (startX !in 0 until width || startY !in 0 until height) return

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        if (pixels[startY * width + startX] != targetColor) return

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
                pixels[cy * width + i] = replacementColor
            }

            // Above
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
            // Below
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
    }

    /**
     * Ultimate Performance Global Replace.
     * O(N) complexity with only 2 JNI calls.
     */
    fun globalReplace(bitmap: Bitmap, targetColor: Int, replacementColor: Int) {
        if (targetColor == replacementColor) return

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var modified = false
        for (i in pixels.indices) {
            if (pixels[i] == targetColor) {
                pixels[i] = replacementColor
                modified = true
            }
        }

        if (modified) {
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

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
            drawLine(left, top, right, top, brushSize, plot)
            drawLine(left, bottom, right, bottom, brushSize, plot)
            drawLine(left, top, left, bottom, brushSize, plot)
            drawLine(right, top, right, bottom, brushSize, plot)
        }
    }

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
        var x = radius
        var y = 0
        var err = 0

        while (x >= y) {
            if (filled) {
                drawLine(x0 - x, y0 + y, x0 + x, y0 + y, 1, plot)
                drawLine(x0 - x, y0 - y, x0 + x, y0 - y, 1, plot)
                drawLine(x0 - y, y0 + x, x0 + y, y0 + x, 1, plot)
                drawLine(x0 - y, y0 - x, x0 + y, y0 - x, 1, plot)
            } else {
                plotBrush(x0 + x, y0 + y, brushSize, plot)
                plotBrush(x0 + y, y0 + x, brushSize, plot)
                plotBrush(x0 - y, y0 + x, brushSize, plot)
                plotBrush(x0 - x, y0 + y, brushSize, plot)
                plotBrush(x0 - x, y0 - y, brushSize, plot)
                plotBrush(x0 - y, y0 - x, brushSize, plot)
                plotBrush(x0 + y, y0 - x, brushSize, plot)
                plotBrush(x0 + x, y0 - y, brushSize, plot)
            }

            if (err <= 0) {
                y += 1
                err += 2 * y + 1
            }
            if (err > 0) {
                x -= 1
                err -= 2 * x + 1
            }
        }
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
