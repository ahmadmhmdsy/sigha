package com.sigha.converter.engine.office

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.engine.data.Csv
import com.sigha.converter.engine.text.EngineFormats
import com.sigha.converter.engine.text.ParagraphStyle
import com.sigha.converter.engine.text.TextIo
import com.sigha.converter.engine.text.TextParagraph
import com.sigha.converter.engine.text.TextPdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Office document converters.
 *
 * DOCX, XLSX and PPTX are read from their OOXML packages; ODT and ODS from the
 * ODF content part. Everything runs offline and streams from the package parts
 * instead of loading a whole document into memory.
 */
fun officeConverters(app: Application): List<Converter> = listOf(
    DocxToTxt(),
    DocxToHtml(),
    DocxToPdf(),
    XlsxToCsv(),
    XlsxToTsv(),
    XlsxToPdf(),
    CsvToXlsx(),
    PptxToTxt(),
    OdtToTxt(),
    OdsToCsv()
)

private class DocxToTxt : Converter {
    override val id: String = "office-docx-to-txt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("docx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = withDocument(request, ".docx") { zip -> readWord(zip) }
            request.onProgress(0.6f)
            if (paragraphs.isEmpty()) throw ConversionException("No text was found in the document.")
            TextIo.writeText(request.outputFile, OfficeTextWriter.plain(paragraphs))
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.OfficeTextOnly
            )
        }
}

private class DocxToHtml : Converter {
    override val id: String = "office-docx-to-html"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("docx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("html"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = withDocument(request, ".docx") { zip -> readWord(zip) }
            request.onProgress(0.6f)
            if (paragraphs.isEmpty()) throw ConversionException("No text was found in the document.")
            TextIo.writeText(request.outputFile, OfficeTextWriter.html("Document", paragraphs))
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.OfficeTextOnly
            )
        }
}

private class DocxToPdf : Converter {
    override val id: String = "office-docx-to-pdf"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("docx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("pdf"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = withDocument(request, ".docx") { zip -> readWord(zip) }
            if (paragraphs.isEmpty()) throw ConversionException("No text was found in the document.")
            request.onProgress(0.1f)
            val writer = TextPdfWriter(paragraphs.size, request.onProgress, request.isCancelled)
            try {
                for (paragraph in paragraphs) {
                    writer.addParagraph(paragraph.text, paragraph.style)
                }
                writer.finish(request.outputFile)
            } finally {
                writer.close()
            }
            ConversionResult(
                listOf(request.outputFile),
                Note.OfficeImagesOmitted
            )
        }
}

private class XlsxToCsv : Converter {
    override val id: String = "office-xlsx-to-csv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("xlsx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) { convertSheet(request, ',') }
}

private class XlsxToTsv : Converter {
    override val id: String = "office-xlsx-to-tsv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("xlsx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("tsv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) { convertSheet(request, '\t') }
}

private class XlsxToPdf : Converter {
    override val id: String = "office-xlsx-to-pdf"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("xlsx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("pdf"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = ArrayList<TextParagraph>()
            withDocument(request, ".xlsx") { zip ->
                val sheet = WorkbookReader.locateFirstSheet(zip)
                val shared = WorkbookReader.sharedStrings(zip)
                request.onProgress(0.1f)
                WorkbookReader.forEachRow(zip, sheet.path, shared) { cells ->
                    if (paragraphs.size >= MAX_PDF_ROWS) {
                        throw ConversionException("The worksheet has too many rows for PDF export.")
                    }
                    val text = cells.joinToString("  |  ") { it.replace('\t', ' ') }
                    if (text.isNotBlank()) paragraphs.add(TextParagraph(text, ParagraphStyle.MONO))
                }
            }
            if (paragraphs.isEmpty()) throw ConversionException("The worksheet has no data to convert.")
            request.onProgress(0.2f)
            val writer = TextPdfWriter(paragraphs.size, request.onProgress, request.isCancelled)
            try {
                for (paragraph in paragraphs) {
                    writer.addParagraph(paragraph.text, paragraph.style)
                }
                writer.finish(request.outputFile)
            } finally {
                writer.close()
            }
            ConversionResult(
                listOf(request.outputFile),
                Note.OfficeSimpleTable
            )
        }

    private companion object {
        const val MAX_PDF_ROWS = 20_000
    }
}

private class CsvToXlsx : Converter {
    override val id: String = "office-csv-to-xlsx"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("xlsx"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            var rows = 0
            TextIo.open(request.context, request.input).use { source ->
                val parser = Csv.Parser(source.reader, ',')
                val rowSequence = generateSequence {
                    if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                    parser.readRow()
                }
                rows = WorkbookWriter.write(request.outputFile, rowSequence, request.isCancelled) { count ->
                    request.onProgress((0.05f + count / 200_000f).coerceAtMost(0.9f))
                }
            }
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.CsvRowsWritten(rows)
            )
        }
}

private class PptxToTxt : Converter {
    override val id: String = "office-pptx-to-txt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("pptx"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = withDocument(request, ".pptx") { zip -> PresentationReader.read(zip) }
            request.onProgress(0.6f)
            if (paragraphs.isEmpty()) throw ConversionException("No text was found in the presentation.")
            val text = paragraphs.joinToString("\n\n") { it.text }
            TextIo.writeText(request.outputFile, text + "\n")
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.PptxTextOnly
            )
        }
}

private class OdtToTxt : Converter {
    override val id: String = "office-odt-to-txt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("odt"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val paragraphs = withDocument(request, ".odt") { zip -> OpenDocumentReader.readText(zip) }
            request.onProgress(0.6f)
            if (paragraphs.isEmpty()) throw ConversionException("No text was found in the document.")
            TextIo.writeText(request.outputFile, OfficeTextWriter.plain(paragraphs))
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.OfficeTextOnly
            )
        }
}

private class OdsToCsv : Converter {
    override val id: String = "office-ods-to-csv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("ods"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            var rows = 0
            withDocument(request, ".ods") { zip ->
                request.onProgress(0.1f)
                openTextOutput(request).use { writer ->
                    var seen = 0
                    rows = OpenDocumentReader.readSheetRows(zip) { cells ->
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        Csv.appendRow(writer, cells, ',', "\r\n")
                        seen++
                        if (seen % 512 == 0) {
                            request.onProgress((0.15f + seen / 200_000f).coerceAtMost(0.9f))
                        }
                    }
                    writer.flush()
                }
            }
            if (rows == 0) throw ConversionException("The spreadsheet has no data to convert.")
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.combine(listOf(Note.FirstSheetOnly, Note.DisplayedCellText))
            )
        }
}

private fun convertSheet(request: ConversionRequest, delimiter: Char): ConversionResult {
    var rows = 0
    var sheetNames: List<String> = emptyList()
    withDocument(request, ".xlsx") { zip ->
        val sheet = WorkbookReader.locateFirstSheet(zip)
        sheetNames = sheet.names
        val shared = WorkbookReader.sharedStrings(zip)
        request.onProgress(0.1f)
        openTextOutput(request).use { writer ->
            var seen = 0
            rows = WorkbookReader.forEachRow(zip, sheet.path, shared) { cells ->
                if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                Csv.appendRow(writer, cells, delimiter, "\r\n")
                seen++
                if (seen % 512 == 0) {
                    request.onProgress((0.15f + seen / 200_000f).coerceAtMost(0.9f))
                }
            }
            writer.flush()
        }
    }
    if (rows == 0) throw ConversionException("The worksheet has no data to convert.")
    request.onProgress(1f)
    val note = Note.combine(
        listOfNotNull(
            Note.FirstSheetOnly.takeIf { sheetNames.size > 1 },
            Note.DatesAsSerial
        )
    )
    return ConversionResult(listOf(request.outputFile), note)
}

private fun readWord(zip: SafePackage): List<TextParagraph> {
    val stream = zip.open("word/document.xml")
        ?: throw ConversionException("This DOCX file has no document text.")
    return stream.use { WordReader.read(it) }
}

private fun openTextOutput(request: ConversionRequest): BufferedWriter {
    request.outputFile.parentFile?.mkdirs()
    return TextIo.writer(
        BufferedOutputStream(request.outputFile.outputStream(), TextIo.BUFFER_SIZE)
    )
}

private fun <T> withDocument(
    request: ConversionRequest,
    suffix: String,
    block: (SafePackage) -> T
): T {
    val temp = OfficeIo.copyToTemp(request, suffix)
    try {
        SafePackage.open(temp).use { zip ->
            try {
                return block(zip)
            } catch (error: XmlPullParserException) {
                throw ConversionException("The document contains invalid XML.", error)
            } catch (error: IOException) {
                throw ConversionException("The document could not be read.", error)
            }
        }
    } finally {
        OfficeIo.deleteQuietly(temp)
    }
}
