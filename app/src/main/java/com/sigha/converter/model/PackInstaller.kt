package com.sigha.converter.model

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException

/**
 * Downloads and installs one pack. Callers run this on Dispatchers.IO.
 *
 * Guarantees:
 *  - one partial file per expected download, so a killed process resumes instead of restarting;
 *  - every byte is checked against the hard-coded SHA-256 before it is moved into place;
 *  - a corrupt file never replaces an installed one;
 *  - at most [MAX_ATTEMPTS] attempts per file, and a hash mismatch is final (no retry loop).
 */
internal class PackInstaller(private val context: Context) {

    fun install(pack: ModelPack, onProgress: (Float) -> Unit): File {
        val modelsRoot = PackLayout.modelsRoot(context)
        if (!modelsRoot.isDirectory && !modelsRoot.mkdirs()) {
            throw PackException("Could not prepare storage for the download.")
        }
        val missing = pack.files.filterNot { isValid(pack, it) }
        if (missing.isEmpty()) {
            onProgress(1f)
            return PackLayout.installDir(context, pack)
        }
        requireFreeSpace(pack, missing)
        val totalBytes = missing.sumOf { it.sizeBytes }
        var completedBytes = 0L
        var lastReported = -1f

        val report: (Float) -> Unit = { value ->
            val clamped = value.coerceIn(0f, 1f)
            if (clamped >= 1f || clamped - lastReported >= PROGRESS_STEP) {
                lastReported = clamped
                onProgress(clamped)
            }
        }
        report(0f)
        for (file in missing) {
            val part = PackLayout.partFile(context, pack, file)
            downloadOne(pack, file, part) { bytesForFile ->
                val done = completedBytes + bytesForFile
                report(if (totalBytes > 0L) (done.toDouble() / totalBytes).toFloat() else 0f)
            }
            completedBytes += file.sizeBytes
        }
        report(1f)
        return PackLayout.installDir(context, pack)
    }

    private fun downloadOne(
        pack: ModelPack,
        file: PackFile,
        part: File,
        onBytes: (Long) -> Unit
    ) {
        var lastError: IOException? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            var resumeAt = resumeOffset(file, part)
            if (resumeAt == file.sizeBytes) {
                // The partial file already holds every byte (killed right after the last write,
                // or a crash during install). Installing it is cheaper and safer than asking the
                // server for "bytes=N-", which answers 416 and used to surface as a generic
                // download failure. A failed hash check restarts from zero.
                val complete = runCatching {
                    verify(part, file)
                    installFile(pack, file, part)
                    true
                }.getOrDefault(false)
                if (complete) return
                deleteQuietly(part)
                resumeAt = 0L
            }
            val connection = try {
                open(file.url, resumeAt)
            } catch (error: IOException) {
                lastError = error
                if (attempt < MAX_ATTEMPTS) sleep(attempt)
                continue
            }
            try {
                streamTo(file, part, connection, resumeAt, onBytes)
                verify(part, file)
                installFile(pack, file, part)
                return
            } catch (cancelled: CancellationException) {
                // Keep the partial file: the next run resumes from it.
                throw cancelled
            } catch (tampered: PackException) {
                deleteQuietly(part)
                throw tampered
            } catch (error: IOException) {
                lastError = error
                if (attempt < MAX_ATTEMPTS) sleep(attempt)
            } finally {
                connection.disconnect()
            }
        }
        throw PackException("Download failed. Check your connection and try again.", lastError)
    }

    /** Bytes already on disk for [file]; a longer-than-expected part is discarded. */
    private fun resumeOffset(file: PackFile, part: File): Long {
        if (!part.isFile) return 0L
        val length = part.length()
        if (length <= 0L) {
            deleteQuietly(part)
            return 0L
        }
        if (length == file.sizeBytes) return length
        if (length > file.sizeBytes) {
            deleteQuietly(part)
            return 0L
        }
        return length
    }

    private fun open(url: String, rangeStart: Long): HttpURLConnection {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            val connection = URL(current).openConnection() as? HttpURLConnection
                ?: throw IOException("Unsupported download address.")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Encoding", "identity")
            if (rangeStart > 0L) {
                connection.setRequestProperty("Range", "bytes=" + rangeStart + "-")
            }
            val code = connection.responseCode
            if (code in 200..299) return connection
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("The download server sent an empty redirect.")
                }
                current = URL(URL(current), location).toString()
            } else {
                connection.disconnect()
                // 4xx means this URL will never serve the file: retrying it only delays the
                // honest error. 408 and 429 are explicitly transient and stay retryable.
                val transient = code == 408 || code == 429 || code == 416
                val message = "The download server answered with HTTP " + code + "."
                if (!transient && code in 400..499) {
                    throw PackException(message)
                }
                throw IOException(message)
            }
        }
        throw IOException("Too many redirects.")
    }

    private fun streamTo(
        file: PackFile,
        part: File,
        connection: HttpURLConnection,
        requestedStart: Long,
        onBytes: (Long) -> Unit
    ) {
        // A server that ignores Range answers 200; restart the file from zero then.
        val resumed = requestedStart > 0L && connection.responseCode == HTTP_PARTIAL_CONTENT
        if (!resumed) deleteQuietly(part)
        val start = if (resumed) requestedStart else 0L
        val announced = connection.contentLengthLong
        val expectedTotal = if (announced > 0L) start + announced else -1L
        if (expectedTotal > 0L && expectedTotal != file.sizeBytes) {
            throw IOException("The server announced an unexpected file size.")
        }
        var readTotal = start
        connection.inputStream.use { input ->
            FileOutputStream(part, resumed).buffered(BUFFER_SIZE).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    readTotal += read
                    if (readTotal > file.sizeBytes) {
                        throw IOException("The downloaded file is larger than expected.")
                    }
                    onBytes(readTotal)
                }
            }
        }
        if (readTotal != file.sizeBytes) throw IOException("The download stopped early.")
        onBytes(readTotal)
    }

    private fun verify(part: File, file: PackFile) {
        if (!part.isFile || part.length() != file.sizeBytes) {
            throw IOException("The download stopped early.")
        }
        if (!Digest.sha256(part).equals(file.sha256, ignoreCase = true)) {
            throw PackException("The downloaded file failed its security check. Please try again.")
        }
    }

    private fun installFile(pack: ModelPack, file: PackFile, part: File) {
        val target = PackLayout.targetFile(context, pack, file)
        val parent = target.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Could not create the pack folder.")
        }
        if (target.exists() && !target.delete()) {
            throw IOException("Could not replace the old pack file.")
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            deleteQuietly(part)
        }
        if (!target.isFile || target.length() != file.sizeBytes) {
            throw IOException("Could not install the downloaded file.")
        }
    }

    private fun isValid(pack: ModelPack, file: PackFile): Boolean {
        val target = PackLayout.targetFile(context, pack, file)
        if (!target.isFile || target.length() != file.sizeBytes) return false
        return try {
            Digest.sha256(target).equals(file.sha256, ignoreCase = true)
        } catch (_: IOException) {
            false
        }
    }

    private fun requireFreeSpace(pack: ModelPack, missing: List<PackFile>) {
        val remaining = missing.sumOf { file ->
            val part = PackLayout.partFile(context, pack, file)
            (file.sizeBytes - (if (part.isFile) part.length() else 0L)).coerceAtLeast(0L)
        }
        val needed = remaining + FREE_SPACE_MARGIN
        val available = PackLayout.modelsRoot(context).usableSpace
        if (available > 0L && available < needed) {
            val megabytes = ((needed + MEGABYTE - 1) / MEGABYTE).coerceAtLeast(1)
            throw PackException(
                "Not enough free space. This pack needs about " + megabytes + " MB free."
            )
        }
    }

    private fun sleep(attempt: Int) {
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Download interrupted")
        }
    }

    private fun deleteQuietly(file: File) {
        runCatching { if (file.exists()) file.delete() }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 30_000
        const val RETRY_DELAY_MS = 1_500L
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_STEP = 0.005f
        const val MEGABYTE = 1024L * 1024L
        const val FREE_SPACE_MARGIN = 4L * MEGABYTE
        const val HTTP_PARTIAL_CONTENT = 206
        const val USER_AGENT = "Sigha/1.0 (Android) model pack downloader"
    }
}
