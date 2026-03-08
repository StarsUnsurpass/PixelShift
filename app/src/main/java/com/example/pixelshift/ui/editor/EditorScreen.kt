package com.example.pixelshift.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val viewModel: EditorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pixel Art Creator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                uiState.preview?.let { bitmap ->
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Processed Image",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Text("No image selected")
                
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Settings", style = MaterialTheme.typography.titleMedium)

                    // Pixel Size (previously Scale)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pixel Size", modifier = Modifier.width(80.dp))
                        Slider(
                            value = uiState.config.pixelSize.toFloat(),
                            onValueChange = { viewModel.updatePixelSize(it.toInt()) },
                            valueRange = 1f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${uiState.config.pixelSize}x", modifier = Modifier.width(40.dp))
                    }

                    // Contrast (since colorCount isn't directly in config the same way)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Contrast", modifier = Modifier.width(80.dp))
                        Slider(
                            value = uiState.config.contrast,
                            onValueChange = { viewModel.updateContrast(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dither", modifier = Modifier.width(80.dp))
                        DitherDropdown(
                            selectedDither = uiState.config.ditherType,
                            onDitherSelected = { viewModel.updateDither(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Smooth Image Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Smooth Image")
                        Switch(
                            checked = uiState.config.smoothImage,
                            onCheckedChange = { viewModel.toggleSmoothImage(it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DitherDropdown(
    selectedDither: DitherType,
    onDitherSelected: (DitherType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDither.name)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DitherType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onDitherSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
