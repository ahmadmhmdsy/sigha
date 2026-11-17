package com.sigha.converter.data

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.sigha.converter.AppGraph
import com.sigha.converter.R
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionOptions
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.FileFormat
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
                input, target, startedAt, context.getString(R.string.err_target_same)
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
                    input, target, startedAt, context.getString(R.string.err_no_route)
                )
            var currentUri: Uri = input.uri
            var currentFormat: FileFormat = input.format
            var finalFiles: List<File> = emptyList()

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
                    ?: throw ConversionException(context.getString(R.string.err_no_route))
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
                    converter.convert(request).outputs.filter { it.isFile && it.length() > 0L }
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
                    throw ConversionException(context.getString(R.string.err_empty_output))
                }
                finalFiles = outputs
                currentUri = Uri.fromFile(outputs.first())
                currentFormat = stepTarget
                onProgress(((index + 1f) / route.size).coerceIn(0f, 1f))
            }

            coroutineContext.ensureActive()
            val published = finalFiles.mapIndexed { index, file ->
                publisher.publish(
                    source = file,
                    fileName = outputName(input.displayName, target.extension, index),
                    mime = target.mime.ifBlank { DEFAULT_MIME },
                    target = publishTarget
                )
            }
            onProgress(1f)
            ConversionItemResult(
                input = input,
                target = target,
                success = true,
                outputs = published,
                errorMessage = null,
                durationMs = SystemClock.elapsedRealtime() - startedAt
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: ConversionException) {
            failed(
                input,
                target,
                startedAt,
                error.message ?: context.getString(R.string.err_conversion_failed, target.label)
            )
        } catch (error: Exception) {
            failed(
                input,
                target,
                startedAt,
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
        message: String
    ): ConversionItemResult = ConversionItemResult(
        input = input,
        target = target,
        success = false,
        outputs = emptyList(),
        errorMessage = message,
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
