package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ProjectState
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.ViewConfiguration
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
fun PixelCanvas(
    projectState: ProjectState,
    viewportState: com.example.pixelshift.ui.editor.common.ViewportState,
    onTap: (Int, Int, Float, Float) -> Unit,
    onDragStart: (Int, Int, Float, Float, Boolean) -> Unit,
    onDrag: (Int, Int, Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressStop: () -> Unit,
    brushSize: Int = 1,
    hoverPosition: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val touchSlop = remember { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
    
    val checkSizePx = with(density) { 8.dp.toPx() }
    val maxScale = remember(density.density) { density.density * 40f }

    val gridLinesBuffer = remember { FloatArray(2000) }
    val gridPaint = remember { 
        Paint().apply {
            isAntiAlias = false
            strokeWidth = 0f 
            style = Paint.Style.STROKE
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        var isInitialized by remember(projectState.id) { mutableStateOf(false) }
        val projectWidth = projectState.width
        val projectHeight = projectState.height

        var isViewportMode by remember { mutableStateOf(false) }
        var isLongPressMode by remember { mutableStateOf(false) }

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
                            if (isLongPressMode) return@detectTransformGestures
                            isViewportMode = true
                            viewportState.zoom(zoom, centroid.x, centroid.y, maxScale = maxScale)
                            viewportState.pan(pan.x, pan.y)
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var isCanceled = false
                            isViewportMode = false
                            
                            // 1. Long Press Hijacking Window (300ms)
                            val longPressTriggered = withTimeoutOrNull(300L) {
                                var totalDist = 0f
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size > 1) {
                                        isViewportMode = true
                                        isCanceled = true
                                        return@withTimeoutOrNull null
                                    }
                                    val change = event.changes.first()
                                    if (change.pressed) {
                                        totalDist += change.positionChange().getDistance()
                                        if (totalDist > touchSlop) {
                                            isCanceled = true // Moved too much, normal drawing
                                            return@withTimeoutOrNull null
                                        }
                                    } else {
                                        // Released before timeout
                                        val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                        onTap(px, py, change.position.x, change.position.y)
                                        isCanceled = true
                                        return@withTimeoutOrNull null
                                    }
                                }
                            } ?: false // If timeout reached and not canceled

                            if (longPressTriggered == true && !isCanceled) {
                                isLongPressMode = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPressStart()
                                
                                // Drag loop for Eyedropper navigation
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    if (change.pressed) {
                                        val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                        onDrag(px, py, change.position.x, change.position.y)
                                        change.consume()
                                    } else {
                                        val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                        onDrag(px, py, change.position.x, change.position.y) // Final pick
                                        onLongPressStop()
                                        isLongPressMode = false
                                        break
                                    }
                                }
                            } else if (!isCanceled) {
                                // 2. Normal Drawing Path
                                val (px, py) = viewportState.mapScreenToBitmap(down.position.x, down.position.y)
                                onDragStart(px, py, down.position.x, down.position.y, false)
                                
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (isViewportMode || event.changes.size > 1) {
                                        onDragEnd()
                                        break
                                    }
                                    val change = event.changes.first()
                                    if (change.pressed) {
                                        val (dx, dy) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                        onDrag(dx, dy, change.position.x, change.position.y)
                                        change.consume()
                                    } else {
                                        onDragEnd()
                                        break
                                    }
                                }
                            }
                        }
                    }
        ) {
            val scale = viewportState.scale
            val offsetX = viewportState.offsetX
            val offsetY = viewportState.offsetY
            
            drawIntoCanvas { canvas ->
                canvas.save()
                val canvasRect = androidx.compose.ui.geometry.Rect(offsetX, offsetY, offsetX + projectWidth * scale, offsetY + projectHeight * scale)
                canvas.clipRect(canvasRect.left, canvasRect.top, canvasRect.right, canvasRect.bottom)

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
                    drawRect(color = projectState.backgroundColor, topLeft = Offset(offsetX, offsetY), size = Size(projectWidth * scale, projectHeight * scale))
                }
                canvas.restore()
            }

            drawIntoCanvas { canvas ->
                val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false; isDither = false }
                val matrix = viewportState.getMatrix()
                
                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        canvas.nativeCanvas.drawBitmap(layer.bitmap as Bitmap, matrix, paint)
                    }
                }
                
                projectState.selection?.let { selection ->
                    val selMatrix = viewportState.getMatrix().apply { preTranslate(selection.x.toFloat(), selection.y.toFloat()) }
                    canvas.nativeCanvas.drawBitmap(selection.bitmap as Bitmap, selMatrix, paint)
                    drawRect(
                        color = Color.Blue,
                        topLeft = Offset(offsetX + selection.x * scale, offsetY + selection.y * scale),
                        size = Size(selection.bitmap.width * scale, selection.bitmap.height * scale),
                        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }
                
                projectState.previewLayer?.let { layer ->
                    if (layer.isVisible) canvas.nativeCanvas.drawBitmap(layer.bitmap as Bitmap, matrix, paint)
                }
            }

            val gridAlpha = ((scale - 6f) / (10f - 6f)).coerceIn(0f, 1f)
            if (gridAlpha > 0f) {
                drawIntoCanvas { canvas ->
                    gridPaint.color = Color.Gray.copy(alpha = gridAlpha * 0.3f).toArgb()
                    val leftBitmap = floor(max(0f, -offsetX / scale)).toInt()
                    val rightBitmap = ceil(min(projectWidth.toFloat(), (size.width - offsetX) / scale)).toInt()
                    val topBitmap = floor(max(0f, -offsetY / scale)).toInt()
                    val bottomBitmap = ceil(min(projectHeight.toFloat(), (size.height - offsetY) / scale)).toInt()
                    
                    var index = 0
                    for (x in leftBitmap..rightBitmap) {
                        if (index + 4 > gridLinesBuffer.size) break
                        val lineX = offsetX + x * scale
                        gridLinesBuffer[index++] = lineX
                        gridLinesBuffer[index++] = max(offsetY, 0f)
                        gridLinesBuffer[index++] = lineX
                        gridLinesBuffer[index++] = min(offsetY + projectHeight * scale, size.height)
                    }
                    for (y in topBitmap..bottomBitmap) {
                        if (index + 4 > gridLinesBuffer.size) break
                        val lineY = offsetY + y * scale
                        gridLinesBuffer[index++] = max(offsetX, 0f)
                        gridLinesBuffer[index++] = lineY
                        gridLinesBuffer[index++] = min(offsetX + projectWidth * scale, size.width)
                        gridLinesBuffer[index++] = lineY
                    }
                    if (index > 0) canvas.nativeCanvas.drawLines(gridLinesBuffer, 0, index, gridPaint)
                }
            }

            hoverPosition?.let { (hx, hy) ->
                val cursorX = if (brushSize % 2 != 0) offsetX + (hx - brushSize / 2) * scale else offsetX + hx * scale
                val cursorY = if (brushSize % 2 != 0) offsetY + (hy - brushSize / 2) * scale else offsetY + hy * scale
                drawRect(color = Color.Gray, topLeft = Offset(cursorX, cursorY), size = Size(brushSize * scale, brushSize * scale), style = Stroke(width = 1f))
            }
            drawRect(color = Color.DarkGray, topLeft = Offset(offsetX - 1f, offsetY - 1f), size = Size(projectWidth * scale + 2f, projectHeight * scale + 2f), style = Stroke(width = 1f))
        }
    }
}
