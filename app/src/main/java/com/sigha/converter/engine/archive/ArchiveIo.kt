package com.sigha.converter.engine.archive

import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 * Copies content Uris into the app cache before an archive is opened, so the
 * converters can use seekable File APIs safely and always clean up afterwards.
 */
internal object ArchiveIo {

    private const val WORK_DIR_NAME = "sigha-work"
    private const val MAX_TEMP_AGE_MS = 6L * 60L * 60L * 1000L

    fun copyToTemp(request: ConversionRequest, suffix: String): File {
        val base = request.context.cacheDir ?: request.context.filesDir
        val dir = File(base, WORK_DIR_NAME)
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            throw ConversionException("Not enough storage space to start the conversion.")
        }
        pruneOldFiles(dir)
        val temp = File(dir, UUID.randomUUID().toString() + suffix)
        try {
            val stream = request.context.contentResolver.openInputStream(request.input)
                ?: throw ConversionException("Cannot read the selected file.")
            var total = 0L
            stream.use { input ->
                BufferedOutputStream(temp.outputStream(), ArchiveGuards.BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(ArchiveGuards.BUFFER_SIZE)
                    while (true) {
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        if (total > ArchiveGuards.MAX_TOTAL_BYTES) {
                            throw ConversionException("The file is larger than the 2 GB conversion limit.")
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            deleteQuietly(temp)
            if (error is IOException) throw ConversionException("Cannot copy the selected file.", error)
            throw error
        }
        return temp
    }

    fun deleteQuietly(file: File) {
        runCatching {
            if (file.exists()) file.delete()
        }
    }

    private fun pruneOldFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_TEMP_AGE_MS
        runCatching {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val stamp = file.lastModified()
                    if (stamp > 0L && stamp < cutoff) file.delete()
                }
            }
        }
    }
}
