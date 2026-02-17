package com.example.pixelshift.ui.creation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.ui.Screen
import com.example.pixelshift.ui.components.SimpleFlowRow
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationScreen(
        navController: NavController,
        themeViewModel: ThemeViewModel? = null,
        viewModel: CreationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentConfigs by viewModel.recentConfigs.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(screenPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text("新建项目", style = MaterialTheme.typography.headlineLarge)

            // Recent Section
            if (recentConfigs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("最近使用", style = MaterialTheme.typography.titleMedium)

                    SimpleFlowRow(horizontalGap = 8.dp, verticalGap = 8.dp) {
                        recentConfigs.forEach { config ->
                            val parts = config.split("x")
                            if (parts.size == 2) {
                                val w = parts[0]
                                val h = parts[1]
                                SuggestionChip(
                                        onClick = {
                                            viewModel.updateWidth(w)
                                            viewModel.updateHeight(h)
                                        },
                                        label = { Text(config) }
                                )
                            }
                        }
                    }
                }
            }

            // Presets Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("预设", style = MaterialTheme.typography.titleMedium)
                SimpleFlowRow(horizontalGap = 8.dp, verticalGap = 8.dp) {
                    PresetChip("16x16", "图标", 16, 16, viewModel)
                    PresetChip("24x24", "图标", 24, 24, viewModel)
                    PresetChip("32x32", "角色", 32, 32, viewModel)
                    PresetChip("48x48", "角色", 48, 48, viewModel)
                    PresetChip("64x64", "综合", 64, 64, viewModel)
                    PresetChip("128x128", "场景", 128, 128, viewModel)
                    PresetChip("160x144", "GB", 160, 144, viewModel)
                    PresetChip("256x240", "NES", 256, 240, viewModel)
                }
            }

            // Custom Size Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("自定义尺寸", style = MaterialTheme.typography.titleMedium)

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                            value = uiState.width,
                            onValueChange = { viewModel.updateWidth(it) },
                            label = { Text("宽度") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.widthError != null,
                            supportingText = { uiState.widthError?.let { Text(it) } },
                            suffix = { Text("px") }
                    )

                    Column(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = { viewModel.toggleAspectRatioLock() }) {
                            Icon(
                                    if (uiState.isAspectRatioLocked) Icons.Default.Link
                                    else Icons.Default.LinkOff,
                                    contentDescription = "锁定长宽比",
                                    tint =
                                            if (uiState.isAspectRatioLocked)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.swapDimensions() }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "交换尺寸")
                        }
                    }

                    OutlinedTextField(
                            value = uiState.height,
                            onValueChange = { viewModel.updateHeight(it) },
                            label = { Text("高度") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.heightError != null,
                            supportingText = { uiState.heightError?.let { Text(it) } },
                            suffix = { Text("px") }
                    )
                }
            }

            // Background Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("背景", style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                            selected = uiState.backgroundType == "Transparent",
                            onClick = { viewModel.setBackgroundType("Transparent") }
                    )
                    Text(
                            "透明",
                            modifier =
                                    Modifier.clickable {
                                        viewModel.setBackgroundType("Transparent")
                                    }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                            modifier =
                                    Modifier.size(24.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.Gray, CircleShape)
                                            .background(Color.LightGray)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                            selected = uiState.backgroundType == "Solid",
                            onClick = { viewModel.setBackgroundType("Solid") }
                    )
                    Text(
                            "纯色",
                            modifier = Modifier.clickable { viewModel.setBackgroundType("Solid") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (uiState.backgroundType == "Solid") {
                        Box(
                                modifier =
                                        Modifier.size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(uiState.backgroundColor))
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .clickable { showColorPicker = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create Button
            Button(
                    onClick = {
                        viewModel.createCanvas { w, h, transparent, color ->
                            navController.navigate(
                                    Screen.PixelArtEditor.createRoute(
                                            w,
                                            h,
                                            transparent,
                                            if (transparent) null else color
                                    )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
            ) { Text("开始绘制", style = MaterialTheme.typography.titleMedium) }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showColorPicker) {
            val controller = rememberColorPickerController()
            AlertDialog(
                    onDismissRequest = { showColorPicker = false },
                    title = { Text("Select Background Color") },
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
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QuickColorButton(Color.White) {
                                    viewModel.setBackgroundColor(it.toArgb())
                                }
                                QuickColorButton(Color.Black) {
                                    viewModel.setBackgroundColor(it.toArgb())
                                }
                                QuickColorButton(Color.Gray) {
                                    viewModel.setBackgroundColor(it.toArgb())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showColorPicker = false }) { Text("Done") }
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetChip(label: String, type: String, w: Int, h: Int, viewModel: CreationViewModel) {
    FilterChip(
            selected = false,
            onClick = { viewModel.applyPreset(w, h) },
            label = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(type, style = MaterialTheme.typography.labelSmall)
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
    )
}

@Composable
fun QuickColorButton(color: Color, onClick: (Color) -> Unit) {
    Box(
            modifier =
                    Modifier.size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onClick(color) }
    )
}

private val screenPadding = 16.dp
