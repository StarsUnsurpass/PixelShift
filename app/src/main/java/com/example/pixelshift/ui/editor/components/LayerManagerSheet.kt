package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layers", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onAddLayer) {
                Icon(Icons.Default.Add, contentDescription = "Add Layer")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
                modifier = Modifier.fillMaxWidth().height(300.dp) // Fixed height for sheet content
        ) {
            // Show layers in reverse order (top layer first)
            items(layers.asReversed()) { layer ->
                LayerItem(
                        layer = layer,
                        isActive = layer.id == activeLayerId,
                        onSelect = { onLayerSelected(layer.id) },
                        onVisibilityChange = { onLayerVisibilityChanged(layer.id, it) },
                        onDelete = { onDeleteLayer(layer.id) }
                        // TODO: Opacity slider
                        )
                Divider()
            }
        }
    }
}

@Composable
fun LayerItem(
        layer: PixelLayer,
        isActive: Boolean,
        onSelect: () -> Unit,
        onVisibilityChange: (Boolean) -> Unit,
        onDelete: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onSelect() }
                            .background(
                                    if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                            )
                            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Image(
                bitmap = layer.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier =
                        Modifier.size(40.dp)
                                .background(Color.White) // Checkerboard logic needed here too?
                                .border(1.dp, Color.Gray),
                contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
                text = layer.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
        )

        IconButton(onClick = { onVisibilityChange(!layer.isVisible) }) {
            Icon(
                    imageVector =
                            if (layer.isVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                    contentDescription = null
            )
        }

        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
    }
}
