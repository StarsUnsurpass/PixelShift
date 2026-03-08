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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

class PixelArtViewModel : ViewModel() {

    private val _projectState = MutableStateFlow<ProjectState?>(null)
    val projectState: StateFlow<ProjectState?> = _projectState.asStateFlow()

    private val _currentTool = MutableStateFlow(Tool.PENCIL)
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    // Temporary override tool for gestures like Long Press to Pick Color
    private val _overrideTool = MutableStateFlow<Tool?>(null)
    val activeTool: StateFlow<Tool> = MutableStateFlow(Tool.PENCIL).apply {
        // Simple derived state logic in a more robust way for ViewModel
    }.asStateFlow()
    
    // Improved active tool logic using StateFlow combining if needed, 
    // but for simplicity and performance we can use a getter or derived state in UI.
    // Here we'll manage it via updates.

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

    fun addColorToPalette(color: Color) {
        if (!_palette.value.contains(color)) {
            _palette.update { it + color }
        }
    }

    fun removeColorFromPalette(index: Int) {
        if (index in _palette.value.indices) {
            _palette.update { it.toMutableList().apply { removeAt(index) } }
        }
    }

    fun swapPaletteColors(fromIndex: Int, toIndex: Int) {
        if (fromIndex in _palette.value.indices && toIndex in _palette.value.indices) {
            _palette.update {
                val list = it.toMutableList()
                java.util.Collections.swap(list, fromIndex, toIndex)
                list
            }
        }
    }

    fun loadPreset(preset: String) {
        val colors = when (preset.lowercase()) {
            "gameboy" -> listOf(Color(0xFF0f380f), Color(0xFF306230), Color(0xFF8bac0f), Color(0xFF9bbc0f))
            "pico-8" -> listOf(
                Color(0xFF000000), Color(0xFF1D2B53), Color(0xFF7E2553), Color(0xFF008751),
                Color(0xFFAB5236), Color(0xFF5F574F), Color(0xFFC2C3C7), Color(0xFFFFF1E8),
                Color(0xFFFF004D), Color(0xFFFFA300), Color(0xFFFFEC27), Color(0xFF00E436),
                Color(0xFF29ADFF), Color(0xFF83769C), Color(0xFFFF77A8), Color(0xFFFFCCAA)
            )
            "nes" -> listOf(
                Color(0xFF7C7C7C), Color(0xFF0000FC), Color(0xFF0000BC), Color(0xFF4428BC),
                Color(0xFF940084), Color(0xFFA80020), Color(0xFFA81000), Color(0xFF881400),
                Color(0xFF503000), Color(0xFF007800), Color(0xFF006800), Color(0xFF005800),
                Color(0xFF004058), Color(0xFF000000)
            )
            else -> return
        }
        _palette.value = colors
    }

    fun fetchLospecPalette(slug: String) {
        viewModelScope.launch {
            val fetchedColors = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://lospec.com/palette-list/$slug.json")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val jsonObject = JSONObject(response)
                        val colorsArray = jsonObject.getJSONArray("colors")
                        val newColors = mutableListOf<Color>()
                        for (i in 0 until colorsArray.length()) {
                            val hex = colorsArray.getString(i)
                            newColors.add(Color(android.graphics.Color.parseColor("#$hex")))
                        }
                        newColors
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            if (fetchedColors != null) {
                _palette.value = fetchedColors
            }
        }
    }

    private val _currentColor = MutableStateFlow(Color.Black)
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private val _secondaryColor = MutableStateFlow(Color.White)
    val secondaryColor: StateFlow<Color> = _secondaryColor.asStateFlow()

    private val undoStack = Stack<List<PixelLayer>>()
    private val redoStack = Stack<List<PixelLayer>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

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

    private var startX: Int? = null
    private var startY: Int? = null
    private var lastX: Int? = null
    private var lastY: Int? = null
    
    private var lastValidDragX: Int? = null
    private var lastValidDragY: Int? = null

    private var persistentPreviewBitmap: Bitmap? = null
    private var savedToolBeforeShortcut: Tool? = null

    fun initializeProject(width: Int, height: Int, isTransparent: Boolean, backgroundColor: Color) {
        val bgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (!isTransparent) bgBitmap.eraseColor(backgroundColor.toArgb())
        val bgLayer = PixelLayer(name = "Layer 0", bitmap = bgBitmap, isLocked = true)
        val draftBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val draftLayer = PixelLayer(name = "Layer 1", bitmap = draftBitmap, isLocked = false)
        persistentPreviewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val layers = listOf(draftLayer, bgLayer)
        _projectState.value = ProjectState(width = width, height = height, layers = layers, activeLayerId = draftLayer.id, backgroundColor = if (isTransparent) Color.Transparent else backgroundColor, previewLayer = PixelLayer(id = "preview", name = "Preview", bitmap = persistentPreviewBitmap!!, isVisible = false))
        undoStack.clear(); redoStack.clear()
    }

    fun setTool(tool: Tool) {
        commitSelection()
        _currentTool.value = tool
    }

    // Gesture Override logic
    fun startEyedropperOverride() {
        _overrideTool.value = Tool.EYEDROPPER
    }

    fun stopEyedropperOverride() {
        _overrideTool.value = null
        _magnifierState.update { it.copy(visible = false) }
    }

    fun setColor(color: Color) { _currentColor.value = color }
    fun setSecondaryColor(color: Color) { _secondaryColor.value = color }
    fun toggleEraseToBackground(enabled: Boolean) { _toolSettings.update { it.copy(eraseToBackground = enabled) } }
    fun toggleSampleAllLayers(enabled: Boolean) { _toolSettings.update { it.copy(sampleAllLayers = enabled) } }
    fun toggleContiguous(enabled: Boolean) { _toolSettings.update { it.copy(contiguous = enabled) } }
    fun toggleRectFilled(enabled: Boolean) { _toolSettings.update { it.copy(rectFilled = enabled) } }
    fun toggleCircleFilled(enabled: Boolean) { _toolSettings.update { it.copy(circleFilled = enabled) } }
    fun togglePixelPerfect(enabled: Boolean) { _toolSettings.update { it.copy(pixelPerfect = enabled) } }
    fun setToolSize(size: Int) { _toolSettings.update { it.copy(size = size) } }

    fun onPixelAction(x: Int, y: Int, isDrag: Boolean, isActionEnd: Boolean = false, rawX: Float = 0f, rawY: Float = 0f, isShortcut: Boolean = false) {
        if (isShortcut && !isActionEnd && _currentTool.value != Tool.EYEDROPPER) {
            savedToolBeforeShortcut = _currentTool.value
            _currentTool.value = Tool.EYEDROPPER
        }
        _hoverPosition.value = if (isActionEnd) null else (x to y)
        val state = _projectState.value ?: return
        
        // Use override tool if present, otherwise fall back to selected tool
        val tool = _overrideTool.value ?: _currentTool.value

        if (tool == Tool.MOVE && state.selection != null) {
            if (!isDrag && !isActionEnd) {
                // Save state BEFORE starting to move
                saveState()
            }
            if (!isActionEnd && lastX != null && lastY != null) {
                val dx = x - lastX!!; val dy = y - lastY!!
                if (dx != 0 || dy != 0) moveSelection(dx, dy)
            }
            lastX = x; lastY = y
            if (isActionEnd) { lastX = null; lastY = null }
            return
        }

        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        if (activeLayer.isLocked) return
        val bitmap = activeLayer.bitmap
        val color = _currentColor.value.toArgb()
        val size = _toolSettings.value.size

        if (isActionEnd) {
            if (startX != null && startY != null) {
                val commitX = if (x in 0 until state.width) x else (lastValidDragX ?: startX!!)
                val commitY = if (y in 0 until state.height) y else (lastValidDragY ?: startY!!)
                if (tool == Tool.SELECTION_RECTANGLE) {
                    val left = minOf(startX!!, commitX); val right = maxOf(startX!!, commitX)
                    val top = minOf(startY!!, commitY); val bottom = maxOf(startY!!, commitY)
                    createRectSelection(android.graphics.Rect(left, top, right + 1, bottom + 1))
                } else if (tool == Tool.SELECTION_MAGIC_WAND) {
                    createMagicWandSelection(commitX, commitY)
                } else if (tool != Tool.EYEDROPPER) {
                    when (tool) {
                        Tool.SHAPE_LINE -> DrawingAlgorithms.drawLine(startX!!, startY!!, commitX, commitY, size) { px, py -> if (px in 0 until state.width && py in 0 until state.height) bitmap.setPixel(px, py, color) }
                        Tool.SHAPE_RECTANGLE -> DrawingAlgorithms.drawRectangle(startX!!, startY!!, commitX, commitY, size, _toolSettings.value.rectFilled) { px, py -> if (px in 0 until state.width && py in 0 until state.height) bitmap.setPixel(px, py, color) }
                        Tool.SHAPE_CIRCLE -> DrawingAlgorithms.drawCircle(startX!!, startY!!, commitX, commitY, size, _toolSettings.value.circleFilled) { px, py -> if (px in 0 until state.width && py in 0 until state.height) bitmap.setPixel(px, py, color) }
                        else -> {}
                    }
                }
            }
            
            if (tool == Tool.EYEDROPPER) {
                val pixelColorInt = getPixelColor(x.coerceIn(0, state.width-1), y.coerceIn(0, state.height-1), state)
                _currentColor.value = Color(pixelColorInt)
                _magnifierState.update { it.copy(visible = false) }
            }

            if (tool == Tool.SELECTION_MAGIC_WAND) commitSelection()

            persistentPreviewBitmap?.eraseColor(Color.Transparent.toArgb())
            _projectState.update { it?.copy(previewLayer = it.previewLayer?.copy(isVisible = false), version = it.version + 1) }
            startX = null; startY = null; lastX = null; lastY = null; lastValidDragX = null; lastValidDragY = null
            return
        }

        if (!isDrag) {
            startX = x; startY = y; lastX = x; lastY = y; lastValidDragX = x; lastValidDragY = y
            
            // --- CRITICAL FIX: SAVE STATE BEFORE MODIFICATION ---
            // Only save state for tools that actually change the pixel data or selection
            if (tool != Tool.EYEDROPPER) {
                saveState()
            }

            if (tool == Tool.SHAPE_LINE || tool == Tool.SHAPE_RECTANGLE || tool == Tool.SHAPE_CIRCLE || tool == Tool.SELECTION_RECTANGLE) {
                persistentPreviewBitmap?.eraseColor(Color.Transparent.toArgb())
                _projectState.update { it?.copy(previewLayer = it.previewLayer?.copy(isVisible = true), version = it.version + 1) }
            }
        }

        val isOutOfBounds = x !in 0 until state.width || y !in 0 until state.height
        if (isOutOfBounds) {
            if (tool == Tool.EYEDROPPER && isDrag) {
                // Keep magnifier visible even slightly out of bounds
                _magnifierState.update { it.copy(screenX = rawX, screenY = rawY) }
            }
            return
        }
        lastValidDragX = x; lastValidDragY = y

        when (tool) {
            Tool.PENCIL -> { drawStroke(x, y, color, size, isDrag, state, bitmap); lastX = x; lastY = y }
            Tool.ERASER -> { val c = if (_toolSettings.value.eraseToBackground) _secondaryColor.value.toArgb() else Color.Transparent.toArgb(); drawStroke(x, y, c, size, isDrag, state, bitmap); lastX = x; lastY = y }
            Tool.FILL -> if (!isDrag) { val t = bitmap.getPixel(x, y); if (_toolSettings.value.contiguous) DrawingAlgorithms.scanlineFloodFill(bitmap, x, y, t, color) else DrawingAlgorithms.globalReplace(bitmap, t, color) }
            Tool.EYEDROPPER -> {
                val pixelColorInt = getPixelColor(x, y, state)
                val pixelColor = Color(pixelColorInt)
                if (isDrag) {
                    _magnifierState.value = MagnifierState(visible = true, x = x, y = y, screenX = rawX, screenY = rawY, targetColor = pixelColor)
                    _currentColor.value = pixelColor // Real-time feedback
                } else {
                    _currentColor.value = pixelColor
                }
            }
            Tool.SHAPE_LINE, Tool.SHAPE_RECTANGLE, Tool.SHAPE_CIRCLE, Tool.SELECTION_RECTANGLE -> if (isDrag) {
                if (startX == null) { startX = x; startY = y; _projectState.update { it?.copy(previewLayer = it.previewLayer?.copy(isVisible = true)) } }
                val previewBitmap = persistentPreviewBitmap ?: return
                previewBitmap.eraseColor(Color.Transparent.toArgb())
                when (tool) {
                    Tool.SHAPE_LINE -> DrawingAlgorithms.drawLine(startX!!, startY!!, x, y, size) { px, py -> if (px in 0 until state.width && py in 0 until state.height) previewBitmap.setPixel(px, py, color) }
                    Tool.SHAPE_RECTANGLE -> DrawingAlgorithms.drawRectangle(startX!!, startY!!, x, y, size, _toolSettings.value.rectFilled) { px, py -> if (px in 0 until state.width && py in 0 until state.height) previewBitmap.setPixel(px, py, color) }
                    Tool.SHAPE_CIRCLE -> DrawingAlgorithms.drawCircle(startX!!, startY!!, x, y, size, _toolSettings.value.circleFilled) { px, py -> if (px in 0 until state.width && py in 0 until state.height) previewBitmap.setPixel(px, py, color) }
                    Tool.SELECTION_RECTANGLE -> DrawingAlgorithms.drawRectangle(startX!!, startY!!, x, y, 1, false) { px, py -> if (px in 0 until state.width && py in 0 until state.height) previewBitmap.setPixel(px, py, Color.Gray.toArgb()) }
                    else -> {}
                }
                _projectState.update { it?.copy(version = it.version + 1) }
            }
            else -> {}
        }
        _projectState.update { it?.copy(layers = it.layers.toList(), version = it.version + 1) }
    }

    private fun saveState() {
        val state = _projectState.value ?: return
        val layersCopy = state.layers.map { it.copy(bitmap = it.bitmap.copy(Bitmap.Config.ARGB_8888, true)) }
        undoStack.push(layersCopy); redoStack.clear()
        if (undoStack.size > 20) undoStack.removeAt(0)
        
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val state = _projectState.value ?: return
        redoStack.push(state.layers.map { it.copy(bitmap = it.bitmap.copy(Bitmap.Config.ARGB_8888, true)) })
        _projectState.update { it?.copy(layers = undoStack.pop(), version = it.version + 1) }
        
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val state = _projectState.value ?: return
        undoStack.push(state.layers.map { it.copy(bitmap = it.bitmap.copy(Bitmap.Config.ARGB_8888, true)) })
        _projectState.update { it?.copy(layers = redoStack.pop(), version = it.version + 1) }
        
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun addLayer() {
        val state = _projectState.value ?: return
        val newLayer = PixelLayer(name = "Layer ${state.layers.size + 1}", bitmap = Bitmap.createBitmap(state.width, state.height, Bitmap.Config.ARGB_8888))
        _projectState.update { it?.copy(layers = listOf(newLayer) + it.layers, activeLayerId = newLayer.id, version = it.version + 1) }
    }

    fun removeLayer(layerId: String) {
        val state = _projectState.value ?: return
        if (state.layers.size <= 1) return
        val newLayers = state.layers.filter { it.id != layerId }
        val newActiveId = if (state.activeLayerId == layerId) newLayers.first().id else state.activeLayerId
        _projectState.update { it?.copy(layers = newLayers, activeLayerId = newActiveId, version = it.version + 1) }
    }

    fun toggleLayerVisibility(layerId: String, isVisible: Boolean) {
        val state = _projectState.value ?: return
        val newLayers = state.layers.map { 
            if (it.id == layerId) it.copy(isVisible = isVisible) else it
        }
        _projectState.update { it?.copy(layers = newLayers, version = it.version + 1) }
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val state = _projectState.value ?: return
        val newLayers = state.layers.map { 
            if (it.id == layerId) it.copy(opacity = opacity) else it
        }
        _projectState.update { it?.copy(layers = newLayers, version = it.version + 1) }
    }

    fun setLayerBlendMode(layerId: String, blendMode: com.example.pixelshift.ui.editor.common.LayerBlendMode) {
        val state = _projectState.value ?: return
        val newLayers = state.layers.map { 
            if (it.id == layerId) it.copy(blendMode = blendMode) else it
        }
        _projectState.update { it?.copy(layers = newLayers, version = it.version + 1) }
    }

    fun duplicateLayer(layerId: String) {
        val state = _projectState.value ?: return
        val layers = state.layers
        val index = layers.indexOfFirst { it.id == layerId }
        if (index == -1) return
        
        saveState() // Snapshot before duplication
        
        val original = layers[index]
        // DEEP COPY: Creating a new physical memory space for the bitmap
        val config = original.bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888
        val newBitmap = original.bitmap.copy(config, true)
        val duplicatedLayer = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            bitmap = newBitmap
        )
        
        val newLayers = layers.toMutableList().apply { add(index, duplicatedLayer) }
        _projectState.update { it?.copy(layers = newLayers, activeLayerId = duplicatedLayer.id, version = it.version + 1) }
    }

    fun mergeDown(layerId: String) {
        val state = _projectState.value ?: return
        val layers = state.layers
        val index = layers.indexOfFirst { it.id == layerId }
        
        // Cannot merge if it's the bottom-most layer (index = size - 1)
        if (index == -1 || index >= layers.size - 1) return
        
        saveState() // Snapshot before destructive merge
        
        val upperLayer = layers[index]
        val lowerLayer = layers[index + 1]
        
        // --- ATOMIC MERGE TRANSACTION ---
        // 1. Connect Skia engine to the Lower Layer's memory
        val canvas = android.graphics.Canvas(lowerLayer.bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = false
            // 2. Extract visual states (Alpha)
            alpha = (upperLayer.opacity * 255).toInt().coerceIn(0, 255)
            
            // 3. Extract BlendMode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                blendMode = when (upperLayer.blendMode) {
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                }
            } else {
                @Suppress("DEPRECATION")
                xfermode = android.graphics.PorterDuffXfermode(
                    when (upperLayer.blendMode) {
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.NORMAL -> android.graphics.PorterDuff.Mode.SRC_OVER
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.MULTIPLY -> android.graphics.PorterDuff.Mode.MULTIPLY
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.SCREEN -> android.graphics.PorterDuff.Mode.SCREEN
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.OVERLAY -> android.graphics.PorterDuff.Mode.OVERLAY
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.DARKEN -> android.graphics.PorterDuff.Mode.DARKEN
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.LIGHTEN -> android.graphics.PorterDuff.Mode.LIGHTEN
                    }
                )
            }
        }
        
        // 4. Physical "Baking" of pixels
        canvas.drawBitmap(upperLayer.bitmap, 0f, 0f, paint)
        
        // 5. State Cleanup: Remove upper layer and fix pointer
        val newLayers = layers.toMutableList().apply { removeAt(index) }
        _projectState.update { it?.copy(layers = newLayers, activeLayerId = lowerLayer.id, version = it.version + 1) }
    }

    fun swapLayers(fromIndex: Int, toIndex: Int) {
        val state = _projectState.value ?: return
        val layers = state.layers.toMutableList()
        if (fromIndex !in layers.indices || toIndex !in layers.indices) return
        
        // --- HIGH PERFORMANCE SWAP ---
        // We do NOT saveState() here to avoid clogging the Undo stack 
        // with every single intermediate step of the drag.
        java.util.Collections.swap(layers, fromIndex, toIndex)
        
        _projectState.update { it?.copy(layers = layers, version = it.version + 1) }
    }

    fun finalizeReorder() {
        // --- SILENT UNDO SETTLEMENT ---
        // Now that the user has released their finger, we commit the final
        // permutation of layers as a single atomic history event.
        saveState()
    }

    fun toggleLayerLock(layerId: String, isLocked: Boolean) {
        val state = _projectState.value ?: return
        val newLayers = state.layers.map { 
            if (it.id == layerId) it.copy(isLocked = isLocked) else it
        }
        _projectState.update { it?.copy(layers = newLayers, version = it.version + 1) }
    }

    fun toggleXSymmetry(enabled: Boolean) {
        _projectState.update { it?.copy(symmetry = it.symmetry.copy(xEnabled = enabled), version = it.version + 1) }
    }

    fun toggleYSymmetry(enabled: Boolean) {
        _projectState.update { it?.copy(symmetry = it.symmetry.copy(yEnabled = enabled), version = it.version + 1) }
    }

    fun setReferenceImage(bitmap: Bitmap) {
        _projectState.update { it?.copy(referenceImage = it.referenceImage.copy(bitmap = bitmap, visible = true), version = it.version + 1) }
    }

    fun setReferenceImageOpacity(opacity: Float) {
        _projectState.update { it?.copy(referenceImage = it.referenceImage.copy(opacity = opacity), version = it.version + 1) }
    }

    fun updateReferenceImageMatrix(matrix: android.graphics.Matrix) {
        _projectState.update { it?.copy(referenceImage = it.referenceImage.copy(matrix = matrix), version = it.version + 1) }
    }

    fun clearReferenceImage() {
        _projectState.update { it?.copy(referenceImage = it.referenceImage.copy(bitmap = null), version = it.version + 1) }
    }

    fun selectLayer(layerId: String) { 
        _projectState.update { it?.copy(activeLayerId = layerId, version = it.version + 1) } 
    }

    fun generateExportBitmap(scale: Int): Bitmap? {
        val state = _projectState.value ?: return null
        val w = state.width * scale; val h = state.height * scale
        val destBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)
        
        // Background
        if (state.backgroundColor != Color.Transparent) {
            canvas.drawColor(state.backgroundColor.toArgb())
        }
        
        val paint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false // CRITICAL: NEAREST NEIGHBOR FOR CRISP PIXELS
            isDither = false
        }
        
        // Matrix for upscaling
        val matrix = android.graphics.Matrix()
        matrix.postScale(scale.toFloat(), scale.toFloat())
        
        // --- REUSE RENDERING PIPELINE FOR EXPORT ---
        val layerList = state.layers
        for (i in layerList.indices.reversed()) {
            val layer = layerList[i]
            if (!layer.isVisible) continue

            paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                paint.blendMode = when (layer.blendMode) {
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
                    com.example.pixelshift.ui.editor.common.LayerBlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                }
            } else {
                @Suppress("DEPRECATION")
                paint.xfermode = android.graphics.PorterDuffXfermode(
                    when (layer.blendMode) {
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.NORMAL -> android.graphics.PorterDuff.Mode.SRC_OVER
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.MULTIPLY -> android.graphics.PorterDuff.Mode.MULTIPLY
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.SCREEN -> android.graphics.PorterDuff.Mode.SCREEN
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.OVERLAY -> android.graphics.PorterDuff.Mode.OVERLAY
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.DARKEN -> android.graphics.PorterDuff.Mode.DARKEN
                        com.example.pixelshift.ui.editor.common.LayerBlendMode.LIGHTEN -> android.graphics.PorterDuff.Mode.LIGHTEN
                    }
                )
            }
            canvas.drawBitmap(layer.bitmap, matrix, paint)
        }
        return destBitmap
    }

    fun saveProject(outputStream: java.io.OutputStream) {
        val state = _projectState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipOut = java.util.zip.ZipOutputStream(outputStream)
                
                // 1. Create Manifest JSON
                val manifest = JSONObject().apply {
                    put("width", state.width)
                    put("height", state.height)
                    put("backgroundColor", state.backgroundColor.toArgb())
                    
                    val layersArray = org.json.JSONArray()
                    state.layers.forEach { layer ->
                        layersArray.put(JSONObject().apply {
                            put("id", layer.id)
                            put("name", layer.name)
                            put("isVisible", layer.isVisible)
                            put("opacity", layer.opacity.toDouble())
                            put("blendMode", layer.blendMode.name)
                            put("isLocked", layer.isLocked)
                        })
                    }
                    put("layers", layersArray)
                    
                    val paletteArray = org.json.JSONArray()
                    _palette.value.forEach { paletteArray.put(it.toArgb()) }
                    put("palette", paletteArray)
                }
                
                zipOut.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                zipOut.write(manifest.toString().toByteArray())
                zipOut.closeEntry()
                
                // 2. Save Layers as PNGs
                state.layers.forEach { layer ->
                    zipOut.putNextEntry(java.util.zip.ZipEntry("layers/${layer.id}.png"))
                    layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()
                }
                
                zipOut.finish()
                zipOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadProject(inputStream: java.io.InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipIn = java.util.zip.ZipInputStream(inputStream)
                var entry = zipIn.nextEntry
                var manifestJson: JSONObject? = null
                val layerBitmaps = mutableMapOf<String, Bitmap>()
                
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" -> {
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(zipIn))
                            manifestJson = JSONObject(reader.readText())
                        }
                        entry.name.startsWith("layers/") -> {
                            val layerId = entry.name.substringAfter("layers/").substringBefore(".png")
                            val bitmap = android.graphics.BitmapFactory.decodeStream(zipIn)
                            if (bitmap != null) {
                                layerBitmaps[layerId] = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                zipIn.close()
                
                if (manifestJson != null) {
                    val width = manifestJson.getInt("width")
                    val height = manifestJson.getInt("height")
                    val bgColor = Color(manifestJson.getInt("backgroundColor"))
                    
                    val layersArray = manifestJson.getJSONArray("layers")
                    val newLayers = mutableListOf<PixelLayer>()
                    for (i in 0 until layersArray.length()) {
                        val lObj = layersArray.getJSONObject(i)
                        val id = lObj.getString("id")
                        val bitmap = layerBitmaps[id] ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        newLayers.add(PixelLayer(
                            id = id,
                            name = lObj.getString("name"),
                            bitmap = bitmap,
                            isVisible = lObj.getBoolean("isVisible"),
                            opacity = lObj.getDouble("opacity").toFloat(),
                            blendMode = com.example.pixelshift.ui.editor.common.LayerBlendMode.valueOf(lObj.getString("blendMode")),
                            isLocked = lObj.optBoolean("isLocked", false)
                        ))
                    }
                    
                    val palArray = manifestJson.getJSONArray("palette")
                    val newPalette = mutableListOf<Color>()
                    for (i in 0 until palArray.length()) {
                        newPalette.add(Color(palArray.getInt(i)))
                    }
                    
                    withContext(Dispatchers.Main) {
                        _palette.value = newPalette
                        _projectState.value = ProjectState(
                            width = width,
                            height = height,
                            layers = newLayers,
                            activeLayerId = newLayers.firstOrNull()?.id ?: "",
                            backgroundColor = bgColor,
                            version = 0L
                        )
                        undoStack.clear()
                        redoStack.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawStroke(x: Int, y: Int, color: Int, size: Int, isDrag: Boolean, state: ProjectState, bitmap: Bitmap) {
        val plotPixel: (Int, Int) -> Unit = { px, py ->
            if (px in 0 until state.width && py in 0 until state.height) {
                val originalColor = bitmap.getPixel(px, py); bitmap.setPixel(px, py, color)
                if (_toolSettings.value.pixelPerfect && size == 1) {
                    currentStrokePath.add(PixelNode(px, py, originalColor))
                    if (currentStrokePath.size >= 3) {
                        val c = currentStrokePath.last(); val b = currentStrokePath[currentStrokePath.size - 2]; val a = currentStrokePath[currentStrokePath.size - 3]
                        if (kotlin.math.abs(a.x - c.x) == 1 && kotlin.math.abs(a.y - c.y) == 1) {
                            if ((b.x == a.x && b.y == c.y) || (b.x == c.x && b.y == a.y)) { bitmap.setPixel(b.x, b.y, b.oldColor); currentStrokePath.removeAt(currentStrokePath.size - 2) }
                        }
                    }
                }
            }
        }

        val symmetry = state.symmetry
        val multiPlot: (Int, Int) -> Unit = { px, py ->
            plotPixel(px, py)
            if (symmetry.xEnabled) {
                plotPixel(state.width - 1 - px, py)
            }
            if (symmetry.yEnabled) {
                plotPixel(px, state.height - 1 - py)
            }
            if (symmetry.xEnabled && symmetry.yEnabled) {
                plotPixel(state.width - 1 - px, state.height - 1 - py)
            }
        }

        if (lastX != null && lastY != null && isDrag) DrawingAlgorithms.drawLine(lastX!!, lastY!!, x, y, size, multiPlot)
        else { if (!isDrag) currentStrokePath.clear(); DrawingAlgorithms.drawLine(x, y, x, y, size, multiPlot) }
    }

    private fun getPixelColor(x: Int, y: Int, state: ProjectState): Int {
        if (x !in 0 until state.width || y !in 0 until state.height) return Color.Transparent.toArgb()
        if (_toolSettings.value.sampleAllLayers) {
            var finalR = 0f; var finalG = 0f; var finalB = 0f; var finalA = 0f
            state.layers.forEach { layer -> if (layer.isVisible) {
                val pixel = layer.bitmap.getPixel(x, y); val srcColor = Color(pixel); val srcA = srcColor.alpha
                if (srcA > 0f) {
                    if (finalA == 0f) { finalR = srcColor.red; finalG = srcColor.green; finalB = srcColor.blue; finalA = srcA }
                    else { val dstA = finalA; val newA = dstA + srcA * (1f - dstA); if (newA > 0f) { finalR = (finalR * dstA + srcColor.red * srcA * (1f - dstA)) / newA; finalG = (finalG * dstA + srcColor.green * srcA * (1f - dstA)) / newA; finalB = (finalB * dstA + srcColor.blue * srcA * (1f - dstA)) / newA; finalA = newA } }
                    if (finalA >= 0.99f) return@forEach
                }
            } }
            if (finalA < 1f && state.backgroundColor != Color.Transparent) {
                val bg = state.backgroundColor; val srcA = bg.alpha; val dstA = finalA; val newA = dstA + srcA * (1f - dstA)
                if (newA > 0f) { finalR = (finalR * dstA + bg.red * srcA * (1f - dstA)) / newA; finalG = (finalG * dstA + bg.green * srcA * (1f - dstA)) / newA; finalB = (finalB * dstA + bg.blue * srcA * (1f - dstA)) / newA; finalA = newA }
            }
            return Color(finalR, finalG, finalB, finalA).toArgb()
        } else {
            val activeLayer = state.layers.find { it.id == state.activeLayerId }
            return activeLayer?.bitmap?.getPixel(x, y) ?: Color.Transparent.toArgb()
        }
    }

    private fun createRectSelection(rect: android.graphics.Rect) {
        val state = _projectState.value ?: return; val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        val mask = DrawingAlgorithms.createRectMask(state.width, state.height, rect)
        val result = DrawingAlgorithms.extractSelection(activeLayer.bitmap, mask)
        if (result != null) {
            val (floatingBitmap, bounds) = result
            val selection = com.example.pixelshift.ui.editor.common.SelectionState(bitmap = floatingBitmap, mask = mask, x = bounds.left, y = bounds.top)
            _projectState.update { it?.copy(selection = selection, version = it.version + 1) }
        }
    }

    private fun createMagicWandSelection(x: Int, y: Int) {
        val state = _projectState.value ?: return; val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        val mask = DrawingAlgorithms.createMagicWandMask(activeLayer.bitmap, x, y)
        val result = DrawingAlgorithms.extractSelection(activeLayer.bitmap, mask)
        if (result != null) {
            val (floatingBitmap, bounds) = result
            val selection = com.example.pixelshift.ui.editor.common.SelectionState(bitmap = floatingBitmap, mask = mask, x = bounds.left, y = bounds.top)
            _projectState.update { it?.copy(selection = selection, version = it.version + 1) }
        }
    }

    fun commitSelection() {
        val state = _projectState.value ?: return; val selection = state.selection ?: return
        val activeLayer = state.layers.find { it.id == state.activeLayerId } ?: return
        DrawingAlgorithms.mergeSelection(activeLayer.bitmap, selection.bitmap, selection.x, selection.y)
        _projectState.update { it?.copy(selection = null, version = it.version + 1) }
    }

    fun moveSelection(dx: Int, dy: Int) {
        val state = _projectState.value ?: return; val selection = state.selection ?: return
        val newSelection = selection.copy(x = selection.x + dx, y = selection.y + dy)
        _projectState.update { it?.copy(selection = newSelection, version = it.version + 1) }
    }

    fun rotateSelection() {
        val state = _projectState.value ?: return; val selection = state.selection ?: return
        val newBitmap = DrawingAlgorithms.rotateBitmap90(selection.bitmap)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap), version = it.version + 1) }
    }

    fun flipSelection(horizontal: Boolean) {
        val state = _projectState.value ?: return; val selection = state.selection ?: return
        val newBitmap = DrawingAlgorithms.flipBitmap(selection.bitmap, horizontal)
        _projectState.update { it?.copy(selection = selection.copy(bitmap = newBitmap), version = it.version + 1) }
    }

    fun clearSelection() { commitSelection() }
}
