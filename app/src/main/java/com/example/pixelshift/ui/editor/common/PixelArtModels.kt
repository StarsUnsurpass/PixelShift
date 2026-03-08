package com.example.pixelshift.ui.editor.common

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * Supported Blend Modes for professional layer compositing.
 */
enum class LayerBlendMode {
    NORMAL,      // SRC_OVER
    MULTIPLY,    // Multiply colors
    SCREEN,      // Screen colors
    OVERLAY,     // Overlay colors
    DARKEN,      // Darken colors
    LIGHTEN      // Lighten colors
}

/**
 * PixelLayer is the core Entity of the layer system.
 * It follows the principle of Non-Destructive Editing.
 */
data class PixelLayer(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val bitmap: Bitmap,
        val isVisible: Boolean = true,
        val opacity: Float = 1f,
        val isLocked: Boolean = false,
        val blendMode: LayerBlendMode = LayerBlendMode.NORMAL
)

enum class Tool {
        PENCIL,
        ERASER,
        FILL,
        EYEDROPPER,
        SHAPE_LINE,
        SHAPE_RECTANGLE,
        SHAPE_CIRCLE,
        SELECTION_RECTANGLE,
        SELECTION_MAGIC_WAND,
        MOVE
}

data class ToolSettings(
        val size: Int = 1, // 1px, 2px, 4px...
        val rectFilled: Boolean = false,
        val circleFilled: Boolean = false,
        val pixelPerfect: Boolean = false,
        val eraseToBackground: Boolean = false,
        val sampleAllLayers: Boolean = true,
        val contiguous: Boolean = true // Bucket Fill Mode: true = Contiguous, false = Global
)

data class SelectionState(
        val bitmap: Bitmap, // The floating pixels
        val mask: Bitmap, // Binary mask (alpha channel mainly used)
        val x: Int, // Current X position on canvas
        val y: Int // Current Y position on canvas
)

data class ProjectState(
        val id: String = java.util.UUID.randomUUID().toString(),
        val width: Int,
        val height: Int,
        val layers: List<PixelLayer>,
        val activeLayerId: String,
        val backgroundColor: Color = Color.Transparent,
        val previewLayer: PixelLayer? = null,
        val selection: SelectionState? = null,
        val version: Long = 0L
)
