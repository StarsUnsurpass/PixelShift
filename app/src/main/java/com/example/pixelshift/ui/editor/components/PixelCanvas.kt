package com.example.pixelshift.ui.editor.components

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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ProjectState
import com.example.pixelshift.ui.editor.common.PixelLayer
import com.example.pixelshift.ui.editor.common.SelectionState
import android.graphics.Bitmap
import kotlin.math.floor

@Composable
fun PixelCanvas(
    projectState: ProjectState,
    viewportState: com.example.pixelshift.ui.editor.common.ViewportState,
    onTap: (Int, Int) -> Unit,
    onDragStart: (Int, Int) -> Unit,
    onDrag: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    brushSize: Int = 1,
    hoverPosition: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val checkSizePx = with(density) { 8.dp.toPx() }

    // We use BoxWithConstraints to get the available screen area for initial centering
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        val screenHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }

        // Reset initialization state when project ID changes
        var isInitialized by remember(projectState.id) { mutableStateOf(false) }

        val projectWidth = projectState.width
        val projectHeight = projectState.height

        // Initial centering/scaling logic
        LaunchedEffect(projectState.id, screenWidthPx, screenHeightPx) {
            if (!isInitialized && screenWidthPx > 0 && screenHeightPx > 0) {
                // Calculate initial scale to fit 80% of screen
                val scaleX = (screenWidthPx * 0.8f) / projectWidth
                val scaleY = (screenHeightPx * 0.8f) / projectHeight
                val initialScale = minOf(scaleX, scaleY).coerceAtLeast(1f)

                // Calculate initial offsets to center
                val contentWidth = projectWidth * initialScale
                val contentHeight = projectHeight * initialScale
                val initialOffsetX = (screenWidthPx - contentWidth) / 2f
                val initialOffsetY = (screenHeightPx - contentHeight) / 2f

                viewportState.set(initialScale, initialOffsetX, initialOffsetY)
                isInitialized = true
            }
        }

        Canvas(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = viewportState.scale
                            val newScale = (oldScale * zoom).coerceIn(0.1f, 500f)
                            
                            // Adjust offset to keep centroid fixed during zoom
                            val cx = centroid.x
                            val cy = centroid.y
                            val ox = viewportState.offsetX
                            val oy = viewportState.offsetY
                            
                            val nx = cx - (cx - ox) * (newScale / oldScale)
                            val ny = cy - (cy - oy) * (newScale / oldScale)
                            
                            viewportState.set(newScale, nx + pan.x, ny + pan.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val scale = viewportState.scale
                            val offsetX = viewportState.offsetX
                            val offsetY = viewportState.offsetY
                            
                            val (pixelX, pixelY) = viewportState.mapScreenToBitmap(offset.x, offset.y)

                            if (pixelX in 0 until projectWidth && pixelY in 0 until projectHeight) {
                                onTap(pixelX, pixelY)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val scale = viewportState.scale
                                val offsetX = viewportState.offsetX
                                val offsetY = viewportState.offsetY
                                
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(offset.x, offset.y)
                                onDragStart(pixelX, pixelY)
                            },
                            onDrag = { change, _ ->
                                val scale = viewportState.scale
                                val offsetX = viewportState.offsetX
                                val offsetY = viewportState.offsetY
                                
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                onDrag(pixelX, pixelY)
                            },
                            onDragEnd = { onDragEnd() }
                        )
                    }
        ) {
            val scale = viewportState.scale
            val offsetX = viewportState.offsetX
            val offsetY = viewportState.offsetY
            
            // 1. Draw Background
            val canvasRect = androidx.compose.ui.geometry.Rect(
                offsetX,
                offsetY,
                offsetX + projectWidth * scale,
                offsetY + projectHeight * scale
            )

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipRect(
                    left = canvasRect.left,
                    top = canvasRect.top,
                    right = canvasRect.right,
                    bottom = canvasRect.bottom
                )

                if (projectState.backgroundColor == Color.Transparent) {
                    // Stable Checkerboard: Fixed 8dp squares that don't scale
                    // Using pre-calculated checkSizePx to avoid Composable context error
                    
                    // We draw covering the viewport but clipped to project bounds
                    val horizontalChecks = (size.width / checkSizePx).toInt() + 2
                    val verticalChecks = (size.height / checkSizePx).toInt() + 2
                    
                    for (i in -1..horizontalChecks) {
                        for (j in -1..verticalChecks) {
                            if ((i + j) % 2 == 0) {
                                drawRect(
                                    color = Color(0xFFE0E0E0), // Light gray
                                    topLeft = Offset(i * checkSizePx, j * checkSizePx),
                                    size = Size(checkSizePx, checkSizePx)
                                )
                            } else {
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(i * checkSizePx, j * checkSizePx),
                                    size = Size(checkSizePx, checkSizePx)
                                )
                            }
                        }
                    }
                } else {
                    drawRect(
                        color = projectState.backgroundColor,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(projectWidth * scale, projectHeight * scale)
                    )
                }
                canvas.restore()
            }

            // Draw Border
            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(offsetX - 1f, offsetY - 1f),
                size = Size(projectWidth * scale + 2f, projectHeight * scale + 2f),
                style = Stroke(width = 1f)
            )

            // 2. Draw Layers
            with(drawContext.canvas.nativeCanvas) {
                // Reuse a single Paint instance
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = false
                    isFilterBitmap = false
                    isDither = false
                }

                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        val bitmap = layer.bitmap as android.graphics.Bitmap
                        
                        // Professional Secret: Use a single Matrix for the entire canvas draw
                        // rather than per-bitmap Rect calculations if possible.
                        // For now, we use the matrix to ensure zero-drift consistency.
                        val matrix = viewportState.getMatrix()
                        drawBitmap(bitmap, matrix, paint)
                    }
                }
            }
            
             // 2.5 Draw Floating Selection
            projectState.selection?.let { selection ->
                  with(drawContext.canvas.nativeCanvas) {
                      val paint = android.graphics.Paint().apply {
                          isAntiAlias = false
                          isFilterBitmap = false
                          isDither = false
                      }
                      val bitmap = selection.bitmap as android.graphics.Bitmap
                      val matrix = viewportState.getMatrix().apply {
                          preTranslate(selection.x.toFloat(), selection.y.toFloat())
                      }
                      drawBitmap(bitmap, matrix, paint)
                  }
                 
                 // Draw selection border
                  drawRect(
                      color = Color.Blue,
                      topLeft = Offset(offsetX + selection.x * scale, offsetY + selection.y * scale),
                      size = Size(selection.bitmap.width * scale, selection.bitmap.height * scale),
                      style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                  )
            }
            
            // 3. Draw Preview Layer
             projectState.previewLayer?.let { layer ->
                with(drawContext.canvas.nativeCanvas) {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = false
                        isFilterBitmap = false
                        isDither = false
                    }
                    val bitmap = layer.bitmap as android.graphics.Bitmap
                    drawBitmap(bitmap, viewportState.getMatrix(), paint)
                }
            }
            
            // 4. Adaptive Grid
            if (scale > 10f) {
                // Draw vertical lines
                for (x in 0..projectWidth) {
                    val lineX = offsetX + x * scale
                    if (lineX in 0f..size.width) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(lineX, offsetY),
                            end = Offset(lineX, offsetY + projectHeight * scale),
                            strokeWidth = 1f
                        )
                    }
                }
                
                // Draw horizontal lines
                for (y in 0..projectHeight) {
                    val lineY = offsetY + y * scale
                    if (lineY in 0f..size.height) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(offsetX, lineY),
                            end = Offset(offsetX + projectWidth * scale, lineY),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // 5. Brush Ghost Cursor
            hoverPosition?.let { (hx, hy) ->
                val startX: Float
                val startY: Float

                if (brushSize % 2 != 0) {
                    // Odd: Center
                    val offset = brushSize / 2
                    startX = offsetX + (hx - offset) * scale
                    startY = offsetY + (hy - offset) * scale
                } else {
                    // Even: Top-Left
                    startX = offsetX + hx * scale
                    startY = offsetY + hy * scale
                }

                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(startX, startY),
                    size = Size(brushSize * scale, brushSize * scale),
                    style = Stroke(width = 1f)
                )
            }
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
