package com.example.pixelshift.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor")
    object Creation : Screen("creation")
    object PixelArtEditor :
            Screen("pixel_art_editor/{width}/{height}/{transparent}/{backgroundColor}") {
        fun createRoute(width: Int, height: Int, transparent: Boolean, backgroundColor: Int?) =
                "pixel_art_editor/$width/$height/$transparent/${backgroundColor ?: 0}"
    }
    object FormatConversion : Screen("format_conversion?mode={mode}") {
        fun createRoute(mode: String? = null) = "format_conversion?mode=$mode"
    }
    object Settings : Screen("settings")
}
