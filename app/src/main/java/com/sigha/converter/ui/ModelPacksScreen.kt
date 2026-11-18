package com.sigha.converter.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.R
import com.sigha.converter.data.PackDownloadState
import com.sigha.converter.data.Repo
import com.sigha.converter.model.ModelPacks
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import kotlinx.coroutines.launch

/**
 * Optional on-device model packs. Nothing is fetched unless the user confirms the download, and
 * the confirmation states the size (plus a warning on mobile data). The download itself is
 * app-scoped: leaving this screen does not cancel it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPacksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val packs = remember { Repo.packs.catalog() }
    val downloadStates by Repo.packDownloads.states.collectAsStateWithLifecycle()
    var pendingPack by remember { mutableStateOf<ModelPacks.PackInfo?>(null) }
    var pendingResume by remember { mutableStateOf(false) }
    var seenStates by remember { mutableStateOf<Map<String, PackDownloadState>>(emptyMap()) }

    // Recomputed when a download finishes; the cheap check reads file sizes only.
    val installedIds = remember(downloadStates) {
        packs.filter { pack ->
            downloadStates[pack.id] is PackDownloadState.Installed || Repo.packs.isInstalled(pack.id)
        }.map { pack -> pack.id }.toSet()
    }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(downloadStates) {
        downloadStates.forEach { (id, state) ->
            val previous = seenStates[id]
            when {
                state is PackDownloadState.Installed && previous !is PackDownloadState.Installed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.model_packs_download_done))
                state is PackDownloadState.Failed && previous !is PackDownloadState.Failed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.model_packs_download_failed))
            }
        }
        seenStates = downloadStates
    }

    fun openLicense(pack: ModelPacks.PackInfo) {
        val url = pack.licenseUrl
        when {
            url.isNullOrBlank() -> showMessage(context.getString(R.string.model_packs_license_missing))
            !openUrl(context, url) -> showMessage(context.getString(R.string.model_packs_open_license_failed))
            else -> Unit
        }
    }

    fun requestDownload(pack: ModelPacks.PackInfo) {
        pendingResume = ModelPacks.hasPartialDownload(pack.id)
        pendingPack = pack
    }

    pendingPack?.let { pack ->
        val size = formatFileSize(context, pack.sizeBytes).orEmpty()
        val metered = remember(pack.id) { isMeteredConnection(context) }
        AlertDialog(
            onDismissRequest = { pendingPack = null },
            title = { Text(stringResource(R.string.ui_pack_download_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.ui_pack_download_body,
                            stringResource(pack.titleRes),
                            size
                        )
                    )
                    if (metered) {
                        Text(
                            text = stringResource(R.string.ui_pack_metered_note, size),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (pendingResume) {
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
                    pendingPack = null
                    Repo.packDownloads.start(pack.id)
                }) {
                    Text(stringResource(R.string.ui_pack_download_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPack = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.model_packs_title),
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.model_packs_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SighaCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.model_packs_offline_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionHeader(title = stringResource(R.string.model_packs_section_ocr))
            }
            if (packs.isEmpty()) {
                item {
                    SighaCard {
                        Text(
                            text = stringResource(R.string.model_packs_no_packs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    SighaCard {
                        Column {
                            packs.forEach { pack ->
                                val state = downloadStates[pack.id] ?: PackDownloadState.Idle
                                PackRow(
                                    pack = pack,
                                    installed = pack.id in installedIds,
                                    downloading = state is PackDownloadState.Downloading,
                                    progress = (state as? PackDownloadState.Downloading)?.progress ?: 0f,
                                    onDownload = { requestDownload(pack) },
                                    onCancel = { Repo.packDownloads.cancel() },
                                    onLicense = { openLicense(pack) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(
    pack: ModelPacks.PackInfo,
    installed: Boolean,
    downloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onLicense: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(pack.titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(pack.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                installed -> InstalledPill()
                downloading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else -> Button(onClick = onDownload) {
                    Text(stringResource(R.string.model_packs_download))
                }
            }
        }
        if (downloading) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.model_packs_downloading, (progress * 100).toInt().coerceIn(0, 100)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val size = formatFileSize(context, pack.sizeBytes)
            if (size != null) {
                Text(
                    text = size,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(
                    R.string.model_packs_license,
                    pack.license.ifBlank { stringResource(R.string.model_packs_license_unknown) }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onLicense,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_view_license),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun InstalledPill() {
    Text(
        text = stringResource(R.string.model_packs_installed),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** True when the active connection reports itself as metered (mobile data, some hotspots). */
private fun isMeteredConnection(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
