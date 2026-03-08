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

    private val _secondaryColor = MutableStateFlow(Color.White)
    val secondaryColor: StateFlow<Color> = _secondaryColor.asStateFlow()

    private val undoStack = Stack<List<PixelLayer>>()
    private val redoStack = Stack<List<PixelLayer>>()

    private data class PixelNode(val x: Int, val y: Int, val oldColor: Int)
    private val currentStrokePath = java.util.LinkedList<PixelNode>()

    private val _hoverPosition = MutableStateFlow<Pair<Int, Int>?>(null)
    val hoverPosition: StateFlow<Pair<Int, Int>?> = _hoverPosition.asStateFlow()

    data class MagnifierState(
            val visible: Boolean = false,
            val x: Int = 0,
            val y: Int = 0,
            val screenX: Float = 0f,
            val screenY: Float = 0f,
            val targetColor: Color = Color.Transparent
    )
    private val _magnifierState = MutableStateFlow(MagnifierState())
    val magnifierState: StateFlow<MagnifierState> = _magnifierState.asStateFlow()

    private var savedToolBeforeShortcut: Tool? = null
    
    private var startX: Int? = null
    private var startY: Int? = null
    private var lastX: Int? = null
    private var lastY: Int? = null
    
    private var lastValidDragX: Int? = null
    private var lastValidDragY: Int? = null

    fun initializeProject(width: Int, height: Int, isTransparent: Boolean, backgroundColor: Color) {
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

        val draftBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val draftLayer =
                PixelLayer(
                        name = "Layer 1",
                        bitmap = draftBitmap,
                        isLocked = false
                )

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

    fun toggleRectFilled(enabled: Boolean) {
        _toolSettings.update { it.copy(rectFilled = enabled) }
    }

    fun toggleCircleFilled(enabled: Boolean) {
        _toolSettings.update { it.copy(circleFilled = enabled) }
    }

    fun togglePixelPerfect(enabled: Boolean) {
        _toolSettings.update { it.copy(pixelPerfect = enabled) }
    }

    fun setToolSize(size: Int) {
        _toolSettings.update { it.copy(size = size) }
    }

    fun onPixelAction(x: Int, y: Int, isDrag: Boolean, isActionEnd: Boolean = false, rawX: Float = 0f, rawY: Float = 0f, isShortcut: Boolean = false) {
        if (isShortcut && !isActionEnd && _currentTool.value != Tool.EYEDROPPER) {
            savedToolBeforeShortcut = _currentTool.value
            _currentTool.value = Tool.EYEDROPPER
        }
        
        _hoverPosition.value = if (isActionEnd) null else (x to y)
        val state = _projectState.value ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        if (activeLayer.isLocked) return
        val bitmap = activeLayer.bitmap

        val tool = _currentTool.value
        val color = _currentColor.value.toArgb()
        val size = _toolSettings.value.size

        if (!isDrag && !isActionEnd) {
            startX = x
            startY = y
            lastX = x
            lastY = y
            lastValidDragX = x
            lastValidDragY = y
            
            if (tool == Tool.SHAPE_LINE || tool == Tool.SHAPE_RECTANGLE || tool == Tool.SHAPE_CIRCLE || tool == Tool.SELECTION_RECTANGLE) {
                val previewBitmap = Bitmap.createBitmap(state.width, state.height, Bitmap.Config.ARGB_8888)
                val previewLayer = PixelLayer(id = "preview", name = "Preview", bitmap = previewBitmap)
                _projectState.update { it?.copy(previewLayer = previewLayer, version = it.version + 1) }
            }
        }

        if (isActionEnd) {
            if (startX != null && startY != null) {
                val commitX = if (x in 0 until state.width) x else (lastValidDragX ?: startX!!)
                val commitY = if (y in 0 until state.height) y else (lastValidDragY ?: startY!!)
                
                when (tool) {
                    Tool.SHAPE_LINE ->
                            DrawingAlgorithms.drawLine(startX!!, startY!!, commitX, commitY, size) { px: Int, py: Int ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SHAPE_RECTANGLE ->
                            DrawingAlgorithms.drawRectangle(
                                    startX!!,
                                    startY!!,
                                    commitX,
                                    commitY,
                                    size,
                                    _toolSettings.value.rectFilled
                            ) { px: Int, py: Int ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SHAPE_CIRCLE ->
                            DrawingAlgorithms.drawCircle(
                                    startX!!,
                                    startY!!,
                                    commitX,
                                    commitY,
                                    size,
                                    _toolSettings.value.circleFilled
                            ) { px: Int, py: Int ->
                                if (px in 0 until state.width && py in 0 until state.height)
                                        bitmap.setPixel(px, py, color)
                            }
                    Tool.SELECTION_RECTANGLE -> {
                        val left = minOf(startX!!, commitX)
                        val right = maxOf(startX!!, commitX)
                        val top = minOf(startY!!, commitY)
                        val bottom = maxOf(startY!!, commitY)
                        createRectSelection(android.graphics.Rect(left, top, right + 1, bottom + 1))
                    }
                    else -> {}
                }
            }
            
            if (tool == Tool.SELECTION_MAGIC_WAND) {
                commitSelection()
                if (x in 0 until state.width && y in 0 until state.height) {
                    createMagicWandSelection(x, y)
                }
            }

            _projectState.update { it?.copy(previewLayer = null, version = it.version + 1) }
            startX = null
            startY = null
            lastX = null
            lastY = null
            lastValidDragX = null
            lastValidDragY = null
            saveState()
            return
        }

        val isOutOfBounds = x !in 0 until state.width || y !in 0 until state.height
        if (isOutOfBounds) return
        
        lastValidDragX = x
        lastValidDragY = y

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
                            MagnifierState(
                                visible = true,
                                x = x,
                                y = y,
                                screenX = rawX,
                                screenY = rawY,
                                targetColor = pixelColor
                            )
                } else {
                    _currentColor.value = pixelColor
                    _currentTool.value = Tool.PENCIL
                }
            }
            Tool.SHAPE_LINE, Tool.SHAPE_RECTANGLE, Tool.SHAPE_CIRCLE, Tool.SELECTION_RECTANGLE -> {
                if (isDrag) {
                    if (startX == null) {
                        startX = x
                        startY = y
                        val previewBitmap = Bitmap.createBitmap(state.width, state.height, Bitmap.Config.ARGB_8888)
                        val previewLayer = PixelLayer(id = "preview", name = "Preview", bitmap = previewBitmap)
                        _projectState.update { it?.copy(previewLayer = previewLayer) }
                    }
                    
                    val previewBitmap = _projectState.value?.previewLayer?.bitmap ?: return
                    previewBitmap.eraseColor(Color.Transparent.toArgb())

                    when (tool) {
                        Tool.SHAPE_LINE ->
                                DrawingAlgorithms.drawLine(startX!!, startY!!, x, y, size) { px: Int, py: Int ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SHAPE_RECTANGLE ->
                                DrawingAlgorithms.drawRectangle(
                                        startX!!,
                                        startY!!,
                                        x,
                                        y,
                                        size,
                                        _toolSettings.value.rectFilled
                                ) { px: Int, py: Int ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SHAPE_CIRCLE ->
                                DrawingAlgorithms.drawCircle(
                                        startX!!,
                                        startY!!,
                                        x,
                                        y,
                                        size,
                                        _toolSettings.value.circleFilled
                                ) { px: Int, py: Int ->
                                    if (px in 0 until state.width && py in 0 until state.height)
                                            previewBitmap.setPixel(px, py, color)
                                }
                        Tool.SELECTION_RECTANGLE ->
                                DrawingAlgorithms.drawRectangle(startX!!, startY!!, x, y, 1, false) { px: Int, py: Int ->
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

        _projectState.update { it?.copy(layers = it.layers.toList(), version = it.version + 1) }
    }

    private fun saveState() {
        val state = _projectState.value ?: return
        val layersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        undoStack.push(layersCopy)
        redoStack.clear()

        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val state = _projectState.value ?: return
        val currentLayersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        redoStack.push(currentLayersCopy)

        val previousLayers = undoStack.pop()
        _projectState.update { it?.copy(layers = previousLayers, version = it.version + 1) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val state = _projectState.value ?: return
        val currentLayersCopy =
                state.layers.map { layer ->
                    layer.copy(bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true))
                }
        undoStack.push(currentLayersCopy)

        val nextLayers = redoStack.pop()
        _projectState.update { it?.copy(layers = nextLayers, version = it.version + 1) }
    }

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
                    layers = listOf(newLayer) + it.layers,
                    activeLayerId = newLayer.id,
                    version = it.version + 1
            )
        }
    }

    fun removeLayer(layerId: String) {
        val state = _projectState.value ?: return
        if (state.layers.size <= 1) return

        val newLayers = state.layers.filter { it.id != layerId }
        val newActiveId =
                if (state.activeLayerId == layerId) newLayers.first().id else state.activeLayerId

        _projectState.update { it?.copy(layers = newLayers, activeLayerId = newActiveId, version = it.version + 1) }
    }

    fun toggleLayerVisibility(layerId: String, isVisible: Boolean) {
        val state = _projectState.value ?: return
        val layer = state.layers.find { it.id == layerId } ?: return
        layer.isVisible = isVisible
        _projectState.update { it?.copy(layers = it.layers.toList(), version = it.version + 1) }
    }

    fun selectLayer(layerId: String) {
        _projectState.update { it?.copy(activeLayerId = layerId, version = it.version + 1) }
    }

    fun generateExportBitmap(scale: Int): Bitmap? {
        val state = _projectState.value ?: return null
        val w = state.width * scale
        val h = state.height * scale

        val destBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)

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
        val plotPixel: (Int, Int) -> Unit = { px: Int, py: Int ->
            if (px in 0 until state.width && py in 0 until state.height) {
                val originalColor = bitmap.getPixel(px, py)
                bitmap.setPixel(px, py, color)

                if (_toolSettings.value.pixelPerfect && size == 1) {
                    currentStrokePath.add(PixelNode(px, py, originalColor))

                    if (currentStrokePath.size >= 3) {
                        val c = currentStrokePath.last()
                        val b = currentStrokePath[currentStrokePath.size - 2]
                        val a = currentStrokePath[currentStrokePath.size - 3]

                        if (kotlin.math.abs(a.x - c.x) == 1 && kotlin.math.abs(a.y - c.y) == 1) {
                            if ((b.x == a.x && b.y == c.y) || (b.x == c.x && b.y == a.y)) {
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
            if (!isDrag) currentStrokePath.clear()
            DrawingAlgorithms.drawLine(x, y, x, y, size, plotPixel)
        }
    }

    private fun getPixelColor(x: Int, y: Int, state: ProjectState): Int {
        if (x !in 0 until state.width || y !in 0 until state.height)
                return Color.Transparent.toArgb()

        return if (_toolSettings.value.sampleAllLayers) {
        var finalR = 0f
        var finalG = 0f
        var finalB = 0f
        var finalA = 0f

        state.layers.forEach { layer ->
            if (layer.isVisible) {
                val pixel = layer.bitmap.getPixel(x, y)
                val srcColor = Color(pixel)
                val srcA = srcColor.alpha

                if (srcA > 0f) {
                    if (finalA == 0f) {
                        finalR = srcColor.red
                        finalG = srcColor.green
                        finalB = srcColor.blue
                        finalA = srcA
                    } else {
                        val dstA = finalA
                        val newA = dstA + srcA * (1f - dstA)
                        if (newA > 0f) {
                            finalR = (finalR * dstA + srcColor.red * srcA * (1f - dstA)) / newA
                            finalG = (finalG * dstA + srcColor.green * srcA * (1f - dstA)) / newA
                            finalB = (finalB * dstA + srcColor.blue * srcA * (1f - dstA)) / newA
                            finalA = newA
                        }
                    }
                    if (finalA >= 0.99f) return@forEach
                }
            }
        }

        if (finalA < 1f && state.backgroundColor != Color.Transparent) {
            val bg = state.backgroundColor
            val srcA = bg.alpha
            val dstA = finalA
            val newA = dstA + srcA * (1f - dstA)
            if (newA > 0f) {
                finalR = (finalR * dstA + bg.red * srcA * (1f - dstA)) / newA
                finalG = (finalG * dstA + bg.green * srcA * (1f - dstA)) / newA
                finalB = (finalB * dstA + bg.blue * srcA * (1f - dstA)) / newA
                finalA = newA
            }
        }

        Color(finalR, finalG, finalB, finalA).toArgb()
    } else {
        val activeLayer = state.layers.find { it.id == state.activeLayerId }
        activeLayer?.bitmap?.getPixel(x, y) ?: Color.Transparent.toArgb()
    }
}

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
            _projectState.update { it?.copy(selection = selection, version = it.version + 1) }
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
            _projectState.update { it?.copy(selection = selection, version = it.version + 1) }
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

        _projectState.update { it?.copy(selection = null, version = it.version + 1) }
    }

    fun moveSelection(dx: Int, dy: Int) {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newSelection = selection.copy(x = selection.x + dx, y = selection.y + dy)
        _projectState.update { it?.copy(selection = newSelection, version = it.version + 1) }
    }

    fun rotateSelection() {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newBitmap = DrawingAlgorithms.rotateBitmap90(selection.bitmap)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap), version = it.version + 1) }
    }

    fun flipSelection(horizontal: Boolean) {
        val state = _projectState.value ?: return
        val selection = state.selection ?: return

        val newBitmap = DrawingAlgorithms.flipBitmap(selection.bitmap, horizontal)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap), version = it.version + 1) }
    }

    fun clearSelection() {
        commitSelection()
    }
}
