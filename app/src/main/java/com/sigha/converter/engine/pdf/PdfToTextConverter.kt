package com.sigha.converter.engine.pdf

import android.content.Context
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.engine.ocr.OcrSupport
import com.sigha.converter.engine.ocr.PdfTextConverter
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
 *
 * Scanned PDFs have no text layer: when the extraction produced no visible text the converter
 * falls back to the OCR engine in this same module, so "PDF -> TXT" does not fail just because
 * the document is a scan. Without the OCR pack the user gets a precise instruction instead of an
 * empty file.
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

    private suspend fun extractText(source: File, request: ConversionRequest): ConversionResult {
        val document = openDocument(source)
        try {
            val pageCount = document.numberOfPages
            if (pageCount <= 0) {
                throw ConversionException(
                    "This PDF has no pages to convert.",
                    kind = ConversionFailureKind.CORRUPT_INPUT
                )
            }
            val pages = selectedPages(request, pageCount)
            request.onProgress(0.1f)
            writeText(document, pages, request)
        } finally {
            runCatching { document.close() }
        }
        request.onProgress(0.92f)
        if (hasVisibleText(request.outputFile)) {
            request.onProgress(1f)
            return ConversionResult(outputs = listOf(request.outputFile))
        }
        request.outputFile.delete()
        return ocrFallback(request)
    }

    /**
     * The text layer is empty (a scan). Delegate to the OCR PDF converter instead of returning an
     * empty file. A missing pack is reported with a precise instruction.
     */
    private suspend fun ocrFallback(request: ConversionRequest): ConversionResult {
        val ocr = PdfTextConverter(PdfFormats.PDF, PdfFormats.TXT)
        val result = try {
            ocr.convert(request)
        } catch (e: ConversionException) {
            if (e.message == OcrSupport.INSTALL_MESSAGE) {
                throw ConversionException(
                    "This PDF has no text layer (it looks like a scan). Install the OCR pack to read scanned PDFs.",
                    e,
                    ConversionFailureKind.NO_TEXT_LAYER
                )
            }
            throw e
        }
        val note = Note.combine(listOf(Note.OcrFallback, result.note))
        return ConversionResult(outputs = result.outputs, note = note)
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

    private fun openDocument(source: File): PDDocument {
        val memory = MemoryUsageSetting.setupTempFileOnly()
        PdfSupport.scratchDir(context)?.let(memory::setTempDir)
        return try {
            // load(File, MemoryUsageSetting) already tries the empty password; any other password fails here.
            PDDocument.load(source, memory)
        } catch (e: InvalidPasswordException) {
            throw ConversionException(
                "This PDF is password-protected. Remove the password and try again.",
                e,
                ConversionFailureKind.PASSWORD_PROTECTED
            )
        } catch (e: IOException) {
            throw ConversionException(
                "Could not read this PDF. The file may be damaged or use an encryption this app cannot open.",
                e,
                ConversionFailureKind.CORRUPT_INPUT
            )
        }
    }

    private fun writeText(document: PDDocument, pages: List<Int>, request: ConversionRequest) {
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
                    pages.forEachIndexed { index, page ->
                        PdfSupport.checkCancelled(request.isCancelled)
                        stripper.startPage = page
                        stripper.endPage = page
                        stripper.writeText(document, writer)
                        writer.flush()
                        request.onProgress(0.1f + 0.8f * ((index + 1).toFloat() / pages.size))
                    }
                }
            }
        } catch (e: Throwable) {
            output.delete()
            throw e
        }
    }

    /** True when the file holds at least one non-whitespace character. */
    private fun hasVisibleText(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            file.inputStream().buffered().use { stream ->
                val reader = stream.reader(Charsets.UTF_8)
                val buffer = CharArray(8192)
                var found = false
                var done = false
                while (!done) {
                    val read = reader.read(buffer)
                    if (read < 0) {
                        done = true
                    } else {
                        for (index in 0 until read) {
                            if (!buffer[index].isWhitespace()) {
                                found = true
                                done = true
                                break
                            }
                        }
                    }
                }
                found
            }
        } catch (e: IOException) {
            false
        }
    }
}
