package com.sigha.converter.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.model.OcrPageRange
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scanned PDF to text: rasterise each page through [PdfRenderer] at 200 dpi and OCR it.
 *
 * The page window comes from the "pages" option; without one the first
 * [OcrPageRange.DEFAULT_MAX_PAGES] pages are read and the result note says so, so a huge
 * document never silently loses pages.
 */
internal class PdfTextConverter(
    inputFormat: FileFormat,
    outputFormat: FileFormat
) : Converter {

    override val id: String = "ocr.pdf-to-txt"
    override val inputs: Set<FileFormat> = setOf(inputFormat)
    override val outputs: Set<FileFormat> = setOf(outputFormat)

    override val requiresModelPack: Boolean = true

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            val context = request.context
            val language = OcrSupport.languageCode(context, request)
            val datapath = OcrSupport.datapath(context)
            OcrSupport.checkCancelled(request)
            request.onProgress(0.02f)
            val source = File(
                request.outputFile.parentFile ?: context.cacheDir,
                "ocr-source-" + System.nanoTime() + ".pdf"
            )
            try {
                copyToCache(context, request.input, source)
                var note: Note? = null
                val text = StringBuilder()
                openRenderer(source).use { renderer ->
                    val pageCount = renderer.pageCount
                    val selection = OcrPageRange.select(request.options.pages, pageCount)
                    note = selection.note
                    if (selection.pages.isEmpty()) {
                        // The English message is a developer diagnostic (details only); the
                        // localized headline comes from the failure kind.
                        throw ConversionException("This PDF has no pages to read")
                    }
                    OcrEngine(datapath, language).use { engine ->
                        for ((index, pageNumber) in selection.pages.withIndex()) {
                            OcrSupport.checkCancelled(request)
                            request.onProgress(
                                0.05f + 0.9f * index / selection.pages.size
                            )
                            val pageText = readPage(renderer, pageNumber, engine)
                            if (pageText.isNotBlank()) {
                                if (text.isNotEmpty()) text.append("\n\n")
                                text.append(pageText)
                            }
                        }
                    }
                }
                OcrSupport.checkCancelled(request)
                request.onProgress(0.97f)
                val result = OcrFiles.writeText(request, text.toString(), EMPTY_MESSAGE, note)
                request.onProgress(1f)
                result
            } finally {
                source.delete()
            }
        }

    private fun openRenderer(file: File): PdfRenderer {
        val descriptor = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (error: IOException) {
            throw ConversionException("Could not read this PDF", error)
        }
        return try {
            PdfRenderer(descriptor)
        } catch (error: SecurityException) {
            closeQuietly(descriptor)
            throw ConversionException("This PDF is password-protected", error)
        } catch (error: IOException) {
            closeQuietly(descriptor)
            throw ConversionException("Could not open this PDF", error)
        } catch (error: IllegalArgumentException) {
            closeQuietly(descriptor)
            throw ConversionException("Could not open this PDF", error)
        } catch (error: Exception) {
            closeQuietly(descriptor)
            throw ConversionException("Could not open this PDF", error)
        }
    }

    private fun readPage(renderer: PdfRenderer, pageNumber: Int, engine: OcrEngine): String {
        val page = try {
            renderer.openPage(pageNumber - 1)
        } catch (error: IllegalArgumentException) {
            throw ConversionException("Could not open page " + pageNumber + " of this PDF", error)
        }
        return try {
            val bitmap = renderPage(page)
            try {
                engine.recognize(bitmap)
            } finally {
                bitmap.recycle()
            }
        } catch (error: ConversionException) {
            throw error
        } catch (error: Exception) {
            throw ConversionException("Could not read page " + pageNumber + " of this PDF", error)
        } finally {
            page.close()
        }
    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        val scale = RENDER_DPI / 72f
        var width = Math.round(page.width * scale).coerceAtLeast(1)
        var height = Math.round(page.height * scale).coerceAtLeast(1)
        val longest = maxOf(width, height)
        if (longest > MAX_RENDER_SIDE) {
            val shrink = MAX_RENDER_SIDE.toFloat() / longest
            width = (width * shrink).toInt().coerceAtLeast(1)
            height = (height * shrink).toInt().coerceAtLeast(1)
        }
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            bitmap
        } catch (error: OutOfMemoryError) {
            throw ConversionException("A page of this PDF is too large to read", error)
        }
    }

    private fun copyToCache(context: Context, uri: Uri, target: File) {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw ConversionException("Could not read this PDF")
            input.use { source ->
                target.outputStream().buffered().use { sink ->
                    source.copyTo(sink, COPY_BUFFER)
                }
            }
        } catch (error: ConversionException) {
            throw error
        } catch (error: IOException) {
            throw ConversionException("Could not read this PDF", error)
        }
    }

    private fun closeQuietly(descriptor: ParcelFileDescriptor?) {
        runCatching { descriptor?.close() }
    }

    private companion object {
        const val RENDER_DPI = 200f
        const val MAX_RENDER_SIDE = 4096
        const val COPY_BUFFER = 64 * 1024
        const val EMPTY_MESSAGE =
            "No text recognised in this PDF. Scanned pages must show visible text."
    }
}
