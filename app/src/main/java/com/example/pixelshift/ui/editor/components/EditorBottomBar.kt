package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.Tool

@Composable
fun EditorBottomBar(
    currentTool: Tool,
    onToolSelected: (Tool) -> Unit,
    currentColor: Color,
    onPaletteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton(
            icon = Icons.Default.Brush, // Pencil
            isSelected = currentTool == Tool.PENCIL,
            onClick = { onToolSelected(Tool.PENCIL) }
        )
        ToolButton(
            icon = Icons.Default.Delete, // Eraser (Using Delete icon for now)
            isSelected = currentTool == Tool.ERASER,
            onClick = { onToolSelected(Tool.ERASER) }
        )
        ToolButton(
            icon = Icons.Default.FormatPaint, // Bucket Fill
            isSelected = currentTool == Tool.FILL,
            onClick = { onToolSelected(Tool.FILL) }
        )
        ToolButton(
            icon = Icons.Default.Colorize, // Eyedropper
            isSelected = currentTool == Tool.EYEDROPPER,
            onClick = { onToolSelected(Tool.EYEDROPPER) }
        )
        
        // Color Preview / Palette Trigger
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(currentColor)
                .clickable { onPaletteClick() }
                .then(
                    if (currentColor == Color.Transparent || currentColor == Color.White) {
                         Modifier.background(Color.Gray.copy(alpha = 0.2f)) // Borderish
                    } else Modifier
                )
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}
