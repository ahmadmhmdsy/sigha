package com.sigha.converter.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.R
import com.sigha.converter.data.OutputMode
import com.sigha.converter.data.Repo
import com.sigha.converter.data.ThemeMode
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import kotlinx.coroutines.launch

private val OCR_LANGUAGES = listOf(
    "eng" to R.string.ocr_language_eng,
    "ara" to R.string.ocr_language_ara,
    "eng+ara" to R.string.ocr_language_eng_ara
)

/** Theme, output destination, OCR language and links to the secondary screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenModelPacks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by Repo.settings.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showOcrDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val showPickerMissing: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.ui_no_file_picker)) }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            // A tree the provider lets us read but not write would fail at save time; refuse it
            // here instead of letting a conversion fail later.
            val writable = runCatching {
                DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
            }.getOrDefault(false)
            if (!writable) {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.ui_folder_not_writable))
                }
                return@rememberLauncherForActivityResult
            }
            // Keep the grant across restarts; providers that don't support persistence
            // still work for this session.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            scope.launch {
                Repo.settings.setOutputFolder(uri.toString())
                Repo.settings.setOutputMode(OutputMode.FOLDER)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { SighaTopAppBar(title = stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = stringResource(R.string.settings_appearance))
            SighaCard {
                ThemeSelector(
                    selected = settings.themeMode,
                    onSelect = { mode -> scope.launch { Repo.settings.setThemeMode(mode) } }
                )
            }

            SectionHeader(title = stringResource(R.string.settings_output))
            SighaCard {
                Column {
                    Text(
                        text = stringResource(R.string.settings_output_mode),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Column(modifier = Modifier.selectableGroup()) {
                        OutputModeRow(
                            title = stringResource(R.string.output_mode_downloads),
                            description = stringResource(R.string.output_mode_downloads_desc),
                            selected = settings.outputMode == OutputMode.DOWNLOADS,
                            onClick = { scope.launch { Repo.settings.setOutputMode(OutputMode.DOWNLOADS) } }
                        )
                        OutputModeRow(
                            title = stringResource(R.string.output_mode_folder),
                            description = stringResource(R.string.output_mode_folder_desc),
                            selected = settings.outputMode == OutputMode.FOLDER,
                            onClick = {
                                if (settings.outputFolderUri.isNullOrBlank()) {
                                    folderPicker.launchSaf(null, showPickerMissing)
                                } else {
                                    scope.launch { Repo.settings.setOutputMode(OutputMode.FOLDER) }
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = folderDisplayName(settings.outputFolderUri)
                                ?.let { name -> stringResource(R.string.settings_folder_value, name) }
                                ?: stringResource(R.string.settings_folder_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            modifier = Modifier.heightIn(min = 48.dp),
                            onClick = {
                                folderPicker.launchSaf(
                                    settings.outputFolderUri?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() },
                                    showPickerMissing
                                )
                            }
                        ) {
                            Text(
                                text = if (settings.outputFolderUri.isNullOrBlank()) {
                                    stringResource(R.string.action_choose_folder)
                                } else {
                                    stringResource(R.string.action_change)
                                }
                            )
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.settings_ocr))
            SighaCard {
                Column {
                    SettingsNavRow(
                        icon = Icons.Outlined.TextFields,
                        title = stringResource(R.string.settings_ocr_language),
                        value = ocrLanguageLabel(settings.ocrLanguage),
                        onClick = { showOcrDialog = true }
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_ocr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        icon = Icons.Outlined.Extension,
                        title = stringResource(R.string.settings_model_packs),
                        description = stringResource(R.string.settings_model_packs_desc),
                        onClick = onOpenModelPacks
                    )
                }
            }

            SighaCard {
                Column {
                    SettingsNavRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about),
                        description = stringResource(R.string.settings_about_desc),
                        onClick = onOpenAbout
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        icon = Icons.Outlined.Article,
                        title = stringResource(R.string.settings_licenses),
                        description = stringResource(R.string.settings_licenses_desc),
                        onClick = onOpenLicenses
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.ui_settings_privacy),
                        description = stringResource(R.string.ui_settings_privacy_desc),
                        onClick = onOpenPrivacy
                    )
                }
            }
        }
    }

    if (showOcrDialog) {
        AlertDialog(
            onDismissRequest = { showOcrDialog = false },
            title = { Text(stringResource(R.string.settings_ocr_language)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    OCR_LANGUAGES.forEach { option ->
                        val selected = settings.ocrLanguage == option.first
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        showOcrDialog = false
                                        scope.launch { Repo.settings.setOcrLanguage(option.first) }
                                    }
                                )
                                .heightIn(min = 48.dp)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(option.second),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOcrDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(themeLabel(mode)) }
            )
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun ocrLanguageLabel(id: String): String = when (id) {
    "ara" -> stringResource(R.string.ocr_language_ara)
    "eng+ara" -> stringResource(R.string.ocr_language_eng_ara)
    else -> stringResource(R.string.ocr_language_eng)
}

@Composable
private fun OutputModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row owns the click and the state; the radio is only its visual representation,
        // so TalkBack sees one focus stop that announces "selected, radio button".
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    description: String? = null,
    value: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Turns a SAF tree uri into something readable like "Download/Sigha". */
private fun folderDisplayName(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
    val segment = uri.lastPathSegment ?: return null
    val colon = segment.lastIndexOf(':')
    val name = if (colon >= 0 && colon < segment.length - 1) segment.substring(colon + 1) else segment
    return name.takeIf { it.isNotBlank() }
}
