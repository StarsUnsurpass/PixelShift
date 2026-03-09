package com.example.pixelshift.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.ui.editor.components.EditorBottomBar
import com.example.pixelshift.ui.editor.components.LayerManagerSheet
import com.example.pixelshift.ui.editor.components.Magnifier
import com.example.pixelshift.ui.editor.components.PixelCanvas
import com.example.pixelshift.ui.editor.common.rememberViewportState
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.ui.editor.common.Tool

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
    val currentColor by viewModel.currentColor.collectAsState()
    val magnifierState by viewModel.magnifierState.collectAsState()
    
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var showLayerSheet by remember { mutableStateOf(false) }
    var showToolSettingsSheet by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val viewportState = rememberViewportState()

    val saveProjectLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.saveProject(stream)
            }
        }
    }

    val loadProjectLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.loadProject(stream)
            }
        }
    }
    
    val referenceImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                    val bitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                    viewModel.setReferenceImage(bitmap)
                } else {
                    @Suppress("DEPRECATION")
                    val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    viewModel.setReferenceImage(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (projectState == null) {
            val bgColor = if (isTransparentBackground) Color.Transparent else Color(backgroundColor)
            viewModel.initializeProject(width, height, isTransparentBackground, bgColor)
        }
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { 
                            Text(
                                text = "像素编辑器",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            IconButton(onClick = { loadProjectLauncher.launch(arrayOf("application/octet-stream")) }) {
                                Icon(Icons.Default.Folder, contentDescription = "加载项目")
                            }
                            IconButton(onClick = { saveProjectLauncher.launch("my_drawing.pxl") }) {
                                Icon(Icons.Default.Save, contentDescription = "保存项目")
                            }
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = canUndo
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo, 
                                    contentDescription = "撤销",
                                    tint = if (canUndo) Color.Unspecified else Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = canRedo
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo, 
                                    contentDescription = "重做",
                                    tint = if (canRedo) Color.Unspecified else Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = { showToolSettingsSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "工具设置")
                            }
                            IconButton(onClick = { showLayerSheet = true }) {
                                Icon(Icons.Default.Layers, contentDescription = "图层")
                            }
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.Save, contentDescription = "保存")
                            }
                        }
                )
            },
            bottomBar = {
                val toolSettings by viewModel.toolSettings.collectAsState()
                EditorBottomBar(
                        currentTool = currentTool,
                        onToolSelected = { viewModel.setTool(it) },
                        toolSettings = toolSettings,
                        onToggleShapeFilled = { 
                            if (currentTool == Tool.SHAPE_RECTANGLE) {
                                viewModel.toggleRectFilled(!toolSettings.rectFilled)
                            } else if (currentTool == Tool.SHAPE_CIRCLE) {
                                viewModel.toggleCircleFilled(!toolSettings.circleFilled)
                            }
                        },
                        currentColor = currentColor,
                        onPaletteClick = { showColorPickerSheet = true },
                        palette = viewModel.palette.collectAsState().value,
                        onPaletteColorSelected = { viewModel.setColor(it) }
                )
            }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenSizePx = androidx.compose.ui.geometry.Size(screenWidthPx, screenHeightPx)

            if (projectState != null) {
                val toolSettings by viewModel.toolSettings.collectAsState()
                val hoverPos by viewModel.hoverPosition.collectAsState()

                PixelCanvas(
                        projectState = projectState!!,
                        viewportState = viewportState,
                        brushSize = toolSettings.size,
                        hoverPosition = hoverPos,
                        onTap = { x, y, rx, ry ->
                            viewModel.onPixelAction(x, y, isDrag = false, isActionEnd = true, rawX = rx, rawY = ry)
                        },
                        onDragStart = { x, y, rx, ry, isShortcut ->
                            viewModel.onPixelAction(x, y, isDrag = false, rawX = rx, rawY = ry, isShortcut = isShortcut)
                        },
                        onDrag = { x, y, rx, ry ->
                            viewModel.onPixelAction(x, y, isDrag = true, rawX = rx, rawY = ry)
                        },
                        onDragEnd = {
                            viewModel.onPixelAction(-1, -1, isDrag = true, isActionEnd = true)
                        },
                        onLongPressStart = {
                            viewModel.startEyedropperOverride()
                        },
                        onLongPressStop = {
                            viewModel.stopEyedropperOverride()
                        },
                        onUndo = { if (canUndo) viewModel.undo() },
                        onRedo = { if (canRedo) viewModel.redo() }
                )

                // Overlays
                Box(modifier = Modifier.fillMaxSize()) {
                    if (magnifierState.visible) {
                        val loupeSizeDp = 140.dp
                        val loupeSizePx = with(density) { loupeSizeDp.toPx() }
                        val offsetDp = 100.dp
                        val offsetPx = with(density) { offsetDp.toPx() }
                        
                        val isNearTop = magnifierState.screenY < with(density) { 150.dp.toPx() }
                        val targetY = if (isNearTop) {
                            magnifierState.screenY + offsetPx - loupeSizePx / 2
                        } else {
                            magnifierState.screenY - offsetPx - loupeSizePx / 2
                        }
                        
                        val targetX = magnifierState.screenX - loupeSizePx / 2

                        Magnifier(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        targetX.toInt(),
                                        targetY.toInt()
                                    )
                                },
                            magnifierState = magnifierState,
                            projectState = projectState!!,
                            brushSize = 1,
                            magnifierSize = loupeSizeDp
                        )
                    }
                    
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                         com.example.pixelshift.ui.editor.components.Navigator(
                             projectState = projectState!!,
                             viewportState = viewportState,
                             screenSize = screenSizePx
                         )
                    }
                }
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
                        onLayerOpacityChanged = { id, opacity ->
                            viewModel.setLayerOpacity(id, opacity)
                        },
                        onLayerBlendModeChanged = { id, mode ->
                            viewModel.setLayerBlendMode(id, mode)
                        },
                        onDuplicateLayer = { id ->
                            viewModel.duplicateLayer(id)
                        },
                        onMergeDown = { id ->
                            viewModel.mergeDown(id)
                        },
                        onReorder = { from, to ->
                            viewModel.swapLayers(from, to)
                        },
                        onFinalizeReorder = {
                            viewModel.finalizeReorder()
                        },
                        onAddLayer = { viewModel.addLayer() },
                        onDeleteLayer = { viewModel.removeLayer(it) },
                        onClose = { showLayerSheet = false }
                )
            }
        }

        if (showToolSettingsSheet) {
            ModalBottomSheet(
                    onDismissRequest = { showToolSettingsSheet = false },
                    sheetState = sheetState
            ) {
                com.example.pixelshift.ui.editor.components.ToolSettingsSheet(
                        settings = viewModel.toolSettings.collectAsState().value,
                        onSizeChange = { viewModel.setToolSize(it) },
                        onPixelPerfectChange = { viewModel.togglePixelPerfect(it) },
                        onEraseToBackgroundChange = { viewModel.toggleEraseToBackground(it) },
                        secondaryColor = viewModel.secondaryColor.collectAsState().value,
                        onSecondaryColorChange = { viewModel.setSecondaryColor(it) },
                        onSampleAllLayersChange = { viewModel.toggleSampleAllLayers(it) },
                        onContiguousChange = { viewModel.toggleContiguous(it) },
                        onRectFilledChange = { viewModel.toggleRectFilled(it) },
                        onCircleFilledChange = { viewModel.toggleCircleFilled(it) },
                        hasSelection = projectState?.selection != null,
                        onRotateSelection = { viewModel.rotateSelection() },
                        onFlipHorizontal = { viewModel.flipSelection(true) },
                        onFlipVertical = { viewModel.flipSelection(false) },
                        onClearSelection = { viewModel.clearSelection() },
                        symmetry = projectState?.symmetry ?: com.example.pixelshift.ui.editor.common.SymmetryState(),
                        onXSymmetryChange = { viewModel.toggleXSymmetry(it) },
                        onYSymmetryChange = { viewModel.toggleYSymmetry(it) },
                        referenceVisible = projectState?.referenceImage?.bitmap != null,
                        onReferenceImagePick = { referenceImagePicker.launch("image/*") },
                        onReferenceClear = { viewModel.clearReferenceImage() },
                        onClose = { showToolSettingsSheet = false }
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
                        onLoadPreset = { viewModel.loadPreset(it) },
                        onFetchLospec = { viewModel.fetchLospecPalette(it) },
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
    }
}
