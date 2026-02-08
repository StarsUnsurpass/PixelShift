package com.example.pixelshift.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixelshift.ui.dashboard.DashboardScreen
import com.example.pixelshift.ui.editor.EditorScreen
import com.example.pixelshift.ui.settings.SettingsScreen
import com.example.pixelshift.ui.theme.ThemeViewModel

@Composable
fun Navigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController, themeViewModel) }
        composable(Screen.Editor.route) {
            EditorScreen(navController = navController, themeViewModel = themeViewModel)
        }
        composable(Screen.Settings.route) {
            // Assume ThemeViewModel is available via Hilt or passed down, but here we passed it to
            // Dashboard
            // In Navigation, we receive it, so we can pass it to SettingsScreen too.
            // However, SettingsScreen uses viewModel() by default, dealing with its own instance if
            // not passed.
            // But since we want to share the same instance (scoped to Activity or NavGraph), we
            // should pass the one we received.
            // We need to import SettingsScreen.
            SettingsScreen(navController = navController, themeViewModel = themeViewModel)
        }
    }
}
