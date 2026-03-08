package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerSheet(
    initialColor: Color,
    palette: List<Color>,
    onColorSelected: (Color) -> Unit,
    onLoadPreset: (String) -> Unit,
    onFetchLospec: (String) -> Unit,
    onClose: () -> Unit
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(initialColor) }
    
    // Hex input state
    var hexInput by remember { mutableStateOf(colorToHex(initialColor)) }
    var lospecSlug by remember { mutableStateOf("") }

    // Sync from picker to Hex input
    LaunchedEffect(selectedColor) {
        val newHex = colorToHex(selectedColor)
        if (newHex.uppercase() != hexInput.uppercase()) {
            hexInput = newHex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .height(600.dp) // Fixed height to allow internal scrolling if needed
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Colors / 颜色", style = MaterialTheme.typography.titleLarge)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Color Picker
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            controller = controller,
            initialColor = initialColor,
            onColorChanged = { envelope ->
                selectedColor = envelope.color
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            controller = controller
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            controller = controller
        )

        Spacer(modifier = Modifier.height(16.dp))

        // HEX Input (Two-way binding)
        OutlinedTextField(
            value = hexInput,
            onValueChange = { 
                hexInput = it
                if (it.length == 6 || it.length == 8) {
                    try {
                        val parsedColor = Color(android.graphics.Color.parseColor(if (it.startsWith("#")) it else "#$it"))
                        selectedColor = parsedColor
                        // Note: controller doesn't easily update from external state without recomposition.
                        // We rely on the Select button to actually apply the color.
                    } catch (e: Exception) {
                        // Ignore invalid hex
                    }
                }
            },
            label = { Text("HEX Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Presets & Lospec
        Text("Presets / 预设", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onLoadPreset("gameboy"); onClose() }) { Text("GameBoy") }
            OutlinedButton(onClick = { onLoadPreset("pico-8"); onClose() }) { Text("Pico-8") }
            OutlinedButton(onClick = { onLoadPreset("nes"); onClose() }) { Text("NES") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = lospecSlug,
                onValueChange = { lospecSlug = it },
                label = { Text("Lospec Slug (e.g. dawnbringer-16)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { 
                    if (lospecSlug.isNotBlank()) {
                        onFetchLospec(lospecSlug)
                        onClose()
                    }
                }
            ) {
                Text("Fetch")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { 
                onColorSelected(selectedColor)
                onClose() 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select / 确认选择")
        }
    }
}

private fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return if (a == 255) {
        String.format("%02X%02X%02X", r, g, b)
    } else {
        String.format("%02X%02X%02X%02X", a, r, g, b)
    }
}
