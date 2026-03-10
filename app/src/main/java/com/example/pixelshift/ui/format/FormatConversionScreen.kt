package com.example.pixelshift.ui.format

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.ui.components.AdaptiveLayoutScreen
import com.example.pixelshift.ui.components.BottomButtonsBlock
import com.example.pixelshift.ui.components.SectionTitleWithInfo
import com.example.pixelshift.ui.components.TitleItem
import com.example.pixelshift.ui.components.checkerboardBackground
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@Composable
fun FormatConversionScreen(
        navController: NavController,
        viewModel: FormatConversionViewModel = viewModel(),
        themeViewModel: ThemeViewModel? = null,
        mode: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val themeState = themeViewModel?.themeState?.collectAsState()?.value
    val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

    LaunchedEffect(mode) {
        if (mode == "scaling") {
            viewModel.setUseTargetSizeScaling(true, context)
            viewModel.setFormatSelectionEnabled(false)
        } else {
            viewModel.setFormatSelectionEnabled(true)
        }
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.updateUris(uris, context)
                }
            }

    LaunchedEffect(uiState.selectedUri) {
        if (uiState.selectedUri != null && uiState.previewBitmap == null) {
            viewModel.loadPreview(context)
        }
    }

    AdaptiveLayoutScreen(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (mode == "scaling") "尺寸缩放" else "格式转换")
                    if (uiState.uris.isNotEmpty()) {
                        Text(
                                text = "${uiState.uris.size} 张图片",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onGoBack = { navController.popBackStack() },
            topAppBarType = com.example.pixelshift.ui.components.EnhancedTopAppBarType.Center,
            imagePreview = {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .checkerboardBackground(),
                        contentAlignment = Alignment.Center
                ) {
                    if (uiState.uris.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { launcher.launch("image/*") }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("打开图片")
                            }
                        }
                    } else if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        uiState.previewBitmap?.let { bitmap ->
                            Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            },
            controls = {
                if (uiState.isFormatSelectionEnabled) {
                    SectionTitleWithInfo(
                            text = "目标格式",
                            infoTitle = "关于图片格式",
                            infoContent =
                                    "不同的图片格式适用于不同的场景。\n\n" +
                                    "- PNG: 无损压缩，适合保存像素画，边缘清晰。\n" +
                                    "- JPEG: 有损压缩，适合照片，体积小。\n" +
                                    "- WEBP: 谷歌开发的高效格式，支持有损和无损。\n" +
                                    "- BMP/TIFF: 传统无损格式，适合专业用途。\n" +
                                    "- QOI: Quite OK Image，极速编码，适合像素画。\n" +
                                    "- ICO: 图标格式。\n" +
                                    "- GIF: 传统网络动图格式（当前仅支持静态帧）。"
                    )
                    Card(
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            com.example.pixelshift.ui.components.SimpleFlowRow(
                                    horizontalGap = 8.dp,
                                    verticalGap = 8.dp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                ImageFormat.values().forEach { format ->
                                    FilterChip(
                                            selected = uiState.targetFormat == format,
                                            onClick = {
                                                HapticFeedbackManager.performHapticFeedback(
                                                        view,
                                                        hapticEnabled
                                                )
                                                viewModel.setTargetFormat(format, context)
                                            },
                                            label = { Text(format.label) },
                                            leadingIcon =
                                                    if (uiState.targetFormat == format) {
                                                        {
                                                            Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null
                                                            )
                                                        }
                                                    } else null
                                    )
                                }
                            }

                            val showQualitySlider = when(uiState.targetFormat) {
                                ImageFormat.JPEG, ImageFormat.WEBP_LOSSY -> true
                                else -> false
                            }

                            if (showQualitySlider) {
                                Text(
                                        "质量: ${uiState.quality}%",
                                        style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                        value = uiState.quality.toFloat(),
                                        onValueChange = { viewModel.setQuality(it.toInt(), context) },
                                        valueRange = 1f..100f,
                                        steps = 99,
                                        onValueChangeFinished = {
                                            HapticFeedbackManager.performHapticFeedback(
                                                    view,
                                                    hapticEnabled
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                SectionTitleWithInfo(
                        text = "缩放设置",
                        infoTitle = "缩放说明",
                        infoContent = "支持按百分比比例缩放或直接指定宽高。开启“保持宽高比”可防止图片拉伸变形。缩放结果将实时估算文件体积。"
                )

                Card(
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (mode != "scaling") {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                            "启用缩放",
                                            style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                            "调整图片尺寸",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                        checked = uiState.useTargetSizeScaling,
                                        onCheckedChange = {
                                            HapticFeedbackManager.performHapticFeedback(
                                                    view,
                                                    hapticEnabled
                                            )
                                            viewModel.setUseTargetSizeScaling(it, context)
                                        }
                                )
                            }
                        }

                        if (uiState.useTargetSizeScaling) {
                            if (mode != "scaling") {
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            TabRow(
                                selectedTabIndex = uiState.scalingMode.ordinal,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).height(40.dp),
                                indicator = @Composable {}
                            ) {
                                ScalingMode.values().forEach { modeOption ->
                                    val selected = uiState.scalingMode == modeOption
                                    Tab(
                                        selected = selected,
                                        onClick = {
                                            HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                                            viewModel.setScalingMode(modeOption, context)
                                        },
                                        text = {
                                            Text(
                                                text = if (modeOption == ScalingMode.PERCENTAGE) "按比例" else "按尺寸",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))

                            if (uiState.scalingMode == ScalingMode.PERCENTAGE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        "缩放比例: ${uiState.scalePercentage}%",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    EstimationSizeText(uiState.estimatedFileSize)
                                }
                                Slider(
                                        value = uiState.scalePercentage.toFloat(),
                                        onValueChange = { viewModel.setScalePercentage(it.toInt(), context) },
                                        valueRange = 1f..100f,
                                        steps = 99,
                                        onValueChangeFinished = {
                                            HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                                        }
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (uiState.targetWidth == 0) "" else uiState.targetWidth.toString(),
                                        onValueChange = { 
                                            val value = it.toIntOrNull() ?: 0
                                            viewModel.setTargetDimensions(value, uiState.targetHeight, context)
                                        },
                                        label = { Text("宽度") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    Text("×", style = MaterialTheme.typography.titleLarge)
                                    OutlinedTextField(
                                        value = if (uiState.targetHeight == 0) "" else uiState.targetHeight.toString(),
                                        onValueChange = { 
                                            val value = it.toIntOrNull() ?: 0
                                            viewModel.setTargetDimensions(uiState.targetWidth, value, context)
                                        },
                                        label = { Text("高度") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = uiState.maintainAspectRatio,
                                            onCheckedChange = { viewModel.setMaintainAspectRatio(it) }
                                        )
                                        Text("保持宽高比", style = MaterialTheme.typography.labelMedium)
                                    }
                                    EstimationSizeText(uiState.estimatedFileSize)
                                }
                            }
                            
                            if (uiState.previewBitmap != null) {
                                val finalW = if (uiState.scalingMode == ScalingMode.PERCENTAGE) 
                                    (uiState.previewBitmap!!.width * (uiState.scalePercentage / 100f)).toInt()
                                    else uiState.targetWidth
                                val finalH = if (uiState.scalingMode == ScalingMode.PERCENTAGE) 
                                    (uiState.previewBitmap!!.height * (uiState.scalePercentage / 100f)).toInt()
                                    else uiState.targetHeight
                                    
                                Text(
                                    "最终尺寸: $finalW x $finalH 像素",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else if (mode == "scaling") {
                            Text("缩放功能已禁用。")
                        }
                    }
                }

                // Simple selector for current preview
                if (uiState.uris.size > 1) {
                    TitleItem(text = "已选图片 (${uiState.uris.size})")
                    LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(uiState.uris) { uri ->
                            FilterChip(
                                    selected = uri == uiState.selectedUri,
                                    onClick = {
                                        viewModel.selectUri(uri, context)
                                    },
                                    label = {
                                        Text(
                                                text = uri.lastPathSegment?.takeLast(10) ?: "Image",
                                                maxLines = 1
                                        )
                                    }
                            )
                        }
                    }
                }
            },
            buttons = {
                BottomButtonsBlock(
                        targetState = (uiState.uris.isNotEmpty()) to (uiState.uris.isNotEmpty()),
                        onSecondaryButtonClick = { launcher.launch("image/*") },
                        secondaryButtonIcon = Icons.Default.Add,
                        secondaryButtonText = "添加",
                        onPrimaryButtonClick = {
                            HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                            viewModel.convertAndSaveAll(context) { count ->
                                Toast.makeText(context, "成功处理 $count 张图片", Toast.LENGTH_SHORT)
                                        .show()
                            }
                        },
                        primaryButtonIcon = Icons.Default.Save,
                        primaryButtonText = if (uiState.isSaving) "处理中..." else if (mode == "scaling") "缩放 & 保存" else "转换 & 保存"
                )
            },
            canShowScreenData = true,
            showImagePreviewAsStickyHeader = true
    )

    if (uiState.isSaving) {
        AlertDialog(
                onDismissRequest = {},
                title = { Text("正在处理") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("正在处理 ${uiState.conversionProgress}/${uiState.uris.size}")
                    }
                },
                confirmButton = {}
        )
    }
}

@Composable
fun EstimationSizeText(size: Long) {
    if (size > 0) {
        val sizeText = if (size > 1024 * 1024) {
            String.format("%.2f MB", size / (1024f * 1024f))
        } else {
            String.format("%.1f KB", size / 1024f)
        }
        Text(
            "估算大小: $sizeText",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
