package com.t8rin.imagetoolbox.feature.bitmap_editor.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun BitmapEditorView(
    width: Int,
    height: Int,
    backgroundColor: Color,
    onColorChange: (Int, Int, Color) -> Unit,
    pixelData: Array<Array<Color>>,
    selectedColor: Color
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

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
                    }
                }
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

            val pixelSize = size.width / width
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
        drawGrid(width, height)
    }
}

private fun DrawScope.drawGrid(width: Int, height: Int) {
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
