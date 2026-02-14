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

    // Undo/Redo Stacks (Simplified for now - storing full bitmaps)
    // In a real app, we'd store diffs or commands to save memory.
    private val undoStack = Stack<List<PixelLayer>>()
    private val redoStack = Stack<List<PixelLayer>>()

    private var lastX: Int? = null
    private var lastY: Int? = null

    fun initializeProject(width: Int, height: Int, backgroundColor: Color) {
        val initialLayer =
                PixelLayer(
                        name = "Layer 1",
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                )

        // If background is transparent, we leave the bitmap empty (0).
        // If it's a solid color, we fill it.
        // Wait, the project has a background color property for the *canvas*, but layers are
        // transparent by default.
        // Let's assume the bottom-most layer/canvas background handles the solid color,
        // OR we fill the first layer if the user requested a solid background fill.
        // The requirement says "Background: Transparent or Solid".
        // Use the project state background color for the "base", layers are on top.

        _projectState.value =
                ProjectState(
                        width = width,
                        height = height,
                        layers = listOf(initialLayer),
                        activeLayerId = initialLayer.id,
                        backgroundColor = backgroundColor
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

    fun setToolSize(size: Int) {
        _toolSettings.update { it.copy(size = size) }
    }

    // Drawing Logic (Placeholder for now)
    fun onPixelAction(x: Int, y: Int, isDrag: Boolean, isActionEnd: Boolean = false) {
        val state = _projectState.value ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        val bitmap = activeLayer.bitmap

        if (x !in 0 until state.width || y !in 0 until state.height) {
            if (isActionEnd) {
                lastX = null
                lastY = null
            }
            return
        }

        val tool = _currentTool.value
        val color = _currentColor.value.toArgb()
        val size = _toolSettings.value.size

        when (tool) {
            Tool.PENCIL -> {
                if (lastX != null && lastY != null && isDrag) {
                    DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) { px, py ->
                        if (px in 0 until state.width && py in 0 until state.height) {
                            bitmap.setPixel(px, py, color)
                        }
                    }
                } else {
                    DrawingAlgorithms.drawLine(x, y, x, y, size) { px, py ->
                        if (px in 0 until state.width && py in 0 until state.height) {
                            bitmap.setPixel(px, py, color)
                        }
                    }
                }
                lastX = x
                lastY = y
            }
            Tool.ERASER -> {
                if (lastX != null && lastY != null && isDrag) {
                    DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) { px, py ->
                        if (px in 0 until state.width && py in 0 until state.height) {
                            bitmap.setPixel(px, py, Color.Transparent.toArgb())
                        }
                    }
                } else {
                    DrawingAlgorithms.drawLine(x, y, x, y, size) { px, py ->
                        if (px in 0 until state.width && py in 0 until state.height) {
                            bitmap.setPixel(px, py, Color.Transparent.toArgb())
                        }
                    }
                }
                lastX = x
                lastY = y
            }
            Tool.FILL -> {
                if (!isDrag) {
                    DrawingAlgorithms.floodFill(bitmap, x, y, bitmap.getPixel(x, y), color)
                }
            }
            Tool.EYEDROPPER -> {
                if (!isDrag) {
                    val pixelColor = bitmap.getPixel(x, y)
                    _currentColor.value = Color(pixelColor)
                    _currentTool.value = Tool.PENCIL // Auto switch back
                }
            }
            Tool.SHAPE_LINE, Tool.SHAPE_RECTANGLE, Tool.SHAPE_CIRCLE -> {
                // Initialize Preview Layer if needed
                if (isDrag && lastX == null) {
                    // Start of drag
                    lastX = x
                    lastY = y
                    // Create/Reset Preview Layer
                    val previewBitmap =
                            Bitmap.createBitmap(state.width, state.height, Bitmap.Config.ARGB_8888)
                    val previewLayer =
                            PixelLayer(id = "preview", name = "Preview", bitmap = previewBitmap)
                    _projectState.update { it?.copy(previewLayer = previewLayer) }
                }

                if (isDrag && lastX != null) {
                    // Draw on preview layer
                    _projectState.value?.previewLayer?.bitmap?.eraseColor(
                            Color.Transparent.toArgb()
                    )
                    val previewBitmap = _projectState.value?.previewLayer?.bitmap ?: return

                    when (tool) {
                        Tool.SHAPE_LINE ->
                                DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) { px, py ->
                                    if (px in 0 until state.width && py in 0 until state.height) {
                                        previewBitmap.setPixel(px, py, color)
                                    }
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
                                    if (px in 0 until state.width && py in 0 until state.height) {
                                        previewBitmap.setPixel(px, py, color)
                                    }
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
                                    if (px in 0 until state.width && py in 0 until state.height) {
                                        previewBitmap.setPixel(px, py, color)
                                    }
                                }
                        else -> {}
                    }
                    // Force update to show preview
                    _projectState.update { it }
                }

                if (isActionEnd && lastX != null) {
                    // Commit to active layer
                    val previewBitmap = _projectState.value?.previewLayer?.bitmap
                    if (previewBitmap != null) {
                        // Draw preview bitmap onto active layer
                        // We can iterate pixels or use Canvas
                        // Iterating is safer for "aliased" requirement if we just copy
                        // non-transparent pixels
                        // But DrawBitmap should work if logic is correct.
                        // Let's reuse the algos to draw directly on active layer to be safe and
                        // consistent

                        when (tool) {
                            Tool.SHAPE_LINE ->
                                    DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size) {
                                            px,
                                            py ->
                                        if (px in 0 until state.width && py in 0 until state.height
                                        ) {
                                            bitmap.setPixel(px, py, color)
                                        }
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
                                        if (px in 0 until state.width && py in 0 until state.height
                                        ) {
                                            bitmap.setPixel(px, py, color)
                                        }
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
                                        if (px in 0 until state.width && py in 0 until state.height
                                        ) {
                                            bitmap.setPixel(px, py, color)
                                        }
                                    }
                            else -> {}
                        }
                    }

                    // Clear preview layer
                    _projectState.update { it?.copy(previewLayer = null) }
                }
            }
            else -> {}
        }

        if (isActionEnd) {
            lastX = null
            lastY = null
            saveState()
        }

        // Trigger recomposition
        _projectState.update { it?.copy(layers = it.layers.toList()) }
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
        _projectState.update { it?.copy(layers = it.layers.toList()) }
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
}
