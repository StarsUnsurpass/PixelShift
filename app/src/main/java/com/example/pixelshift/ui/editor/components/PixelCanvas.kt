package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ProjectState
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.ViewConfiguration
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*

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
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    brushSize: Int = 1,
    hoverPosition: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val touchSlop = remember { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
    val maxScale = remember(density.density) { density.density * 40f }

    val gridLinesBuffer = remember { FloatArray(2000) }
    val gridPaint = remember { 
        Paint().apply {
            isAntiAlias = false; strokeWidth = 0f; style = Paint.Style.STROKE
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        var isInitialized by remember(projectState.id) { mutableStateOf(false) }
        val projectWidth = projectState.width
        val projectHeight = projectState.height

        LaunchedEffect(projectState.id, screenWidthPx, screenHeightPx) {
            if (!isInitialized && screenWidthPx > 0 && screenHeightPx > 0) {
                val scaleX = (screenWidthPx * 0.8f) / projectWidth
                val scaleY = (screenHeightPx * 0.8f) / projectHeight
                val initialScale = minOf(scaleX, scaleY).coerceAtLeast(1f)
                viewportState.set(initialScale, (screenWidthPx - projectWidth * initialScale) / 2f, (screenHeightPx - projectHeight * initialScale) / 2f)
                isInitialized = true
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isCanceled = false
                        var totalDist = 0f
                        var interactionMode = 0 // 0: Idle, 1: Drawing, 2: Viewport, 3: Eyedropper, 4: Multi-Tap Detection

                        // 1. Initial State Monitoring Window (300ms)
                        val longPressResult = withTimeoutOrNull(300L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.size > 1) {
                                    // Multiple fingers down within 300ms
                                    interactionMode = 4
                                    return@withTimeoutOrNull null
                                }
                                val change = event.changes.first()
                                if (change.pressed) {
                                    totalDist += change.positionChange().getDistance()
                                    if (totalDist > touchSlop) {
                                        interactionMode = 1 // Moved, Drawing Mode
                                        return@withTimeoutOrNull null
                                    }
                                } else {
                                    // Released early - Single Tap
                                    val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                    onTap(px, py, change.position.x, change.position.y)
                                    isCanceled = true
                                    return@withTimeoutOrNull null
                                }
                            }
                        }

                        if (!isCanceled) {
                            if (interactionMode == 0 && longPressResult == null) {
                                interactionMode = 3 // Long Press
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPressStart()
                            } else if (interactionMode == 0) {
                                interactionMode = 1
                            }

                            when (interactionMode) {
                                1 -> { // DRAWING
                                    val (px, py) = viewportState.mapScreenToBitmap(down.position.x, down.position.y)
                                    onDragStart(px, py, down.position.x, down.position.y, false)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) { onDragEnd(); break }
                                        val change = event.changes.first()
                                        if (change.pressed) {
                                            val (dx, dy) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                            onDrag(dx, dy, change.position.x, change.position.y)
                                            change.consume()
                                        } else { onDragEnd(); break }
                                    }
                                }
                                3 -> { // EYEDROPPER
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()
                                        if (change.pressed) {
                                            val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                            onDrag(px, py, change.position.x, change.position.y)
                                            change.consume()
                                        } else {
                                            val (px, py) = viewportState.mapScreenToBitmap(change.position.x, change.position.y)
                                            onDrag(px, py, change.position.x, change.position.y)
                                            onLongPressStop()
                                            break
                                        }
                                    }
                                }
                                4 -> { // MULTI-TOUCH (Viewport or Shortcut Tap)
                                    val startTime = System.currentTimeMillis()
                                    var maxFingers = currentEvent.changes.size
                                    var hasMoved = false
                                    
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        maxFingers = max(maxFingers, event.changes.size)
                                        if (event.changes.any { it.positionChange().getDistance() > touchSlop }) hasMoved = true
                                        
                                        if (event.changes.none { it.pressed }) {
                                            // All fingers lifted
                                            val duration = System.currentTimeMillis() - startTime
                                            if (!hasMoved && duration < 300) {
                                                if (maxFingers == 2) onUndo()
                                                else if (maxFingers == 3) onRedo()
                                            }
                                            break
                                        }
                                        
                                        if (hasMoved || System.currentTimeMillis() - startTime > 300) {
                                            // Transition to Viewport Mode (Pan/Zoom)
                                            var prevCentroid = Offset.Unspecified
                                            while (true) {
                                                val innerEvent = if (hasMoved) event else awaitPointerEvent()
                                                hasMoved = true // Ensure we don't re-read the first event forever
                                                if (innerEvent.changes.none { it.pressed }) break
                                                
                                                val active = innerEvent.changes.filter { it.pressed }
                                                val centroid = active.fold(Offset.Zero) { acc, c -> acc + c.position } / active.size.toFloat()
                                                
                                                if (active.size >= 2) {
                                                    viewportState.zoom(innerEvent.calculateZoom(), centroid.x, centroid.y, maxScale = maxScale)
                                                    viewportState.pan(innerEvent.calculatePan().x, innerEvent.calculatePan().y)
                                                } else if (active.size == 1 && prevCentroid.isSpecified) {
                                                    viewportState.pan(centroid.x - prevCentroid.x, centroid.y - prevCentroid.y)
                                                }
                                                prevCentroid = centroid
                                                innerEvent.changes.forEach { it.consume() }
                                                if (active.isEmpty()) break
                                            }
                                            break
                                        }
                                    }
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
                    val checkSize = with(density) { 8.dp.toPx() }
                    val horizontalChecks = (size.width / checkSize).toInt() + 2
                    val verticalChecks = (size.height / checkSize).toInt() + 2
                    for (i in -1..horizontalChecks) {
                        for (j in -1..verticalChecks) {
                            drawRect(color = if ((i + j) % 2 == 0) Color(0xFFE0E0E0) else Color.White, topLeft = Offset(i * checkSize, j * checkSize), size = Size(checkSize, checkSize))
                        }
                    }
                } else {
                    drawRect(color = projectState.backgroundColor, topLeft = Offset(offsetX, offsetY), size = Size(projectWidth * scale, projectHeight * scale))
                }
                canvas.restore()
            }

            drawIntoCanvas { canvas ->
                val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false; isDither = false }
                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        canvas.nativeCanvas.drawBitmap(layer.bitmap as Bitmap, viewportState.getMatrix(), paint)
                    }
                }
                projectState.selection?.let { selection ->
                    val selMatrix = viewportState.getMatrix().apply { preTranslate(selection.x.toFloat(), selection.y.toFloat()) }
                    canvas.nativeCanvas.drawBitmap(selection.bitmap as Bitmap, selMatrix, paint)
                    drawRect(color = Color.Blue, topLeft = Offset(offsetX + selection.x * scale, offsetY + selection.y * scale), size = Size(selection.bitmap.width * scale, selection.bitmap.height * scale), style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                }
                projectState.previewLayer?.let { layer ->
                    if (layer.isVisible) canvas.nativeCanvas.drawBitmap(layer.bitmap as Bitmap, viewportState.getMatrix(), paint)
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
                        gridLinesBuffer[index++] = lineX; gridLinesBuffer[index++] = max(offsetY, 0f); gridLinesBuffer[index++] = lineX; gridLinesBuffer[index++] = min(offsetY + projectHeight * scale, size.height)
                    }
                    for (y in topBitmap..bottomBitmap) {
                        if (index + 4 > gridLinesBuffer.size) break
                        val lineY = offsetY + y * scale
                        gridLinesBuffer[index++] = max(offsetX, 0f); gridLinesBuffer[index++] = lineY; gridLinesBuffer[index++] = min(offsetX + projectWidth * scale, size.width); gridLinesBuffer[index++] = lineY
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
