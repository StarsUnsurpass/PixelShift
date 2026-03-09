package com.example.pixelshift.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveLayoutScreen(
        title: @Composable () -> Unit,
        onGoBack: () -> Unit,
        actions: @Composable RowScope.() -> Unit = {},
        imagePreview: @Composable () -> Unit = {},
        controls: (@Composable ColumnScope.() -> Unit)? = null,
        buttons: @Composable (RowScope.() -> Unit)? = null,
        isPortrait: Boolean =
                LocalConfiguration.current.orientation ==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT,
        canShowScreenData: Boolean = true,
        contentPadding: Dp = 16.dp,
        showImagePreviewAsStickyHeader: Boolean = true,
        shouldDisableBackHandler: Boolean = false,
        floatingActionButton: @Composable () -> Unit = {},
        topAppBarType: EnhancedTopAppBarType = EnhancedTopAppBarType.Medium
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
            Column(Modifier.fillMaxSize()) {
                EnhancedTopAppBar(
                        title = title,
                        navigationIcon = {
                            if (!shouldDisableBackHandler) {
                                IconButton(onClick = onGoBack) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        type = topAppBarType
                )

                if (isPortrait) {
                    LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp + contentPadding),
                            modifier = Modifier.weight(1f)
                    ) {
                        if (canShowScreenData) {
                            if (showImagePreviewAsStickyHeader) {
                                item {
                                    Box(
                                            modifier =
                                                    Modifier.fillMaxWidth().padding(contentPadding),
                                            contentAlignment = Alignment.Center
                                    ) { imagePreview() }
                                }
                            }
                            item {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(horizontal = contentPadding),
                                        verticalArrangement = Arrangement.spacedBy(contentPadding)
                                ) { controls?.invoke(this) }
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.weight(1f)) {
                        if (canShowScreenData) {
                            Box(
                                    modifier =
                                            Modifier.weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(contentPadding),
                                    contentAlignment = Alignment.Center
                            ) { imagePreview() }
                            LazyColumn(
                                    contentPadding =
                                            PaddingValues(
                                                    bottom = 88.dp + contentPadding,
                                                    top = contentPadding,
                                                    end = contentPadding,
                                                    start = contentPadding
                                            ),
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(contentPadding)
                            ) {
                                item {
                                    Column(
                                            verticalArrangement =
                                                    Arrangement.spacedBy(contentPadding)
                                    ) { controls?.invoke(this) }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            if (buttons != null) {
                Box(
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(contentPadding)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            content = { buttons() }
                    )
                }
            }
            
            // FAB
             Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
            ) {
                floatingActionButton()
            }
        }
    }
}
