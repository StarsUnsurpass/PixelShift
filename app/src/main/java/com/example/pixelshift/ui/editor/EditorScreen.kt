package com.example.pixelshift.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.pixelshift.domain.DitherType
import com.example.pixelshift.ui.components.AdaptiveLayoutScreen
import com.example.pixelshift.ui.components.BottomButtonsBlock
import com.example.pixelshift.ui.components.SectionTitleWithInfo
import com.example.pixelshift.ui.components.SimpleFlowRow
import com.example.pixelshift.ui.components.checkerboardBackground
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val viewModel: EditorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val themeState = themeViewModel.themeState.collectAsState().value
    val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadImage(context, it) }
    }

    AdaptiveLayoutScreen(
        title = { Text("8位转换器") },
        onGoBack = { navController.popBackStack() },
        topAppBarType = com.example.pixelshift.ui.components.EnhancedTopAppBarType.Center,
        imagePreview = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .checkerboardBackground(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.original == null) {
                    Button(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("打开图片")
                    }
                } else if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    uiState.preview?.let { bitmap ->
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
                text = "转换设置",
                infoTitle = "关于8位转换",
                infoContent = "8位转换通过减少颜色数量和像素化来模拟复古游戏风格。\n\n" +
                        "- 像素大小: 增加此值会使图像更具像素感。\n" +
                        "- 对比度: 调整图像的明暗对比，影响转换后的细节。\n" +
                        "- 抖动算法: 在颜色受限的情况下，通过点阵模拟过渡色。"
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Pixel Size
                    Column {
                        Text("像素大小: ${uiState.config.pixelSize}x", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = uiState.config.pixelSize.toFloat(),
                            onValueChange = { viewModel.updatePixelSize(it.toInt()) },
                            valueRange = 1f..32f,
                            steps = 31
                        )
                    }

                    // Contrast
                    Column {
                        Text("对比度: ${"%.1f".format(uiState.config.contrast)}", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = uiState.config.contrast,
                            onValueChange = { viewModel.updateContrast(it) },
                            valueRange = 0.5f..2.0f
                        )
                    }

                    // Dither
                    Column {
                        Text("抖动算法", style = MaterialTheme.typography.labelLarge)
                        SimpleFlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalGap = 8.dp,
                            verticalGap = 8.dp
                        ) {
                            DitherType.entries.forEach { type ->
                                FilterChip(
                                    selected = uiState.config.ditherType == type,
                                    onClick = { viewModel.updateDither(type) },
                                    label = { Text(type.displayName) }
                                )
                            }
                        }
                    }

                    // Smooth Image Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("平滑处理", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = uiState.config.smoothImage,
                            onCheckedChange = { viewModel.toggleSmoothImage(it) }
                        )
                    }
                }
            }
        },
        buttons = {
            BottomButtonsBlock(
                targetState = (uiState.original != null) to (uiState.original != null),
                onSecondaryButtonClick = { launcher.launch("image/*") },
                secondaryButtonIcon = Icons.Default.Add,
                secondaryButtonText = "更换图片",
                onPrimaryButtonClick = {
                    HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                    val uri = viewModel.exportImage(context)
                    if (uri != null) {
                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                },
                primaryButtonIcon = Icons.Default.Save,
                primaryButtonText = "保存图片"
            )
        },
        canShowScreenData = true,
        showImagePreviewAsStickyHeader = true
    )
}
