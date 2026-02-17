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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Paint as ComposePaint
import android.graphics.Paint as NativePaint
import android.graphics.Rect as NativeRect
import android.graphics.RectF as NativeRectF
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import com.example.pixelshift.ui.editor.PixelArtViewModel.MagnifierState
import com.example.pixelshift.ui.editor.common.ProjectState

@Composable
fun Magnifier(
        modifier: Modifier = Modifier,
        magnifierState: MagnifierState,
        projectState: ProjectState,
        brushSize: Int = 1,
        zoomLevel: Float = 16f,
        magnifierSize: Dp = 140.dp
) {
    if (!magnifierState.visible) return

    val density = LocalDensity.current
    val magnifierSizePx = with(density) { magnifierSize.toPx() }

    Box(
            modifier =
                    modifier.size(magnifierSize)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.DarkGray, CircleShape)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw checkered background
            val checkerSize = with(density) { 4.dp.toPx() }
            for (i in 0 until (canvasWidth / checkerSize).toInt() + 1) {
                for (j in 0 until (canvasHeight / checkerSize).toInt() + 1) {
                    val color = if ((i + j) % 2 == 0) Color(0xFFCCCCCC) else Color.White
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
                        NativePaint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                            isDither = false
                        }

                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        val src =
                                NativeRect(
                                        startX,
                                        startY,
                                        startX + viewWidthInPixels,
                                        startY + viewHeightInPixels
                                )

                        val dst =
                                NativeRectF(
                                        0f,
                                        0f,
                                        canvasWidth,
                                        canvasHeight
                                )

                        drawBitmap(layer.bitmap, src, dst, paint)
                    }
                }
            }

            // Draw Precision Crosshair (1px Inverted)
            val cx = canvasWidth / 2f
            val cy = canvasHeight / 2f

            // Use drawIntoCanvas to apply Difference blend mode for inversion
            drawIntoCanvas { canvas ->
                val paint = ComposePaint().apply {
                    color = Color.White
                    blendMode = BlendMode.Difference
                    strokeWidth = with(density) { 1.dp.toPx() }
                    style = PaintingStyle.Stroke
                }
                
                // Horizontal line
                canvas.drawLine(
                    p1 = Offset(0f, cy),
                    p2 = Offset(canvasWidth, cy),
                    paint = paint
                )
                // Vertical line
                canvas.drawLine(
                    p1 = Offset(cx, 0f),
                    p2 = Offset(cx, canvasHeight),
                    paint = paint
                )
            }
            
            // Outer circular guide for the crosshair
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = with(density) { 6.dp.toPx() },
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )
        }
    }
}
