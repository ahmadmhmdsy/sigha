package com.sigha.converter.engine.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

/**
 * image -> one A4 page PDF. The page is A4 at the requested DPI (1240 x 1754 at the default 150),
 * the picture is fitted inside a small white margin and centred.
 */
internal class ImageToPdfConverter(private val context: Context) : Converter {

    override val id: String = "image-to-pdf"
    override val inputs: Set<FileFormat> = ImageFormats.inputs
    override val outputs: Set<FileFormat> = setOf(ImageFormats.PDF)

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            try {
                convertNow(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConversionException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw ConversionException("Not enough memory to build this PDF. Try a lower DPI.", e)
            } catch (e: Exception) {
                throw ConversionException("Could not convert this image to PDF. The file may be damaged.", e)
            }
        }

    private fun convertNow(request: ConversionRequest): ConversionResult {
        ImageDecoding.checkNotCancelled(request.isCancelled)
        request.onProgress(0.02f)
        val bitmap = ImageDecoding.decode(
            context = context,
            uri = request.input,
            formatId = request.from.id,
            maxDimension = request.options.maxDimension,
            isCancelled = request.isCancelled
        )
        try {
            ImageDecoding.checkNotCancelled(request.isCancelled)
            request.onProgress(0.65f)
            writePdf(bitmap, request)
            request.onProgress(1f)
            return ConversionResult(outputs = listOf(request.outputFile))
        } catch (e: Throwable) {
            request.outputFile.delete()
            throw e
        } finally {
            bitmap.recycle()
        }
    }

    private fun writePdf(bitmap: Bitmap, request: ConversionRequest) {
        val dpi = request.options.dpi.coerceIn(MIN_DPI, MAX_DPI)
        val pageWidth = (A4_WIDTH_INCHES * dpi).roundToInt()
        val pageHeight = (A4_HEIGHT_INCHES * dpi).roundToInt()
        val margin = (PAGE_MARGIN_INCHES * dpi).roundToInt().coerceAtLeast(1)
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            try {
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val availableWidth = (pageWidth - 2 * margin).coerceAtLeast(1)
                val availableHeight = (pageHeight - 2 * margin).coerceAtLeast(1)
                val scale = minOf(
                    availableWidth.toFloat() / bitmap.width,
                    availableHeight.toFloat() / bitmap.height
                )
                val drawWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val drawHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                val left = (pageWidth - drawWidth) / 2f
                val top = (pageHeight - drawHeight) / 2f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)
            } finally {
                document.finishPage(page)
            }
            val output = request.outputFile
            output.parentFile?.let { parent ->
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw ConversionException("Could not create the output folder. Check the device's free storage.")
                }
            }
            FileOutputStream(output).use { stream -> document.writeTo(stream) }
        } finally {
            document.close()
        }
    }

    private companion object {
        const val MIN_DPI = 72
        const val MAX_DPI = 300
        const val A4_WIDTH_INCHES = 8.27f
        const val A4_HEIGHT_INCHES = 11.69f
        const val PAGE_MARGIN_INCHES = 0.2f
    }
}
