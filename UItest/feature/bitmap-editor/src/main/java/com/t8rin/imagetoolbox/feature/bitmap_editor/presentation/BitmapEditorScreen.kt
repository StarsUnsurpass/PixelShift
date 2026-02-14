package com.t8rin.imagetoolbox.feature.bitmap_editor.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitmapEditorScreen(
    component: BitmapEditorComponent
) {
    var showEditor by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf("32") }
    var height by remember { mutableStateOf("32") }
    var backgroundType by remember { mutableStateOf("Transparent") }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }
    var selectedTool by remember { mutableStateOf("Pencil") }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedShape by remember { mutableStateOf("Line") }
    var selectedSelectionMode by remember { mutableStateOf("Rectangle") }
    var selection by remember { mutableStateOf<Rect?>(null) }
    var copiedPixels by remember { mutableStateOf<Array<Array<Color>>?>(null) }

    val pixelData = remember {
        mutableStateOf(Array(0) { Array(0) { Color.Transparent } })
    }

    if (showEditor) {
        Column {
            BitmapEditorView(
                width = width.toIntOrNull() ?: 32,
                height = height.toIntOrNull() ?: 32,
                backgroundColor = backgroundColor,
                onColorChange = { x, y, color ->
                    if (selectedTool == "Bucket") {
                        floodFill(x, y, color, pixelData.value) {
                            pixelData.value = it
                        }
                    } else if (selectedTool == "Selection" && selectedSelectionMode == "MagicWand") {
                        selection = magicWandSelection(x, y, pixelData.value)
                    } else {
                        val newPixelData = pixelData.value.copyOf()
                        newPixelData[x][y] = if (selectedTool == "Eraser") backgroundColor else color
                        pixelData.value = newPixelData
                    }
                },
                onDrawLine = { x1, y1, x2, y2, color ->
                    val newPixelData = pixelData.value.copyOf()
                    drawLine(x1, y1, x2, y2, color, newPixelData)
                    pixelData.value = newPixelData
                },
                onDrawRectangle = { x1, y1, x2, y2, color ->
                    val newPixelData = pixelData.value.copyOf()
                    drawRectangle(x1, y1, x2, y2, color, newPixelData)
                    pixelData.value = newPixelData
                },
                onDrawCircle = { x1, y1, x2, y2, color ->
                    val newPixelData = pixelData.value.copyOf()
                    drawCircle(x1, y1, x2, y2, color, newPixelData)
                    pixelData.value = newPixelData
                },
                onSelectRectangle = { x1, y1, x2, y2 ->
                    selection = Rect(minOf(x1, x2).toFloat(), minOf(y1, y2).toFloat(), maxOf(x1, x2).toFloat(), maxOf(y1, y2).toFloat())
                },
                pixelData = pixelData.value,
                selectedColor = selectedColor,
                selectedTool = selectedTool,
                selectedShape = selectedShape,
                selectedSelectionMode = selectedSelectionMode
            )
            if (selectedTool == "Shapes") {
                ShapesToolbar(
                    selectedShape = selectedShape,
                    onShapeChange = { selectedShape = it }
                )
            }
            if (selectedTool == "Selection") {
                SelectionToolbar(
                    selectedSelectionMode = selectedSelectionMode,
                    onSelectionModeChange = { selectedSelectionMode = it },
                    onCopy = {
                        selection?.let {
                            val newCopiedPixels = Array(it.width.toInt()) { Array(it.height.toInt()) { Color.Transparent } }
                            for (x in 0 until it.width.toInt()) {
                                for (y in 0 until it.height.toInt()) {
                                    newCopiedPixels[x][y] = pixelData.value[it.left.toInt() + x][it.top.toInt() + y]
                                }
                            }
                            copiedPixels = newCopiedPixels
                        }
                    },
                    onPaste = {
                        copiedPixels?.let { copied ->
                            selection?.let {
                                val newPixelData = pixelData.value.copyOf()
                                for (x in 0 until copied.size) {
                                    for (y in 0 until copied[0].size) {
                                        newPixelData[it.left.toInt() + x][it.top.toInt() + y] = copied[x][y]
                                    }
                                }
                                pixelData.value = newPixelData
                            }
                        }
                    }
                )
            }
            Toolbox(
                selectedTool = selectedTool,
                onToolChange = { selectedTool = it }
            )
            ColorPalette(
                selectedColor = selectedColor,
                onColorChange = { selectedColor = it }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Canvas Setup", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            // Custom Size
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Width") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Presets
            Text("Presets", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row {
                Button(onClick = { width = "16"; height = "16" }) { Text("16x16") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { width = "32"; height = "32" }) { Text("32x32") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { width = "64"; height = "64" }) { Text("64x64") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { width = "128"; height = "128" }) { Text("128x128") }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Background
            Text("Background", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = backgroundType == "Transparent",
                    onClick = { backgroundType = "Transparent" }
                )
                Text("Transparent")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = backgroundType == "Solid",
                    onClick = { backgroundType = "Solid" }
                )
                Text("Solid Color")
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                backgroundColor = if (backgroundType == "Transparent") {
                    Color.Transparent
                } else {
                    Color.White // TODO: Add a color picker
                }
                pixelData.value = Array(width.toIntOrNull() ?: 32) { Array(height.toIntOrNull() ?: 32) { backgroundColor } }
                showEditor = true
            }) {
                Text("Create")
            }
        }
    }
}

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
    pixelData: Array<Array<Color>>,
    selectedColor: Color,
    selectedTool: String,
    selectedShape: String,
    selectedSelectionMode: String
) {
    val pixelSize = 20.dp
    var startPosition by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        startPosition = it
                    },
                    onDragEnd = {
                        startPosition?.let { start ->
                            val end = startPosition!!
                            val x1 = (start.x / pixelSize.toPx()).toInt()
                            val y1 = (start.y / pixelSize.toPx()).toInt()
                            val x2 = (end.x / pixelSize.toPx()).toInt()
                            val y2 = (end.y / pixelSize.toPx()).toInt()
                            when (selectedTool) {
                                "Shapes" -> {
                                    when (selectedShape) {
                                        "Line" -> onDrawLine(x1, y1, x2, y2, selectedColor)
                                        "Rectangle" -> onDrawRectangle(x1, y1, x2, y2, selectedColor)
                                        "Circle" -> onDrawCircle(x1, y1, x2, y2, selectedColor)
                                    }
                                }
                                "Selection" -> {
                                    if (selectedSelectionMode == "Rectangle") {
                                        onSelectRectangle(x1, y1, x2, y2)
                                    }
                                }
                                else -> {
                                    val x = (end.x / pixelSize.toPx()).toInt()
                                    val y = (end.y / pixelSize.toPx()).toInt()
                                    if (x in 0 until width && y in 0 until height) {
                                        onColorChange(x, y, selectedColor)
                                    }
                                }
                            }
                        }
                        startPosition = null
                    }
                ) { change, _ ->
                    startPosition = change.position
                }
            }
    ) {
        drawRect(
            color = backgroundColor,
            size = Size(width * pixelSize.toPx(), height * pixelSize.toPx())
        )

        for (x in 0 until width) {
            for (y in 0 until height) {
                drawRect(
                    color = pixelData[x][y],
                    topLeft = Offset(x * pixelSize.toPx(), y * pixelSize.toPx()),
                    size = Size(pixelSize.toPx(), pixelSize.toPx())
                )
            }
        }

        // Draw grid
        for (x in 0..width) {
            drawLine(
                color = Color.Gray,
                start = Offset(x * pixelSize.toPx(), 0f),
                end = Offset(x * pixelSize.toPx(), height * pixelSize.toPx())
            )
        }
        for (y in 0..height) {
            drawLine(
                color = Color.Gray,
                start = Offset(0f, y * pixelSize.toPx()),
                end = Offset(width * pixelSize.toPx(), y * pixelSize.toPx())
            )
        }
    }
}

@Composable
fun Toolbox(
    selectedTool: String,
    onToolChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { onToolChange("Pencil") },
            enabled = selectedTool != "Pencil"
        ) {
            Icon(Icons.Rounded.Brush, contentDescription = "Pencil")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onToolChange("Eraser") },
            enabled = selectedTool != "Eraser"
        ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Eraser")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onToolChange("Eyedropper") },
            enabled = selectedTool != "Eyedropper"
        ) {
            Icon(Icons.Rounded.Colorize, contentDescription = "Eyedropper")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onToolChange("Bucket") },
            enabled = selectedTool != "Bucket"
        ) {
            Icon(Icons.Rounded.InvertColors, contentDescription = "Bucket")
        }
    }
}

@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorChange: (Color) -> Unit
) {
    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta
    )
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        colors.forEach { color ->
            Card(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp)
                    .clickable { onColorChange(color) },
                shape = CircleShape,
                border = if (selectedColor == color) BorderStroke(2.dp, Color.Black) else null,
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

fun floodFill(
    x: Int,
    y: Int,
    newColor: Color,
    pixelData: Array<Array<Color>>,
    onPixelDataChange: (Array<Array<Color>>) -> Unit
) {
    val oldColor = pixelData[x][y]
    if (oldColor == newColor) return

    val newPixelData = pixelData.copyOf()
    val queue = mutableListOf<Pair<Int, Int>>()
    queue.add(Pair(x, y))

    while (queue.isNotEmpty()) {
        val (px, py) = queue.removeAt(0)
        if (px >= 0 && px < newPixelData.size && py >= 0 && py < newPixelData[0].size && newPixelData[px][py] == oldColor) {
            newPixelData[px][py] = newColor
            queue.add(Pair(px + 1, py))
            queue.add(Pair(px - 1, py))
            queue.add(Pair(px, py + 1))
            queue.add(Pair(px, py - 1))
        }
    }
    onPixelDataChange(newPixelData)
}


fun drawLine(
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    color: Color,
    pixelData: Array<Array<Color>>
) {
    var x = x1
    var y = y1
    val dx = abs(x2 - x1)
    val dy = abs(y2 - y1)
    val sx = if (x1 < x2) 1 else -1
    val sy = if (y1 < y2) 1 else -1
    var err = dx - dy

    while (true) {
        if (x >= 0 && x < pixelData.size && y >= 0 && y < pixelData[0].size) {
            pixelData[x][y] = color
        }
        if (x == x2 && y == y2) break
        val e2 = 2 * err
        if (e2 > -dy) {
            err -= dy
            x += sx
        }
        if (e2 < dx) {
            err += dx
            y += sy
        }
    }
}

fun drawRectangle(
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    color: Color,
    pixelData: Array<Array<Color>>
) {
    for (x in minOf(x1, x2)..maxOf(x1, x2)) {
        for (y in minOf(y1, y2)..maxOf(y1, y2)) {
            if (x >= 0 && x < pixelData.size && y >= 0 && y < pixelData[0].size) {
                pixelData[x][y] = color
            }
        }
    }
}

fun drawCircle(
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    color: Color,
    pixelData: Array<Array<Color>>
) {
    val radius = sqrt(abs(x2 - x1).toDouble().pow(2) + abs(y2 - y1).toDouble().pow(2))
    val centerX = x1
    val centerY = y1

    for (x in 0 until pixelData.size) {
        for (y in 0 until pixelData[0].size) {
            val distance = sqrt((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
            if (distance <= radius) {
                pixelData[x][y] = color
            }
        }
    }
}

fun magicWandSelection(
    x: Int,
    y: Int,
    pixelData: Array<Array<Color>>
): Rect {
    val oldColor = pixelData[x][y]
    val visited = Array(pixelData.size) { BooleanArray(pixelData[0].size) }
    val queue = mutableListOf<Pair<Int, Int>>()
    queue.add(Pair(x, y))

    var minX = x
    var minY = y
    var maxX = x
    var maxY = y

    while (queue.isNotEmpty()) {
        val (px, py) = queue.removeAt(0)
        if (px >= 0 && px < pixelData.size && py >= 0 && py < pixelData[0].size && !visited[px][py] && pixelData[px][py] == oldColor) {
            visited[px][py] = true
            minX = minOf(minX, px)
            minY = minOf(minY, py)
            maxX = maxOf(maxX, px)
            maxY = maxOf(maxY, py)
            queue.add(Pair(px + 1, py))
            queue.add(Pair(px - 1, py))
            queue.add(Pair(px, py + 1))
            queue.add(Pair(px, py - 1))
        }
    }
    return Rect(minX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat())
}

@Composable
fun ShapesToolbar(
    selectedShape: String,
    onShapeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { onShapeChange("Line") },
            enabled = selectedShape != "Line"
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Line")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onShapeChange("Rectangle") },
            enabled = selectedShape != "Rectangle"
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Rectangle")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onShapeChange("Circle") },
            enabled = selectedShape != "Circle"
        ) {
            Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = "Circle")
        }
    }
}

@Composable
fun SelectionToolbar(
    selectedSelectionMode: String,
    onSelectionModeChange: (String) -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { onSelectionModeChange("Rectangle") },
            enabled = selectedSelectionMode != "Rectangle"
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Rectangle")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onSelectionModeChange("MagicWand") },
            enabled = selectedSelectionMode != "MagicWand"
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Magic Wand")
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onCopy) {
            Icon(Icons.Rounded.Remove, contentDescription = "Copy")
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onPaste) {
            Icon(Icons.Rounded.Remove, contentDescription = "Paste")
        }
    }
}
