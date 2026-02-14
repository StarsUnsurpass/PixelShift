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

        // Odd sizes (1, 3, 5...): Center at (cx, cy)
        // Even sizes (2, 4...): Top-Left at (cx, cy)

        val startX: Int
        val startY: Int

        if (size % 2 != 0) {
            // Odd: Center
            val offset = size / 2
            startX = cx - offset
            startY = cy - offset
        } else {
            // Even: Top-Left (no offset, cx/cy is the top-left pixel)
            // Wait, if users taps (10,10) with 2px brush.
            // They expect (10,10), (11,10), (10,11), (11,11) ?
            // Yes, that's standard Top-Left anchoring.
            startX = cx
            startY = cy

            // NOTE: Some users might prefer Ceil/Floor centering for even numbers.
            // But "Top-Left" is explicit in requirements.
        }

        for (x in startX until startX + size) {
            for (y in startY until startY + size) {
                plot(x, y)
            }
        }
    }

    // Use Stack-based Scanline Flood Fill to avoid recursion and conform to project requirements.
    // Based on standard scanline flood fill algorithm.
    fun scanlineFloodFill(bitmap: Bitmap, x: Int, y: Int, targetColor: Int, replacementColor: Int) {
        if (targetColor == replacementColor) return
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) return
        if (bitmap.getPixel(x, y) != targetColor) return

        val stack = java.util.Stack<Pair<Int, Int>>()
        stack.push(x to y)

        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.pop()

            // Move left to find start of scanline
            var lx = cx
            while (lx >= 0 && bitmap.getPixel(lx, cy) == targetColor) {
                lx--
            }
            lx++ // Back to first valid pixel

            // Move right to find end of scanline
            var rx = cx
            while (rx < bitmap.width && bitmap.getPixel(rx, cy) == targetColor) {
                rx++
            }
            rx-- // Back to last valid pixel

            // Fill the scanline
            for (i in lx..rx) {
                bitmap.setPixel(i, cy, replacementColor)
            }

            // Check lines above and below
            scanLine(lx, rx, cy - 1, targetColor, bitmap, stack)
            scanLine(lx, rx, cy + 1, targetColor, bitmap, stack)
        }
    }

    private fun scanLine(
            lx: Int,
            rx: Int,
            y: Int,
            targetColor: Int,
            bitmap: Bitmap,
            stack: java.util.Stack<Pair<Int, Int>>
    ) {
        if (y < 0 || y >= bitmap.height) return

        var i = lx
        while (i <= rx) {
            // Skip non-target pixels
            while (i <= rx && bitmap.getPixel(i, y) != targetColor) {
                i++
            }

            if (i <= rx) {
                // Found a segment of targetColor
                stack.push(i to y)
                // Skip the rest of this segment to avoid re-adding
                while (i <= rx && bitmap.getPixel(i, y) == targetColor) {
                    i++
                }
            }
        }
    }

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
            // Top and Bottom
            // For stroke with brushSize > 1, this needs careful handling to avoid overlap/gaps
            // Simpler approach: Draw 4 lines
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
        // Radius based on distance
        val radius =
                kotlin.math.sqrt(((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)).toDouble()).toInt()

        var x = radius
        var y = 0
        var err = 0

        while (x >= y) {
            if (filled) {
                // Scanline fill
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

    // --- Selection Algorithms ---

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

        val maskPixels = ByteArray(width * height) // 0 for transparent, 255 for opaque (selected)

        // Use stack-based scanline fill logic to generating mask
        val stack = java.util.Stack<Pair<Int, Int>>()
        stack.push(startX to startY)

        // Helper to check if pixel is target and not yet visited
        fun isTarget(x: Int, y: Int): Boolean {
            if (x < 0 || x >= width || y < 0 || y >= height) return false
            if (maskPixels[y * width + x] != 0.toByte()) return false // Already visited/selected
            return pixels[y * width + x] == targetColor
        }

        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.pop()

            var lx = cx
            while (isTarget(lx - 1, cy)) lx--

            var rx = cx
            while (isTarget(rx + 1, cy)) rx++

            for (x in lx..rx) {
                maskPixels[cy * width + x] = 255.toByte() // Mark as selected
            }

            // Check lines above and below
            val checkLine = { y: Int ->
                if (y in 0 until height) {
                    var x = lx
                    while (x <= rx) {
                        if (isTarget(x, y)) {
                            stack.push(x to y)
                            while (x <= rx && isTarget(x, y)) x++ // Skip filled
                        } else {
                            x++
                        }
                    }
                }
            }
            checkLine(cy - 1)
            checkLine(cy + 1)
        }

        // Copy byteArray to Bitmap. ALPHA_8 expects bytes.
        // Actually ALPHA_8 wraps a byte buffer. But setPixels isn't directly compatible with
        // ALPHA_8 in standard way easily without Buffer.
        // Easier approach: Create ARGB_8888 and convert, or just use Canvas/Points.
        // For performance, let's just loop setPixel on a mutable bitmap or use ByteBuffer.
        // ByteBuffer is best for ALPHA_8.
        val buffer = java.nio.ByteBuffer.wrap(maskPixels)
        mask.copyPixelsFromBuffer(buffer)

        return mask
    }

    // Cut pixels from source based on mask, returning the floating bitmap (cropped to bounds)
    // and modifying source (clearing pixels)
    fun extractSelection(source: Bitmap, mask: Bitmap): Pair<Bitmap, android.graphics.Rect>? {
        val width = source.width
        val height = source.height

        // 1. Calculate bounds of the mask
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var found = false

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Check if mask has value (alpha > 0)
                if (mask.getPixel(x, y) ushr 24 > 0) { // Check alpha channel
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
        val selectionWidth = rect.width()
        val selectionHeight = rect.height()

        val floatingBitmap =
                Bitmap.createBitmap(selectionWidth, selectionHeight, Bitmap.Config.ARGB_8888)

        for (y in 0 until selectionHeight) {
            for (x in 0 until selectionWidth) {
                val sysX = minX + x
                val sysY = minY + y

                // If mask is set at this position in world space
                if (mask.getPixel(sysX, sysY) ushr 24 > 0) {
                    val color = source.getPixel(sysX, sysY)
                    floatingBitmap.setPixel(x, y, color)
                    source.setPixel(sysX, sysY, android.graphics.Color.TRANSPARENT) // Cut
                }
            }
        }

        return floatingBitmap to rect
    }

    fun mergeSelection(source: Bitmap, selectionBitmap: Bitmap, x: Int, y: Int) {
        val width = source.width
        val height = source.height
        val selW = selectionBitmap.width
        val selH = selectionBitmap.height

        for (sy in 0 until selH) {
            for (sx in 0 until selW) {
                val dx = x + sx
                val dy = y + sy

                if (dx in 0 until width && dy in 0 until height) {
                    val selColor = selectionBitmap.getPixel(sx, sy)
                    if ((selColor ushr 24) > 0) { // If selection pixel is not transparent
                        source.setPixel(dx, dy, selColor)
                    }
                }
            }
        }
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
