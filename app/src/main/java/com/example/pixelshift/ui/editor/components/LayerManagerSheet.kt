package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.PixelLayer

@Composable
fun LayerManagerSheet(
        layers: List<PixelLayer>,
        activeLayerId: String,
        onLayerSelected: (String) -> Unit,
        onLayerVisibilityChanged: (String, Boolean) -> Unit,
        onLayerOpacityChanged: (String, Float) -> Unit,
        onAddLayer: () -> Unit,
        onDeleteLayer: (String) -> Unit,
        onClose: () -> Unit
) {
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

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(layers) { layer ->
                                val isSelected = layer.id == activeLayerId
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clip(MaterialTheme.shapes.medium)
                                                        .background(
                                                                if (isSelected)
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer
                                                                else Color.Transparent
                                                        )
                                                        .clickable { onLayerSelected(layer.id) }
                                                        .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        text = layer.name,
                                                        style = MaterialTheme.typography.bodyLarge
                                                )
                                                if (isSelected) {
                                                        Slider(
                                                                value = layer.opacity,
                                                                onValueChange = {
                                                                        onLayerOpacityChanged(
                                                                                layer.id,
                                                                                it
                                                                        )
                                                                },
                                                                modifier = Modifier.fillMaxWidth()
                                                        )
                                                }
                                        }

                                        IconButton(
                                                onClick = {
                                                        onLayerVisibilityChanged(
                                                                layer.id,
                                                                !layer.isVisible
                                                        )
                                                }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                if (layer.isVisible)
                                                                        Icons.Default.Visibility
                                                                else Icons.Default.VisibilityOff,
                                                        contentDescription = "Toggle Visibility"
                                                )
                                        }

                                        IconButton(onClick = { onDeleteLayer(layer.id) }) {
                                                Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Layer"
                                                )
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
