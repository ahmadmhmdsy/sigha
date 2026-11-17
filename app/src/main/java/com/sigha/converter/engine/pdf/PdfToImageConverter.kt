package com.sigha.converter.engine.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * PDF -> PNG / JPG, one file per selected page, rendered by the platform PdfRenderer.
 * A single selected page keeps the caller's output file name; several pages become
 * "name-pageN.png" next to it and all of them are returned.
 */
internal class PdfToImageConverter(private val context: Context) : Converter {

    override val id: String = "pdf-to-image"
    override val inputs: Set<FileFormat> = setOf(PdfFormats.PDF)
    override val outputs: Set<FileFormat> = setOf(PdfFormats.PNG, PdfFormats.JPG)

    /** One selected page keeps the caller's name; several pages write "name-pageN" files. */
    override val producesMultipleOutputs: Boolean = true

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            try {
                val source = PdfSupport.copyToCacheFile(context, request.input, request.isCancelled)
                try {
                    renderPages(source, request)
                } finally {
                    source.delete()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConversionException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw ConversionException("Not enough memory to render this PDF. Try a lower DPI.", e)
            } catch (e: Exception) {
                throw ConversionException(
                    "Could not convert this PDF. The file may be damaged or the device may be out of storage.",
                    e
                )
            }
        }

    private fun renderPages(source: File, request: ConversionRequest): ConversionResult {
        val extension = if (request.to.id == "jpg") "jpg" else "png"
        val format = if (request.to.id == "jpg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val quality = request.options.quality.coerceIn(1, 100)
        val dpi = request.options.dpi.coerceIn(MIN_DPI, MAX_DPI)
        val scale = dpi / 72.0

        val descriptor = try {
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: IOException) {
            throw ConversionException("Could not open this PDF. The file may be damaged or unavailable.", e)
        }
        var renderer: PdfRenderer? = null
        try {
            renderer = try {
                PdfRenderer(descriptor)
            } catch (e: SecurityException) {
                throw wrongPassword()
            } catch (e: IOException) {
                throw ConversionException(
                    "Could not read this PDF. The file may be damaged or use a format this device cannot render.",
                    e
                )
            }
            val pageCount = renderer.pageCount
            if (pageCount <= 0) throw ConversionException("This PDF has no pages to convert.")
            val pages = selectedPages(request, pageCount)
            val singlePage = pages.size == 1
            val outputs = ArrayList<File>(pages.size)
            try {
                pages.forEachIndexed { index, pageNumber ->
                    PdfSupport.checkCancelled(request.isCancelled)
                    val page = renderer.openPage(pageNumber - 1)
                    try {
                        val width = max(1, (page.width * scale).roundToInt())
                        val height = max(1, (page.height * scale).roundToInt())
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            // PDF pages are transparent where nothing is painted: start from white.
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val output = if (singlePage) {
                                request.outputFile
                            } else {
                                pageOutputFile(request.outputFile, pageNumber, extension)
                            }
                            writeBitmap(bitmap, format, quality, output)
                            outputs += output
                        } finally {
                            bitmap.recycle()
                        }
                    } finally {
                        page.close()
                    }
                    request.onProgress(0.05f + 0.93f * ((index + 1).toFloat() / pages.size))
                }
            } catch (e: Throwable) {
                outputs.forEach { it.delete() }
                throw e
            }
            request.onProgress(1f)
            return ConversionResult(outputs = outputs)
        } finally {
            renderer?.close()
            try {
                descriptor.close()
            } catch (_: IOException) {
                // The renderer may have closed the descriptor already; ParcelFileDescriptor tolerates it.
            }
        }
    }

    private fun selectedPages(request: ConversionRequest, pageCount: Int): List<Int> {
        val spans = try {
            parsePageSpans(request.options.pages)
        } catch (e: IllegalArgumentException) {
            throw ConversionException(
                "Page selection is not valid. Use page numbers or ranges such as 1-3,5,8-.",
                e
            )
        }
        val pages = resolvePageIndices(spans, pageCount)
        if (pages.isEmpty()) {
            throw ConversionException(
                "The page selection does not match this PDF, which has " + pageCount + " page(s)."
            )
        }
        return pages
    }

    private fun writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, output: File) {
        output.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) {
                throw ConversionException("Could not create the output folder. Check the device's free storage.")
            }
        }
        FileOutputStream(output).use { stream ->
            if (!bitmap.compress(format, quality, stream)) {
                throw ConversionException("Could not write the rendered page. The device may be out of storage.")
            }
        }
    }

    private fun wrongPassword(): ConversionException =
        ConversionException("This PDF is password-protected. Remove the password and try again.")

    private companion object {
        const val MIN_DPI = 72
        const val MAX_DPI = 300
    }
}
