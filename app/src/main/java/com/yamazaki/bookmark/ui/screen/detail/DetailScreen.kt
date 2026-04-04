package com.yamazaki.bookmark.ui.screen.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yamazaki.bookmark.ui.component.LinkStatusBadge
import com.yamazaki.bookmark.ui.component.ThumbnailImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("削除確認") },
            text = { Text("このブックマークを削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { viewModel.saveEdit() }) {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                        IconButton(onClick = { viewModel.cancelEdit() }) {
                            Icon(Icons.Default.Close, contentDescription = "キャンセル")
                        }
                    } else {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "編集")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.bookmark == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ブックマークが見つかりません")
                }
            }
            else -> {
                val bookmark = uiState.bookmark!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Thumbnail
                    ThumbnailImage(
                        url = bookmark.thumbnailUrl,
                        contentDescription = bookmark.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title
                        if (uiState.isEditing) {
                            OutlinedTextField(
                                value = uiState.editTitle,
                                onValueChange = { viewModel.updateEditTitle(it) },
                                label = { Text("タイトル") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = bookmark.title,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // URL
                        Text(
                            text = bookmark.url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Link status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("リンク状態: ", style = MaterialTheme.typography.bodyMedium)
                            LinkStatusBadge(status = bookmark.status)
                            if (bookmark.lastCheckedAt != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "最終チェック: ${bookmark.lastCheckedAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { viewModel.checkLink() },
                                enabled = !uiState.isChecking,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(end = 8.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text("リンクチェック")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { openInChrome(context, bookmark.url) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Chromeで開く")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Delete button
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("ブックマークを削除")
                        }
                    }
                }
            }
        }
    }
}

private fun openInChrome(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage("com.android.chrome")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(fallback)
        } catch (_: ActivityNotFoundException) {
            // No browser available
        }
    }
}
