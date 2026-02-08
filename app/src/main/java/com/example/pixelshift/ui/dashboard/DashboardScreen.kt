package com.example.pixelshift.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pixelshift.ui.Screen
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, themeViewModel: ThemeViewModel? = null) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val view = LocalView.current

        // Safely collect state if viewModel is present
        val themeState = themeViewModel?.themeState?.collectAsState()?.value
        val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                        LargeTopAppBar(
                                title = { Text("PixelShift 像素转换") },
                                scrollBehavior = scrollBehavior,
                                actions = {
                                        IconButton(
                                                onClick = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                hapticEnabled
                                                        )
                                                        navController.navigate(
                                                                Screen.Settings.route
                                                        )
                                                }
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Settings"
                                                )
                                        }
                                }
                        )
                }
        ) { paddingValues ->
                LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                        item {
                                ToolCard(
                                        title = "8位转换器",
                                        icon = Icons.Default.Apps,
                                        onClick = {
                                                HapticFeedbackManager.performHapticFeedback(
                                                        view,
                                                        hapticEnabled
                                                )
                                                navController.navigate(Screen.Editor.route)
                                        }
                                )
                        }
                        // Future tools can be added here
                }
        }
}

@Composable
fun ToolCard(title: String, icon: ImageVector, onClick: () -> Unit) {
        Card(
                onClick = onClick,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                modifier = Modifier.aspectRatio(1f) // Square card
        ) {
                Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}
