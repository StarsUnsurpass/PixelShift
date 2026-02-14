package com.example.pixelshift.ui.editor.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pixelshift.ui.editor.common.ProjectState
import kotlin.math.floor

@Composable
fun PixelCanvas(
    projectState: ProjectState,
    onTap: (x: Int, y: Int) -> Unit,
    onDragStart: (x: Int, y: Int) -> Unit,
    onDrag: (x: Int, y: Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val projectWidth = projectState.width
    val projectHeight = projectState.height

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 50f)
                    // Adjust offset logic to keep focus or at least not jump wildly
                    // Simplified for now
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Transform touch coordinates to pixel coordinates
                    // (offset - translation) / scale
                    val canvasX = (offset.x - offsetX) / scale
                    val canvasY = (offset.y - offsetY) / scale
                    
                    val pixelX = floor(canvasX).toInt()
                    val pixelY = floor(canvasY).toInt()
                    
                    onTap(pixelX, pixelY)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                         val canvasX = (offset.x - offsetX) / scale
                         val canvasY = (offset.y - offsetY) / scale
                         onDragStart(floor(canvasX).toInt(), floor(canvasY).toInt())
                    },
                    onDrag = { change, _ -> 
                         val canvasX = (change.position.x - offsetX) / scale
                         val canvasY = (change.position.y - offsetY) / scale
                         onDrag(floor(canvasX).toInt(), floor(canvasY).toInt())
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
    ) {
        // Draw Checkerboard Background
        // Optimization: Draw one big rect with a checkerboard shader or just looping?
        // For small canvases, looping is fine. For large, maybe shader.
        // Let's do a simple optimization: draw a solid color first if specified.
        
        // 1. Draw Canvas Background
         drawIntoCanvas { canvas ->
             // If transparent, we should draw checkerboard. 
             // Implementing a simple checkerboard pattern for the active area
             val pixelSize = scale // 1 project pixel = 'scale' screen pixels
             
             // Draw the project area background
             drawRect(
                 color = Color.White, // Placeholder for checkerboard
                 topLeft = Offset(offsetX, offsetY),
                 size = Size(projectWidth * scale, projectHeight * scale)
             )
             
             // TODO: Real checkerboard
         }

        // 2. Draw Layers
        projectState.layers.forEach { layer ->
            if (layer.isVisible) {
                // Draw bitmap
                // We need to scale it up.
                // Using drawImage with dstSize or scaling the canvas?
                // Scaling the canvas context is easier.
                
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(offsetX, offsetY)
                    canvas.scale(scale, scale)
                    
                    // Important: Nearest Neighbor for crisp pixels!
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = false
                        isFilterBitmap = false
                    }
                    
                    canvas.nativeCanvas.drawBitmap(
                        layer.bitmap,
                        0f, 0f,
                        paint
                    )
                    
                    canvas.restore()
                }
            }
        }
        
        // 3. Draw Preview Layer
        projectState.previewLayer?.let { layer ->
             drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(offsetX, offsetY)
                    canvas.scale(scale, scale)
                    
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = false
                        isFilterBitmap = false
                    }
                    
                    canvas.nativeCanvas.drawBitmap(
                        layer.bitmap,
                        0f, 0f,
                        paint
                    )
                    
                    canvas.restore()
             }
        }
        
        // 4. Draw Grid (if zoomed in)
        if (scale > 10f) { // Threshold for grid
             drawGrid(
                 offsetX = offsetX,
                 offsetY = offsetY,
                 scale = scale,
                 rows = projectHeight,
                 cols = projectWidth,
                 color = Color.Gray.copy(alpha = 0.3f)
             )
        }
    }
}

private fun DrawScope.drawGrid(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    rows: Int,
    cols: Int,
    color: Color
) {
    // Vertical lines
    for (c in 0..cols) {
        val x = offsetX + c * scale
        drawLine(
            color = color,
            start = Offset(x, offsetY),
            end = Offset(x, offsetY + rows * scale),
            strokeWidth = 1f
        )
    }
    // Horizontal lines
    for (r in 0..rows) {
        val y = offsetY + r * scale
        drawLine(
            color = color,
            start = Offset(offsetX, y),
            end = Offset(offsetX + cols * scale, y),
            strokeWidth = 1f
        )
    }
}
