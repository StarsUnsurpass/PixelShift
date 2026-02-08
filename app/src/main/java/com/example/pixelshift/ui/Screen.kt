package com.example.pixelshift.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor")
}
