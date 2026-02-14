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
            modifier =
                    modifier.fillMaxSize()
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
                                            onDragStart(
                                                    floor(canvasX).toInt(),
                                                    floor(canvasY).toInt()
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            val canvasX = (change.position.x - offsetX) / scale
                                            val canvasY = (change.position.y - offsetY) / scale
                                            onDrag(floor(canvasX).toInt(), floor(canvasY).toInt())
                                        },
                                        onDragEnd = { onDragEnd() }
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
            // Draw the project area background
            val bgColor = projectState.backgroundColor

            if (bgColor == Color.Transparent) {
                // Draw Checkerboard
                val gridSize = 10f * scale // Size of checker squares
                // Simplified checkerboard: just use a light gray/white pattern
                // We can draw a large rect of white, then draw gray squares?
                // Or just draw the rects.
                // For performance on large canvas, infinite scrolling checkerboard is better done
                // with a Shader.
                // But for now, let's just fill with white and draw gray rects IF strictly needed.
                // ACTUALLY: The user complained about "white background" effectively being blank?
                // "Empty background".

                // Let's draw a distinct checkerboard.
                drawRect(
                        color = Color.White,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(projectWidth * scale, projectHeight * scale)
                )

                val checkerSize = 20f
                // This loop might be heavy for very large canvas if we loop per checker.
                // But typically screen size is limited.
                // We should loop over the VIEWPORT, not the whole project if possible.
                // But the project is small (pixel art).

                // Better: A shader. But let's stick to simple Compose primitives for now.
                // Let's just draw a Gray rectangle for the whole thing if transparent, to
                // distinguish from the "App Background".
                // Or a border.

                drawRect(
                        color = Color.LightGray,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(projectWidth * scale, projectHeight * scale),
                        style = Stroke(width = 2f)
                )

                // If the user wants a checkerboard, we can add it later.
                // For now, a border helps show where the canvas IS.
            } else {
                drawRect(
                        color = bgColor,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(projectWidth * scale, projectHeight * scale)
                )
            }
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
                    val paint =
                            Paint().asFrameworkPaint().apply {
                                isAntiAlias = false
                                isFilterBitmap = false
                            }

                    canvas.nativeCanvas.drawBitmap(layer.bitmap, 0f, 0f, paint)

                    canvas.restore()
                }
            }
        }

        // 2.5 Draw Floating Selection
        projectState.selection?.let { selection ->
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)

                val paint =
                        Paint().asFrameworkPaint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                        }

                canvas.nativeCanvas.drawBitmap(
                        selection.bitmap,
                        selection.x.toFloat(),
                        selection.y.toFloat(),
                        paint
                )

                // Optional: Draw border/marching ants here.
                // For now, the floating content is visible on top.

                canvas.restore()
            }
        }

        // 3. Draw Preview Layer
        projectState.previewLayer?.let { layer ->
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)

                val paint =
                        Paint().asFrameworkPaint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                        }

                canvas.nativeCanvas.drawBitmap(layer.bitmap, 0f, 0f, paint)

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
