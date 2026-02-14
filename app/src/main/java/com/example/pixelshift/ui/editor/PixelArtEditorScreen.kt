package com.example.pixelshift.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.ui.editor.components.EditorBottomBar
import com.example.pixelshift.ui.editor.components.LayerManagerSheet
import com.example.pixelshift.ui.editor.components.PixelCanvas
import com.example.pixelshift.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelArtEditorScreen(
        navController: NavController,
        width: Int,
        height: Int,
        isTransparentBackground: Boolean,
        backgroundColor: Int,
        viewModel: PixelArtViewModel = viewModel(),
        themeViewModel: ThemeViewModel? = null
) {
    val projectState by viewModel.projectState.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState() // Added observation

    var showLayerSheet by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        if (projectState == null) {
            val bgColor =
                    if (isTransparentBackground) Color.Transparent else Color(backgroundColor)
            viewModel.initializeProject(width, height, bgColor)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Pixel Editor") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.undo() }) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo")
                            }
                            IconButton(onClick = { viewModel.redo() }) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo")
                            }
                            IconButton(onClick = { showLayerSheet = true }) {
                                Icon(Icons.Default.Layers, contentDescription = "Layers")
                            }
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                )
            },
            bottomBar = {
                EditorBottomBar(
                        currentTool = currentTool,
                        onToolSelected = { viewModel.setTool(it) },
                        currentColor = currentColor,
                        onPaletteClick = { showColorPickerSheet = true }
                )
            }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (projectState != null) {
                PixelCanvas(
                        projectState = projectState!!,
                        onTap = { x, y ->
                            viewModel.onPixelAction(x, y, isDrag = false, isActionEnd = true)
                        },
                        onDragStart = { x, y -> viewModel.onPixelAction(x, y, isDrag = false) },
                        onDrag = { x, y -> viewModel.onPixelAction(x, y, isDrag = true) },
                        onDragEnd = {
                            viewModel.onPixelAction(0, 0, isDrag = true, isActionEnd = true)
                        }
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        if (showLayerSheet && projectState != null) {
            ModalBottomSheet(
                    onDismissRequest = { showLayerSheet = false },
                    sheetState = sheetState
            ) {
                LayerManagerSheet(
                        layers = projectState!!.layers,
                        activeLayerId = projectState!!.activeLayerId,
                        onLayerSelected = { viewModel.selectLayer(it) },
                        onLayerVisibilityChanged = { id, visible ->
                            viewModel.toggleLayerVisibility(id, visible)
                        },
                        onLayerOpacityChanged = { _, _ -> /* TODO */ },
                        onAddLayer = { viewModel.addLayer() },
                        onDeleteLayer = { viewModel.removeLayer(it) },
                        onClose = { showLayerSheet = false }
                )
            }
        }

        if (showColorPickerSheet) {
            ModalBottomSheet(
                    onDismissRequest = { showColorPickerSheet = false },
                    sheetState = sheetState
            ) {
                com.example.pixelshift.ui.editor.components.ColorPickerSheet(
                        initialColor = currentColor,
                        palette = viewModel.palette.collectAsState().value,
                        onColorSelected = {
                            viewModel.setColor(it)
                            showColorPickerSheet = false
                        },
                        onClose = { showColorPickerSheet = false }
                )
            }
        }

        if (showExportDialog) {
            com.example.pixelshift.ui.editor.components.ExportDialog(
                    onDismiss = { showExportDialog = false },
                    onExport = { filename, scale ->
                        val bitmap = viewModel.generateExportBitmap(scale)
                        if (bitmap != null) {
                            saveImageToGallery(context, bitmap, filename)
                        }
                        showExportDialog = false
                    }
            )
        }
    }
}

fun saveImageToGallery(
        context: android.content.Context,
        bitmap: android.graphics.Bitmap,
        filename: String
) {
    val resolver = context.contentResolver
    val contentValues =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/PixelShift"
                )
            }

    val uri =
            resolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            )

    if (uri != null) {
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        }
        // Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
    }
}
