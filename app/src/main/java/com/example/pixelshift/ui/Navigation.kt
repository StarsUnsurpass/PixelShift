package com.example.pixelshift.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixelshift.ui.dashboard.DashboardScreen
import com.example.pixelshift.ui.editor.EditorScreen
import com.example.pixelshift.ui.theme.ThemeViewModel

@Composable
fun Navigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController, themeViewModel = themeViewModel)
        }
        composable(Screen.Editor.route) { EditorScreen(navController = navController) }
    }
}
