package com.example.pixelshift.ui.creation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelshift.ui.components.SimpleFlowRow
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectSheet(
    onDismiss: () -> Unit,
    onCreate: (width: Int, height: Int, transparent: Boolean, backgroundColor: Int?) -> Unit,
    viewModel: CreationViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val recentConfigs by viewModel.recentConfigs.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "新建画布",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            // Tier 0: History
            if (recentConfigs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("历史记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                         contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recentConfigs.take(5)) { config ->
                             val parts = config.split("x")
                             if (parts.size == 2) {
                                 val w = parts[0].toIntOrNull() ?: 32
                                 val h = parts[1].toIntOrNull() ?: 32
                                 AssistChip(
                                    onClick = { 
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                        viewModel.applyPreset(w, h) 
                                    },
                                    label = { Text("${w}x${h}") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                 )
                             }
                        }
                    }
                }
            }
            // Tier 1: Presets (Categorized)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Quick Start with Supporting Text
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("快速开始", style = MaterialTheme.typography.titleMedium)
                    Text("选择标准尺寸，一键进入工作台", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                
                Text("游戏资源", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SimpleFlowRow(
                    verticalGap = 8.dp,
                    horizontalGap = 8.dp
                ) {
                    PresetChip("道具 (小)", "16x16", 16, 16, Icons.Default.FlashOn, uiState, viewModel, view)
                    PresetChip("道具 (中)", "24x24", 24, 24, Icons.Default.AutoAwesome, uiState, viewModel, view)
                    PresetChip("角色/砖块", "32x32", 32, 32, Icons.Default.Face, uiState, viewModel, view)
                    PresetChip("半身像", "64x64", 64, 64, Icons.Default.Portrait, uiState, viewModel, view)
                    PresetChip("场景/海报", "128x128", 128, 128, Icons.Default.Image, uiState, viewModel, view)
                }

                // Systems Category
                Text("经典系统", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SimpleFlowRow(
                    verticalGap = 8.dp,
                    horizontalGap = 8.dp
                ) {
                    PresetChip("GameBoy", "160x144", 160, 144, Icons.Default.VideogameAsset, uiState, viewModel, view)
                    PresetChip("NES", "256x240", 256, 240, Icons.Default.Tv, uiState, viewModel, view)
                }
            }

            // Tier 2: Custom Parameters
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("自定义大小", style = MaterialTheme.typography.titleMedium)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Width Input
                    OutlinedTextField(
                        value = uiState.width,
                        onValueChange = { viewModel.updateWidth(it) },
                        label = { Text("宽度") },
                        suffix = { Text("px", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.headlineMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.widthError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )

                    // Central Controls
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = { 
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            viewModel.toggleAspectRatioLock() 
                        }) {
                            Icon(
                                if (uiState.isAspectRatioLocked) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = "锁定宽高比",
                                tint = if (uiState.isAspectRatioLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { 
                             view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                             viewModel.swapDimensions() 
                        }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "交换宽高")
                        }
                    }

                    // Height Input
                    OutlinedTextField(
                        value = uiState.height,
                        onValueChange = { viewModel.updateHeight(it) },
                        label = { Text("高度") },
                        suffix = { Text("px", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.headlineMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.heightError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                         colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                }
                
                // Error/Warning Text
                val widthVal = uiState.width.toIntOrNull() ?: 0
                val heightVal = uiState.height.toIntOrNull() ?: 0
                val isLarge = widthVal > 512 || heightVal > 512
                val hasError = uiState.widthError != null || uiState.heightError != null

                if (hasError || isLarge) {
                    Text(
                        text = when {
                            hasError -> uiState.widthError ?: uiState.heightError ?: "非法尺寸"
                            isLarge -> "像素画的魅力在于限制，建议将尺寸控制在 512px 以内哦。比如绘制精细场景，128x128 往往是极佳的选择。"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Tier 3: Background & Confirm
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("背景设定", style = MaterialTheme.typography.titleMedium)
                
                // Segmented Control for Type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isTransparent = uiState.backgroundType == "Transparent"
                    
                    // Transparent Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isTransparent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { 
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.setBackgroundType("Transparent") 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                           // Material 3 Style Checkerboard Icon
                           Box(
                               modifier = Modifier
                                   .size(20.dp)
                                   .clip(RoundedCornerShape(4.dp))
                                   .background(Color.White)
                           ) {
                               Canvas(modifier = Modifier.fillMaxSize()) {
                                   val s = this.size.width
                                   val cellSize = s / 2
                                   drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize))
                                   drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(cellSize, cellSize), size = Size(cellSize, cellSize))
                               }
                           }
                           Spacer(modifier = Modifier.width(8.dp))
                           Text(
                               "透明材质", 
                               style = MaterialTheme.typography.labelLarge,
                               color = if (isTransparent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                           )
                        }
                    }
                    
                    // Vertical Divider
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))

                    // Solid Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (!isTransparent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { 
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.setBackgroundType("Solid") 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                               "实色背景", 
                               style = MaterialTheme.typography.labelLarge,
                               color = if (!isTransparent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                           )
                    }
                }

                // Solid Color Options (Visible only if Solid)
                androidx.compose.animation.AnimatedVisibility(visible = uiState.backgroundType == "Solid") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                    ) {
                        // White
                        BackgroundOption(
                            isSelected = uiState.backgroundColor == android.graphics.Color.WHITE,
                            onClick = { viewModel.setBackgroundColor(android.graphics.Color.WHITE) },
                            icon = {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White).border(1.dp, Color.Gray, CircleShape))
                            },
                            label = "白色"
                        )
                        
                        // Black
                        BackgroundOption(
                            isSelected = uiState.backgroundColor == android.graphics.Color.BLACK,
                            onClick = { viewModel.setBackgroundColor(android.graphics.Color.BLACK) },
                             icon = {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black).border(1.dp, Color.Gray, CircleShape))
                            },
                            label = "黑色"
                        )
                        
                        // Custom
                        IconButton(
                            onClick = { showColorPicker = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                        listOf(Color.Red, Color.Green, Color.Blue, Color.Red)
                                    ), 
                                    shape = CircleShape
                                )
                                .padding(2.dp) // Ring effect
                                .clip(CircleShape)
                                .background(Color(uiState.backgroundColor)) // Center shows current
                        ) {
                             Icon(Icons.Default.Link, contentDescription = "Custom", tint = if (androidx.core.graphics.ColorUtils.calculateLuminance(uiState.backgroundColor) > 0.5) Color.Black else Color.White)
                        }
                        Text("自定义", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel (Ghost Button)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(56.dp).weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("取消", style = MaterialTheme.typography.titleMedium)
                }

                // Create Button
                Button(
                    onClick = {
                        viewModel.createCanvas { w, h, transparent, color ->
                            onCreate(w, h, transparent, color)
                        }
                    },
                    modifier = Modifier.height(56.dp).weight(2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("开始创作", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    
    // Memory Warning Dialog
    if (uiState.showMemoryConfirmation) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss without choice */ },
            title = { Text("内存占用预警") },
            text = { Text("当前画布尺寸较大，创建过多图层可能会在部分设备上造成操作卡顿。是否继续？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmCreateCanvas { w, h, transparent, color ->
                        onCreate(w, h, transparent, color)
                    }
                }) {
                    Text("继续创作")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.resetMemoryConfirmation()
                }) {
                    Text("返回调整")
                }
            }
        )
    }

    if (showColorPicker) {
        val controller = rememberColorPickerController()
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("选择背景颜色") },
            text = {
                Column {
                    HsvColorPicker(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        controller = controller,
                        onColorChanged = { colorEnvelope: ColorEnvelope ->
                            viewModel.setBackgroundColor(colorEnvelope.color.toArgb())
                        },
                        initialColor = Color(uiState.backgroundColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BrightnessSlider(
                        modifier = Modifier.fillMaxWidth(),
                        controller = controller,
                        initialColor = Color(uiState.backgroundColor)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("确认") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetChip(
    title: String,
    dimensions: String, // e.g. "16x16"
    w: Int,
    h: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    uiState: CreationState, 
    viewModel: CreationViewModel,
    view: android.view.View
) {
    val isSelected = uiState.width == w.toString() && uiState.height == h.toString()
    
    FilterChip(
        selected = isSelected,
        onClick = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            viewModel.applyPreset(w, h)
        },
        label = { 
            Text(
                if (isSelected) "$title $dimensions" else title,
                style = MaterialTheme.typography.labelMedium
            ) 
        },
        leadingIcon = { 
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp)
            ) 
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun BackgroundOption(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
         Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
