package com.example.pixelshift.ui.creation

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import coil.compose.rememberAsyncImagePainter

@Composable
fun BitmapEditorView(
    width: Int,
    height: Int,
    backgroundColor: Color,
    onColorChange: (Int, Int, Color) -> Unit,
    onDrawLine: (Int, Int, Int, Int, Color) -> Unit,
    onDrawRectangle: (Int, Int, Int, Int, Color) -> Unit,
    onDrawCircle: (Int, Int, Int, Int, Color) -> Unit,
    onSelectRectangle: (Int, Int, Int, Int) -> Unit,
    onMoveSelection: (Int, Int) -> Unit,
    pixelData: Array<Array<Color>>,
    selectedColor: Color,
    selectedTool: String,
    selectedShape: String,
    selectedSelectionMode: String,
    selection: Rect?,
    symmetryMode: String,
    referenceImageUri: Uri?
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var startDrag by remember { mutableStateOf<Offset?>(null) }
    var currentDrag by remember { mutableStateOf<Offset?>(null) }
    val painter = rememberAsyncImagePainter(referenceImageUri)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pixelSize = size.width / width
                    val x = (offset.x / pixelSize).toInt()
                    val y = (offset.y / pixelSize).toInt()
                    if (x in 0 until width && y in 0 until height) {
                        onColorChange(x, y, selectedColor)
                        when (symmetryMode) {
                            "Horizontal" -> onColorChange(width - 1 - x, y, selectedColor)
                            "Vertical" -> onColorChange(x, height - 1 - y, selectedColor)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startDrag = it },
                    onDrag = { change, dragAmount ->
                        currentDrag = change.position
                        change.consume()
                    },
                    onDragEnd = {
                        if (selectedTool == "Shapes") {
                            startDrag?.let { start ->
                                val pixelSize = size.width / width
                                val x1 = (start.x / pixelSize).toInt()
                                val y1 = (start.y / pixelSize).toInt()
                                val x2 = (currentDrag?.x ?: 0f / pixelSize).toInt()
                                val y2 = (currentDrag?.y ?: 0f / pixelSize).toInt()
                                when (selectedShape) {
                                    "Line" -> onDrawLine(x1, y1, x2, y2, selectedColor)
                                    "Rectangle" -> onDrawRectangle(x1, y1, x2, y2, selectedColor)
                                    "Circle" -> onDrawCircle(x1, y1, x2, y2, selectedColor)
                                }
                            }
                        } else if (selectedTool == "Selection" && selectedSelectionMode == "Rectangle") {
                            startDrag?.let { start ->
                                val pixelSize = size.width / width
                                val x1 = (start.x / pixelSize).toInt()
                                val y1 = (start.y / pixelSize).toInt()
                                val x2 = (currentDrag?.x ?: 0f / pixelSize).toInt()
                                val y2 = (currentDrag?.y ?: 0f / pixelSize).toInt()
                                onSelectRectangle(x1, y1, x2, y2)
                            }
                        } else if (selectedTool == "Selection" && selection != null) {
                            startDrag?.let { start ->
                                val pixelSize = size.width / width
                                val dx = ((currentDrag?.x ?: 0f - start.x) / pixelSize).toInt()
                                val dy = ((currentDrag?.y ?: 0f - start.y) / pixelSize).toInt()
                                onMoveSelection(dx, dy)
                            }
                        }
                        startDrag = null
                        currentDrag = null
                    }
                )
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    ) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawColor(backgroundColor.hashCode())

            with(painter) {
                draw(size, alpha = 0.5f)
            }

            val pixelSize = size.width / width
            if (pixelData.isNotEmpty()) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        drawRect(
                            color = pixelData[x][y],
                            topLeft = Offset(x * pixelSize, y * pixelSize),
                            size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize)
                        )
                    }
                }
            }
        }
        drawGrid(width, height)
        selection?.let {
            val pixelSize = size.width / width
            drawRect(
                color = Color.White,
                topLeft = Offset(it.left * pixelSize, it.top * pixelSize),
                size = androidx.compose.ui.geometry.Size(it.width * pixelSize, it.height * pixelSize),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
        }
    }
}

private fun DrawScope.drawGrid(width: Int, height: Int) {
    if (width == 0) return
    val pixelSize = size.width / width
    for (x in 0..width) {
        drawLine(
            color = Color.Gray,
            start = Offset(x * pixelSize, 0f),
            end = Offset(x * pixelSize, size.height)
        )
    }
    for (y in 0..height) {
        drawLine(
            color = Color.Gray,
            start = Offset(0f, y * pixelSize),
            end = Offset(size.width, y * pixelSize)
        )
    }
}
