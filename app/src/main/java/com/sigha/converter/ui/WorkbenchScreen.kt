package com.sigha.converter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.text.BidiFormatter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory
import com.sigha.converter.data.ConversionInput
import com.sigha.converter.data.OutputMode
import com.sigha.converter.data.PackDownloadState
import com.sigha.converter.data.PublishTarget
import com.sigha.converter.data.Repo
import com.sigha.converter.model.ModelPacks
import com.sigha.converter.ui.components.EmptyState
import com.sigha.converter.ui.components.FormatBadge
import com.sigha.converter.ui.components.FormatIcon
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import com.sigha.converter.ui.components.StatusPill
import com.sigha.converter.ui.components.categoryLabel
import com.sigha.converter.ui.components.mimeOrDefault
import com.sigha.converter.ui.components.statusColor
import com.sigha.converter.work.QueueStatus
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The conversion workbench: pick files, pick the intersection of reachable target formats,
 * tune options, convert. Per-file progress mirrors the app-scoped queue, so leaving this
 * screen does not stop a conversion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchScreen(
    viewModel: WorkbenchViewModel,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appSettings by Repo.settings.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val showPickerMissing: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.ui_no_file_picker)) }
    }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.convert()
        } else {
            // A denial is not a dead end: the user can grant it in Settings or pick an output
            // folder there instead, so the snackbar carries the way out.
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.workbench_storage_permission_denied),
                    actionLabel = context.getString(R.string.action_open_settings),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) openAppSettings(context)
            }
        }
    }

    // API 29+ uses MediaStore, the folder mode uses a persisted SAF grant: neither needs
    // WRITE_EXTERNAL_STORAGE. Only legacy Downloads publishing does -- and that includes a
    // chosen folder that is no longer writable, because the runner then falls back to Downloads.
    val folderUnavailable = appSettings.outputMode == OutputMode.FOLDER && run {
        val raw = appSettings.outputFolderUri ?: return@run true
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return@run true
        !Repo.publisher.hasWriteAccess(PublishTarget.Folder(uri))
    }
    val needsStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        (appSettings.outputMode == OutputMode.DOWNLOADS || folderUnavailable)

    // Notification permission is asked at the moment it matters (a batch or a long media
    // conversion), with a one-line rationale first -- not as a cold prompt on first launch.
    var showNotificationPriming by remember { mutableStateOf(false) }
    val notificationMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    val markNotificationsAsked: () -> Unit = {
        showNotificationPriming = false
        scope.launch { runCatching { Repo.settings.markNotificationsAsked() } }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        markNotificationsAsked()
        viewModel.convert()
    }
    val shouldPrimeNotification: () -> Boolean = {
        notificationMissing && !appSettings.notificationsAsked && (
            state.files.size >= 2 ||
                state.target?.category == FormatCategory.AUDIO ||
                state.target?.category == FormatCategory.VIDEO
            )
    }

    val requestConvert: () -> Unit = {
        val granted = !needsStoragePermission || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else if (shouldPrimeNotification()) {
            showNotificationPriming = true
        } else {
            viewModel.convert()
        }
    }

    if (showNotificationPriming) {
        AlertDialog(
            onDismissRequest = {
                markNotificationsAsked()
                viewModel.convert()
            },
            title = { Text(stringResource(R.string.notif_prime_title)) },
            text = { Text(stringResource(R.string.notif_prime_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPriming = false
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text(stringResource(R.string.notif_prime_turn_on))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        markNotificationsAsked()
                        viewModel.convert()
                    }
                ) {
                    Text(stringResource(R.string.notif_prime_not_now))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListBulleted,
                            contentDescription = stringResource(R.string.action_open_queue)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.files.isNotEmpty()) {
                ConvertBar(
                    state = state,
                    onConvert = requestConvert,
                    onCancel = viewModel::cancelConversion
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (state.files.isEmpty()) {
            // Scrollable on purpose: in landscape the empty state is taller than the viewport,
            // and the choose-files button must stay reachable instead of being clipped away.
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
                        icon = Icons.Outlined.SwapHoriz,
                        title = stringResource(R.string.workbench_empty_title),
                        body = stringResource(R.string.workbench_empty_body),
                        footnote = stringResource(R.string.workbench_empty_supported),
                        action = {
                            Button(
                                onClick = { pickFiles.launchSaf(arrayOf("*/*"), showPickerMissing) },
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_choose_files),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    )
                }
                if (state.isLoadingFiles) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isLoadingFiles) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(
                            title = pluralStringResource(
                                R.plurals.workbench_files_count,
                                state.files.size,
                                state.files.size
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { pickFiles.launchSaf(arrayOf("*/*"), showPickerMissing) },
                            enabled = !state.isConverting
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.workbench_add_more))
                        }
                        IconButton(
                            onClick = viewModel::clearSelection,
                            enabled = !state.isConverting
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.workbench_clear_selection)
                            )
                        }
                    }
                }
                items(items = state.files, key = { file -> file.uri.toString() }) { file ->
                    FileCard(
                        file = file,
                        enabled = !state.isConverting,
                        onRemove = { viewModel.removeFile(file.uri) }
                    )
                }
                item {
                    TargetSection(
                        targets = state.targets,
                        selected = state.target,
                        audioTargetsHidden = state.audioTargetsHidden,
                        onSelect = viewModel::selectTarget
                    )
                }
                if (state.showOcrHint) {
                    item {
                        OcrPackCard(
                            state = state.ocrPackState,
                            onInstall = viewModel::installOcrPack,
                            onCancel = viewModel::cancelOcrPackDownload
                        )
                    }
                }
                if (state.showQuality || state.showBitrate || state.showPages) {
                    item {
                        OptionsSection(
                            state = state,
                            onQuality = viewModel::setQuality,
                            onBitrate = viewModel::setBitrate,
                            onPages = viewModel::setPages
                        )
                    }
                }
                if (state.progressRows.isNotEmpty()) {
                    item {
                        ProgressSection(
                            rows = state.progressRows,
                            isConverting = state.isConverting,
                            onOpenQueue = onOpenQueue,
                            onDismiss = viewModel::clearProgress,
                            onRetryFailed = viewModel::retryFailed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    file: ConversionInput,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    SighaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormatIcon(format = file.format)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatBadge(format = file.format)
                    val size = formatFileSize(context, file.sizeBytes)
                    if (size != null) {
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetSection(
    targets: List<FileFormat>,
    selected: FileFormat?,
    audioTargetsHidden: Boolean,
    onSelect: (FileFormat) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = stringResource(R.string.workbench_target_title))
        if (audioTargetsHidden) {
            Text(
                text = stringResource(R.string.workbench_audio_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (targets.isEmpty()) {
            SighaCard {
                Text(
                    text = stringResource(R.string.workbench_no_target),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SighaCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (category in FormatCategory.entries) {
                        val formats = targets.filter { it.category == category }
                        if (formats.isEmpty()) continue
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = categoryLabel(category),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                formats.forEach { format ->
                                    val isSelected = selected?.id == format.id
                                    val leading: (@Composable () -> Unit)? = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelect(format) },
                                        label = { Text(format.label) },
                                        leadingIcon = leading
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsSection(
    state: WorkbenchUiState,
    onQuality: (Int) -> Unit,
    onBitrate: (Int?) -> Unit,
    onPages: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = stringResource(R.string.workbench_options_title))
        SighaCard {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (state.showQuality) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.option_image_quality),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.option_value_percent, state.options.quality),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val qualityLabel = stringResource(R.string.option_image_quality)
                        val qualityValue = stringResource(R.string.option_value_percent, state.options.quality)
                        Slider(
                            value = state.options.quality.toFloat(),
                            onValueChange = { value -> onQuality(value.roundToInt()) },
                            valueRange = 20f..100f,
                            steps = 15,
                            modifier = Modifier.semantics {
                                contentDescription = qualityLabel
                                stateDescription = qualityValue
                            }
                        )
                    }
                }
                if (state.showBitrate) {
                    BitrateOptions(selected = state.options.bitrateKbps, onSelect = onBitrate)
                }
                if (state.showPages) {
                    val pagesErrorText = stringResource(R.string.option_pages_error)
                    OutlinedTextField(
                        value = state.options.pages.orEmpty(),
                        onValueChange = onPages,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                if (state.pagesError) error(pagesErrorText)
                            },
                        label = { Text(stringResource(R.string.option_pages)) },
                        placeholder = { Text(stringResource(R.string.option_pages_hint)) },
                        supportingText = {
                            Text(
                                text = stringResource(
                                    if (state.pagesError) {
                                        R.string.option_pages_error
                                    } else {
                                        R.string.option_pages_helper
                                    }
                                ),
                                modifier = if (state.pagesError) {
                                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                                } else {
                                    Modifier
                                }
                            )
                        },
                        isError = state.pagesError,
                        singleLine = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BitrateOptions(selected: Int?, onSelect: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.option_bitrate),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (selected == null) {
                    stringResource(R.string.option_auto)
                } else {
                    stringResource(R.string.option_value_kbps, selected)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val values = listOf<Int?>(null, 64, 96, 128, 192, 256, 320)
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = {
                        Text(
                            text = if (value == null) {
                                stringResource(R.string.option_auto)
                            } else {
                                value.toString()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(
    rows: List<ProgressRow>,
    isConverting: Boolean,
    onOpenQueue: () -> Unit,
    onDismiss: () -> Unit,
    onRetryFailed: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = LocalContext.current
        val doneRows = rows.filter { it.status == QueueStatus.DONE && it.outputUri != null }
        val doneUris = doneRows.mapNotNull { it.outputUri }
        // Two or more results make the per-row Share icons a chore; one share sheet for the whole
        // batch is what people expect after a batch conversion. Mixed targets share as wildcards.
        val shareAll: (@Composable () -> Unit)? = if (!isConverting && doneUris.size >= 2) {
            {
                val mimes = doneRows
                    .mapNotNull { row -> AppGraph.formats.get(row.toId)?.mimeOrDefault() }
                    .distinct()
                val mime = if (mimes.size == 1) mimes.first() else "*/*"
                TextButton(
                    onClick = {
                        if (!shareFiles(context, doneUris, mime)) {
                            Toast.makeText(context, R.string.share_all_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(pluralStringResource(R.plurals.action_share_all, doneUris.size, doneUris.size))
                }
            }
        } else {
            null
        }
        val trailing: (@Composable () -> Unit)? = if (isConverting) {
            null
        } else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    shareAll?.invoke()
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
            }
        }
        SectionHeader(
            title = stringResource(R.string.workbench_progress_title),
            trailing = trailing
        )
        SighaCard {
            Column {
                rows.forEachIndexed { index, row ->
                    ProgressRowItem(row = row)
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        // Only failures that can actually be retried get the action; a source that can no longer
        // be opened says to add the file again, and a retry button for it would do nothing.
        val failedCount = rows.count {
            it.status == QueueStatus.FAILED && it.errorKind != ConversionFailureKind.FILE_ACCESS_LOST
        }
        if (!isConverting && failedCount > 0) {
            // Retry without re-picking: the queue still holds the inputs and the persisted
            // read grant, so a transient engine failure costs one tap, not a whole redo.
            Button(onClick = onRetryFailed, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.queue_retry_failed, failedCount, failedCount))
            }
        }
        if (!isConverting) {
            TextButton(onClick = onOpenQueue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workbench_open_queue))
            }
        }
    }
}

@Composable
private fun ProgressRowItem(row: ProgressRow) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val icon = when (row.status) {
            QueueStatus.DONE -> Icons.Outlined.CheckCircle
            QueueStatus.FAILED -> Icons.Outlined.ErrorOutline
            QueueStatus.CANCELLED -> Icons.Outlined.Cancel
            QueueStatus.QUEUED -> Icons.Outlined.Schedule
            QueueStatus.RUNNING -> Icons.Outlined.SwapHoriz
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor(row.status),
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.targetLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (row.status == QueueStatus.RUNNING || row.status == QueueStatus.QUEUED) {
                Spacer(Modifier.height(6.dp))
                val progressModifier = if (row.status == QueueStatus.QUEUED) {
                    // Queued has no progress yet; announcing a determinate 0% is misleading.
                    Modifier.fillMaxWidth().clearAndSetSemantics {}
                } else {
                    Modifier.fillMaxWidth()
                }
                LinearProgressIndicator(
                    progress = { if (row.status == QueueStatus.QUEUED) 0f else row.progress },
                    modifier = progressModifier
                )
            }
            if (row.status == QueueStatus.FAILED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(row.errorKind.headlineRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (row.status == QueueStatus.DONE && row.note != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = noteText(row.note).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            row.displayPath?.let { path ->
                if (row.status == QueueStatus.DONE) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.queue_saved_to,
                            BidiFormatter.getInstance().unicodeWrap(path)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.Content
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        StatusPill(status = row.status)
        val output = row.outputUri
        if (row.status == QueueStatus.DONE && output != null) {
            val mime = AppGraph.formats.get(row.toId)?.mimeOrDefault() ?: "*/*"
            // Share is the first thing most people do with a converted file (send it on), and it
            // used to exist only in the queue screen, which a single conversion never shows.
            IconButton(
                onClick = {
                    if (!shareFile(context, output, mime)) {
                        Toast.makeText(context, R.string.queue_share_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.action_share)
                )
            }
            IconButton(
                onClick = { openFileWith(context, output, mime) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.action_open_file)
                )
            }
        }
    }
}

/**
 * Inline install for the optional OCR pack. OCR targets are only shown once the pack exists, so
 * this card is the visible, gated route that replaces the old dead-end hint.
 */
@Composable
private fun OcrPackCard(
    state: PackDownloadState,
    onInstall: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    val pack = remember { Repo.packs.catalog().firstOrNull { it.id == ModelPacks.OCR_PACK_ID } }
    val size = formatFileSize(context, pack?.sizeBytes ?: 0L)
    SighaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.model_packs_section_ocr),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(R.string.ui_ocr_targets_hidden),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (state) {
                is PackDownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.model_packs_downloading,
                                (state.progress * 100).toInt().coerceIn(0, 100)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                is PackDownloadState.Failed -> {
                    Text(
                        text = stringResource(R.string.model_packs_download_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { confirming = true }) {
                        Text(stringResource(R.string.model_packs_download))
                    }
                }
                else -> {
                    Button(onClick = { confirming = true }) {
                        Text(
                            text = if (size != null) {
                                stringResource(R.string.workbench_ocr_install, size)
                            } else {
                                stringResource(R.string.model_packs_download)
                            }
                        )
                    }
                }
            }
        }
    }
    if (confirming) {
        val metered = remember { isMeteredConnection(context) }
        val resume = remember { ModelPacks.hasPartialDownload(ModelPacks.OCR_PACK_ID) }
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.ui_pack_download_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.ui_pack_download_body,
                            pack?.let { stringResource(it.titleRes) }
                                ?: stringResource(R.string.model_packs_section_ocr),
                            size.orEmpty()
                        )
                    )
                    if (metered) {
                        Text(
                            text = stringResource(R.string.ui_pack_metered_note, size.orEmpty()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (resume) {
                        Text(
                            text = stringResource(R.string.ui_pack_resume_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onInstall()
                }) {
                    Text(stringResource(R.string.ui_pack_download_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ConvertBar(
    state: WorkbenchUiState,
    onConvert: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Column {
            // Only explain a target choice that exists: when no format is reachable the target
            // card already says so, and "choose a target" would be impossible advice.
            val targetHint = if (!state.isConverting && state.target == null &&
                state.files.isNotEmpty() && state.targets.isNotEmpty()
            ) {
                stringResource(R.string.workbench_target_hint)
            } else {
                null
            }
            if (targetHint != null) {
                Text(
                    text = targetHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isConverting) {
                    val rows = state.progressRows
                    val total = rows.size.takeIf { it > 0 } ?: state.files.size
                    val runningIndex = rows.indexOfFirst { it.status == QueueStatus.RUNNING }
                    val finished = rows.count {
                        it.status == QueueStatus.DONE || it.status == QueueStatus.FAILED ||
                            it.status == QueueStatus.CANCELLED
                    }
                    val position = when {
                        runningIndex >= 0 -> runningIndex + 1
                        else -> (finished + 1).coerceAtMost(total.coerceAtLeast(1))
                    }
                    val current = rows.getOrNull(if (runningIndex >= 0) runningIndex else finished)
                    val bidi = BidiFormatter.getInstance()
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.workbench_converting_progress,
                                position,
                                total,
                                bidi.unicodeWrap(state.target?.label ?: current?.targetLabel.orEmpty())
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                        current?.let { row ->
                            Text(
                                text = bidi.unicodeWrap(row.displayName),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDirection = TextDirection.Content
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                } else {
                    Button(
                        onClick = onConvert,
                        enabled = state.target != null && state.files.isNotEmpty() &&
                            !state.isLoadingFiles && !state.pagesError,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (targetHint != null) {
                                    Modifier.semantics { stateDescription = targetHint }
                                } else {
                                    Modifier
                                }
                            ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val targetLabel = state.target?.label
                        Text(
                            text = when {
                                // No target yet: the CTA must not end in a dangling "to".
                                targetLabel == null && state.files.size == 1 ->
                                    stringResource(R.string.workbench_convert)
                                targetLabel == null -> pluralStringResource(
                                    R.plurals.workbench_convert_files,
                                    state.files.size,
                                    state.files.size
                                )
                                state.files.size == 1 ->
                                    stringResource(R.string.workbench_convert_to, targetLabel)
                                else -> pluralStringResource(
                                    R.plurals.workbench_convert_files_to,
                                    state.files.size,
                                    state.files.size,
                                    targetLabel
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** True when the active connection reports itself as metered (mobile data, some hotspots). */
private fun isMeteredConnection(context: android.content.Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
