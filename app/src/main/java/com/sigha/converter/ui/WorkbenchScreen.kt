package com.sigha.converter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory
import com.sigha.converter.data.ConversionInput
import com.sigha.converter.data.OutputMode
import com.sigha.converter.data.Repo
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

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.convert()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.workbench_storage_permission_denied))
            }
        }
    }

    // API 29+ uses MediaStore, the folder mode uses a persisted SAF grant: neither needs
    // WRITE_EXTERNAL_STORAGE. Only legacy Downloads publishing does.
    val needsStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        appSettings.outputMode == OutputMode.DOWNLOADS

    val requestConvert: () -> Unit = {
        val granted = !needsStoragePermission || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.convert()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_tagline),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(
                    icon = Icons.Outlined.SwapHoriz,
                    title = stringResource(R.string.workbench_empty_title),
                    body = stringResource(R.string.workbench_empty_body),
                    footnote = stringResource(R.string.workbench_empty_supported),
                    action = {
                        Button(
                            onClick = { pickFiles.launch(arrayOf("*/*")) },
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
                if (state.isLoadingFiles) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    )
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
                            title = stringResource(R.string.workbench_files_count, state.files.size),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { pickFiles.launch(arrayOf("*/*")) },
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
                        onSelect = viewModel::selectTarget
                    )
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
                            onDismiss = viewModel::clearProgress
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
    onSelect: (FileFormat) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = stringResource(R.string.workbench_target_title))
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
                        Slider(
                            value = state.options.quality.toFloat(),
                            onValueChange = { value -> onQuality(value.roundToInt()) },
                            valueRange = 20f..100f,
                            steps = 15
                        )
                    }
                }
                if (state.showBitrate) {
                    BitrateOptions(selected = state.options.bitrateKbps, onSelect = onBitrate)
                }
                if (state.showPages) {
                    OutlinedTextField(
                        value = state.options.pages.orEmpty(),
                        onValueChange = onPages,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.option_pages)) },
                        placeholder = { Text(stringResource(R.string.option_pages_hint)) },
                        supportingText = { Text(stringResource(R.string.option_pages_helper)) },
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
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val trailing: (@Composable () -> Unit)? = if (isConverting) {
            null
        } else {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_clear))
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
            .padding(vertical = 10.dp),
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
                LinearProgressIndicator(
                    progress = { if (row.status == QueueStatus.QUEUED) 0f else row.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!row.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = row.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        StatusPill(status = row.status)
        val output = row.outputUri
        if (row.status == QueueStatus.DONE && output != null) {
            IconButton(
                onClick = {
                    val mime = AppGraph.formats.get(row.toId)?.mimeOrDefault() ?: "*/*"
                    openFileWith(context, output, mime)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.action_open_file)
                )
            }
        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isConverting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.workbench_converting),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            } else {
                Button(
                    onClick = onConvert,
                    enabled = state.target != null && state.files.isNotEmpty() && !state.isLoadingFiles,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.files.size == 1) {
                            stringResource(R.string.workbench_convert)
                        } else {
                            stringResource(R.string.workbench_convert_files, state.files.size)
                        }
                    )
                }
            }
        }
    }
}
