package com.example.pixelshift.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.domain.Palette
import com.example.pixelshift.ui.components.CommonTopBar
import com.example.pixelshift.ui.components.checkerboardBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, viewModel: EditorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri: Uri? ->
                uri?.let { viewModel.loadImage(context, it) }
            }

    LaunchedEffect(Unit) {
        if (uiState.original == null) {
            launcher.launch("image/*")
        }
    }

    Scaffold(
            topBar = {
                CommonTopBar(
                        title = "PixelShift",
                        onBack = { navController.popBackStack() },
                        actions = {
                            IconButton(onClick = { /* Share TODO */}) {
                                Icon(Icons.Default.Share, contentDescription = "分享")
                            }
                            IconButton(
                                    onClick = {
                                        val uri = viewModel.exportImage(context)
                                        if (uri != null) {
                                            haptic.performHapticFeedback(
                                                    androidx.compose.ui.hapticfeedback
                                                            .HapticFeedbackType.LongPress
                                            )
                                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT)
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Preview Area (Weight 1f)
            Box(
                    modifier =
                            Modifier.weight(1f)
                                    .fillMaxWidth()
                                    .checkerboardBackground()
                                    .clip(
                                            RoundedCornerShape(
                                                    bottomStart = 24.dp,
                                                    bottomEnd = 24.dp
                                            )
                                    )
            ) {
                if (uiState.original == null) {
                    Button(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.align(Alignment.Center)
                    ) { Text("打开图片") }
                } else {
                    val original = uiState.original
                    val preview = uiState.preview
                    if (original != null) {
                        ZoomableImage(
                                original = original,
                                preview = preview,
                                modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            // Controls Area
            Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp
            ) { EditorControls(uiState = uiState, viewModel = viewModel) }
        }
    }
}

@Composable
fun ZoomableImage(original: Bitmap, preview: Bitmap?, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showOriginal by remember { mutableStateOf(false) }

    // If preview is null logic is handled, but here for safety
    val bitmapToDisplay = if (showOriginal || preview == null) original else preview

    // Label for mode
    val labelText = if (showOriginal) "原图 (Original)" else "预览 (Preview)"

    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(0.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 10f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                        onPress = {
                                            showOriginal = true
                                            tryAwaitRelease()
                                            showOriginal = false
                                        }
                                )
                            }
    ) {
        Image(
                bitmap = bitmapToDisplay.asImageBitmap(),
                contentDescription = null,
                modifier =
                        Modifier.align(Alignment.Center)
                                .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                )
                                .shadow(4.dp),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                contentScale = ContentScale.Fit
        )

        Text(
                text = labelText,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier =
                        Modifier.align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EditorControls(uiState: EditorUiState, viewModel: EditorViewModel) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Basic Info
        item {
            ControlGroup(title = "基础信息") {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                                text =
                                        "尺寸: ${uiState.original?.width ?: 0} x ${uiState.original?.height ?: 0}",
                                style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = { /* TODO Crop/Rotate */}) {
                        Icon(Icons.Default.CropRotate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("裁剪/旋转")
                    }
                }
            }
        }

        // 2. Pixelation
        item {
            ControlGroup(title = "像素密度 (Block Size)") {
                Column {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) { Text("大小: ${uiState.config.pixelSize}px") }
                    Slider(
                            value = uiState.config.pixelSize.toFloat(),
                            onValueChange = { viewModel.updatePixelSize(it.toInt()) },
                            onValueChangeFinished = {
                                haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                .LongPress
                                )
                            },
                            valueRange = 1f..100f,
                            steps = 99
                    )
                }
            }
        }

        // 3. Palette
        item {
            ControlGroup(title = "调色板 (Palette)") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                            listOf(
                                    Palette.None,
                                    Palette.GameBoy,
                                    Palette.NES,
                                    Palette.CGA,
                                    Palette.BW
                            )
                    ) { palette ->
                        PaletteChip(
                                palette = palette,
                                selected = uiState.config.palette == palette,
                                onClick = { viewModel.updatePalette(palette) }
                        )
                    }
                    item {
                        AssistChip(
                                onClick = { /* TODO Custom Palette */},
                                label = { Icon(Icons.Default.Add, contentDescription = "Add") }
                        )
                    }
                }
            }
        }

        // 4. Advanced
        item {
            ControlGroup(title = "高级处理 (Advanced)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("抖动算法 (Dithering)", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DitherType.values()) { type ->
                            FilterChip(
                                    selected = uiState.config.ditherType == type,
                                    onClick = { viewModel.updateDither(type) },
                                    label = { Text(type.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                            "对比度 (Contrast): ${String.format("%.1f", uiState.config.contrast)}",
                            style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                            value = uiState.config.contrast,
                            onValueChange = { viewModel.updateContrast(it) },
                            onValueChangeFinished = {
                                haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                .LongPress
                                )
                            },
                            valueRange = 0.5f..2.0f
                    )
                }
            }
        }

        // 5. Output
        item {
            ExpandableCard(title = "输出设置 (Output)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Format
                    Text("格式", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val formats =
                                listOf(
                                        Bitmap.CompressFormat.PNG,
                                        Bitmap.CompressFormat.JPEG,
                                        Bitmap.CompressFormat.WEBP
                                )
                        formats.forEach { format ->
                            FilterChip(
                                    selected = uiState.outputFormat == format,
                                    onClick = { viewModel.setOutputFormat(format) },
                                    label = { Text(format.name) }
                            )
                        }
                    }

                    // Upscale
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pixel Perfect Upscale (防止模糊)")
                        Switch(
                                checked = uiState.usePixelPerfectUpscale,
                                onCheckedChange = { viewModel.togglePixelPerfectUpscale(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlGroup(title: String, content: @Composable () -> Unit) {
    Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ExpandableCard(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                )
                Icon(
                        imageVector =
                                if (expanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                        contentDescription = null
                )
            }
            AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
            ) { Column(modifier = Modifier.padding(top = 12.dp)) { content() } }
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
