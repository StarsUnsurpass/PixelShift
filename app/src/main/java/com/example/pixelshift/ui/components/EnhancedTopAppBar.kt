package com.example.pixelshift.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopAppBar(
        title: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        navigationIcon: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
        windowInsets: WindowInsets =
                TopAppBarDefaults.windowInsets.union(WindowInsets.displayCutout),
        colors: TopAppBarColors =
                TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
        scrollBehavior: TopAppBarScrollBehavior? = null,
        type: EnhancedTopAppBarType = EnhancedTopAppBarType.Large
) {
    when (type) {
        EnhancedTopAppBarType.Center -> {
            CenterAlignedTopAppBar(
                    title = title,
                    modifier = modifier,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior
            )
        }
        EnhancedTopAppBarType.Small -> {
            TopAppBar(
                    title = title,
                    modifier = modifier,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior
            )
        }
        EnhancedTopAppBarType.Medium -> {
            MediumTopAppBar(
                    title = title,
                    modifier = modifier,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior
            )
        }
        EnhancedTopAppBarType.Large -> {
            LargeTopAppBar(
                    title = { title() },
                    modifier = modifier,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior
            )
        }
    }
}

enum class EnhancedTopAppBarType {
    Center,
    Small,
    Medium,
    Large
}
