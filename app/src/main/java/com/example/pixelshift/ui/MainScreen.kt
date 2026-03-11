package com.example.pixelshift.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelshift.data.ExifRepository
import com.example.pixelshift.ui.dashboard.DashboardScreen
import com.example.pixelshift.ui.editor.EditorScreen
import com.example.pixelshift.ui.editor.PixelArtEditorScreen
import com.example.pixelshift.ui.exif.ExifEditorScreen
import com.example.pixelshift.ui.format.FormatConversionScreen
import com.example.pixelshift.ui.settings.SettingsScreen
import com.example.pixelshift.ui.theme.ThemeViewModel

@Composable
fun MainScreen(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val exifRepository = remember { ExifRepository(context) }
    
    Scaffold { innerPadding ->
        NavHost(
                navController,
                startDestination = Screen.Dashboard.route,
                Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController, themeViewModel) }
            composable(Screen.Editor.route) {
                EditorScreen(navController, themeViewModel = themeViewModel)
            }
            composable(
                    route = Screen.FormatConversion.route,
                    arguments = listOf(navArgument("mode") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode")
                FormatConversionScreen(navController, themeViewModel = themeViewModel, mode = mode)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController, themeViewModel = themeViewModel)
            }
            composable(
                    route = Screen.ExifEditor.route,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                val uri = android.net.Uri.parse(uriString)
                ExifEditorScreen(
                    repository = exifRepository,
                    uri = uri,
                    onNavigateBack = { navController.popBackStack() }
                )
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
