package com.example.pixelshift.ui.editor.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class ViewportState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
) {
    var scale by mutableFloatStateOf(initialScale)
    var offsetX by mutableFloatStateOf(initialOffsetX)
    var offsetY by mutableFloatStateOf(initialOffsetY)

    /**
     * Performs an incremental zoom around a centroid.
     * This follows the Affine Transformation principle to keep the point under centroid fixed.
     */
    fun zoom(zoomFactor: Float, centroidX: Float, centroidY: Float, maxScale: Float = 200f) {
        val oldScale = scale
        val newScale = (scale * zoomFactor).coerceIn(0.1f, maxScale)
        
        if (oldScale != newScale) {
            // Affine Logic: newOffset = centroid - (centroid - oldOffset) * (newScale / oldScale)
            val ratio = newScale / oldScale
            offsetX = centroidX - (centroidX - offsetX) * ratio
            offsetY = centroidY - (centroidY - offsetY) * ratio
            scale = newScale
        }
    }

    fun pan(deltaX: Float, deltaY: Float) {
        offsetX += deltaX
        offsetY += deltaY
    }
    
    fun set(scale: Float, offsetX: Float, offsetY: Float) {
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    /**
     * Calculates the transformation matrix for the current viewport state.
     */
    fun getMatrix(): android.graphics.Matrix {
        return android.graphics.Matrix().apply {
            postScale(scale, scale)
            postTranslate(offsetX, offsetY)
        }
    }

    /**
     * Maps a screen coordinate to a bitmap coordinate with zero-drift precision.
     * Uses the inverse of the viewport matrix and strict floor() rounding.
     */
    fun mapScreenToBitmap(screenX: Float, screenY: Float): Pair<Int, Int> {
        val inverse = android.graphics.Matrix()
        getMatrix().invert(inverse)
        
        val pts = floatArrayOf(screenX, screenY)
        inverse.mapPoints(pts)
        
        // Zero-drift: strictly floor the mapped floating point coordinates
        return kotlin.math.floor(pts[0].toDouble()).toInt() to kotlin.math.floor(pts[1].toDouble()).toInt()
    }

    companion object {
        val Saver: Saver<ViewportState, Any> = listSaver(
            save = { listOf(it.scale, it.offsetX, it.offsetY) },
            restore = {
                ViewportState(
                    initialScale = it[0] as Float,
                    initialOffsetX = it[1] as Float,
                    initialOffsetY = it[2] as Float
                )
            }
        )
    }
}

@Composable
fun rememberViewportState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
): ViewportState {
    return rememberSaveable(saver = ViewportState.Saver) {
        ViewportState(initialScale, initialOffsetX, initialOffsetY)
    }
}
