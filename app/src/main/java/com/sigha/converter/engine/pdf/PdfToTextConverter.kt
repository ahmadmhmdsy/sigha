package com.sigha.converter.engine.pdf

import android.content.Context
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * PDF -> UTF-8 TXT through PDFBox. PDFBox's scratch storage is forced to temp files, so a large
 * document cannot be pulled into the heap; text is streamed page by page into the output file.
 */
internal class PdfToTextConverter(private val context: Context) : Converter {

    override val id: String = "pdf-to-text"
    override val inputs: Set<FileFormat> = setOf(PdfFormats.PDF)
    override val outputs: Set<FileFormat> = setOf(PdfFormats.TXT)

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            try {
                PdfSupport.ensurePdfBoxReady(context)
                val source = PdfSupport.copyToCacheFile(context, request.input, request.isCancelled)
                try {
                    extractText(source, request)
                } finally {
                    source.delete()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConversionException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw ConversionException("Not enough memory to read this PDF.", e)
            } catch (e: IOException) {
                throw ConversionException(
                    "Could not read this PDF. The file may be damaged or use an encryption this app cannot open.",
                    e
                )
            } catch (e: Exception) {
                throw ConversionException("Could not convert this PDF to text.", e)
            }
        }

    private fun extractText(source: File, request: ConversionRequest): ConversionResult {
        val document = openDocument(source)
        try {
            val pageCount = document.numberOfPages
            if (pageCount <= 0) throw ConversionException("This PDF has no pages to convert.")
            request.onProgress(0.1f)
            writeText(document, pageCount, request)
            request.onProgress(1f)
            val note = if (request.outputFile.length() == 0L) {
                "No selectable text was found. This PDF may be a scan or contain only images."
            } else {
                null
            }
            return ConversionResult(outputs = listOf(request.outputFile), note = note)
        } finally {
            runCatching { document.close() }
        }
    }

    private fun openDocument(source: File): PDDocument {
        val memory = MemoryUsageSetting.setupTempFileOnly()
        PdfSupport.scratchDir(context)?.let(memory::setTempDir)
        return try {
            // load(File, MemoryUsageSetting) already tries the empty password; any other password fails here.
            PDDocument.load(source, memory)
        } catch (e: InvalidPasswordException) {
            throw ConversionException("This PDF is password-protected. Remove the password and try again.", e)
        } catch (e: IOException) {
            throw ConversionException(
                "Could not read this PDF. The file may be damaged or use an encryption this app cannot open.",
                e
            )
        }
    }

    private fun writeText(document: PDDocument, pageCount: Int, request: ConversionRequest) {
        val output = request.outputFile
        output.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) {
                throw ConversionException("Could not create the output folder. Check the device's free storage.")
            }
        }
        val stripper = PDFTextStripper()
        try {
            FileOutputStream(output).use { stream ->
                stream.bufferedWriter().use { writer ->
                    for (page in 1..pageCount) {
                        PdfSupport.checkCancelled(request.isCancelled)
                        stripper.startPage = page
                        stripper.endPage = page
                        stripper.writeText(document, writer)
                        writer.flush()
                        request.onProgress(0.1f + 0.85f * (page.toFloat() / pageCount))
                    }
                }
            }
        } catch (e: Throwable) {
            output.delete()
            throw e
        }
    }
}
