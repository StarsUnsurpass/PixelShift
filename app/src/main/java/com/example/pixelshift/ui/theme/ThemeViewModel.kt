package com.example.pixelshift.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelshift.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeState(
        val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
        val useDynamicColor: Boolean = true,
        val themeColor: Int = 0xFF6650a4.toInt(),
        val hapticFeedbackEnabled: Boolean = true
)

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val themeState: StateFlow<ThemeState> =
            combine(
                            repository.themeMode,
                            repository.dynamicColor,
                            repository.themeColor,
                            repository.hapticFeedback
                    ) { mode, dynamic, color, haptic -> ThemeState(mode, dynamic, color, haptic) }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5_000),
                            initialValue = ThemeState()
                    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setThemeColor(color: Int) {
        viewModelScope.launch { repository.setThemeColor(color) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticFeedback(enabled) }
    }

    // Deprecated simple toggle, mapping to new logic for compatibility if needed
    // But better to remove or adapt. For now, let's remove the old methods to force update.
}
