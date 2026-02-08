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
        themeViewModel: ThemeViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val themeState = themeViewModel?.themeState?.collectAsState()?.value
    val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.updateUris(uris)
                    viewModel.loadPreview(context)
                }
            }

    LaunchedEffect(uiState.selectedUri) {
        if (uiState.selectedUri != null && uiState.previewBitmap == null) {
            viewModel.loadPreview(context)
        }
    }

    // Auto-launch removed
    // LaunchedEffect(Unit) {
    //    if (uiState.uris.isEmpty()) {
    //        launcher.launch("image/*")
    //    }
    // }

    AdaptiveLayoutScreen(
            title = {
                Column {
                    Text("格式转换")
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
                SectionTitleWithInfo(
                        text = "目标格式 (Target Format)",
                        infoTitle = "关于图片格式",
                        infoContent =
                                "不同的图片格式适用于不同的场景。\n\n- PNG: 无损压缩，适合保存像素画，边缘清晰。\n- JPEG: 有损压缩，适合照片，体积小但可能有噪点。\n- WEBP: 谷歌开发的格式，兼顾质量和体积。"
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
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val formats =
                                    listOf(
                                            Bitmap.CompressFormat.PNG,
                                            Bitmap.CompressFormat.JPEG,
                                            Bitmap.CompressFormat.WEBP
                                    )
                            formats.forEach { format ->
                                FilterChip(
                                        selected = uiState.targetFormat == format,
                                        onClick = {
                                            HapticFeedbackManager.performHapticFeedback(
                                                    view,
                                                    hapticEnabled
                                            )
                                            viewModel.setTargetFormat(format)
                                        },
                                        label = { Text(format.name) },
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

                        if (uiState.targetFormat != Bitmap.CompressFormat.PNG) {
                            Text(
                                    "质量: ${uiState.quality}%",
                                    style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                    value = uiState.quality.toFloat(),
                                    onValueChange = { viewModel.setQuality(it.toInt()) },
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
                                        viewModel.selectUri(uri)
                                        viewModel.loadPreview(context)
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
                                Toast.makeText(context, "成功转换 $count 张图片", Toast.LENGTH_SHORT)
                                        .show()
                            }
                        },
                        primaryButtonIcon = Icons.Default.Save,
                        primaryButtonText = if (uiState.isSaving) "转换中..." else "转换 & 保存"
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
                        Text("正在转换 ${uiState.conversionProgress}/${uiState.uris.size}")
                    }
                },
                confirmButton = {}
        )
    }
}
