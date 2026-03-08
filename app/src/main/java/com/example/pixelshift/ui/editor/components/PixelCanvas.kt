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
    onTap: (Int, Int, Float, Float) -> Unit,
    onDragStart: (Int, Int, Float, Float, Boolean) -> Unit,
    onDrag: (Int, Int, Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    brushSize: Int = 1,
    hoverPosition: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val checkSizePx = with(density) { 8.dp.toPx() }
    
    // Ergonomic Max Scale: Ensure 1 pixel is approx 40dp on screen
    val maxScale = remember(density.density) { density.density * 40f }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        var isInitialized by remember(projectState.id) { mutableStateOf(false) }
        val projectWidth = projectState.width
        val projectHeight = projectState.height

        // Strict Gesture State Machine
        var isViewportMode by remember { mutableStateOf(false) }

        LaunchedEffect(projectState.id, screenWidthPx, screenHeightPx) {
            if (!isInitialized && screenWidthPx > 0 && screenHeightPx > 0) {
                val scaleX = (screenWidthPx * 0.8f) / projectWidth
                val scaleY = (screenHeightPx * 0.8f) / projectHeight
                val initialScale = minOf(scaleX, scaleY).coerceAtLeast(1f)
                val initialOffsetX = (screenWidthPx - projectWidth * initialScale) / 2f
                val initialOffsetY = (screenHeightPx - projectHeight * initialScale) / 2f
                viewportState.set(initialScale, initialOffsetX, initialOffsetY)
                isInitialized = true
            }
        }

        Canvas(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                            // Once 2+ fingers are down, lock into Viewport Mode
                            isViewportMode = true
                            
                            // 1. Perform standard viewport transformation
                            viewportState.zoom(zoom, centroid.x, centroid.y, maxScale = maxScale)
                            viewportState.pan(pan.x, pan.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (isViewportMode) return@detectTapGestures
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(offset.x, offset.y)
                                onTap(pixelX, pixelY, offset.x, offset.y)
                            },
                            onLongPress = { offset ->
                                if (isViewportMode) return@detectTapGestures
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(offset.x, offset.y)
                                onDragStart(pixelX, pixelY, offset.x, offset.y, true)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Reset viewport mode on fresh start
                                isViewportMode = false
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(offset.x, offset.y)
                                onDragStart(pixelX, pixelY, offset.x, offset.y, false)
                            },
                            onDrag = { change, _ ->
                                // If we've entered viewport mode (2nd finger dropped), stop drawing immediately
                                if (isViewportMode) return@detectDragGestures
                                
                                val (pixelX, pixelY) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                onDrag(pixelX, pixelY, change.position.x, change.position.y)
                            },
                            onDragEnd = { 
                                onDragEnd()
                                // Re-enable drawing mode only after all fingers are lifted
                                isViewportMode = false
                            },
                            onDragCancel = {
                                onDragEnd()
                                isViewportMode = false
                            }
                        )
                    }
        ) {
            val scale = viewportState.scale
            val offsetX = viewportState.offsetX
            val offsetY = viewportState.offsetY
            
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
                    val horizontalChecks = (size.width / checkSizePx).toInt() + 2
                    val verticalChecks = (size.height / checkSizePx).toInt() + 2
                    for (i in -1..horizontalChecks) {
                        for (j in -1..verticalChecks) {
                            drawRect(
                                color = if ((i + j) % 2 == 0) Color(0xFFE0E0E0) else Color.White,
                                topLeft = Offset(i * checkSizePx, j * checkSizePx),
                                size = Size(checkSizePx, checkSizePx)
                            )
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

            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(offsetX - 1f, offsetY - 1f),
                size = Size(projectWidth * scale + 2f, projectHeight * scale + 2f),
                style = Stroke(width = 1f)
            )

            with(drawContext.canvas.nativeCanvas) {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = false; isFilterBitmap = false; isDither = false
                }

                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        drawBitmap(layer.bitmap as Bitmap, viewportState.getMatrix(), paint)
                    }
                }
            }
            
            projectState.selection?.let { selection ->
                  with(drawContext.canvas.nativeCanvas) {
                      val paint = android.graphics.Paint().apply { isAntiAlias = false; isFilterBitmap = false; isDither = false }
                      val matrix = viewportState.getMatrix().apply { preTranslate(selection.x.toFloat(), selection.y.toFloat()) }
                      drawBitmap(selection.bitmap as Bitmap, matrix, paint)
                  }
                  drawRect(
                      color = Color.Blue,
                      topLeft = Offset(offsetX + selection.x * scale, offsetY + selection.y * scale),
                      size = Size(selection.bitmap.width * scale, selection.bitmap.height * scale),
                      style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                  )
            }
            
             projectState.previewLayer?.let { layer ->
                if (layer.isVisible) {
                    with(drawContext.canvas.nativeCanvas) {
                        val paint = android.graphics.Paint().apply { isAntiAlias = false; isFilterBitmap = false; isDither = false }
                        drawBitmap(layer.bitmap as Bitmap, viewportState.getMatrix(), paint)
                    }
                }
            }
            
            if (scale > 10f) {
                for (x in 0..projectWidth) {
                    val lineX = offsetX + x * scale
                    if (lineX in 0f..size.width) {
                        drawLine(color = Color.Gray.copy(alpha = 0.3f), start = Offset(lineX, offsetY), end = Offset(lineX, offsetY + projectHeight * scale), strokeWidth = 1f)
                    }
                }
                for (y in 0..projectHeight) {
                    val lineY = offsetY + y * scale
                    if (lineY in 0f..size.height) {
                        drawLine(color = Color.Gray.copy(alpha = 0.3f), start = Offset(offsetX, lineY), end = Offset(offsetX + projectWidth * scale, lineY), strokeWidth = 1f)
                    }
                }
            }

            hoverPosition?.let { (hx, hy) ->
                val startX = if (brushSize % 2 != 0) offsetX + (hx - brushSize / 2) * scale else offsetX + hx * scale
                val startY = if (brushSize % 2 != 0) offsetY + (hy - brushSize / 2) * scale else offsetY + hy * scale
                drawRect(color = Color.Gray, topLeft = Offset(startX, startY), size = Size(brushSize * scale, brushSize * scale), style = Stroke(width = 1f))
            }
        }
    }
}
