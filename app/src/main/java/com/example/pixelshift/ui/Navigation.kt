package com.example.pixelshift.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixelshift.ui.dashboard.DashboardScreen
import com.example.pixelshift.ui.editor.EditorScreen
import com.example.pixelshift.ui.settings.SettingsScreen
import com.example.pixelshift.ui.theme.ThemeViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.pixelshift.ui.editor.PixelArtEditorScreen

@Composable
fun Navigation(themeViewModel: ThemeViewModel) {
    MainScreen(themeViewModel = themeViewModel)
}
