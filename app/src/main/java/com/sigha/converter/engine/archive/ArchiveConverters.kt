package com.sigha.converter.engine.archive

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.engine.text.EngineFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Archive converters built on commons-compress.
 *
 * zip -> gz produces a real gzip-compressed tar archive (the app has a "gz"
 * format but no separate ".tar.gz" entry, and the note says so).
 * gz -> zip and 7z -> zip assume the gzip payload is a tar archive; RAR is not
 * supported because no decoder is available offline.
 */
fun archiveConverters(app: Application): List<Converter> = listOf(
    ZipToTarGz(),
    TarGzToZip(),
    SevenZipToZip()
)

private const val DIR_MODE = 0x1ED // 0755
private const val FILE_MODE = 0x1A4 // 0644

private class ZipToTarGz : Converter {
    override val id: String = "archive-zip-to-targz"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("zip"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("gz"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val temp = ArchiveIo.copyToTemp(request, ".zip")
            try {
                request.onProgress(0.05f)
                ZipFile(temp).use { zip ->
                    val entries = zip.entries().toList()
                    checkZipEntries(entries)
                    writeTarGz(zip, entries, request)
                }
                request.onProgress(1f)
                ConversionResult(
                    listOf(request.outputFile),
                    Note.TarGzOutput
                )
            } catch (error: IOException) {
                throw ConversionException("The ZIP archive could not be converted.", error)
            } finally {
                ArchiveIo.deleteQuietly(temp)
            }
        }

    private fun checkZipEntries(entries: List<ZipEntry>) {
        if (entries.size > ArchiveGuards.MAX_ENTRIES) {
            throw ConversionException("The archive has too many entries (limit 50000).")
        }
        var total = 0L
        for (entry in entries) {
            if (ArchiveGuards.normalizeEntryName(entry.name) == null) {
                throw ConversionException("The archive contains an unsafe entry path: " + entry.name)
            }
            if (!entry.isDirectory) {
                val size = entry.size
                if (size > 0L) total += size
                if (total > ArchiveGuards.MAX_TOTAL_BYTES) {
                    throw ConversionException("The archive expands beyond the 2 GB limit.")
                }
            }
        }
    }

    private fun writeTarGz(zip: ZipFile, entries: List<ZipEntry>, request: ConversionRequest) {
        request.outputFile.parentFile?.mkdirs()
        BufferedOutputStream(FileOutputStream(request.outputFile), ArchiveGuards.BUFFER_SIZE).use { fileOut ->
            GzipCompressorOutputStream(fileOut).use { gzip ->
                TarArchiveOutputStream(gzip).use { tar ->
                    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                    val buffer = ByteArray(ArchiveGuards.BUFFER_SIZE)
                    var index = 0
                    var writtenTotal = 0L
                    for (entry in entries) {
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        val safe = ArchiveGuards.normalizeEntryName(entry.name)
                            ?: throw ConversionException("The archive contains an unsafe entry path: " + entry.name)
                        if (entry.isDirectory) {
                            val dir = TarArchiveEntry(if (safe.endsWith("/")) safe else safe + "/")
                            dir.mode = DIR_MODE
                            tar.putArchiveEntry(dir)
                            tar.closeArchiveEntry()
                        } else if (entry.size >= 0L) {
                            val size = entry.size
                            val archiveEntry = TarArchiveEntry(safe)
                            archiveEntry.size = size
                            archiveEntry.mode = FILE_MODE
                            if (entry.time > 0L) archiveEntry.setModTime(Date(entry.time))
                            tar.putArchiveEntry(archiveEntry)
                            var written = 0L
                            zip.getInputStream(entry).use { input ->
                                while (written < size) {
                                    if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                                    val want = minOf(buffer.size.toLong(), size - written).toInt()
                                    val read = input.read(buffer, 0, want)
                                    if (read < 0) break
                                    tar.write(buffer, 0, read)
                                    written += read
                                }
                            }
                            if (written != size) {
                                throw ConversionException("The archive contains an entry with an inconsistent size: " + safe)
                            }
                            tar.closeArchiveEntry()
                            writtenTotal += size
                            if (writtenTotal > ArchiveGuards.MAX_TOTAL_BYTES) {
                                throw ConversionException("The archive expands beyond the 2 GB limit.")
                            }
                        } else {
                            // Unknown sizes are only known after streaming; count them against the
                            // same budget so a zip bomb cannot bypass the limit.
                            writtenTotal += writeUnknownSizeEntry(zip, entry, safe, tar, request, buffer)
                            if (writtenTotal > ArchiveGuards.MAX_TOTAL_BYTES) {
                                throw ConversionException("The archive expands beyond the 2 GB limit.")
                            }
                        }
                        index++
                        request.onProgress(0.05f + 0.95f * index / entries.size.coerceAtLeast(1))
                    }
                }
            }
        }
    }

    /** Tar needs the size before the data, so unknown-size entries spill to a temp file. */
    private fun writeUnknownSizeEntry(
        zip: ZipFile,
        entry: ZipEntry,
        safe: String,
        tar: TarArchiveOutputStream,
        request: ConversionRequest,
        buffer: ByteArray
    ): Long {
        val spill = File.createTempFile("sigha-entry", ".tmp", request.context.cacheDir)
        try {
            val size = spill.outputStream().buffered(ArchiveGuards.BUFFER_SIZE).use { output ->
                zip.getInputStream(entry).use { input ->
                    copyFully(input, output, request, buffer)
                }
            }
            val archiveEntry = TarArchiveEntry(safe)
            archiveEntry.size = size
            archiveEntry.mode = FILE_MODE
            if (entry.time > 0L) archiveEntry.setModTime(Date(entry.time))
            tar.putArchiveEntry(archiveEntry)
            spill.inputStream().buffered(ArchiveGuards.BUFFER_SIZE).use { input ->
                copyFully(input, tar, request, buffer)
            }
            tar.closeArchiveEntry()
            return size
        } finally {
            ArchiveIo.deleteQuietly(spill)
        }
    }
}

private class TarGzToZip : Converter {
    override val id: String = "archive-targz-to-zip"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("gz"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("zip"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val temp = ArchiveIo.copyToTemp(request, ".tar.gz")
            try {
                request.outputFile.parentFile?.mkdirs()
                val stats: TarStats
                try {
                    BufferedOutputStream(FileOutputStream(request.outputFile), ArchiveGuards.BUFFER_SIZE).use { fileOut ->
                        ZipOutputStream(fileOut).use { zip ->
                            FileInputStream(temp).use { fileIn ->
                                BufferedInputStream(fileIn, ArchiveGuards.BUFFER_SIZE).use { buffered ->
                                    GzipCompressorInputStream(buffered).use { gzip ->
                                        TarArchiveInputStream(gzip).use { tar ->
                                            stats = readTar(tar, zip, request, temp, gzip)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (error: IOException) {
                    throw ConversionException(
                        "The .gz file does not contain a readable tar archive.",
                        error
                    )
                }
                request.onProgress(1f)
                ConversionResult(listOf(request.outputFile), stats.toNote())
            } finally {
                ArchiveIo.deleteQuietly(temp)
            }
        }

    private fun readTar(
        tar: TarArchiveInputStream,
        zip: ZipOutputStream,
        request: ConversionRequest,
        temp: File,
        gzip: GzipCompressorInputStream
    ): TarStats {
        val buffer = ByteArray(ArchiveGuards.BUFFER_SIZE)
        val seen = HashSet<String>()
        var count = 0
        var duplicates = 0
        var links = 0
        var total = 0L
        val expected = temp.length().coerceAtLeast(1L)
        var entry: TarArchiveEntry? = tar.nextEntry
        while (entry != null) {
            if (request.isCancelled()) throw CancellationException("Conversion cancelled")
            count++
            if (count > ArchiveGuards.MAX_ENTRIES) {
                throw ConversionException("The archive has too many entries (limit 50000).")
            }
            val name = entry.name ?: ""
            val safe = ArchiveGuards.normalizeEntryName(name)
            if (safe == null) {
                if (entry.isDirectory) {
                    entry = tar.nextEntry
                    continue
                }
                throw ConversionException("The archive contains an unsafe entry path: " + name)
            }
            if (entry.isDirectory) {
                if (seen.add(safe)) {
                    val zipEntry = ZipEntry(safe + "/")
                    if (entry.modTime != null) zipEntry.time = entry.modTime.time
                    zip.putNextEntry(zipEntry)
                    zip.closeEntry()
                } else {
                    duplicates++
                }
            } else if (entry.isFile) {
                if (!seen.add(safe)) {
                    duplicates++
                } else {
                    val zipEntry = ZipEntry(safe)
                    if (entry.modTime != null) zipEntry.time = entry.modTime.time
                    zip.putNextEntry(zipEntry)
                    while (true) {
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        val read = tar.read(buffer, 0, buffer.size)
                        if (read < 0) break
                        zip.write(buffer, 0, read)
                        total += read
                        if (total > ArchiveGuards.MAX_TOTAL_BYTES) {
                            throw ConversionException("The archive expands beyond the 2 GB limit.")
                        }
                    }
                    zip.closeEntry()
                }
            } else {
                links++
            }
            if (count % 16 == 0) {
                val fraction = gzip.compressedCount.toDouble() / expected
                request.onProgress(fraction.toFloat().coerceIn(0.05f, 0.98f))
            }
            entry = tar.nextEntry
        }
        return TarStats(count, duplicates, links)
    }
}

private class SevenZipToZip : Converter {
    override val id: String = "archive-7z-to-zip"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("7z"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("zip"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val temp = ArchiveIo.copyToTemp(request, ".7z")
            try {
                val inspected = inspectSevenZip(temp)
                request.onProgress(0.05f)
                request.outputFile.parentFile?.mkdirs()
                var duplicates = 0
                var skipped = 0
                try {
                    BufferedOutputStream(FileOutputStream(request.outputFile), ArchiveGuards.BUFFER_SIZE).use { fileOut ->
                        ZipOutputStream(fileOut).use { zip ->
                            SevenZFile.builder()
                                .setFile(temp)
                                .setMaxMemoryLimitKb(ArchiveGuards.MAX_SEVEN_ZIP_MEMORY_KB)
                                .get()
                                .use { seven ->
                                    val buffer = ByteArray(ArchiveGuards.BUFFER_SIZE)
                                    val seen = HashSet<String>()
                                    var index = 0
                                    var writtenTotal = 0L
                                    var entry: SevenZArchiveEntry? = seven.nextEntry
                                    while (entry != null) {
                                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                                        val current = entry
                                        index++
                                        val name = current.name ?: ""
                                        val safe = ArchiveGuards.normalizeEntryName(name)
                                        if (safe == null) {
                                            if (current.isDirectory) {
                                                entry = seven.nextEntry
                                                continue
                                            }
                                            throw ConversionException("The archive contains an unsafe entry path: " + name)
                                        }
                                        if (!seen.add(safe)) {
                                            duplicates++
                                        } else if (current.isDirectory) {
                                            val zipEntry = ZipEntry(safe + "/")
                                            if (current.hasLastModifiedDate) zipEntry.time = current.lastModifiedDate.time
                                            zip.putNextEntry(zipEntry)
                                            zip.closeEntry()
                                        } else if (current.isAntiItem) {
                                            skipped++
                                        } else {
                                            val zipEntry = ZipEntry(safe)
                                            if (current.hasLastModifiedDate) zipEntry.time = current.lastModifiedDate.time
                                            zip.putNextEntry(zipEntry)
                                            if (current.hasStream()) {
                                                while (true) {
                                                    if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                                                    val read = seven.read(buffer, 0, buffer.size)
                                                    if (read < 0) break
                                                    zip.write(buffer, 0, read)
                                                    writtenTotal += read
                                                    if (writtenTotal > ArchiveGuards.MAX_TOTAL_BYTES) {
                                                        throw ConversionException("The archive expands beyond the 2 GB limit.")
                                                    }
                                                }
                                            }
                                            zip.closeEntry()
                                        }
                                        val fraction = when {
                                            inspected.totalBytes > 0L ->
                                                writtenTotal.toDouble() / inspected.totalBytes
                                            inspected.entries > 0 ->
                                                index.toDouble() / inspected.entries
                                            else -> 0.0
                                        }
                                        request.onProgress((0.05 + 0.9 * fraction).toFloat().coerceIn(0.05f, 0.95f))
                                        entry = seven.nextEntry
                                    }
                                }
                        }
                    }
                } catch (error: IOException) {
                    throw ConversionException(
                        "The 7z archive could not be read. It may be corrupted or password protected.",
                        error
                    )
                }
                request.onProgress(1f)
                ConversionResult(
                    listOf(request.outputFile),
                    buildNote(duplicates, skipped)
                )
            } finally {
                ArchiveIo.deleteQuietly(temp)
            }
        }

    private fun inspectSevenZip(file: File): SevenZipScan {
        var count = 0
        var total = 0L
        SevenZFile.builder()
            .setFile(file)
            .setMaxMemoryLimitKb(ArchiveGuards.MAX_SEVEN_ZIP_MEMORY_KB)
            .get()
            .use { seven ->
                for (entry in seven.entries) {
                    count++
                    if (count > ArchiveGuards.MAX_ENTRIES) {
                        throw ConversionException("The archive has too many entries (limit 50000).")
                    }
                    if (!entry.isDirectory && !entry.isAntiItem) {
                        val size = entry.size
                        if (size > 0L) total += size
                        if (total > ArchiveGuards.MAX_TOTAL_BYTES) {
                            throw ConversionException("The archive expands beyond the 2 GB limit.")
                        }
                    }
                }
            }
        return SevenZipScan(count, total)
    }

    private fun buildNote(duplicates: Int, skipped: Int): Note? = Note.combine(
        listOf(
            duplicates.takeIf { it > 0 }?.let(Note::ArchiveDuplicates),
            skipped.takeIf { it > 0 }?.let(Note::ArchiveDeletedEntries)
        )
    )
}

private fun copyFully(
    input: InputStream,
    output: OutputStream,
    request: ConversionRequest,
    buffer: ByteArray
): Long {
    var total = 0L
    while (true) {
        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
        val read = input.read(buffer)
        if (read < 0) break
        output.write(buffer, 0, read)
        total += read
        if (total > ArchiveGuards.MAX_TOTAL_BYTES) {
            throw ConversionException("The archive expands beyond the 2 GB limit.")
        }
    }
    return total
}

private data class SevenZipScan(val entries: Int, val totalBytes: Long)

private data class TarStats(val entries: Int, val duplicates: Int, val links: Int) {
    fun toNote(): Note? = Note.combine(
        listOf(
            duplicates.takeIf { it > 0 }?.let(Note::ArchiveDuplicates),
            links.takeIf { it > 0 }?.let(Note::ArchiveSymlinks)
        )
    )
}
