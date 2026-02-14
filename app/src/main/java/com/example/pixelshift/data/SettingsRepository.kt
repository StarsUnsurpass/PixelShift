package com.example.pixelshift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "LIGHT", "DARK", "SYSTEM"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_COLOR = intPreferencesKey("theme_color") // Color int
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val RECENT_CANVAS_CONFIGS = stringPreferencesKey("recent_canvas_configs") // "wxh,wxh,..."
    }

    val themeMode: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[THEME_MODE] ?: "SYSTEM" }

    val dynamicColor: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[DYNAMIC_COLOR] ?: true }

    val themeColor: Flow<Int> =
            context.dataStore.data.map { preferences ->
                preferences[THEME_COLOR] ?: 0xFF6650a4.toInt() // Default Purple40
            }

    val hapticFeedback: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[HAPTIC_FEEDBACK] ?: true }

    val recentCanvasConfigs: Flow<List<String>> =
            context.dataStore.data.map { preferences ->
                val raw = preferences[RECENT_CANVAS_CONFIGS] ?: ""
                if (raw.isBlank()) emptyList() else raw.split(",")
            }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setThemeColor(color: Int) {
        context.dataStore.edit { preferences -> preferences[THEME_COLOR] = color }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun addRecentCanvasConfig(width: Int, height: Int) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[RECENT_CANVAS_CONFIGS] ?: ""
            val currentList =
                    if (currentRaw.isBlank()) mutableListOf()
                    else currentRaw.split(",").toMutableList()
            val newItem = "${width}x${height}"

            // Remove if exists to move to top
            currentList.remove(newItem)
            // Add to start
            currentList.add(0, newItem)
            // Keep only last 3
            if (currentList.size > 3) {
                currentList.removeAt(currentList.lastIndex)
            }

            preferences[RECENT_CANVAS_CONFIGS] = currentList.joinToString(",")
        }
    }
}
