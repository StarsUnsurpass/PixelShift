package com.example.pixelshift.ui.theme

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ThemeViewModel : ViewModel() {
    private val _isDarkTheme = MutableStateFlow(false) // Default to system or specific value
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // We can initialize this from system settings in MainActivity or assume a default
    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun toggleTheme() {
        _isDarkTheme.update { !it }
    }
}
