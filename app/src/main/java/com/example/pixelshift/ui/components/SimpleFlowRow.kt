package com.example.pixelshift.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A simple FlowRow implementation that places items in rows, wrapping to the next line if there is
 * not enough space. This is a lightweight alternative to
 * `androidx.compose.foundation.layout.FlowRow` to avoid ABI compatibility issues across Compose
 * versions.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param horizontalGap The horizontal gap between items.
 * @param verticalGap The vertical gap between rows.
 * @param content The content of the layout.
 */
@Composable
fun SimpleFlowRow(
        modifier: Modifier = Modifier,
        horizontalGap: Dp = 0.dp,
        verticalGap: Dp = 0.dp,
        content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val horizontalGapPx = horizontalGap.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        val placeables = measurables.map { it.measure(constraints) }

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
            }
            if (currentRow.isNotEmpty()) currentRowWidth += horizontalGapPx
            currentRow.add(placeable)
            currentRowWidth += placeable.width
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val height =
                rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 } +
                        (rows.size - 1).coerceAtLeast(0) * verticalGapPx

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + horizontalGapPx
                }
                y += rowHeight + verticalGapPx
            }
        }
    }
}
