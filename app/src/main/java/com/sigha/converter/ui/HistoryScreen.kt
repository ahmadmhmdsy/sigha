package com.sigha.converter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.data.HistoryEntry
import com.sigha.converter.data.Repo
import com.sigha.converter.ui.components.ConfirmDialog
import com.sigha.converter.ui.components.EmptyState
import com.sigha.converter.ui.components.FormatBadge
import com.sigha.converter.ui.components.FormatIcon
import com.sigha.converter.ui.components.SighaTopAppBar
import com.sigha.converter.ui.components.mimeOrDefault
import kotlinx.coroutines.launch

/** Last 50 conversions with reopen / share / clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val entries by Repo.history.entries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.history_title),
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_clear_all)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = entries, key = { entry -> entry.timestamp.toString() + entry.outputUri }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onOpen = {
                            if (!entry.success) {
                                // A failed row has no file to open; surface its stored error instead.
                                val message = entry.error?.takeIf { it.isNotBlank() }
                                    ?: context.getString(R.string.history_failed)
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            } else {
                                val mime = AppGraph.formats.get(entry.toId)?.mimeOrDefault() ?: "*/*"
                                val uri = entry.outputUri.takeIf { it.isNotBlank() }
                                    ?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
                                if (uri == null || !openFileWith(context, uri, mime)) {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.history_open_failed)) }
                                }
                            }
                        },
                        onShare = {
                            val mime = AppGraph.formats.get(entry.toId)?.mimeOrDefault() ?: "*/*"
                            val uri = entry.outputUri.takeIf { it.isNotBlank() }?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
                            if (uri == null || !shareFile(context, uri, mime)) {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.history_share_failed)) }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            body = stringResource(R.string.history_clear_body),
            confirmLabel = stringResource(R.string.action_clear_all),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                showClearDialog = false
                scope.launch {
                    Repo.history.clear()
                    snackbarHostState.showSnackbar(context.getString(R.string.history_cleared))
                }
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: () -> Unit, onShare: () -> Unit) {
    val context = LocalContext.current
    val toFormat = AppGraph.formats.get(entry.toId)
    val fromFormat = AppGraph.formats.get(entry.fromId)
    val accent = if (entry.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (toFormat != null) {
                FormatIcon(format = toFormat)
            } else {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.outputName.ifBlank { entry.inputName },
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fromFormat?.let { FormatBadge(format = it) }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    toFormat?.let { FormatBadge(format = it) }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDateTime(entry.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val size = formatFileSize(context, entry.sizeBytes)
                    if (size != null) {
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!entry.success) {
                        Text(
                            text = stringResource(R.string.history_failed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (!entry.success && !entry.error.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (entry.success) {
                IconButton(onClick = onOpen) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.action_open_file)
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.action_share)
                    )
                }
            }
        }
    }
}
