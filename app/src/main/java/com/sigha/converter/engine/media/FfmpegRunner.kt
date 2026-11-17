package com.sigha.converter.engine.media

import android.app.Application
import android.media.MediaMetadataRetriever
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val MEDIA_DIRECTORY = "media"
private const val INPUT_PREFIX = "sigha_in_"
private const val COPY_BUFFER_BYTES = 64 * 1024
private const val CANCEL_POLL_MS = 250L
private const val STALE_TEMP_MS = 6L * 60L * 60L * 1000L
private const val MAX_LOG_LINES = 12
private const val DETAILS_LIMIT = 400
private const val CANCELLED_MESSAGE = "Conversion cancelled."

/**
 * Runs one FFmpegKit command against a private cache copy of the input Uri.
 *
 * FFmpegKit only understands file paths, so content Uris are streamed into the app cache first.
 * Progress, cancellation and user-readable failures are handled here; command construction stays
 * in the pure [FfmpegArgs].
 */
internal class FfmpegRunner(private val app: Application) {

    /** What the media container says about itself; null means "the retriever could not tell". */
    data class Probe(
        val durationMs: Long?,
        val hasAudio: Boolean?,
        val hasVideo: Boolean?,
        val width: Int?,
        val height: Int?
    )

    /** Private input copy plus its probe; the caller must not keep the file after [withMedia]. */
    data class PreparedInput(val file: File, val probe: Probe)

    /** Outcome of one FFmpeg execution. Failures are described by [failure]. */
    data class ExecResult(val success: Boolean, val cancelled: Boolean, val logTail: String)

    /**
     * Prepares the input copy, runs [block] and always cleans up the temporary file. The output
     * file is removed when the block does not complete, so a cancelled or failed conversion never
     * leaves a half-written result behind.
     */
    suspend fun withMedia(
        request: ConversionRequest,
        block: suspend (PreparedInput) -> ConversionResult
    ): ConversionResult {
        val prepared = prepare(request)
        var completed = false
        try {
            val result = block(prepared)
            completed = true
            return result
        } finally {
            prepared.file.delete()
            if (!completed) request.outputFile.delete()
        }
    }

    /** Runs [args] and suspends until FFmpegKit finishes, reporting progress and cancellation. */
    suspend fun execute(
        request: ConversionRequest,
        args: Array<String>,
        plannedDurationMs: Long?
    ): ExecResult = withContext(Dispatchers.IO) {
        if (request.isCancelled()) {
            return@withContext ExecResult(success = false, cancelled = true, logTail = "")
        }
        val completion = CompletableDeferred<FFmpegSession>()
        val logTail = LogTail()
        val progress = ProgressTracker(request.onProgress, plannedDurationMs)
        val session: FFmpegSession = try {
            FFmpegKit.executeWithArgumentsAsync(
                args,
                FFmpegSessionCompleteCallback { finished -> completion.complete(finished) },
                LogCallback { log -> logTail.add(log.message.orEmpty()) },
                StatisticsCallback { statistics -> progress.onStatistics(statistics) }
            )
        } catch (t: Throwable) {
            // Missing native libraries (unsupported ABI) fail while the FFmpegKit class loads.
            throw ConversionException("Media engine unavailable on this device.", t)
        }
        val watchdog = launch {
            while (isActive) {
                delay(CANCEL_POLL_MS)
                if (request.isCancelled()) {
                    runCatching { FFmpegKit.cancel(session.sessionId) }
                    break
                }
            }
        }
        var success = false
        try {
            val finished = completion.await()
            val result = ExecResult(
                success = ReturnCode.isSuccess(finished.returnCode),
                cancelled = ReturnCode.isCancel(finished.returnCode),
                logTail = logTail.text()
            )
            success = result.success
            result
        } catch (e: CancellationException) {
            runCatching { FFmpegKit.cancel(session.sessionId) }
            throw e
        } finally {
            watchdog.cancel()
            progress.finish(success)
        }
    }

    /**
     * Throws the right exception for a finished execution and verifies that the output file was
     * actually written. Call before returning a [ConversionResult].
     */
    fun finish(result: ExecResult, request: ConversionRequest, targetLabel: String) {
        if (result.cancelled || request.isCancelled()) throw CancellationException(CANCELLED_MESSAGE)
        if (!result.success) throw failure(result, request)
        val output = request.outputFile
        if (!output.exists() || output.length() == 0L) {
            throw ConversionException(
                "The conversion finished but no $targetLabel file was written. The input may be empty or damaged."
            )
        }
    }

    /** Builds a readable error: a human sentence first, then the tail of the FFmpeg log. */
    fun failure(result: ExecResult, request: ConversionRequest): ConversionException {
        val friendly = friendlyMessage(result.logTail, request)
        val details = result.logTail.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() && !line.startsWith("frame=") && !line.startsWith("size=") }
            .toList()
            .takeLast(3)
            .joinToString("\n")
            .take(DETAILS_LIMIT)
        return ConversionException(if (details.isBlank()) friendly else "$friendly\n\n$details")
    }

    private suspend fun prepare(request: ConversionRequest): PreparedInput = withContext(Dispatchers.IO) {
        if (request.isCancelled()) throw CancellationException(CANCELLED_MESSAGE)
        request.onProgress(0f)
        val directory = File(app.cacheDir, MEDIA_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw ConversionException("The app could not prepare temporary storage for this conversion.")
        }
        pruneStaleFiles(directory)
        val extension = request.from.extension
            .lowercase()
            .filter { character -> character.isLetterOrDigit() }
            .take(8)
            .ifBlank { "bin" }
        val inputFile = File.createTempFile(INPUT_PREFIX, ".$extension", directory)
        try {
            copyInput(request, inputFile)
        } catch (t: Throwable) {
            inputFile.delete()
            throw t
        }
        if (inputFile.length() == 0L) {
            inputFile.delete()
            throw ConversionException("The selected file is empty, so there is nothing to convert.")
        }
        if (request.isCancelled()) {
            inputFile.delete()
            throw CancellationException(CANCELLED_MESSAGE)
        }
        request.outputFile.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }
        request.outputFile.delete()
        PreparedInput(inputFile, probe(inputFile))
    }

    private fun copyInput(request: ConversionRequest, target: File) {
        val resolver = request.context.contentResolver
        val stream = try {
            resolver.openInputStream(request.input)
        } catch (e: SecurityException) {
            throw ConversionException("The app no longer has access to this file. Pick it again from storage.", e)
        } catch (e: Exception) {
            throw ConversionException("The selected file could not be opened.", e)
        } ?: throw ConversionException("The selected file could not be opened. It may have been moved or deleted.")
        try {
            stream.use { input ->
                FileOutputStream(target).buffered(COPY_BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        if (request.isCancelled()) throw CancellationException(CANCELLED_MESSAGE)
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: java.io.IOException) {
            throw ConversionException(
                "The selected file could not be read. It may be damaged or the storage is unavailable.",
                e
            )
        }
    }

    private fun probe(file: File): Probe {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.trim()?.toLongOrNull()?.takeIf { value -> value > 0L }
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                ?.equals("yes", ignoreCase = true)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                ?.equals("yes", ignoreCase = true)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.trim()?.toIntOrNull()?.takeIf { value -> value > 0 }
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.trim()?.toIntOrNull()?.takeIf { value -> value > 0 }
            Probe(duration, hasAudio, hasVideo, width, height)
        } catch (e: Exception) {
            Probe(null, null, null, null, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun pruneStaleFiles(directory: File) {
        val cutoff = System.currentTimeMillis() - STALE_TEMP_MS
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) runCatching { file.delete() }
        }
    }

    private fun friendlyMessage(log: String, request: ConversionRequest): String {
        val lower = log.lowercase()
        return when {
            "no space left" in lower ->
                "There is not enough free storage to finish this conversion."
            "invalid data found" in lower || "moov atom not found" in lower ||
                "could not find codec parameters" in lower || "error while decoding" in lower ->
                "This file is damaged or is not a valid ${request.from.label} file."
            "does not contain any stream" in lower || "matches no streams" in lower ->
                "This file has no track that could be converted to ${request.to.label}."
            "unknown encoder" in lower || "encoder not found" in lower ||
                "error while opening encoder" in lower || "could not open encoder" in lower ->
                "This device cannot encode ${request.to.label} from this file."
            "permission denied" in lower || "operation not permitted" in lower ->
                "The app could not read the selected file."
            else ->
                "The media conversion failed. The file may be damaged or use a codec this device does not support."
        }
    }

    /** Bounded tail of the FFmpeg log; progress lines are dropped so real errors survive. */
    private class LogTail {
        private val lines = ArrayDeque<String>()

        @Synchronized
        fun add(message: String) {
            val line = message.trim()
            if (line.isEmpty()) return
            if (line.startsWith("frame=") || line.startsWith("size=")) return
            if (lines.size >= MAX_LOG_LINES) lines.removeFirst()
            lines.addLast(line)
        }

        @Synchronized
        fun text(): String = lines.joinToString("\n")
    }

    /** Turns FFmpeg statistics into clamped 0..1 progress; unknown duration reports only 0/1. */
    private class ProgressTracker(
        private val onProgress: (Float) -> Unit,
        private val plannedDurationMs: Long?
    ) {
        private var lastPercent = -1

        @Volatile
        private var closed = false

        fun onStatistics(statistics: Statistics) {
            if (closed) return
            val totalMs = plannedDurationMs?.takeIf { value -> value > 0L } ?: return
            val progress = (statistics.time * 1000.0 / totalMs).coerceIn(0.0, 1.0).toFloat()
            val percent = (progress * 100f).toInt()
            if (percent > lastPercent) {
                lastPercent = percent
                onProgress(progress.coerceAtMost(0.99f))
            }
        }

        fun finish(success: Boolean) {
            closed = true
            if (success) onProgress(1f)
        }
    }
}
