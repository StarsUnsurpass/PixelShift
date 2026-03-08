package com.example.pixelshift.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pixelshift.ui.editor.common.ToolSettings
import com.example.pixelshift.ui.editor.common.SymmetryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSettingsSheet(
        settings: ToolSettings,
        onSizeChange: (Int) -> Unit,
        onPixelPerfectChange: (Boolean) -> Unit,
        onEraseToBackgroundChange: (Boolean) -> Unit,
        secondaryColor: Color,
        onSecondaryColorChange: (Color) -> Unit,
        onSampleAllLayersChange: (Boolean) -> Unit,
        onContiguousChange: (Boolean) -> Unit,
        onRectFilledChange: (Boolean) -> Unit,
        onCircleFilledChange: (Boolean) -> Unit,
        hasSelection: Boolean,
        onRotateSelection: () -> Unit,
        onFlipHorizontal: () -> Unit,
        onFlipVertical: () -> Unit,
        onClearSelection: () -> Unit,
        symmetry: SymmetryState,
        onXSymmetryChange: (Boolean) -> Unit,
        onYSymmetryChange: (Boolean) -> Unit,
        referenceVisible: Boolean,
        onReferenceImagePick: () -> Unit,
        onReferenceClear: () -> Unit,
        onClose: () -> Unit
) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
                Text("Tool Settings / 工具设置", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                // Symmetry Section
                Text("Symmetry / 对称绘制", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("X Axis", modifier = Modifier.weight(1f))
                        Switch(checked = symmetry.xEnabled, onCheckedChange = onXSymmetryChange)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("Y Axis", modifier = Modifier.weight(1f))
                        Switch(checked = symmetry.yEnabled, onCheckedChange = onYSymmetryChange)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Reference Image Section
                Text("Reference / 参考图", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onReferenceImagePick, modifier = Modifier.weight(1f)) {
                        Text("Pick Image")
                    }
                    if (referenceVisible) {
                        OutlinedButton(onClick = onReferenceClear, modifier = Modifier.weight(1f)) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Brush Size
                Text("Brush Size / 画笔大小: ${settings.size}px", style = MaterialTheme.typography.titleMedium)
                Slider(
                        value = settings.size.toFloat(),
                        onValueChange = { onSizeChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                )

                Spacer(Modifier.height(8.dp))

                // Pixel Perfect Toggle
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("Pixel Perfect (1px only)")
                        Switch(
                                checked = settings.pixelPerfect,
                                onCheckedChange = onPixelPerfectChange
                        )
                }

                Spacer(Modifier.height(16.dp))

                // Eraser Settings
                Text("Eraser / Background", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("Erase to Background")
                        Switch(
                                checked = settings.eraseToBackground,
                                onCheckedChange = onEraseToBackgroundChange
                        )
                }

                if (settings.eraseToBackground) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Text("Secondary Color:")
                                val colors =
                                        listOf(
                                                Color.White,
                                                Color.Black,
                                                Color.Red,
                                                Color.Blue,
                                                Color.Green
                                        )
                                colors.forEach { color ->
                                        Box(
                                                modifier =
                                                        Modifier.size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(color)
                                                                .clickable {
                                                                        onSecondaryColorChange(
                                                                                color
                                                                        )
                                                                }
                                                                .then(
                                                                        if (secondaryColor == color)
                                                                                Modifier.background(
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.5f
                                                                                                )
                                                                                )
                                                                        else Modifier
                                                                )
                                        ) {
                                                if (secondaryColor == color) {
                                                        Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier =
                                                                        Modifier.align(
                                                                                Alignment.Center
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Bucket Fill Settings
                Text("Bucket Fill / 油漆桶", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("连续填充 (Contiguous)")
                        Switch(checked = settings.contiguous, onCheckedChange = onContiguousChange)
                }

                Spacer(Modifier.height(16.dp))

                // Shape Options
                Text("Shape Options / 形状", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("填充矩形 (Fill Rectangle)")
                        Switch(
                                checked = settings.rectFilled,
                                onCheckedChange = onRectFilledChange
                        )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("填充圆形 (Fill Circle)")
                        Switch(
                                checked = settings.circleFilled,
                                onCheckedChange = onCircleFilledChange
                        )
                }

                Spacer(Modifier.height(16.dp))

                // Eyedropper Settings
                Text("Eyedropper / 取色", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Text("取样所有图层 (Sample All Layers)")
                        Switch(
                                checked = settings.sampleAllLayers,
                                onCheckedChange = onSampleAllLayersChange
                        )
                }

                if (hasSelection) {
                        Spacer(Modifier.height(16.dp))
                        Text("Transformation / 选区变换", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                OutlinedButton(
                                        onClick = onRotateSelection,
                                        modifier = Modifier.weight(1f)
                                ) { Text("Rot 90") }
                                OutlinedButton(
                                        onClick = onFlipHorizontal,
                                        modifier = Modifier.weight(1f)
                                ) { Text("Flip H") }
                                OutlinedButton(
                                        onClick = onFlipVertical,
                                        modifier = Modifier.weight(1f)
                                ) { Text("Flip V") }
                        }
                        Button(
                                onClick = onClearSelection,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        )
                        ) { Text("Clear Selection / 取消选区") }
                }

                Spacer(Modifier.height(32.dp))
        }
}
