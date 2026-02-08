package com.example.pixelshift.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.checkerboardBackground(
        blockSize: Dp = 20.dp,
        color1: Color = Color.LightGray,
        color2: Color = Color.White
): Modifier {
    return this.drawBehind {
        val blockSizePx = blockSize.toPx()
        val columns = (size.width / blockSizePx).toInt() + 1
        val rows = (size.height / blockSizePx).toInt() + 1

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val color = if ((row + col) % 2 == 0) color1 else color2
                drawRect(
                        color = color,
                        topLeft = Offset(col * blockSizePx, row * blockSizePx),
                        size = Size(blockSizePx, blockSizePx)
                )
            }
        }
    }
}
