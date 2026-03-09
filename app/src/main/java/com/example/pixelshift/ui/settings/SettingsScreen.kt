package com.example.pixelshift.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelshift.ui.theme.ThemeColors
import com.example.pixelshift.ui.theme.ThemeViewModel
import com.example.pixelshift.util.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, themeViewModel: ThemeViewModel = viewModel()) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val themeState by themeViewModel.themeState.collectAsState()
        val view = LocalView.current

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                        CenterAlignedTopAppBar(
                                title = { Text("设置") },
                                navigationIcon = {
                                        IconButton(
                                                onClick = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                themeState.hapticFeedbackEnabled
                                                        )
                                                        navController.navigateUp()
                                                }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "返回"
                                                )
                                        }
                                },
                                scrollBehavior = scrollBehavior
                        )
                }
        ) { paddingValues ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(paddingValues)
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        // Appearance Section
                        SettingsSection(title = "外观") {
                                Text(
                                        text = "主题模式",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        ThemeModeChip(
                                                selected = themeState.themeMode == "SYSTEM",
                                                label = "跟随系统",
                                                onClick = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                themeState.hapticFeedbackEnabled
                                                        )
                                                        themeViewModel.setThemeMode("SYSTEM")
                                                }
                                        )
                                        ThemeModeChip(
                                                selected = themeState.themeMode == "LIGHT",
                                                label = "亮色",
                                                onClick = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                themeState.hapticFeedbackEnabled
                                                        )
                                                        themeViewModel.setThemeMode("LIGHT")
                                                }
                                        )
                                        ThemeModeChip(
                                                selected = themeState.themeMode == "DARK",
                                                label = "暗色",
                                                onClick = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                themeState.hapticFeedbackEnabled
                                                        )
                                                        themeViewModel.setThemeMode("DARK")
                                                }
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Dynamic Color
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column {
                                                Text(
                                                        text = "动态取色",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                        text = "使用壁纸颜色",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        Switch(
                                                checked = themeState.useDynamicColor,
                                                onCheckedChange = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                themeState.hapticFeedbackEnabled
                                                        )
                                                        themeViewModel.setDynamicColor(it)
                                                }
                                        )
                                }

                                // Theme Color (only if Dynamic Color is off)
                                if (!themeState.useDynamicColor) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                                text = "主题颜色",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                items(ThemeColors) { color ->
                                                        ColorCircle(
                                                                color = color,
                                                                selected =
                                                                        themeState.themeColor ==
                                                                                color.toArgb(),
                                                                onClick = {
                                                                        HapticFeedbackManager
                                                                                .performHapticFeedback(
                                                                                        view,
                                                                                        themeState
                                                                                                .hapticFeedbackEnabled
                                                                                )
                                                                        themeViewModel
                                                                                .setThemeColor(
                                                                                        color.toArgb()
                                                                                )
                                                                }
                                                        )
                                                }
                                        }
                                }
                        }

                        HorizontalDivider()

                        // Feedback Section
                        SettingsSection(title = "反馈") {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column {
                                                Text(
                                                        text = "触感反馈",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                        text = "交互时震动",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        Switch(
                                                checked = themeState.hapticFeedbackEnabled,
                                                onCheckedChange = {
                                                        HapticFeedbackManager.performHapticFeedback(
                                                                view,
                                                                true
                                                        )
                                                        themeViewModel.setHapticFeedback(it)
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                )
                content()
        }
}

@Composable
fun ThemeModeChip(selected: Boolean, label: String, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.border(
                                        width = 1.dp,
                                        color =
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                )
                                .clickable(onClick = onClick)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
                Text(
                        text = label,
                        color =
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                )
        }
}

@Composable
fun ColorCircle(color: Color, selected: Boolean, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable(onClick = onClick)
                                .padding(12.dp),
                contentAlignment = Alignment.Center
        ) {
                if (selected) {
                        Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White
                        )
                }
        }
}
