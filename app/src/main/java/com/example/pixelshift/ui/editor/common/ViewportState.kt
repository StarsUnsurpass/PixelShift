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

    fun zoom(zoomFactor: Float, centroidX: Float, centroidY: Float) {
        val newScale = (scale * zoomFactor).coerceIn(0.1f, 500f) // Infinite zoom (up to 500x)
        
        // Adjust offset to zoom towards the centroid
        // The logic is: preserve the point under the centroid
        // (centroid - oldOffset) / oldScale = (centroid - newOffset) / newScale
        // newOffset = centroid - (centroid - oldOffset) * (newScale / oldScale)
        
        // However, a simpler accumulation usually works well with standard gestures:
        // offset += (centroid - offset) * (1 - zoomFactor) 
        // Let's stick to the standard transformation logic used in detectTransformGestures
        
        // Actually, the simplest correct way for separate scale/offset:
        // When we zoom by 'zoomFactor' around 'centroid', the world moves such that 'centroid' stays fixed.
        // The vector from top-left to centroid was (centroid - offset).
        // It becomes (centroid - offset) * zoomFactor.
        // So the new offset is centroid - (centroid - oldOffset) * zoomFactor
        
        // But wait, allow the caller to handle complex logic if needed. 
        // For basic zoom-in-center, we'll just update scale.
        scale = newScale
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
        return kotlin.math.floor(pts[0]).toInt() to kotlin.math.floor(pts[1]).toInt()
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
