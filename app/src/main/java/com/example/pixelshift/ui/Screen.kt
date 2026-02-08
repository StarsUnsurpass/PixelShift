package com.example.pixelshift.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor")
    object FormatConversion : Screen("format_conversion")
    object Settings : Screen("settings")
}
