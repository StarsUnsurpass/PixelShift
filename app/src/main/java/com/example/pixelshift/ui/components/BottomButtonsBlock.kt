package com.example.pixelshift.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomButtonsBlock(
        targetState: Pair<Boolean, Boolean>, // (isPrimaryEnabled, isSecondaryEnabled)
        onSecondaryButtonClick: () -> Unit,
        secondaryButtonIcon: ImageVector,
        secondaryButtonText: String,
        onPrimaryButtonClick: () -> Unit,
        primaryButtonIcon: ImageVector,
        primaryButtonText: String,
        isPrimaryButtonVisible: Boolean = true,
        isSecondaryButtonVisible: Boolean = true
) {
    if (isPrimaryButtonVisible || isSecondaryButtonVisible) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSecondaryButtonVisible) {
                OutlinedButton(
                        onClick = onSecondaryButtonClick,
                        enabled = targetState.second,
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = secondaryButtonIcon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = secondaryButtonText)
                }
            }
            if (isPrimaryButtonVisible) {
                Button(
                        onClick = onPrimaryButtonClick,
                        enabled = targetState.first,
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = primaryButtonIcon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = primaryButtonText)
                }
            }
        }
    }
}
