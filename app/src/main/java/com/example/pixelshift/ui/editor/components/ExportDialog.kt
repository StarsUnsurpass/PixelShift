package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(onDismiss: () -> Unit, onExport: (String, Int) -> Unit) {
    var filename by remember { mutableStateOf("pixel_art") }
    var scale by remember { mutableStateOf(10) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Image") },
            text = {
                Column {
                    OutlinedTextField(
                            value = filename,
                            onValueChange = { filename = it },
                            label = { Text("Filename") },
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Scale: ${scale}x")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = scale == 1, onClick = { scale = 1 })
                        Text("1x")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = scale == 10, onClick = { scale = 10 })
                        Text("10x")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = scale == 20, onClick = { scale = 20 })
                        Text("20x")
                    }
                }
            },
            confirmButton = { Button(onClick = { onExport(filename, scale) }) { Text("Export") } },
            dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}
