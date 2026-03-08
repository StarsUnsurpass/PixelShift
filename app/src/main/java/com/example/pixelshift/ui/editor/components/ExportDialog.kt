package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExportDialog(onDismiss: () -> Unit, onExport: (String, Int) -> Unit) {
    var filename by remember { mutableStateOf("pixel_art_export") }
    var scale by remember { mutableStateOf(10f) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Lossless PNG / 无损导出") },
            text = {
                Column {
                    OutlinedTextField(
                            value = filename,
                            onValueChange = { filename = it },
                            label = { Text("Filename / 文件名") },
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Upscale Factor / 放大倍率: ${scale.toInt()}x", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                    
                    Text(
                        "Technique: Nearest Neighbor (Sharp Edges)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "Output: ${scale.toInt()}x actual size, no blur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = { 
                Button(onClick = { onExport(filename, scale.toInt()) }) { 
                    Text("Export / 导出") 
                } 
            },
            dismissButton = { 
                TextButton(onClick = onDismiss) { 
                    Text("Cancel / 取消") 
                } 
            }
    )
}
