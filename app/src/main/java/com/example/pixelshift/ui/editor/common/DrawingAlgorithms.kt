package com.example.pixelshift.ui.editor.common

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs

object DrawingAlgorithms {

    fun drawLine(
        x0: Int, y0: Int, x1: Int, y1: Int,
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
        val offset = size / 2
        for (x in cx - offset until cx - offset + size) {
            for (y in cy - offset until cy - offset + size) {
                plot(x, y)
            }
        }
    }

    fun floodFill(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        targetColor: Int,
        replacementColor: Int
    ) {
        if (targetColor == replacementColor) return
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) return
        if (bitmap.getPixel(x, y) != targetColor) return

        val queue: Queue<Pair<Int, Int>> = LinkedList()
        queue.add(x to y)

        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.remove()
            if (cx < 0 || cx >= bitmap.width || cy < 0 || cy >= bitmap.height) continue
            if (bitmap.getPixel(cx, cy) != targetColor) continue

            bitmap.setPixel(cx, cy, replacementColor)

            queue.add(cx + 1 to cy)
            queue.add(cx - 1 to cy)
            queue.add(cx to cy + 1)
            queue.add(cx to cy - 1)
        }
    }

    fun drawRectangle(
        x0: Int, y0: Int, x1: Int, y1: Int,
        brushSize: Int, // Usually 1 for shapes, but could be thicker
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
            for (x in left..right) {
                plotBrush(x, top, brushSize, plot)
                plotBrush(x, bottom, brushSize, plot)
            }
            // Left and Right
            for (y in top..bottom) {
                plotBrush(left, y, brushSize, plot)
                plotBrush(right, y, brushSize, plot)
            }
        }
    }

    fun drawCircle(
        x0: Int, y0: Int, x1: Int, y1: Int,
        brushSize: Int,
        filled: Boolean,
        plot: (x: Int, y: Int) -> Unit
    ) {
        // Simple circle from center (x0, y0) with radius distance to (x1, y1)
        // OR Ellipse fitting the bounding box of (x0, y0) and (x1, y1).
        // Let's implement Circle from center for now as it's easier for "Standard" shape tools.
        // Actually, most editors do Ellipse in bounding box.
        // Bresenham Circle is for integer radius.
        
        // Let's do a simple midpoint circle algorithm for a circle defined by center and radius.
        val radius = kotlin.math.sqrt(((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)).toDouble()).toInt()
        
        var x = radius
        var y = 0
        var err = 0

        while (x >= y) {
            if (filled) {
                // Fill scanlines
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
}
