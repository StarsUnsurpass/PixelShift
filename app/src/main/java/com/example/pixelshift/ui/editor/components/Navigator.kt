package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ProjectState
import com.example.pixelshift.ui.editor.common.ViewportState

@Composable
fun Navigator(
    modifier: Modifier = Modifier,
    projectState: ProjectState,
    viewportState: ViewportState,
    screenSize: Size // We need screen size to calculate the viewport rect
) {
    val projectWidth = projectState.width
    val projectHeight = projectState.height

    // Fixed navigator size or adaptive? 
    // Let's make it fixed size for now (e.g. 100dp)
    val navigatorSizeDp = 120.dp
    val navigatorSizePx = with(LocalDensity.current) { navigatorSizeDp.toPx() }
    
    // Calculate scaling to fit project into navigator
    val navScaleX = navigatorSizePx / projectWidth
    val navScaleY = navigatorSizePx / projectHeight
    val navScale = minOf(navScaleX, navScaleY)
    
    val navContentWidth = projectWidth * navScale
    val navContentHeight = projectHeight * navScale
    
    // Centering in navigator
    val navOffsetX = (navigatorSizePx - navContentWidth) / 2f
    val navOffsetY = (navigatorSizePx - navContentHeight) / 2f
    
    Box(
        modifier = modifier
            .size(navigatorSizeDp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        
                        // Convert drag in navigator to pan in main viewport
                        // Navigator Drag Delta / Navigator Scale = Canvas Pixel Delta
                        // Canvas Pixel Delta * Viewport Scale = Viewport Pan Delta (inverted)
                        
                        // Easier: 
                        // Position in Navigator -> Position in Project Pixels
                        // New Viewport Offset = Centroid - (Position in Project Pixels * Viewport Scale)
                        
                        // Relative movement:
                        // scaledDragX = dragAmount.x / navScale
                        // viewportState.offsetX -= scaledDragX * viewportState.scale
                        
                        val pixelDeltaX = dragAmount.x / navScale
                        val pixelDeltaY = dragAmount.y / navScale
                        
                        viewportState.pan(-pixelDeltaX * viewportState.scale, -pixelDeltaY * viewportState.scale)
                    }
                }
        ) {
            // 1. Draw Project Preview
            with(drawContext.canvas.nativeCanvas) {
                 val paint = android.graphics.Paint().apply {
                     isFilterBitmap = true // Smooth preview in navigator
                 }
                 
                 projectState.layers.asReversed().forEach { layer ->
                     if (layer.isVisible) {
                         val src = android.graphics.Rect(0, 0, layer.bitmap.width, layer.bitmap.height)
                         val dst = android.graphics.RectF(
                             navOffsetX,
                             navOffsetY,
                             navOffsetX + navContentWidth,
                             navOffsetY + navContentHeight
                         )
                         drawBitmap(layer.bitmap, src, dst, paint)
                     }
                 }
            }
            
            // 2. Draw Viewport Rect
            // The main viewport shows a rectangle of the canvas:
            // Left (px) = -viewportState.offsetX / viewportState.scale
            // Top (px) = -viewportState.offsetY / viewportState.scale
            // Width (px) = screenSize.width / viewportState.scale
            // Height (px) = screenSize.height / viewportState.scale
            
            val visibleLeft = -viewportState.offsetX / viewportState.scale
            val visibleTop = -viewportState.offsetY / viewportState.scale
            val visibleWidth = screenSize.width / viewportState.scale
            val visibleHeight = screenSize.height / viewportState.scale
            
            // Map to Navigator Coordinates
            val rectLeft = navOffsetX + visibleLeft * navScale
            val rectTop = navOffsetY + visibleTop * navScale
            val rectWidth = visibleWidth * navScale
            val rectHeight = visibleHeight * navScale
            
            drawRect(
                color = Color.Red,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 2f)
            )
        }
    }
}
