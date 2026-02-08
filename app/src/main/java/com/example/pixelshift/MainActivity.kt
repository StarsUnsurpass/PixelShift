package com.example.pixelshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.pixelshift.ui.Navigation
import com.example.pixelshift.ui.theme.PixelShiftTheme
import com.example.pixelshift.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = viewModels<ThemeViewModel>().value
            val themeState by themeViewModel.themeState.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDark =
                    when (themeState.themeMode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> systemDark
                    }

            PixelShiftTheme(
                    darkTheme = isDark,
                    dynamicColor = themeState.useDynamicColor,
                    themeColor = themeState.themeColor
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { Navigation(themeViewModel) }
            }
        }
    }
}
