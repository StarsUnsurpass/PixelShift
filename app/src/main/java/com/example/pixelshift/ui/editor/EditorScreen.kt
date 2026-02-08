package com.example.pixelshift.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.domain.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, viewModel: EditorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri: Uri? ->
                uri?.let { viewModel.loadImage(context, it) }
            }

    // Launch picker if no image loaded
    LaunchedEffect(Unit) {
        if (uiState.original == null) {
            launcher.launch("image/*")
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("图片编辑器") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            IconButton(
                                    onClick = {
                                        val uri = viewModel.exportImage(context)
                                        if (uri != null) {
                                            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT)
                                                    .show()
                                        } else {
                                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT)
                                                    .show()
                                        }
                                    }
                            ) { Icon(Icons.Default.Save, contentDescription = "保存") }
                        }
                )
            },
            bottomBar = {
                if (uiState.original != null) {
                    EditorControls(
                            uiState = uiState,
                            onPixelSizeChange = viewModel::updatePixelSize,
                            onPaletteChange = viewModel::updatePalette,
                            onDitherChange = viewModel::updateDither,
                            onContrastChange = viewModel::updateContrast
                    )
                }
            }
    ) { paddingValues ->
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (uiState.original == null) {
                Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未选择图片")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开图片")
                    }
                }
            } else {
                val bitmapToDisplay = uiState.preview ?: uiState.original
                ZoomableImage(bitmap = bitmapToDisplay!!, modifier = Modifier.fillMaxSize())

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(bitmap: android.graphics.Bitmap, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(0.dp)).pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
    ) {
        Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier =
                        Modifier.align(Alignment.Center)
                                .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                ),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None, // Pixelated look
                contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun EditorControls(
        uiState: EditorUiState,
        onPixelSizeChange: (Int) -> Unit,
        onPaletteChange: (Palette) -> Unit,
        onDitherChange: (DitherType) -> Unit,
        onContrastChange: (Float) -> Unit
) {
    Surface(tonalElevation = 8.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pixel Size
            Text("像素大小: ${uiState.config.pixelSize}", style = MaterialTheme.typography.labelLarge)
            Slider(
                    value = uiState.config.pixelSize.toFloat(),
                    onValueChange = { onPixelSizeChange(it.toInt()) },
                    valueRange = 1f..100f
            )

            // Contrast check (commented out in original, keeping commented out but translated if
            // needed)
            // Text("对比度: ...)

            Spacer(modifier = Modifier.height(8.dp))
            Text("调色板", style = MaterialTheme.typography.labelLarge)
            LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val palettes =
                        listOf(Palette.None, Palette.GameBoy, Palette.NES, Palette.CGA, Palette.BW)
                items(palettes) { palette ->
                    PaletteChip(
                            palette = palette,
                            selected = uiState.config.palette == palette,
                            onClick = { onPaletteChange(palette) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("抖动算法", style = MaterialTheme.typography.labelLarge)
            LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(DitherType.values()) { type ->
                    FilterChip(
                            selected = uiState.config.ditherType == type,
                            onClick = { onDitherChange(type) },
                            label = { Text(type.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
fun PaletteChip(palette: Palette, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(palette.displayName) },
            leadingIcon =
                    if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
    )
}
