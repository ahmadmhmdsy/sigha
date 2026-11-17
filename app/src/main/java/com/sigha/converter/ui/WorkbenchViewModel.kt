package com.sigha.converter.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.ConversionOptions
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory
import com.sigha.converter.model.ModelPacks
import com.sigha.converter.data.ConversionInput
import com.sigha.converter.data.ConversionRunner
import com.sigha.converter.data.IncomingFilesBus
import com.sigha.converter.work.QueueEntry
import com.sigha.converter.data.Repo
import com.sigha.converter.data.takePersistableReadPermission
import com.sigha.converter.work.QueueStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One row in the workbench progress list, mirrored from the app-scoped queue. */
data class ProgressRow(
    val id: String,
    val displayName: String,
    val targetLabel: String,
    val toId: String,
    val status: QueueStatus,
    val progress: Float,
    /** English diagnostic, shown as details only. */
    val errorMessage: String?,
    val errorKind: ConversionFailureKind,
    val note: String?,
    val outputUri: Uri?,
    val outputName: String?,
    val displayPath: String?
)

data class WorkbenchUiState(
    val files: List<ConversionInput> = emptyList(),
    val targets: List<FileFormat> = emptyList(),
    val target: FileFormat? = null,
    val options: ConversionOptions = ConversionOptions(),
    val progressRows: List<ProgressRow> = emptyList(),
    val isConverting: Boolean = false,
    val isLoadingFiles: Boolean = false,
    val showQuality: Boolean = false,
    val showBitrate: Boolean = false,
    val showPages: Boolean = false,
    /** True while OCR targets are hidden because the optional OCR pack is not installed. */
    val showOcrHint: Boolean = false
)

/**
 * Owns the workbench selection and target choice. Conversion itself runs in the app-scoped
 * [com.sigha.converter.work.ConversionQueue] so it survives tab switches; this ViewModel only
 * mirrors the queue into UI rows and forwards user intent.
 */
class WorkbenchViewModel : ViewModel() {

    private val context = AppGraph.app
    private val settings = Repo.settings
    private val queue = Repo.queue

    private val _state = MutableStateFlow(WorkbenchUiState())
    val state: StateFlow<WorkbenchUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var batchIds: List<String> = emptyList()
    private var batchSummarised = false

    init {
        viewModelScope.launch {
            IncomingFilesBus.events.collect { uris ->
                if (uris.isNotEmpty()) {
                    IncomingFilesBus.consume()
                    addFiles(uris)
                }
            }
        }
        viewModelScope.launch {
            queue.entries.collect { entries -> syncProgress(entries) }
        }
        viewModelScope.launch {
            if (Repo.settings.takeInterruptedBatch()) {
                _messages.tryEmit(context.getString(R.string.ui_interrupted_batch))
            }
        }
    }

    /** Detects each URI and appends it to the selection, skipping duplicates and unreadables. */
    fun addFiles(uris: List<Uri>) {
        val incoming = uris.distinct().filter { it.scheme != null }
        if (incoming.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFiles = true) }
            val detected = withContext(Dispatchers.IO) {
                incoming.mapNotNull { uri ->
                    takePersistableReadPermission(context, uri)
                    runCatching { AppGraph.formats.detect(context, uri) }.getOrNull()
                }
            }
            _state.update { current ->
                val existing = current.files.map { it.uri }.toSet()
                val added = detected
                    .filter { it.uri !in existing }
                    .map { ConversionInput(it.uri, it.displayName, it.size, it.format) }
                withDerived(current.copy(files = current.files + added, isLoadingFiles = false))
            }
            val skipped = incoming.size - detected.size
            if (detected.isEmpty()) {
                _messages.tryEmit(context.getString(R.string.workbench_detect_failed))
            } else if (skipped > 0) {
                _messages.tryEmit(context.getString(R.string.ui_unsupported_files))
            }
        }
    }

    fun removeFile(uri: Uri) {
        _state.update { current ->
            withDerived(current.copy(files = current.files.filterNot { it.uri == uri }))
        }
    }

    fun clearSelection() {
        if (_state.value.isConverting) return
        batchIds = emptyList()
        _state.update { current ->
            withDerived(current.copy(files = emptyList(), progressRows = emptyList()))
        }
    }

    fun selectTarget(format: FileFormat) {
        _state.update { current ->
            if (current.targets.none { it.id == format.id }) {
                current
            } else {
                withDerived(current.copy(target = format))
            }
        }
    }

    fun setQuality(value: Int) {
        _state.update { it.copy(options = it.options.copy(quality = value.coerceIn(20, 100))) }
    }

    fun setBitrate(value: Int?) {
        _state.update { it.copy(options = it.options.copy(bitrateKbps = value)) }
    }

    fun setPages(value: String) {
        _state.update { it.copy(options = it.options.copy(pages = value.trim().ifBlank { null })) }
    }

    fun convert() {
        val current = _state.value
        if (current.isConverting || current.files.isEmpty()) return
        val target = current.target
        if (target == null) {
            _messages.tryEmit(context.getString(R.string.workbench_no_target_selected))
            return
        }
        if (queue.isRunning) {
            _messages.tryEmit(context.getString(R.string.ui_queue_busy))
            return
        }
        val options = current.options.copy(ocrLanguage = settings.settings.value.ocrLanguage)
        val ids = queue.start(current.files, target, options)
        if (ids.isEmpty()) {
            _messages.tryEmit(context.getString(R.string.ui_queue_busy))
            return
        }
        batchIds = ids
        batchSummarised = false
        _state.update { it.copy(isConverting = true) }
    }

    fun cancelConversion() {
        queue.cancelAll()
    }

    fun clearProgress() {
        if (_state.value.isConverting) return
        batchIds = emptyList()
        _state.update { it.copy(progressRows = emptyList()) }
    }

    private fun syncProgress(entries: List<QueueEntry>) {
        if (batchIds.isEmpty()) return
        val wasConverting = _state.value.isConverting
        val byId = entries.associateBy { it.id }
        val rows = batchIds.mapNotNull { id -> byId[id] }.map { entry ->
            ProgressRow(
                id = entry.id,
                displayName = entry.displayName,
                targetLabel = AppGraph.formats.get(entry.toId)?.label ?: entry.toId,
                toId = entry.toId,
                status = entry.status,
                progress = entry.progress,
                errorMessage = entry.errorMessage,
                errorKind = entry.errorKind,
                note = entry.note,
                outputUri = entry.outputUri,
                outputName = entry.outputName,
                displayPath = entry.displayPath
            )
        }
        val active = rows.any { it.status == QueueStatus.QUEUED || it.status == QueueStatus.RUNNING }
        _state.update { it.copy(progressRows = rows, isConverting = active) }
        if (!active && rows.isNotEmpty() && wasConverting && !batchSummarised) {
            batchSummarised = true
            val done = rows.count { it.status == QueueStatus.DONE }
            val failed = rows.count { it.status == QueueStatus.FAILED }
            val cancelled = rows.any { it.status == QueueStatus.CANCELLED }
            val message = when {
                cancelled -> context.getString(R.string.workbench_cancelled)
                failed == 0 -> context.getString(R.string.workbench_all_done)
                else -> context.getString(R.string.workbench_conversion_summary, done, failed)
            }
            _messages.tryEmit(message)
        }
    }

    /**
     * Recomputes the intersection of every selected file's reachable targets and keeps the
     * current target only while it stays valid. Options that no longer apply are reset.
     */
    private fun withDerived(state: WorkbenchUiState): WorkbenchUiState {
        val targets = computeTargets(state.files)
        val target = state.target?.takeIf { selected -> targets.any { it.id == selected.id } }
        val categories = (state.files.map { it.format.category } + listOfNotNull(target?.category)).toSet()
        val showQuality = FormatCategory.IMAGE in categories
        val showBitrate = FormatCategory.AUDIO in categories || FormatCategory.VIDEO in categories
        val showPages = state.files.any { it.format.id == "pdf" }
        val ocrInstalled = Repo.packs.isInstalled(ModelPacks.OCR_PACK_ID)
        val showOcrHint = !ocrInstalled && state.files.any { file ->
            file.format.category == FormatCategory.IMAGE || file.format.category == FormatCategory.PDF
        }
        val defaults = ConversionOptions()
        var options = state.options
        if (!showQuality && options.quality != defaults.quality) {
            options = options.copy(quality = defaults.quality)
        }
        if (!showBitrate && options.bitrateKbps != null) {
            options = options.copy(bitrateKbps = null)
        }
        if (!showPages && options.pages != null) {
            options = options.copy(pages = null)
        }
        return state.copy(
            targets = targets,
            target = target,
            options = options,
            showQuality = showQuality,
            showBitrate = showBitrate,
            showPages = showPages,
            showOcrHint = showOcrHint
        )
    }

    /**
     * Targets every selected file can reach. A target is hidden while its only route needs a
     * model pack that is not installed, so the user never starts a conversion that is guaranteed
     * to fail; the hint in [WorkbenchUiState.showOcrHint] explains where the targets went.
     */
    private fun computeTargets(files: List<ConversionInput>): List<FileFormat> {
        if (files.isEmpty()) return emptyList()
        val sourceIds = files.map { it.format.id }.toSet()
        val ocrInstalled = Repo.packs.isInstalled(ModelPacks.OCR_PACK_ID)
        return AppGraph.formats.all().filter { format ->
            format.id !in sourceIds && files.all { input ->
                val route = AppGraph.planner.plan(input.format, format, ConversionRunner.MAX_HOPS)
                route != null && (ocrInstalled || route.none { it.requiresModelPack })
            }
        }
    }
}
