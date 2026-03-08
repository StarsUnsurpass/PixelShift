/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2025 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposePath
import java.util.BitSet
import java.util.LinkedList
import java.util.Queue
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Path as ComposePath

internal class FloodFill(image: Bitmap) {
    private val path = Path()

    private val width: Int = image.width
    private val height: Int = image.height
    private val pixels: IntArray = IntArray(width * height)

    private var tolerance = 0

    private var startColorRed = 0
    private var startColorGreen = 0
    private var startColorBlue = 0
    private var startColorAlpha = 0

    init {
        image.getPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Optimized Scanline Flood Fill algorithm.
     * Reduces stack/queue usage by processing horizontal segments.
     */
    fun performFloodFill(
        x: Int,
        y: Int,
        tolerance: Float
    ): Path? {
        if (x !in 0 until width || y !in 0 until height) return null

        path.rewind()
        this.tolerance = (tolerance * 255).roundToInt().coerceIn(0, 255)

        val startPixel = pixels[y * width + x]
        startColorAlpha = Color.alpha(startPixel)
        startColorRed = Color.red(startPixel)
        startColorGreen = Color.green(startPixel)
        startColorBlue = Color.blue(startPixel)

        val visited = BitSet(width * height)
        val queue: Queue<FloodFillRange> = LinkedList()

        // Initial scanline
        findRangeAndPush(x, y, visited, queue)

        while (queue.isNotEmpty()) {
            val range = queue.poll()!!
            val currY = range.Y

            // Scan above and below
            if (currY > 0) scanRow(range.startX, range.endX, currY - 1, visited, queue)
            if (currY < height - 1) scanRow(range.startX, range.endX, currY + 1, visited, queue)
        }

        return path
    }

    /**
     * Global Color Replace algorithm.
     * Iterates through all pixels and adds matching segments to the Path.
     */
    fun performGlobalReplace(
        x: Int,
        y: Int,
        tolerance: Float
    ): Path? {
        if (x !in 0 until width || y !in 0 until height) return null

        path.rewind()
        this.tolerance = (tolerance * 255).roundToInt().coerceIn(0, 255)

        val startPixel = pixels[y * width + x]
        startColorAlpha = Color.alpha(startPixel)
        startColorRed = Color.red(startPixel)
        startColorGreen = Color.green(startPixel)
        startColorBlue = Color.blue(startPixel)

        for (currY in 0 until height) {
            var currX = 0
            while (currX < width) {
                val idx = currY * width + currX
                if (isPixelColorWithinTolerance(idx)) {
                    val startX = currX
                    while (currX < width && isPixelColorWithinTolerance(currY * width + currX)) {
                        currX++
                    }
                    // Add segment to path
                    addSegmentToPath(startX, currX - 1, currY)
                } else {
                    currX++
                }
            }
        }

        return path
    }

    private fun scanRow(
        startX: Int,
        endX: Int,
        y: Int,
        visited: BitSet,
        queue: Queue<FloodFillRange>
    ) {
        var currX = startX
        while (currX <= endX) {
            val idx = y * width + currX
            if (!visited.get(idx) && isPixelColorWithinTolerance(idx)) {
                // Found a new segment, expand it and push to queue
                findRangeAndPush(currX, y, visited, queue)
                // Skip the rest of this segment in the current row scan
                // The findRangeAndPush will have marked it all as visited or we can find its end
                // To be safe and efficient, we find where this segment ends in the current row's bounds
                while (currX <= endX && isPixelColorWithinTolerance(y * width + currX)) {
                    currX++
                }
            } else {
                currX++
            }
        }
    }

    private fun findRangeAndPush(
        x: Int,
        y: Int,
        visited: BitSet,
        queue: Queue<FloodFillRange>
    ) {
        var left = x
        var idx = y * width + left
        while (left >= 0 && !visited.get(idx) && isPixelColorWithinTolerance(idx)) {
            visited.set(idx)
            left--
            idx--
        }
        left++

        var right = x + 1
        idx = y * width + right
        while (right < width && !visited.get(idx) && isPixelColorWithinTolerance(idx)) {
            visited.set(idx)
            right++
            idx++
        }
        right--

        addSegmentToPath(left, right, y)
        queue.offer(FloodFillRange(left, right, y))
    }

    private fun addSegmentToPath(startX: Int, endX: Int, y: Int) {
        // We use 0.5 offset to target the center of the pixel
        // Stroke width should be 1px
        path.moveTo(startX.toFloat(), y.toFloat() + 0.5f)
        path.lineTo(endX.toFloat() + 1f, y.toFloat() + 0.5f)
    }

    private fun isPixelColorWithinTolerance(px: Int): Boolean {
        val color = pixels[px]
        val alpha = color ushr 24 and 0xff
        val red = color ushr 16 and 0xff
        val green = color ushr 8 and 0xff
        val blue = color and 0xff

        if (tolerance == 0) {
            return alpha == startColorAlpha && red == startColorRed && green == startColorGreen && blue == startColorBlue
        }

        return alpha >= startColorAlpha - tolerance && alpha <= startColorAlpha + tolerance &&
                red >= startColorRed - tolerance && red <= startColorRed + tolerance &&
                green >= startColorGreen - tolerance && green <= startColorGreen + tolerance &&
                blue >= startColorBlue - tolerance && blue <= startColorBlue + tolerance
    }

    private inner class FloodFillRange(val startX: Int, val endX: Int, val Y: Int)
}

fun ImageBitmap.floodFill(
    offset: Offset,
    tolerance: Float
): ComposePath? = FloodFill(
    asAndroidBitmap().let {
        if (it.config != Bitmap.Config.ARGB_8888) {
            it.copy(Bitmap.Config.ARGB_8888, false)
        } else it
    }
).performFloodFill(
    x = offset.x.roundToInt(),
    y = offset.y.roundToInt(),
    tolerance = tolerance
)?.asComposePath()

fun ImageBitmap.globalReplace(
    offset: Offset,
    tolerance: Float
): ComposePath? = FloodFill(
    asAndroidBitmap().let {
        if (it.config != Bitmap.Config.ARGB_8888) {
            it.copy(Bitmap.Config.ARGB_8888, false)
        } else it
    }
).performGlobalReplace(
    x = offset.x.roundToInt(),
    y = offset.y.roundToInt(),
    tolerance = tolerance
)?.asComposePath()
