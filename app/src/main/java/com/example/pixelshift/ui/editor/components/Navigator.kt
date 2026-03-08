package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ProjectState
import com.example.pixelshift.ui.editor.common.ViewportState
import android.graphics.Paint
import android.graphics.Bitmap

@Composable
fun Navigator(
    modifier: Modifier = Modifier,
    projectState: ProjectState,
    viewportState: ViewportState,
    screenSize: Size // We need screen size to calculate the viewport rect
) {
    val navigatorSizeDp = 120.dp
    val navigatorSizePx = with(LocalDensity.current) { navigatorSizeDp.toPx() }
    
    val projectWidth = projectState.width.toFloat()
    val projectHeight = projectState.height.toFloat()
    
    // Calculate scaling to fit project into navigator
    val navScale = min(navigatorSizePx / projectWidth, navigatorSizePx / projectHeight)
    
    val navContentWidth = projectWidth * navScale
    val navContentHeight = projectHeight * navScale
    
    // Centering in navigator
    val navOffsetX = (navigatorSizePx - navContentWidth) / 2f
    val navOffsetY = (navigatorSizePx - navContentHeight) / 2f
    
    Box(
        modifier = modifier
            .size(navigatorSizeDp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .size(navigatorSizeDp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging in navigator pans the main viewport inversely
                        viewportState.pan(-dragAmount.x / navScale * viewportState.scale, -dragAmount.y / navScale * viewportState.scale)
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    isAntiAlias = false
                    isFilterBitmap = true // PROFESSIONAL DOUBLE STANDARD: Reference image and overall preview need filtering
                }
                
                // --- SHARED COMPOSITION RENDERING ---
                val navMatrix = android.graphics.Matrix()
                navMatrix.postScale(navScale, navScale)
                navMatrix.postTranslate(navOffsetX, navOffsetY)
                
                // Draw layers from bottom to top
                val layerList = projectState.layers
                for (i in layerList.indices.reversed()) {
                    val layer = layerList[i]
                    if (layer.isVisible) {
                        paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        canvas.nativeCanvas.drawBitmap(layer.bitmap, navMatrix, paint)
                    }
                }

                // --- VIEWPORT INDICATOR (Inverse Matrix Mapping) ---
                val invMatrix = android.graphics.Matrix()
                viewportState.getMatrix().invert(invMatrix)
                
                // Map the four corners of the screen to bitmap coordinates
                val screenPoints = floatArrayOf(
                    0f, 0f, 
                    screenSize.width, screenSize.height
                )
                invMatrix.mapPoints(screenPoints)
                
                // Transform bitmap coordinates back to navigator coordinates
                val left = screenPoints[0] * navScale + navOffsetX
                val top = screenPoints[1] * navScale + navOffsetY
                val right = screenPoints[2] * navScale + navOffsetX
                val bottom = screenPoints[3] * navScale + navOffsetY
                
                val indicatorPaint = Paint().apply {
                    color = android.graphics.Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.nativeCanvas.drawRect(left, top, right, bottom, indicatorPaint)
            }
        }
    }
}

private fun min(a: Float, b: Float): Float = if (a < b) a else b
