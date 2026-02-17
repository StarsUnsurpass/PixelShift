package com.example.pixelshift.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import com.example.pixelshift.ui.editor.common.DrawingAlgorithms
import com.example.pixelshift.ui.editor.common.PixelLayer
import com.example.pixelshift.ui.editor.common.ProjectState
import com.example.pixelshift.ui.editor.common.Tool
import com.example.pixelshift.ui.editor.common.ToolSettings
import java.util.Stack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PixelArtViewModel : ViewModel() {

    private val _projectState = MutableStateFlow<ProjectState?>(null)
    val projectState: StateFlow<ProjectState?> = _projectState.asStateFlow()

    private val _currentTool = MutableStateFlow(Tool.PENCIL)
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    private val _toolSettings = MutableStateFlow(ToolSettings())
    val toolSettings: StateFlow<ToolSettings> = _toolSettings.asStateFlow()

    private val _palette =
            MutableStateFlow(
                    listOf(
                            Color.Black,
                            Color.White,
                            Color.Red,
                            Color.Green,
                            Color.Blue,
                            Color.Yellow,
                            Color.Cyan,
                            Color.Magenta
                    )
            )
    val palette: StateFlow<List<Color>> = _palette.asStateFlow()

    private val _currentColor = MutableStateFlow(Color.Black)
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private val _secondaryColor = MutableStateFlow(Color.White) // Background/Secondary color
    val secondaryColor: StateFlow<Color> = _secondaryColor.asStateFlow()

    // Undo/Redo Stacks (Simplified for now - storing full bitmaps)
    // In a real app, we'd store diffs or commands to save memory.
    private val undoStack = Stack<List<PixelLayer>>()
    private val redoStack = Stack<List<PixelLayer>>()

    // Pixel Perfect Tracking
    private data class PixelNode(val x: Int, val y: Int, val oldColor: Int)
    private val currentStrokePath = java.util.LinkedList<PixelNode>()

    private val _hoverPosition = MutableStateFlow<Pair<Int, Int>?>(null)
    val hoverPosition: StateFlow<Pair<Int, Int>?> = _hoverPosition.asStateFlow()

    // Magnifier State
    data class MagnifierState(
            val visible: Boolean = false,
            val x: Int = 0,
            val y: Int = 0, // Pixel coordinates
            val targetColor: Color = Color.Transparent
    )
    private val _magnifierState = MutableStateFlow(MagnifierState())
    val magnifierState: StateFlow<MagnifierState> = _magnifierState.asStateFlow()

    private var lastX: Int? = null
    private var lastY: Int? = null

    fun initializeProject(width: Int, height: Int, isTransparent: Boolean, backgroundColor: Color) {
        // Layer 0: Background (Locked)
        // If solid, fill with color. If transparent, it's transparent.
        val bgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (!isTransparent) {
            bgBitmap.eraseColor(backgroundColor.toArgb())
        }
        val bgLayer =
                PixelLayer(
                        name = "Layer 0",
                        bitmap = bgBitmap,
                        isLocked = true
                )

        // Layer 1: Draft (Active, Transparent)
        val draftBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val draftLayer =
                PixelLayer(
                        name = "Layer 1",
                        bitmap = draftBitmap,
                        isLocked = false
                )

        // Layers are stored [Top, Bottom] based on addLayer logic
        val layers = listOf(draftLayer, bgLayer)

        _projectState.value =
                ProjectState(
                        width = width,
                        height = height,
                        layers = layers,
                        activeLayerId = draftLayer.id,
                        backgroundColor = if (isTransparent) Color.Transparent else backgroundColor
                )
        undoStack.clear()
        redoStack.clear()
    }

    fun setTool(tool: Tool) {
        _currentTool.value = tool
    }

    fun setColor(color: Color) {
        _currentColor.value = color
    }

    fun setSecondaryColor(color: Color) {
        _secondaryColor.value = color
    }

    fun toggleEraseToBackground(enabled: Boolean) {
        _toolSettings.update { it.copy(eraseToBackground = enabled) }
    }

    fun toggleSampleAllLayers(enabled: Boolean) {
        _toolSettings.update { it.copy(sampleAllLayers = enabled) }
    }

    fun toggleContiguous(enabled: Boolean) {
        _toolSettings.update { it.copy(contiguous = enabled) }
    }

    fun toggleShapeFilled(enabled: Boolean) {
        _toolSettings.update { it.copy(shapeFilled = enabled) }
    }

    fun togglePixelPerfect(enabled: Boolean) {
        _toolSettings.update { it.copy(pixelPerfect = enabled) }
    }

    fun setToolSize(size: Int) {
        _toolSettings.update { it.copy(size = size) }
    }

    // Drawing Logic (Bresenham + Pixel Perfect)
    fun onPixelAction(x: Int, y: Int, isDrag: Boolean, isActionEnd: Boolean = false) {
        _hoverPosition.value = if (isActionEnd) null else (x to y)
        val state = _projectState.value ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        if (activeLayer.isLocked) return
        val bitmap = activeLayer.bitmap

        val tool = _currentTool.value
        val color = _currentColor.value.toArgb()
        val size = _toolSettings.value.size

        // If coordinates are out of bounds, we might still need to process isActionEnd (e.g. commit shape)
        val isOutOfBounds = x !in 0 until state.width || y !in 0 until state.height

        if (isOutOfBounds) {
            if (isActionEnd) {
                // Determine if we need to finish a shape or just reset
                if (tool == Tool.SHAPE_LINE || tool == Tool.SHAPE_RECTANGLE || tool == Tool.SHAPE_CIRCLE || tool == Tool.SELECTION_RECTANGLE) {
                     // If we are finishing a shape, we might need the last valid X/Y. 
                     // But if x/y are -1 passed from DragEnd, we shouldn't use them as the end point.
                     // The logic below for shapes uses 'x' and 'y' as end points.
                     // We should probably use lastX/lastY if x/y are invalid? 
                     // Actually, if we just want to commit what we have, we might need to handle it.
                     // For now, let's just fall through to the isActionEnd block at the bottom
                     // but ensure we don't 'draw' a new segment to -1,-1.
                } else {
                     // For PENCIL/ERASER, just reset
                     lastX = null
                     lastY = null
                     saveState()
                     return
                }
            } else {
                // If just dragging out of bounds, do nothing
                return
            }
        }



        when (tool) {
            Tool.PENCIL -> {
                val colorToUse = _currentColor.value.toArgb()
                drawStroke(x, y, colorToUse, size, isDrag, state, bitmap)
                lastX = x
                lastY = y
            }
            Tool.ERASER -> {
                val colorToUse =
                        if (_toolSettings.value.eraseToBackground) {
                            _secondaryColor.value.toArgb()
                        } else {
                            Color.Transparent.toArgb()
                        }
                drawStroke(x, y, colorToUse, size, isDrag, state, bitmap)
                lastX = x
                lastY = y
            }
            Tool.FILL -> {
                if (!isDrag) {
                    val targetPixel = bitmap.getPixel(x, y)
                    if (_toolSettings.value.contiguous) {
                        DrawingAlgorithms.scanlineFloodFill(bitmap, x, y, targetPixel, color)
                    } else {
                        DrawingAlgorithms.globalReplace(bitmap, targetPixel, color)
                    }
                }
            }
            Tool.EYEDROPPER -> {
                val pixelColorInt = getPixelColor(x, y, state)
                val pixelColor = Color(pixelColorInt)

                if (isDrag) {
                    _magnifierState.value =
                            MagnifierState(visible = true, x = x, y = y, targetColor = pixelColor)
                } else {
                    if (!isActionEnd) {
                        _currentColor.value = pixelColor
                        _currentTool.value = Tool.PENCIL
                    }
                }

                if (isActionEnd) {
                    if (isDrag) {
                        _currentColor.value = pixelColor
                        _currentTool.value = Tool.PENCIL
                    }
                    _magnifierState.value = _magnifierState.value.copy(visible = false)
                }
            }
            Tool.SHAPE_LINE, Tool.SHAPE_RECTANGLE, Tool.SHAPE_CIRCLE, Tool.SELECTION_RECTANGLE -> {
                // Initialize Preview Layer
                if (isDrag && lastX == null) {
                    lastX = x
                    lastY = y
                    val previewBitmap =
                            Bitmap.createBitmap(state.width, state.height, Bitmap.Config.ARGB_8888)
                    val previewLayer =
                            PixelLayer(id = "preview", name = "Preview", bitmap = previewBitmap)
                    _projectState.update { it?.copy(previewLayer = previewLayer) }
                }

                if (isDrag && lastX != null) {
                    _projectState.value?.previewLayer?.bitmap?.eraseColor(
                            Color.Transparent.toArgb()
                    )
                    val previewBitmap = _projectState.value?.previewLayer?.bitmap ?: return

                    when (tool) {
                        Tool.SHAPE_LINE ->
                                DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) { px, py ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SHAPE_RECTANGLE ->
                                DrawingAlgorithms.drawRectangle(
                                        lastX!!,
                                        lastY!!,
                                        x,
                                        y,
                                        size,
                                        _toolSettings.value.shapeFilled
                                ) { px, py ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SHAPE_CIRCLE ->
                                DrawingAlgorithms.drawCircle(
                                        lastX!!,
                                        lastY!!,
                                        x,
                                        y,
                                        size,
                                        _toolSettings.value.shapeFilled
                                ) { px, py ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SELECTION_RECTANGLE ->
                                DrawingAlgorithms.drawRectangle(lastX!!, lastY!!, x, y, 1, false) {
                                        px,
                                        py ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, Color.Gray.toArgb())
                                }
                        else -> {}
                    }
                    _projectState.update { it?.copy(version = it.version + 1) }
                }
            }
            else -> {}
        }

        if (isActionEnd && lastX != null) {
            // Only commit shape if end coordinates are valid, OR if we want to commit the preview.
            // Current logic redraws. If x,y are -1, we shouldn't draw.
            // If the user released outside, maybe we should use clamp?
            // For now, let's assume if x=-1, we abort the shape commit or commit to last valid?
            // "lastX" is the START point.
            
            // If we received -1, -1, it means we don't have a valid "current" point.
            // But we might have had a valid preview.
            // Ideally we should commit the preview. But here we redraw.
            
            val validX = x.coerceIn(0, state.width - 1)
            val validY = y.coerceIn(0, state.height - 1)
            // Use clamped values if we are out of bounds but valid start exists?
            // Or just check if x/y are -1.
            
            val shouldDraw = x in 0 until state.width && y in 0 until state.height
            
            if (shouldDraw) {
                when (tool) {
                    Tool.SHAPE_LINE ->
                            DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) { px, py ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SHAPE_RECTANGLE ->
                            DrawingAlgorithms.drawRectangle(
                                    lastX!!,
                                    lastY!!,
                                    x,
                                    y,
                                    size,
                                    _toolSettings.value.shapeFilled
                            ) { px, py ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SHAPE_CIRCLE ->
                            DrawingAlgorithms.drawCircle(
                                    lastX!!,
                                    lastY!!,
                                    x,
                                    y,
                                    size,
                                    _toolSettings.value.shapeFilled
                            ) { px, py ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SELECTION_RECTANGLE -> {
                        val left = minOf(lastX!!, x)
                        val right = maxOf(lastX!!, x)
                        val top = minOf(lastY!!, y)
                        val bottom = maxOf(lastY!!, y)
                        _projectState.update { it?.copy(previewLayer = null) }
                        createRectSelection(android.graphics.Rect(left, top, right + 1, bottom + 1))
                    }
                    Tool.SELECTION_MAGIC_WAND -> {
                        createMagicWandSelection(x, y)
                    }
                    else -> {}
                }
            } else {
                 // If we are out of bounds (e.g. -1, -1 passed on release), 
                 // we might still want to clear the preview layer
            }

            if (tool == Tool.SHAPE_LINE ||
                            tool == Tool.SHAPE_RECTANGLE ||
                            tool == Tool.SHAPE_CIRCLE ||
                            tool == Tool.SELECTION_RECTANGLE
            ) {
                _projectState.update { it?.copy(previewLayer = null, version = it.version + 1) }
            }
        }

        if (tool == Tool.MOVE && isActionEnd) {
            lastX = null
            lastY = null
        }

        if (tool == Tool.SELECTION_MAGIC_WAND && isActionEnd && lastX == null) {
            commitSelection()
            createMagicWandSelection(x, y)
        }

        if (isActionEnd) {
            lastX = null
            lastY = null
            saveState()
        }

        _projectState.update { it?.copy(layers = it.layers.toList(), version = it.version + 1) }
    }

    // Undo/Redo
    private fun saveState() {
        val state = _projectState.value ?: return
        // Deep copy layers
        val layersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        undoStack.push(layersCopy)
        redoStack.clear()

        // Limit stack size?
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val state = _projectState.value ?: return
        // Save current to redo
        val currentLayersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        redoStack.push(currentLayersCopy)

        val previousLayers = undoStack.pop()
        _projectState.update { it?.copy(layers = previousLayers) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val state = _projectState.value ?: return
        // Save current to undo
        val currentLayersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        undoStack.push(currentLayersCopy)

        val nextLayers = redoStack.pop()
        _projectState.update { it?.copy(layers = nextLayers) }
    }

    // We'll implement robust drawing (Bresenham) in the next step.
    // Layer Management
    fun addLayer() {
        val state = _projectState.value ?: return
        val newLayer =
                PixelLayer(
                        name = "Layer ${state.layers.size + 1}",
                        bitmap =
                                Bitmap.createBitmap(
                                        state.width,
                                        state.height,
                                        Bitmap.Config.ARGB_8888
                                )
                )
        _projectState.update {
            it?.copy(
                    layers = listOf(newLayer) + it.layers, // Add to top
                    activeLayerId = newLayer.id
            )
        }
    }

    fun removeLayer(layerId: String) {
        val state = _projectState.value ?: return
        if (state.layers.size <= 1) return // Cannot delete last layer

        val newLayers = state.layers.filter { it.id != layerId }
        val newActiveId =
                if (state.activeLayerId == layerId) newLayers.first().id else state.activeLayerId

        _projectState.update { it?.copy(layers = newLayers, activeLayerId = newActiveId) }
    }

    fun toggleLayerVisibility(layerId: String, isVisible: Boolean) {
        val state = _projectState.value ?: return
        val layer = state.layers.find { it.id == layerId } ?: return
        layer.isVisible = isVisible
        // Trigger update
        _projectState.update { it?.copy(layers = it.layers.toList(), version = it.version + 1) }
    }

    fun selectLayer(layerId: String) {
        _projectState.update { it?.copy(activeLayerId = layerId) }
    }

    // Export
    fun generateExportBitmap(scale: Int): Bitmap? {
        val state = _projectState.value ?: return null
        val w = state.width * scale
        val h = state.height * scale

        val destBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)

        // Draw background
        // canvas.drawColor(state.backgroundColor.toArgb()) // If we want to export background
        // Usually pixel art formats support transparency.
        // If background is solid, we should probably draw it.
        if (state.backgroundColor != Color.Transparent) {
            canvas.drawColor(state.backgroundColor.toArgb())
        }

        val paint =
                Paint().apply {
                    isAntiAlias = false
                    isFilterBitmap = false
                    isDither = false
                }

        state.layers.forEach { layer ->
            if (layer.isVisible) {
                val src = android.graphics.Rect(0, 0, state.width, state.height)
                val dst = android.graphics.Rect(0, 0, w, h)
                canvas.drawBitmap(layer.bitmap, src, dst, paint)
            }
        }

        return destBitmap
    }
    private fun drawStroke(
            x: Int,
            y: Int,
            color: Int,
            size: Int,
            isDrag: Boolean,
            state: ProjectState,
            bitmap: Bitmap
    ) {
        // Prepare plot function
        val plotPixel: (Int, Int) -> Unit = { px, py ->
            if (px in 0 until state.width && py in 0 until state.height) {
                val originalColor = bitmap.getPixel(px, py)
                bitmap.setPixel(px, py, color)

                // Pixel Perfect Logic (Only for 1px brush)
                if (_toolSettings.value.pixelPerfect && size == 1) {
                    currentStrokePath.add(PixelNode(px, py, originalColor))

                    // Check for L-shape
                    if (currentStrokePath.size >= 3) {
                        val c = currentStrokePath.last() // Current
                        val b = currentStrokePath[currentStrokePath.size - 2] // Middle
                        val a = currentStrokePath[currentStrokePath.size - 3] // Previous

                        // Check if A and C are diagonal neighbors
                        if (kotlin.math.abs(a.x - c.x) == 1 && kotlin.math.abs(a.y - c.y) == 1) {
                            // Check if B creates an L-shape (shared coord with A and C)
                            if ((b.x == a.x && b.y == c.y) || (b.x == c.x && b.y == a.y)) {
                                // Restore B
                                bitmap.setPixel(b.x, b.y, b.oldColor)
                                currentStrokePath.removeAt(currentStrokePath.size - 2)
                            }
                        }
                    }
                }
            }
        }

        if (lastX != null && lastY != null && isDrag) {
            DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size, plotPixel)
        } else {
            // Reset path on new stroke (ACTION_DOWN or single tap)
            if (!isDrag) currentStrokePath.clear()
            DrawingAlgorithms.drawLine(x, y, x, y, size, plotPixel)
        }
    }

    private fun getPixelColor(x: Int, y: Int, state: ProjectState): Int {
        if (x !in 0 until state.width || y !in 0 until state.height)
                return Color.Transparent.toArgb()

        return if (_toolSettings.value.sampleAllLayers) {
            // Iterate layers from bottom to top for blending
            var r = 0f
            var g = 0f
            var b = 0f
            var a = 0f

            // Start with background
            if (state.backgroundColor != Color.Transparent) {
                val bg = state.backgroundColor
                r = bg.red
                g = bg.green
                b = bg.blue
                a = bg.alpha
            }

            // Blend layers
            state.layers.asReversed().forEach { layer ->
                if (layer.isVisible) {
                    val pixel = layer.bitmap.getPixel(x, y)
                    val color = Color(pixel)
                    if (color.alpha > 0f) {
                        val srcA = color.alpha
                        val dstA = a
                        val outA = srcA + dstA * (1f - srcA)

                        if (outA > 0f) {
                            r = (color.red * srcA + r * dstA * (1f - srcA)) / outA
                            g = (color.green * srcA + g * dstA * (1f - srcA)) / outA
                            b = (color.blue * srcA + b * dstA * (1f - srcA)) / outA
                            a = outA
                        }
                    }
                }
            }
            Color(r, g, b, a).toArgb()
        } else {
            // Current Layer Only
            val activeLayer = state.layers.find { it.id == state.activeLayerId }
            activeLayer?.bitmap?.getPixel(x, y) ?: Color.Transparent.toArgb()
        }
    }

    // --- Selection Helper Methods ---

    private fun createRectSelection(rect: android.graphics.Rect) {
        val state = _projectState.value ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return

        val mask = DrawingAlgorithms.createRectMask(state.width, state.height, rect)
        val result = DrawingAlgorithms.extractSelection(activeLayer.bitmap, mask)

        if (result != null) {
            val (floatingBitmap, bounds) = result
            val selection =
                    com.example.pixelshift.ui.editor.common.SelectionState(
                            bitmap = floatingBitmap,
                            mask = mask,
                            x = bounds.left,
                            y = bounds.top
                    )
            _projectState.update { it?.copy(selection = selection) }
        }
    }

    private fun createMagicWandSelection(x: Int, y: Int) {
        val state = _projectState.value ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return

        val mask = DrawingAlgorithms.createMagicWandMask(activeLayer.bitmap, x, y)
        val result = DrawingAlgorithms.extractSelection(activeLayer.bitmap, mask)

        if (result != null) {
            val (floatingBitmap, bounds) = result
            val selection =
                    com.example.pixelshift.ui.editor.common.SelectionState(
                            bitmap = floatingBitmap,
                            mask = mask,
                            x = bounds.left,
                            y = bounds.top
                    )
            _projectState.update { it?.copy(selection = selection) }
        }
    }

    fun commitSelection() {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return

        DrawingAlgorithms.mergeSelection(
                activeLayer.bitmap,
                selection.bitmap,
                selection.x,
                selection.y
        )

        _projectState.update { it?.copy(selection = null) }
    }

    fun moveSelection(dx: Int, dy: Int) {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newSelection = selection.copy(x = selection.x + dx, y = selection.y + dy)
        _projectState.update { it?.copy(selection = newSelection) }
    }

    fun rotateSelection() {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newBitmap = DrawingAlgorithms.rotateBitmap90(selection.bitmap)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap)) }
    }

    fun flipSelection(horizontal: Boolean) {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newBitmap = DrawingAlgorithms.flipBitmap(selection.bitmap, horizontal)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap)) }
    }

    fun clearSelection() {
        commitSelection()
    }
}
