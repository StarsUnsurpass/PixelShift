package com.example.pixelshift.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
        title: String,
        onBack: () -> Unit,
        actions: @Composable () -> Unit = {},
        modifier: Modifier = Modifier,
        scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior? = null
) {
    CenterAlignedTopAppBar(
            title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                    )
                }
            },
            actions = { actions() },
            colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor =
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                    ),
            scrollBehavior = scrollBehavior,
            modifier = modifier
    )
}
