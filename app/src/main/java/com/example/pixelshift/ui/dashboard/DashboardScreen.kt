package com.example.pixelshift.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import com.example.pixelshift.ui.Screen
import com.example.pixelshift.ui.components.AdaptiveLayoutScreen
import com.example.pixelshift.ui.components.PreferenceItem
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@Composable
fun DashboardScreen(navController: NavController, themeViewModel: ThemeViewModel? = null) {
        val view = LocalView.current
        val themeState = themeViewModel?.themeState?.collectAsState()?.value
        val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true

        AdaptiveLayoutScreen(
                title = { Text("PixelShift 像素转换") },
                onGoBack = { /* No back action on Dashboard root */},
                shouldDisableBackHandler = true,
                actions = {
                        IconButton(
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        navController.navigate(Screen.Settings.route)
                                }
                        ) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                controls = {
                        PreferenceItem(
                                title = "8位转换器",
                                subtitle = "将图片转换为复古8位风格",
                                icon = Icons.Rounded.AutoFixHigh,
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        navController.navigate(Screen.Editor.route)
                                }
                        )
                        // Add more tools here as PreferenceItems
                },
                canShowScreenData = true,
                isPortrait = true, // Dashboard is simple list/grid, can reuse portrait text-only or
                // mixed layout
                showImagePreviewAsStickyHeader = false
        )
}
