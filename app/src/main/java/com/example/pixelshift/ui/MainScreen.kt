package com.example.pixelshift.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelshift.ui.creation.CreationScreen
import com.example.pixelshift.ui.dashboard.DashboardScreen
import com.example.pixelshift.ui.editor.EditorScreen
import com.example.pixelshift.ui.editor.PixelArtEditorScreen
import com.example.pixelshift.ui.theme.ThemeViewModel

@Composable
fun MainScreen(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items =
                        listOf(
                                Screen.Dashboard,
                                Screen.Creation,
                        )
                val isMainScreen = items.any { it.route == currentDestination?.route }

                if (isMainScreen) {
                    NavigationBar {
                        items.forEach { screen ->
                            NavigationBarItem(
                                    icon = {
                                        when (screen) {
                                            Screen.Dashboard ->
                                                    Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = null
                                                    )
                                            Screen.Creation ->
                                                    Icon(
                                                            Icons.Default.Create,
                                                            contentDescription = null
                                                    )
                                            else -> {}
                                        }
                                    },
                                    label = { Text(screen.route) },
                                    selected =
                                            currentDestination?.hierarchy?.any {
                                                it.route == screen.route
                                            } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController,
                startDestination = Screen.Dashboard.route,
                Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController, themeViewModel) }
            composable(Screen.Creation.route) { CreationScreen(navController, themeViewModel) }
            composable(Screen.Editor.route) {
                EditorScreen(navController, themeViewModel = themeViewModel)
            }
            composable(
                    route = Screen.PixelArtEditor.route,
                    arguments =
                            listOf(
                                    navArgument("width") { type = NavType.IntType },
                                    navArgument("height") { type = NavType.IntType },
                                    navArgument("transparent") { type = NavType.BoolType },
                                    navArgument("backgroundColor") { type = NavType.IntType }
                            )
            ) { backStackEntry ->
                val width = backStackEntry.arguments?.getInt("width") ?: 32
                val height = backStackEntry.arguments?.getInt("height") ?: 32
                val transparent = backStackEntry.arguments?.getBoolean("transparent") ?: true
                val backgroundColor = backStackEntry.arguments?.getInt("backgroundColor") ?: 0
                PixelArtEditorScreen(
                        navController,
                        width,
                        height,
                        transparent,
                        backgroundColor,
                        themeViewModel = themeViewModel
                )
            }
        }
    }
}
