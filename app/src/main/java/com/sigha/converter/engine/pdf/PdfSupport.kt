package com.sigha.converter.engine.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sigha.converter.core.ConversionException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CancellationException

/**
 * File plumbing shared by the PDF converters: both PdfRenderer and PDFBox need a seekable local
 * file, so the content URI is streamed into the private cache first and deleted afterwards.
 */
internal object PdfSupport {

    private const val MAX_PDF_BYTES = 500L * 1024L * 1024L
    private const val COPY_BUFFER_BYTES = 64 * 1024

    @Volatile
    private var pdfBoxReady = false

    /** Streams [uri] into a private cache file and returns it; the caller owns and must delete it. */
    fun copyToCacheFile(context: Context, uri: Uri, isCancelled: () -> Boolean): File {
        val knownSize = querySize(context, uri)
        if (knownSize != null && knownSize > MAX_PDF_BYTES) throw tooBig()
        val dir = cacheDir(context, "pdf-input")
        if (knownSize != null) {
            val usable = dir.usableSpace
            if (usable > 0 && usable < knownSize * 2) {
                throw ConversionException("Not enough free storage to process this PDF.")
            }
        }
        val tempFile = try {
            File.createTempFile("sigha-pdf-", ".pdf", dir)
        } catch (e: Exception) {
            throw ConversionException("Could not create a temporary file for this PDF.", e)
        }
        var copied = 0L
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw ConversionException("Could not open the selected PDF. Check that the file is still available.")
            input.use { source ->
                FileOutputStream(tempFile).use { target ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        if (isCancelled()) throw cancellation()
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        copied += read
                        if (copied > MAX_PDF_BYTES) throw tooBig()
                        target.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: Throwable) {
            tempFile.delete()
            throw e
        }
        return tempFile
    }

    /** PDFBox needs its resource loader initialised exactly once before the first document is parsed. */
    fun ensurePdfBoxReady(context: Context) {
        if (pdfBoxReady) return
        synchronized(this) {
            if (!pdfBoxReady) {
                PDFBoxResourceLoader.init(context.applicationContext)
                pdfBoxReady = true
            }
        }
    }

    /** Folder for PDFBox scratch files; null when the cache folder is unusable (PDFBox then uses its default). */
    fun scratchDir(context: Context): File? {
        val dir = File(context.cacheDir, "pdfbox-scratch")
        return if (dir.isDirectory || dir.mkdirs()) dir else null
    }

    fun checkCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw cancellation()
    }

    fun cancellation(): CancellationException = CancellationException("Conversion cancelled.")

    private fun cacheDir(context: Context, name: String): File {
        val dir = File(context.cacheDir, name)
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw ConversionException("Could not create a temporary folder. Check the device's free storage.")
        }
        return dir
    }

    private fun tooBig(): ConversionException =
        ConversionException("This PDF is too large to process. The limit is 500 MB.")

    private fun querySize(context: Context, uri: Uri): Long? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || !cursor.moveToFirst() || cursor.isNull(index)) null else cursor.getLong(index)
        }
    } catch (_: Exception) {
        null
    }
}
