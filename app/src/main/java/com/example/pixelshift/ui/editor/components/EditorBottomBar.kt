package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelshift.ui.editor.common.Tool
import com.example.pixelshift.ui.editor.common.ToolSettings

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border

@Composable
fun EditorBottomBar(
        currentTool: Tool,
        onToolSelected: (Tool) -> Unit,
        toolSettings: ToolSettings,
        onToggleShapeFilled: () -> Unit,
        currentColor: Color,
        onPaletteClick: () -> Unit,
        palette: List<Color>,
        onPaletteColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Group 1: Basic Tools
            ToolButton(
                    icon = Icons.Default.Brush,
                    label = "笔",
                    isSelected = currentTool == Tool.PENCIL,
                    onClick = { onToolSelected(Tool.PENCIL) }
            )
            ToolButton(
                    icon = Icons.Default.Delete,
                    label = "橡皮",
                    isSelected = currentTool == Tool.ERASER,
                    onClick = { onToolSelected(Tool.ERASER) }
            )
            ToolButton(
                    icon = Icons.Default.FormatPaint,
                    label = "油漆桶",
                    isSelected = currentTool == Tool.FILL,
                    onClick = { onToolSelected(Tool.FILL) }
            )
            ToolButton(
                    icon = Icons.Default.Colorize,
                    label = "吸管",
                    isSelected = currentTool == Tool.EYEDROPPER,
                    onClick = { onToolSelected(Tool.EYEDROPPER) }
            )

            // Separator
            Box(Modifier.size(1.dp, 24.dp).background(Color.LightGray))

            // Group 2: Shapes
            ToolButton(
                    icon = Icons.Default.HorizontalRule,
                    label = "直线",
                    isSelected = currentTool == Tool.SHAPE_LINE,
                    onClick = { onToolSelected(Tool.SHAPE_LINE) }
            )
            ToolButton(
                    icon = if (toolSettings.rectFilled) Icons.Default.Stop else Icons.Default.CheckBoxOutlineBlank,
                    label = "矩形",
                    isSelected = currentTool == Tool.SHAPE_RECTANGLE,
                    onClick = { 
                        if (currentTool == Tool.SHAPE_RECTANGLE) onToggleShapeFilled()
                        else onToolSelected(Tool.SHAPE_RECTANGLE)
                    }
            )
            ToolButton(
                    icon = if (toolSettings.circleFilled) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
                    label = "圆形",
                    isSelected = currentTool == Tool.SHAPE_CIRCLE,
                    onClick = { 
                        if (currentTool == Tool.SHAPE_CIRCLE) onToggleShapeFilled()
                        else onToolSelected(Tool.SHAPE_CIRCLE)
                    }
            )

            // Separator
            Box(Modifier.size(1.dp, 24.dp).background(Color.LightGray))

            // Group 3: Selection & Move
            ToolButton(
                    icon = Icons.Default.SelectAll,
                    label = "选区",
                    isSelected = currentTool == Tool.SELECTION_RECTANGLE,
                    onClick = { onToolSelected(Tool.SELECTION_RECTANGLE) }
            )
            ToolButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "魔棒",
                    isSelected = currentTool == Tool.SELECTION_MAGIC_WAND,
                    onClick = { onToolSelected(Tool.SELECTION_MAGIC_WAND) }
            )
            ToolButton(
                    icon = Icons.Default.OpenWith,
                    label = "移动",
                    isSelected = currentTool == Tool.MOVE,
                    onClick = { onToolSelected(Tool.MOVE) }
            )

            // Color Preview (Main Selector)
            Box(
                    modifier =
                            Modifier.size(40.dp)
                                    .clip(CircleShape)
                                    .background(currentColor)
                                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { onPaletteClick() }
                                    .then(
                                            if (currentColor == Color.Transparent ||
                                                            currentColor == Color.White
                                            ) {
                                                Modifier.background(Color.Gray.copy(alpha = 0.2f))
                                            } else Modifier
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                if (currentColor == Color.Transparent) {
                    Text("T", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        // --- THE ACTIVE PALETTE ---
        // Zero-latency UI with Spacer for extreme performance
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(palette) { color ->
                val isSelected = color == currentColor
                Spacer(
                    modifier = Modifier
                        .size(if (isSelected) 32.dp else 24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onPaletteColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector, 
    label: String,
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint =
                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
