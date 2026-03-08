package com.example.pixelshift.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceItem(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        endIcon: ImageVector? = null,
        shape: Shape = RoundedCornerShape(16.dp),
        color: Color = MaterialTheme.colorScheme.surfaceContainer,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
        Surface(
                color = color,
                shape = shape,
                modifier =
                        modifier.fillMaxWidth()
                                .then(
                                        if (onClick != null) {
                                                Modifier.clip(shape).clickable(onClick = onClick)
                                        } else Modifier
                                )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        if (icon != null) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                if (subtitle != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                        if (endIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                        imageVector = endIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}

@Composable
fun TitleItem(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                if (icon != null) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
        }
}

@Composable
fun SectionTitleWithInfo(
        text: String,
        infoTitle: String,
        infoContent: String,
        modifier: Modifier = Modifier
) {
        var showInfo by remember { mutableStateOf(false) }

        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showInfo = true }, modifier = Modifier.size(20.dp)) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                }
        }

        if (showInfo) {
                AlertDialog(
                        onDismissRequest = { showInfo = false },
                        title = { Text(infoTitle) },
                        text = { Text(infoContent) },
                        confirmButton = {
                                TextButton(onClick = { showInfo = false }) { Text("OK") }
                        }
                )
        }
}
