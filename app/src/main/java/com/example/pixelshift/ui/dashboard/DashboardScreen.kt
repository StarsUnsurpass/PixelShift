package com.example.pixelshift.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun DashboardScreen(navController: NavController, themeViewModel: ThemeViewModel? = null) {
        val view = LocalView.current
        val themeState = themeViewModel?.themeState?.collectAsState()?.value
        val hapticEnabled = themeState?.hapticFeedbackEnabled ?: true
        var showNewProjectSheet by remember { mutableStateOf(false) }

        val exifPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
                navController.navigate(Screen.ExifEditor.createRoute(encodedUri))
            }
        }

        AdaptiveLayoutScreen(
                title = { Text("PixelShift 像素转换") },
                onGoBack = { /* No back action on Dashboard root */},
                shouldDisableBackHandler = true,
                topAppBarType = com.example.pixelshift.ui.components.EnhancedTopAppBarType.Center,
                actions = {
                        IconButton(
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        navController.navigate(Screen.Settings.route)
                                }
                        ) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                },
                floatingActionButton = {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            HapticFeedbackManager.performHapticFeedback(view, hapticEnabled)
                            showNewProjectSheet = true
                        }
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "新建项目")
                    }
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
                        PreferenceItem(
                                title = "格式转换",
                                subtitle = "批量转换图片格式",
                                icon = androidx.compose.material.icons.Icons.Default.Sync,
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        navController.navigate(Screen.FormatConversion.createRoute())
                                }
                        )
                        PreferenceItem(
                                title = "尺寸缩放",
                                subtitle = "按占用大小自动缩放图片",
                                icon = Icons.Rounded.AspectRatio,
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        navController.navigate(Screen.FormatConversion.createRoute("scaling"))
                                }
                        )
                        PreferenceItem(
                                title = "EXIF 编辑器",
                                subtitle = "查看、编辑或清除图片的元数据",
                                icon = Icons.Default.Info,
                                onClick = {
                                        HapticFeedbackManager.performHapticFeedback(
                                                view,
                                                hapticEnabled
                                        )
                                        exifPickerLauncher.launch("image/*")
                                }
                        )
                        // Add more tools here as PreferenceItems
                },
                canShowScreenData = true,
                isPortrait = true, // Dashboard is simple list/grid, can reuse portrait text-only or mixed layout
                showImagePreviewAsStickyHeader = false
        )
        
        if (showNewProjectSheet) {
            com.example.pixelshift.ui.creation.NewProjectSheet(
                onDismiss = { showNewProjectSheet = false },
                onCreate = { width, height, transparent, backgroundColor ->
                    showNewProjectSheet = false
                    navController.navigate(
                        Screen.PixelArtEditor.createRoute(
                            width,
                            height,
                            transparent,
                            backgroundColor
                        )
                    )
                }
            )
        }
}
