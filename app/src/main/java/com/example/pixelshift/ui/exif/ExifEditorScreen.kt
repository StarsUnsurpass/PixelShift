package com.example.pixelshift.ui.exif

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pixelshift.data.ExifRepository
import com.example.pixelshift.ui.components.CommonTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifEditorScreen(
    repository: ExifRepository,
    uri: android.net.Uri,
    onNavigateBack: () -> Unit
) {
    val viewModel: ExifEditorViewModel = viewModel(
        factory = ExifEditorViewModel.Factory(repository, uri)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAllTags by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("更改已成功保存")
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "EXIF 编辑器",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showAllTags = !showAllTags }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "切换显示模式",
                            tint = if (showAllTags) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.clearAllMetadata() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "清除所有")
                    }
                    IconButton(onClick = { viewModel.saveChanges() }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val displayTags = if (showAllTags) {
                uiState.tags
            } else {
                uiState.tags.filter { !it.value.isNullOrBlank() }
            }
            
            val groupedTags = displayTags.groupBy { it.category }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Image Preview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "预览图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (groupedTags.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "未检测到元数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                TextButton(onClick = { showAllTags = true }) {
                                    Text("显示所有可编辑标签")
                                }
                            }
                        }
                    }
                }

                groupedTags.forEach { (category, tags) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                        )
                    }
                    items(tags, key = { it.tag }) { tag ->
                        ExifTagItem(
                            label = tag.label,
                            value = tag.value ?: "",
                            onValueChange = { newValue ->
                                viewModel.updateTag(tag.tag, newValue)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExifTagItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (value.isNotBlank()) null else androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}
