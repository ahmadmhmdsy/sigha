package com.sigha.converter.data

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.ConversionOptions
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.core.destinationFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Runs one conversion end to end: asks the route planner for the shortest converter chain,
 * feeds each step's output into the next, then publishes the final files. Every intermediate
 * file lives in cacheDir and is removed afterwards, whatever the outcome.
 *
 * One item never throws for conversion failures - it returns a failed [ConversionItemResult]
 * so the queue can keep going with the remaining files. Coroutine cancellation is honoured.
 *
 * The result carries the engine's typed informational [Note] and a language-independent
 * failure kind, so the UI can show localized text and keep the English diagnostic as details.
 */
class ConversionRunner(
    private val context: Context,
    private val publisher: OutputPublisher
) {

    suspend fun convert(
        input: ConversionInput,
        target: FileFormat,
        options: ConversionOptions,
        publishTarget: PublishTarget,
        onProgress: (Float) -> Unit = {}
    ): ConversionItemResult = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()
        if (input.format.id == target.id) {
            return@withContext failed(
                input, target, startedAt,
                ConversionFailureKind.TARGET_SAME,
                context.getString(R.string.err_target_same)
            )
        }
        val job = coroutineContext[Job]
        val isCancelled = { job?.isCancelled == true }
        pruneStaleWorkDirs()
        val workDir = File(context.cacheDir, "conversions/" + UUID.randomUUID().toString())
        workDir.mkdirs()
        try {
            val route = AppGraph.planner.plan(input.format, target, MAX_HOPS)
                ?: return@withContext failed(
                    input, target, startedAt,
                    ConversionFailureKind.NO_ROUTE,
                    context.getString(R.string.err_no_route)
                )
            var currentUri: Uri = input.uri
            var currentFormat: FileFormat = input.format
            var finalFiles: List<File> = emptyList()
            val notes = ArrayList<Note>()

            for ((index, converter) in route.withIndex()) {
                coroutineContext.ensureActive()
                // One converter instance declares many outputs (jpg->png, jpg->webp, ...), so
                // pick the output that is actually on the planned route: the final target when
                // this is the last step, otherwise an output that can still reach it.
                val candidates = AppGraph.registry.edgesFrom(currentFormat)
                    .filter { candidate -> candidate.converter.id == converter.id }
                    .map { candidate -> candidate.target }
                val stepTarget = candidates.firstOrNull { candidate -> candidate.id == target.id }
                    ?: candidates.firstOrNull { candidate ->
                        AppGraph.planner.plan(candidate, target, MAX_HOPS - (index + 1)) != null
                    }
                    ?: throw ConversionException(
                        context.getString(R.string.err_no_route),
                        kind = ConversionFailureKind.NO_ROUTE
                    )
                val stepFile = File(workDir, "step-" + (index + 1) + "." + stepTarget.extension)
                val request = ConversionRequest(
                    context = context,
                    input = currentUri,
                    from = currentFormat,
                    to = stepTarget,
                    outputFile = stepFile,
                    options = options,
                    onProgress = { stepProgress ->
                        val withinStep = stepProgress.coerceIn(0f, 1f)
                        onProgress(((index + withinStep) / route.size).coerceIn(0f, 1f))
                    },
                    isCancelled = isCancelled
                )
                val produced = try {
                    val result = converter.convert(request)
                    result.note?.let { note -> notes += note }
                    result.outputs.filter { it.isFile && it.length() > 0L }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: ConversionException) {
                    throw error
                } catch (error: Exception) {
                    throw ConversionException(
                        error.message ?: context.getString(R.string.err_conversion_failed, stepTarget.label),
                        error
                    )
                }
                val outputs = produced.ifEmpty {
                    listOfNotNull(stepFile.takeIf { it.isFile && it.length() > 0L })
                }
                if (outputs.isEmpty()) {
                    throw ConversionException(
                        context.getString(R.string.err_empty_output),
                        kind = ConversionFailureKind.EMPTY_OUTPUT
                    )
                }
                finalFiles = outputs
                currentUri = Uri.fromFile(outputs.first())
                currentFormat = stepTarget
                onProgress(((index + 1f) / route.size).coerceIn(0f, 1f))
            }

            coroutineContext.ensureActive()
            // A saved folder can become unavailable (uninstalled SD card, revoked grant).
            // Fall back to Downloads instead of failing a conversion the user already waited for.
            val effectiveTarget = when (val target0 = publishTarget) {
                is PublishTarget.Folder -> {
                    if (publisher.hasWriteAccess(target0)) {
                        target0
                    } else {
                        notes += Note.FolderFallback
                        PublishTarget.Downloads
                    }
                }
                PublishTarget.Downloads -> publishTarget
            }
            // Save each produced file on its own: one failed page must not throw away the pages
            // that were saved, and the user must be told how many made it. The engine already
            // names multi-page outputs ("report-page3.png"), so keep that name instead of
            // flattening them into "report (1).png" and losing the page number.
            val multipleOutputs = finalFiles.size > 1
            val published = ArrayList<PublishedOutput>(finalFiles.size)
            var publishFailures = 0
            var firstPublishError: Throwable? = null
            finalFiles.forEach { file ->
                coroutineContext.ensureActive()
                val name = if (multipleOutputs) file.name else
                    outputName(input.displayName, target.extension, 0)
                runCatching {
                    publisher.publish(
                        source = file,
                        fileName = name,
                        mime = target.mime.ifBlank { DEFAULT_MIME },
                        target = effectiveTarget,
                        destination = destinationFor(target.category, target.mime)
                    )
                }.onSuccess { output -> published += output }.onFailure { error ->
                    publishFailures++
                    if (firstPublishError == null) firstPublishError = error
                }
            }
            if (published.isEmpty()) {
                val error = firstPublishError
                throw if (error is ConversionException) error else ConversionException(
                    context.getString(R.string.err_save_failed),
                    error,
                    ConversionFailureKind.SAVE_FAILED
                )
            }
            if (publishFailures > 0) notes += Note.PartialPublish(publishFailures)
            onProgress(1f)
            ConversionItemResult(
                input = input,
                target = target,
                success = true,
                outputs = published,
                errorMessage = null,
                note = Note.combine(notes),
                durationMs = SystemClock.elapsedRealtime() - startedAt
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            // An OOM inside an engine is a failed item, not a dead app: report it and let the
            // queue continue with the remaining files.
            failed(
                input,
                target,
                startedAt,
                ConversionFailureKind.FILE_TOO_LARGE,
                context.getString(R.string.ui_out_of_memory)
            )
        } catch (error: ConversionException) {
            failed(
                input,
                target,
                startedAt,
                error.kind,
                error.message ?: context.getString(R.string.err_conversion_failed, target.label)
            )
        } catch (error: Exception) {
            failed(
                input,
                target,
                startedAt,
                ConversionFailureKind.UNKNOWN,
                context.getString(R.string.err_conversion_failed, target.label)
            )
        } catch (error: Throwable) {
            // A non-OOM Error is still one failed item, not a dead app: StackOverflowError from
            // hostile nested input, UnsatisfiedLinkError/ExceptionInInitializerError when a
            // native engine cannot load, NoClassDefFoundError after a bad update. Letting it
            // escape would kill the process and leave the queue without its cleanup.
            failed(
                input,
                target,
                startedAt,
                ConversionFailureKind.UNKNOWN,
                context.getString(R.string.err_conversion_failed, target.label)
            )
        } finally {
            runCatching { workDir.deleteRecursively() }
        }
    }

    private fun failed(
        input: ConversionInput,
        target: FileFormat,
        startedAt: Long,
        kind: ConversionFailureKind,
        message: String
    ): ConversionItemResult = ConversionItemResult(
        input = input,
        target = target,
        success = false,
        outputs = emptyList(),
        errorMessage = message,
        errorKind = kind,
        durationMs = SystemClock.elapsedRealtime() - startedAt
    )

    /** Removes scratch directories left behind by an earlier process kill. */
    private fun pruneStaleWorkDirs() {
        val root = File(context.cacheDir, "conversions")
        val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        runCatching {
            root.listFiles()?.forEach { directory ->
                if (directory.isDirectory && directory.lastModified() < cutoff) {
                    directory.deleteRecursively()
                }
            }
        }
    }

    companion object {
        /** Kept in sync with the UI, which uses the same hop limit for its target picker. */
        const val MAX_HOPS = 3
        private const val DEFAULT_MIME = "application/octet-stream"
        private const val CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1000L
        private const val MAX_BASE_LENGTH = 80
        private val ILLEGAL_NAME_CHARS = setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')

        /** "holiday.jpg" -> "holiday.png"; extra outputs in one run get " (1)", " (2)". */
        fun outputName(inputName: String, extension: String, index: Int): String {
            val trimmed = inputName.substringAfterLast('/').substringAfterLast('\\').trim()
            val rawBase = if (trimmed.contains('.')) trimmed.substringBeforeLast('.') else trimmed
            val cleaned = buildString {
                for (character in rawBase) {
                    append(if (character in ILLEGAL_NAME_CHARS || character.code < 0x20) '_' else character)
                }
            }.trim().take(MAX_BASE_LENGTH).trim()
            val base = cleaned.ifBlank { "converted" }
            val suffix = if (index == 0) "" else " (" + index + ")"
            return if (extension.isBlank()) base + suffix else base + suffix + "." + extension
        }
    }
}
