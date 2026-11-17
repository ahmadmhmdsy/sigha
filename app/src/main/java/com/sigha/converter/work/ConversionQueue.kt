package com.sigha.converter.work

import android.content.Context
import android.net.Uri
import com.sigha.converter.R
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.ConversionOptions
import com.sigha.converter.core.FileFormat
import com.sigha.converter.data.AppScope
import com.sigha.converter.data.AppSettings
import com.sigha.converter.data.ConversionInput
import com.sigha.converter.data.ConversionRunner
import com.sigha.converter.data.HistoryEntry
import com.sigha.converter.data.HistoryStore
import com.sigha.converter.data.OutputMode
import com.sigha.converter.data.PublishTarget
import com.sigha.converter.data.SettingsStore
import com.sigha.converter.data.releasePersistableReadPermission
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class QueueStatus { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

/** One row of the conversion queue. */
data class QueueEntry(
    val id: String,
    val displayName: String,
    val fromId: String,
    val toId: String,
    val status: QueueStatus = QueueStatus.QUEUED,
    val progress: Float = 0f,
    val outputName: String? = null,
    val outputUri: Uri? = null,
    val displayPath: String? = null,
    /** English diagnostic; shown only as expandable details. */
    val errorMessage: String? = null,
    /** Drives the localized failure headline. */
    val errorKind: ConversionFailureKind = ConversionFailureKind.UNKNOWN,
    /** Engine note (page cap, OCR fallback, truncation, ...). */
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

/**
 * App-scoped conversion queue. Lives outside the ViewModel so a conversion keeps running while
 * the user browses History or Settings, and so the Queue screen can show live progress.
 *
 * A batch runs one file at a time: media engines are CPU/GPU heavy and sequential work keeps
 * the phone responsive and thermal output predictable. A failed item is recorded and the batch
 * continues; cancellation stops the batch and marks unfinished rows as cancelled.
 *
 * While a batch runs the queue holds a data-sync foreground service with a progress
 * notification, so Android does not freeze the work when the app goes to the background. A
 * durable "batch active" flag detects a process kill: the next start shows an interrupted
 * banner instead of silently losing the batch.
 */
class ConversionQueue(
    private val context: Context,
    private val runner: ConversionRunner,
    private val historyStore: HistoryStore,
    private val settingsStore: SettingsStore
) {

    private val _entries = MutableStateFlow<List<QueueEntry>>(emptyList())
    val entries: StateFlow<List<QueueEntry>> = _entries.asStateFlow()

    private var activeJob: Job? = null

    val isRunning: Boolean get() = activeJob?.isActive == true

    /**
     * Enqueues a batch. Returns the ids of the created rows, or an empty list when a batch is
     * already running or there is nothing to do.
     */
    fun start(inputs: List<ConversionInput>, target: FileFormat, options: ConversionOptions): List<String> {
        if (isRunning || inputs.isEmpty()) return emptyList()
        val batch = inputs.map { input ->
            QueueEntry(
                id = UUID.randomUUID().toString(),
                displayName = input.displayName,
                fromId = input.format.id,
                toId = target.id
            )
        }
        _entries.update { current -> (batch + current).take(MAX_ENTRIES) }
        activeJob = AppScope.launch {
            try {
                ConversionService.start(context)
                settingsStore.markBatchActive(true)
                ConversionService.update(
                    context,
                    context.getString(R.string.ui_service_preparing),
                    null
                )
                runBatch(inputs, target, options, batch)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                markUnfinished(
                    status = QueueStatus.FAILED,
                    message = context.getString(R.string.err_conversion_failed, target.label),
                    kind = ConversionFailureKind.UNKNOWN
                )
            } finally {
                ConversionService.stop(context)
                withContext(NonCancellable) { settingsStore.markBatchActive(false) }
                inputs.forEach { input -> releasePersistableReadPermission(context, input.uri) }
                val job = coroutineContext[Job]
                if (job?.isActive == false) {
                    markUnfinished(status = QueueStatus.CANCELLED, message = null)
                }
            }
        }
        return batch.map { it.id }
    }

    /** Stops the running batch. Unfinished rows become CANCELLED immediately for UI feedback. */
    fun cancelAll() {
        activeJob?.cancel()
        markUnfinished(status = QueueStatus.CANCELLED, message = null)
    }

    /** Removes finished rows from the queue list. Active rows stay. */
    fun clearFinished() {
        _entries.update { current ->
            current.filter { it.status == QueueStatus.QUEUED || it.status == QueueStatus.RUNNING }
        }
    }

    private suspend fun runBatch(
        inputs: List<ConversionInput>,
        target: FileFormat,
        options: ConversionOptions,
        batch: List<QueueEntry>
    ) {
        val settings: AppSettings = settingsStore.settings.value
        val publishTarget = resolvePublishTarget(settings)
        val effectiveOptions = options.copy(
            ocrLanguage = options.ocrLanguage ?: settings.ocrLanguage
        )
        for ((index, entry) in batch.withIndex()) {
            coroutineContext[Job]?.let { job -> if (!job.isActive) return }
            val input = inputs[index]
            update(entry.id) { current -> current.copy(status = QueueStatus.RUNNING, progress = 0f) }
            ConversionService.update(
                context,
                context.getString(R.string.ui_service_converting, entry.displayName),
                0f
            )
            val result = runner.convert(
                input = input,
                target = target,
                options = effectiveOptions,
                publishTarget = publishTarget,
                onProgress = { progress ->
                    update(entry.id) { current ->
                        if (current.status == QueueStatus.RUNNING) {
                            current.copy(progress = progress.coerceIn(0f, 1f))
                        } else {
                            current
                        }
                    }
                    ConversionService.update(
                        context,
                        context.getString(R.string.ui_service_converting, entry.displayName),
                        progress
                    )
                }
            )
            coroutineContext[Job]?.let { job -> if (!job.isActive) return }

            if (result.success && result.outputs.isNotEmpty()) {
                val primary = result.outputs.first()
                update(entry.id) { current ->
                    if (current.status == QueueStatus.CANCELLED) {
                        current
                    } else {
                        current.copy(
                            status = QueueStatus.DONE,
                            progress = 1f,
                            outputName = primary.fileName,
                            outputUri = primary.uri,
                            displayPath = primary.displayPath,
                            note = result.note,
                            finishedAt = System.currentTimeMillis()
                        )
                    }
                }
                result.outputs.forEach { output ->
                    historyStore.record(
                        HistoryEntry(
                            inputName = input.displayName,
                            outputName = output.fileName,
                            fromId = input.format.id,
                            toId = target.id,
                            outputUri = output.uri.toString(),
                            outputPath = output.displayPath,
                            sizeBytes = output.sizeBytes,
                            timestamp = System.currentTimeMillis(),
                            success = true,
                            note = result.note
                        )
                    )
                }
            } else {
                val message = result.errorMessage
                    ?: context.getString(R.string.err_conversion_failed, target.label)
                update(entry.id) { current ->
                    if (current.status == QueueStatus.CANCELLED) {
                        current
                    } else {
                        current.copy(
                            status = QueueStatus.FAILED,
                            progress = 0f,
                            errorMessage = message,
                            errorKind = result.errorKind,
                            finishedAt = System.currentTimeMillis()
                        )
                    }
                }
                historyStore.record(
                    HistoryEntry(
                        inputName = input.displayName,
                        outputName = ConversionRunner.outputName(input.displayName, target.extension, 0),
                        fromId = input.format.id,
                        toId = target.id,
                        outputUri = "",
                        outputPath = null,
                        sizeBytes = 0L,
                        timestamp = System.currentTimeMillis(),
                        success = false,
                        error = message,
                        errorKind = result.errorKind.name
                    )
                )
            }
        }
    }

    private fun resolvePublishTarget(settings: AppSettings): PublishTarget {
        if (settings.outputMode != OutputMode.FOLDER) return PublishTarget.Downloads
        val folder = settings.outputFolderUri
            ?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
            ?: return PublishTarget.Downloads
        return PublishTarget.Folder(folder)
    }

    private fun update(id: String, transform: (QueueEntry) -> QueueEntry) {
        _entries.update { current -> current.map { entry -> if (entry.id == id) transform(entry) else entry } }
    }

    private fun markUnfinished(
        status: QueueStatus,
        message: String?,
        kind: ConversionFailureKind = ConversionFailureKind.UNKNOWN
    ) {
        _entries.update { current ->
            current.map { entry ->
                if (entry.status == QueueStatus.QUEUED || entry.status == QueueStatus.RUNNING) {
                    entry.copy(
                        status = status,
                        errorMessage = message ?: entry.errorMessage,
                        errorKind = if (message != null) kind else entry.errorKind,
                        finishedAt = System.currentTimeMillis()
                    )
                } else {
                    entry
                }
            }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 60
    }
}
