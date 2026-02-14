package com.example.pixelshift.ui.creation

import androidx.compose.ui.graphics.Color

data class Layer(
    val id: Int,
    var pixelData: Array<Array<Color>>,
    var isVisible: Boolean = true,
    var opacity: Float = 1f
)
