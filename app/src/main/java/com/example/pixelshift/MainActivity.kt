package com.example.pixelshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        setContent {
            val themeViewModel: ThemeViewModel by viewModels()
            val systemDark = isSystemInDarkTheme()

            // Initializing with system setting if not set ideally should be done in VM init or
            // specialized logic
            // providing a default value if not yet set in DataStore (if we had one).
            // For now, let's just observe.
            // If VM default is false, it forces light mode.
            // Let's make VM initialized with system dark mode in a real app,
            // but here we can just pass specific value.

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
