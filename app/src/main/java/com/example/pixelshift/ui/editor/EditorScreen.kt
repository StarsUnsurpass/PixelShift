package com.example.pixelshift.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropRotate
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.domain.Palette
import com.example.pixelshift.ui.components.AdaptiveLayoutScreen
import com.example.pixelshift.ui.components.BottomButtonsBlock
import com.example.pixelshift.ui.components.PreferenceItem
import com.example.pixelshift.ui.components.TitleItem
import com.example.pixelshift.ui.components.checkerboardBackground
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@Composable
fun EditorScreen(
        navController: NavController,
        viewModel: EditorViewModel = viewModel(),
        themeViewModel: ThemeViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val themeState = themeViewModel?.themeState?.collectAsState()?.value
    val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

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

    AdaptiveLayoutScreen(
            title = { Text("PixelShift") },
            onGoBack = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { /* Share TODO */}) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                }
            },
            imagePreview = { EditorImagePreview(uiState = uiState, launcher = launcher) },
            controls = {
                EditorControls(
                        uiState = uiState,
                        viewModel = viewModel,
                        hapticEnabled = hapticEnabled,
                        view = view,
                        context = context
                )
            },
            buttons = {
                BottomButtonsBlock(
                        targetState = (uiState.original != null) to (uiState.original != null),
                        onSecondaryButtonClick = { launcher.launch("image/*") },
                        secondaryButtonIcon = Icons.Default.Add,
                        secondaryButtonText = "Pick Image",
                        onPrimaryButtonClick = {
                            val uri = viewModel.exportImage(context)
                            if (uri != null) {
                                HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        primaryButtonIcon = Icons.Default.Save,
                        primaryButtonText = "Save"
                )
            },
            canShowScreenData = true,
            showImagePreviewAsStickyHeader = true
    )
}

@Composable
fun EditorImagePreview(
        uiState: EditorUiState,
        launcher: ManagedActivityResultLauncher<String, Uri?>,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .checkerboardBackground()
    ) {
        if (uiState.original == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { launcher.launch("image/*") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("打开图片")
                }
            }
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
}

@Composable
fun EditorControls(
        uiState: EditorUiState,
        viewModel: EditorViewModel,
        hapticEnabled: Boolean,
        view: android.view.View,
        context: android.content.Context
) {
    // 1. Basic Info & Crop
    TitleItem(text = "基础信息")
    PreferenceItem(
            title = "尺寸: ${uiState.original?.width ?: 0} x ${uiState.original?.height ?: 0}",
            subtitle = "点击裁剪/旋转 (Coming Soon)",
            icon = Icons.Default.CropRotate,
            onClick = {
                HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                Toast.makeText(context, "功能开发中 (Coming Soon)", Toast.LENGTH_SHORT).show()
            }
    )

    // 2. Pixelation
    TitleItem(text = "像素密度 (Block Size)")
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Pixel Size")
                Text(text = "${uiState.config.pixelSize}px")
            }
            Slider(
                    value = uiState.config.pixelSize.toFloat(),
                    onValueChange = { viewModel.updatePixelSize(it.toInt()) },
                    onValueChangeFinished = {
                        HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                    },
                    valueRange = 1f..100f,
                    steps = 99
            )
        }
    }

    // 3. Palette
    TitleItem(text = "调色板 (Palette)")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf(Palette.None, Palette.GameBoy, Palette.NES, Palette.CGA, Palette.BW)) { palette
            ->
            PaletteChip(
                    palette = palette,
                    selected = uiState.config.palette == palette,
                    onClick = {
                        HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                        viewModel.updatePalette(palette)
                    }
            )
        }
    }

    // 4. Advanced
    TitleItem(text = "高级处理 (Advanced)")
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("抖动算法 (Dithering)", style = MaterialTheme.typography.labelLarge)
            LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
            ) {
                items(DitherType.values()) { type ->
                    FilterChip(
                            selected = uiState.config.ditherType == type,
                            onClick = {
                                HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                                viewModel.updateDither(type)
                            },
                            label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                    "对比度 (Contrast): ${String.format("%.1f", uiState.config.contrast)}",
                    style = MaterialTheme.typography.labelLarge
            )
            Slider(
                    value = uiState.config.contrast,
                    onValueChange = { viewModel.updateContrast(it) },
                    onValueChangeFinished = {
                        HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                    },
                    valueRange = 0.5f..2.0f
            )
        }
    }

    // 5. Output
    TitleItem(text = "输出设置 (Output)")
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("格式", style = MaterialTheme.typography.labelLarge)
            Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val formats =
                        listOf(
                                Bitmap.CompressFormat.PNG,
                                Bitmap.CompressFormat.JPEG,
                                Bitmap.CompressFormat.WEBP
                        )
                formats.forEach { format ->
                    FilterChip(
                            selected = uiState.outputFormat == format,
                            onClick = {
                                HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                                viewModel.setOutputFormat(format)
                            },
                            label = { Text(format.name) }
                    )
                }
            }

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pixel Perfect Upscale")
                Switch(
                        checked = uiState.usePixelPerfectUpscale,
                        onCheckedChange = {
                            HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                            viewModel.togglePixelPerfectUpscale(it)
                        }
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(original: Bitmap, preview: Bitmap?, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showOriginal by remember { mutableStateOf(false) }

    val bitmapToDisplay = if (showOriginal || preview == null) original else preview

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

        Surface(
                color =
                        if (showOriginal) MaterialTheme.colorScheme.primaryContainer
                        else Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        ) {
            Text(
                    text = if (showOriginal) "原图 (Original)" else "按住对比 (Hold to Compare)",
                    color =
                            if (showOriginal) MaterialTheme.colorScheme.onPrimaryContainer
                            else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
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
