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
import androidx.compose.runtime.LaunchedEffect
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
            val themeViewModel: ThemeViewModel by viewModels()
            val systemDark = isSystemInDarkTheme()

            // Sync with system theme on first launch (simple approach)
            LaunchedEffect(Unit) { themeViewModel.setTheme(systemDark) }

            val isDark by themeViewModel.isDarkTheme.collectAsState()

            PixelShiftTheme(darkTheme = isDark) {
                // A surface container using the 'background' color from the theme
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { Navigation(themeViewModel) }
            }
        }
    }
}
