package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.PixelArtViewModel.MagnifierState
import com.example.pixelshift.ui.editor.common.ProjectState

@Composable
fun Magnifier(
        modifier: Modifier = Modifier,
        magnifierState: MagnifierState,
        projectState: ProjectState,
        brushSize: Int = 1,
        zoomLevel: Float = 8f, // How much to zoom in within the magnifier
        magnifierSize: Int = 120 // Size of the magnifier bubble in dp
) {
    if (!magnifierState.visible) return

    val density = LocalDensity.current
    val magnifierSizePx = with(density) { magnifierSize.dp.toPx() }

    Box(
            modifier =
                    modifier.size(magnifierSize.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.Gray, CircleShape)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw checkered background
            val checkerSize = 10f
            for (i in 0 until (canvasWidth / checkerSize).toInt() + 1) {
                for (j in 0 until (canvasHeight / checkerSize).toInt() + 1) {
                    val color = if ((i + j) % 2 == 0) Color.LightGray else Color.White
                    drawRect(
                            color = color,
                            topLeft = Offset(i * checkerSize, j * checkerSize),
                            size = Size(checkerSize, checkerSize)
                    )
                }
            }

            // Draw pixels
            if (projectState.backgroundColor != Color.Transparent) {
                drawRect(color = projectState.backgroundColor)
            }

            val viewWidthInPixels = (canvasWidth / zoomLevel).toInt()
            val viewHeightInPixels = (canvasHeight / zoomLevel).toInt()

            val startX = magnifierState.x - viewWidthInPixels / 2
            val startY = magnifierState.y - viewHeightInPixels / 2

             with(drawContext.canvas.nativeCanvas) {
                val paint =
                        android.graphics.Paint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                            isDither = false
                        }

                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        val src =
                                android.graphics.Rect(
                                        startX,
                                        startY,
                                        startX + viewWidthInPixels,
                                        startY + viewHeightInPixels
                                )

                        val dst =
                                android.graphics.RectF(
                                        0f,
                                        0f,
                                        canvasWidth,
                                        canvasHeight
                                )

                        drawBitmap(layer.bitmap, src, dst, paint)
                    }
                }
            }

            // Draw Crosshair
            val crosshairSize = 20f
            val cx = canvasWidth / 2
            val cy = canvasHeight / 2

            drawLine(
                    color = Color.Red,
                    start = Offset(cx - crosshairSize, cy),
                    end = Offset(cx + crosshairSize, cy),
                    strokeWidth = 2f
            )
            drawLine(
                    color = Color.Red,
                    start = Offset(cx, cy - crosshairSize),
                    end = Offset(cx, cy + crosshairSize),
                    strokeWidth = 2f
            )

            // Draw outline of brush area
            val contourStartX: Float
            val contourStartY: Float

            if (brushSize % 2 != 0) {
                // Odd: Center
                val offset = brushSize / 2
                contourStartX = (canvasWidth - zoomLevel) / 2 - offset * zoomLevel
                contourStartY = (canvasHeight - zoomLevel) / 2 - offset * zoomLevel
            } else {
                // Even: Top-Left (Center pixel is the top-left of the brush)
                contourStartX = (canvasWidth - zoomLevel) / 2
                contourStartY = (canvasHeight - zoomLevel) / 2
            }

            drawRect(
                    color = Color.Red,
                    topLeft = Offset(contourStartX, contourStartY),
                    size = Size(brushSize * zoomLevel, brushSize * zoomLevel),
                    style = Stroke(width = 2f)
            )
        }
    }
}
