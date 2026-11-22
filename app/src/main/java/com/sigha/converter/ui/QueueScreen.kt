package com.sigha.converter.ui

import android.text.BidiFormatter
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.work.QueueEntry
import com.sigha.converter.data.Repo
import com.sigha.converter.ui.components.EmptyState
import com.sigha.converter.ui.components.FormatBadge
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import com.sigha.converter.ui.components.StatusPill
import com.sigha.converter.ui.components.mimeOrDefault
import com.sigha.converter.ui.components.statusColor
import com.sigha.converter.work.QueueStatus
import kotlinx.coroutines.launch

/** Live queue plus finished results with Open / Share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    onOpenWorkbench: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by Repo.queue.entries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val active = entries.filter { it.status == QueueStatus.QUEUED || it.status == QueueStatus.RUNNING }
    val finished = entries.filter {
        it.status == QueueStatus.DONE || it.status == QueueStatus.FAILED || it.status == QueueStatus.CANCELLED
    }
    val failedEntries = finished.filter { it.status == QueueStatus.FAILED }
    // Sources lost since the batch started are shown as failed but cannot be retried; the rows
    // explain that the file has to be added again.
    val retryableFailures = failedEntries.filter {
        it.errorKind != ConversionFailureKind.FILE_ACCESS_LOST
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.queue_title),
                onBack = onBack,
                actions = {
                    if (finished.isNotEmpty() && active.isEmpty()) {
                        TextButton(onClick = { Repo.queue.clearFinished() }) {
                            Text(stringResource(R.string.queue_clear_finished))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (active.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { Repo.queue.cancelAll() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (entries.isEmpty()) {
            // Scrollable for the same reason as the workbench empty state: landscape phones
            // and font scale 2.0 must not clip the only content on the screen.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.FormatListBulleted,
                        title = stringResource(R.string.queue_empty_title),
                        body = stringResource(R.string.queue_empty_body),
                        action = {
                            Button(onClick = onOpenWorkbench) {
                                Text(stringResource(R.string.action_choose_files))
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.queue_active_section))
                    }
                    items(items = active, key = { it.id }) { entry ->
                        QueueRow(entry = entry) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                }
                if (finished.isNotEmpty()) {
                    if (retryableFailures.isNotEmpty() && active.isEmpty()) {
                        item {
                            // One tap retries every failed row with the same target and options,
                            // so a batch that hit a transient engine problem never has to be
                            // rebuilt by hand.
                            Button(
                                onClick = {
                                    val outcome = Repo.queue.retryFailed()
                                    if (outcome.startedIds.isEmpty() && outcome.lostCount == 0) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.queue_retry_unavailable)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    pluralStringResource(
                                        R.plurals.queue_retry_failed,
                                        retryableFailures.size,
                                        retryableFailures.size
                                    )
                                )
                            }
                        }
                    }
                    item {
                        val doneEntries = finished.filter {
                            it.status == QueueStatus.DONE && it.outputUri != null
                        }
                        SectionHeader(
                            title = stringResource(R.string.queue_results_section),
                            modifier = Modifier.padding(top = 8.dp),
                            trailing = if (doneEntries.size >= 2) {
                                {
                                    val mimes = doneEntries
                                        .mapNotNull { entry ->
                                            AppGraph.formats.get(entry.toId)?.mimeOrDefault()
                                        }
                                        .distinct()
                                    val mime = if (mimes.size == 1) mimes.first() else "*/*"
                                    TextButton(
                                        onClick = {
                                            val uris = doneEntries.mapNotNull { it.outputUri }
                                            if (!shareFiles(context, uris, mime)) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        context.getString(R.string.share_all_failed)
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.action_share_all,
                                                doneEntries.size,
                                                doneEntries.size
                                            )
                                        )
                                    }
                                }
                            } else {
                                null
                            }
                        )
                    }
                    items(items = finished, key = { it.id }) { entry ->
                        QueueRow(entry = entry) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(entry: QueueEntry, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    val fromFormat = AppGraph.formats.get(entry.fromId)
    val toFormat = AppGraph.formats.get(entry.toId)
    val mime = toFormat?.mimeOrDefault() ?: "*/*"
    val fromLabel = fromFormat?.label ?: entry.fromId
    val toLabel = toFormat?.label ?: entry.toId
    val routeDescription = stringResource(R.string.ui_conversion_from_to, fromLabel, toLabel)

    SighaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val icon = when (entry.status) {
                    QueueStatus.DONE -> Icons.Outlined.CheckCircle
                    QueueStatus.FAILED -> Icons.Outlined.ErrorOutline
                    QueueStatus.CANCELLED -> Icons.Outlined.Cancel
                    QueueStatus.QUEUED -> Icons.Outlined.Schedule
                    QueueStatus.RUNNING -> Icons.Outlined.SwapHoriz
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor(entry.status),
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.outputName ?: entry.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.clearAndSetSemantics { contentDescription = routeDescription },
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
                }
                StatusPill(status = entry.status)
            }
            if (entry.status == QueueStatus.RUNNING || entry.status == QueueStatus.QUEUED) {
                LinearProgressIndicator(
                    progress = { if (entry.status == QueueStatus.QUEUED) 0f else entry.progress },
                    modifier = if (entry.status == QueueStatus.QUEUED) {
                        // Queued has no progress yet; announcing a determinate 0% is misleading.
                        Modifier.fillMaxWidth().clearAndSetSemantics {}
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
            }
            if (entry.status == QueueStatus.FAILED) {
                Text(
                    text = stringResource(entry.errorKind.headlineRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                val details = entry.errorMessage?.takeIf { it.isNotBlank() }
                if (details != null) {
                    var showDetails by remember(entry.id) { mutableStateOf(false) }
                    TextButton(
                        onClick = { showDetails = !showDetails },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (showDetails) R.string.ui_details_hide else R.string.ui_details_show
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (showDetails) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            entry.note?.let { note ->
                noteText(note)?.let { body ->
                    Text(
                        text = stringResource(R.string.ui_note_label, body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val output = entry.outputUri
            if (entry.status == QueueStatus.DONE && output != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayPath
                            ?.let { path ->
                                stringResource(
                                    R.string.queue_saved_to,
                                    BidiFormatter.getInstance().unicodeWrap(path)
                                )
                            }
                            ?: stringResource(R.string.queue_saved_generic),
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.Content
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (!openFileWith(context, output, mime)) {
                                showMessage(context.getString(R.string.queue_open_failed))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.action_open_file)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!shareFile(context, output, mime)) {
                                showMessage(context.getString(R.string.queue_share_failed))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.action_share)
                        )
                    }
                }
            }
        }
    }
}
