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
import androidx.compose.ui.graphics.Paint
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
        zoomLevel: Float = 8f, // How much to zoom in within the magnifier
        magnifierSize: Int = 120 // Size of the magnifier bubble in dp
) {
    if (!magnifierState.visible) return

    val density = LocalDensity.current
    val magnifierSizePx = with(density) { magnifierSize.dp.toPx() }
    val halfSize = magnifierSizePx / 2f

    // Calculate position
    // We want to show the magnifier *above* the finger.
    // The received x,y are *pixel* coordinates.
    // However, to position the floating window on screen, we need *screen* coordinates of the
    // touch.
    // The current MagnifierState only has pixel coordinates (x,y).
    // This is a problem: we don't know where the pixel (x,y) is on screen without Canvas transform
    // info.

    // Alternative:
    // The Magnifier is best placed inside the PixelCanvas or a Layout that shares the coordinate
    // system?
    // OR we just center it on screen or put it in a fixed corner?
    // "Floating ... aligned with current finger" implies it follows the touch.
    // If we only have pixel coords, we can't position it relative to the finger unless we know the
    // canvas transformation (pan/zoom).

    // Since we don't have that info easily exposed here without refactoring PixelCanvas to share
    // state,
    // let's try a fixed position first (e.g. Top Left or Top Right depending on touch),
    // OR change MagnifierState to include raw touch coordinates?

    // For now, let's just implement the *content* in a Box. The caller (PixelCanvas or Screen)
    // should probably handle placement if possible, or we follow the finger if we can.

    // Actually, onPixelAction receives (x,y). The ViewModel stores it.
    // If we want it to float at the finger, we need the screen position.
    // Let's assume for this iteration we display it at a fixed location (e.g. top-left corner)
    // or we can't "float" it effectively without more data.

    // Wait! The user request says "Floating ... aligned with current finger".
    // I should probably update onPixelAction to optionally take raw screen coordinates?
    // Or just put it in a fixed "Loupe" view if tracking is hard.

    // Better idea: Modify MagnifierState to accept `screenX` and `screenY`?
    // But `onPixelAction` is called from `PixelCanvas` which has the `dragAmount` or `position`.
    // Yes. `PixelCanvas` handles the touch logic.
    // It calls `viewModel.onPixelAction(pixelX, pixelY, ...)`.
    // It *could* also pass `screenX, screenY`.

    // Let's implement the standard content rendering first.

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
            // We want to draw area around (magnifierState.x, magnifierState.y)
            // effective viewport in pixels = magnifierSizePx / zoomLevel (pixels per pixel)
            // e.g. 120 / 8 = 15 pixels wide view

            val viewWidthInPixels = (canvasWidth / zoomLevel).toInt()
            val viewHeightInPixels = (canvasHeight / zoomLevel).toInt()

            val startX = magnifierState.x - viewWidthInPixels / 2
            val startY = magnifierState.y - viewHeightInPixels / 2

            // We need to fetch the *composited* image from ProjectState or ActiveLayer
            // Ideally we see what the user sees (all layers + transparency).
            // But doing full composition here is expensive.
            // Let's iterate pixels and draw rects.

            // Optimized approach:
            // 1. Draw solid background if set
            if (projectState.backgroundColor != Color.Transparent) {
                drawRect(color = projectState.backgroundColor)
            }

            // 2. Iterate visible layers bottom-up
            val clipRect =
                    android.graphics.Rect(
                            startX,
                            startY,
                            startX + viewWidthInPixels + 1,
                            startY + viewHeightInPixels + 1
                    )

            // Use native canvas for pixel drawing flexibility? Or DrawScope rects?
            // DrawScope rects are fine for small grid (15x15 = 225 rects * layers).

            // Reuse logic from ViewModel.getPixelColor?
            // Or just draw layer bitmaps with src/dst rects?
            // DrawBitmap is faster and handles nearest neighbor if paint is set.

            drawContext.canvas.nativeCanvas.apply {
                val paint =
                        android.graphics.Paint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                            isDither = false
                        }

                projectState.layers.asReversed().forEach { layer ->
                    if (layer.isVisible) {
                        // Source rect: The area in the bitmap (startX, startY, ...)
                        // Dest rect: The whole magnifier canvas
                        val src =
                                android.graphics.Rect(
                                        startX,
                                        startY,
                                        startX + viewWidthInPixels,
                                        startY + viewHeightInPixels
                                )

                        // We need to scale this up to fill the canvas
                        val dst =
                                android.graphics.Rect(
                                        0,
                                        0,
                                        canvasWidth.toInt(),
                                        canvasHeight.toInt()
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

            // Draw outline of center pixel?
            drawRect(
                    color = Color.Red,
                    topLeft = Offset((canvasWidth - zoomLevel) / 2, (canvasHeight - zoomLevel) / 2),
                    size = Size(zoomLevel, zoomLevel),
                    style = Stroke(width = 2f)
            )
        }
    }
}
