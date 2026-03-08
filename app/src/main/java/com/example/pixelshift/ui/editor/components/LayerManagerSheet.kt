package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.pixelshift.ui.editor.common.LayerBlendMode
import com.example.pixelshift.ui.editor.common.PixelLayer
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerManagerSheet(
    layers: List<PixelLayer>,
    activeLayerId: String,
    onLayerSelected: (String) -> Unit,
    onLayerVisibilityChanged: (String, Boolean) -> Unit,
    onLayerOpacityChanged: (String, Float) -> Unit,
    onLayerBlendModeChanged: (String, LayerBlendMode) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onMergeDown: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onFinalizeReorder: () -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onClose: () -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layers / 图层", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onAddLayer) {
                Icon(Icons.Default.Add, contentDescription = "Add Layer")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            itemsIndexed(
                items = layers,
                key = { _, layer -> layer.id }
            ) { index, layer ->
                val isSelected = layer.id == activeLayerId
                val isBottomLayer = index == layers.size - 1
                val isDragging = draggingIndex == index

                val itemModifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        if (isDragging) IntOffset(0, dragOffset.roundToInt())
                        else IntOffset.Zero
                    }
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                        else if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y

                                val threshold = 60f
                                if (dragOffset > threshold && index < layers.size - 1) {
                                    onReorder(index, index + 1)
                                    draggingIndex = index + 1
                                    dragOffset -= threshold
                                } else if (dragOffset < -threshold && index > 0) {
                                    onReorder(index, index - 1)
                                    draggingIndex = index - 1
                                    dragOffset += threshold
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffset = 0f
                                onFinalizeReorder()
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            }
                        )
                    }
                    .clickable { onLayerSelected(layer.id) }
                    .padding(8.dp)

                Column(modifier = itemModifier) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { onLayerVisibilityChanged(layer.id, !layer.isVisible) }) {
                            Icon(
                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility"
                            )
                        }

                        IconButton(onClick = { onDeleteLayer(layer.id) }, enabled = layers.size > 1) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Layer")
                        }
                    }

                    if (isSelected && !isDragging) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Opacity / 不透明度: ${(layer.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = layer.opacity,
                                onValueChange = { onLayerOpacityChanged(layer.id, it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Blend Mode / 混合模式", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LayerBlendMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = layer.blendMode == mode,
                                        onClick = { onLayerBlendModeChanged(layer.id, mode) },
                                        label = { Text(mode.name, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDuplicateLayer(layer.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Duplicate", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onMergeDown(layer.id) },
                                    enabled = !isBottomLayer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Merge Down", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close / 关闭")
        }
    }
}
