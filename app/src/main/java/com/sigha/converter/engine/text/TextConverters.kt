package com.sigha.converter.engine.text

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Text and markup converters: TXT/Markdown to PDF, HTML to plain text. */
fun textConverters(app: Application): List<Converter> = listOf(
    TxtToPdf(),
    MarkdownToPdf(),
    HtmlToText()
)

private class TxtToPdf : Converter {
    override val id: String = "text-txt-to-pdf"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("pdf"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val text = readInput(request)
            if (text.isBlank()) throw ConversionException("The text file is empty.")
            val lineCount = text.count { it == '\n' } + 1
            val expected = minOf(lineCount, MAX_LINES)
            request.onProgress(0.05f)
            val writer = TextPdfWriter(expected, request.onProgress, request.isCancelled)
            try {
                var exported = 0
                for (line in text.lineSequence()) {
                    if (exported >= MAX_LINES) break
                    writer.addParagraph(line, ParagraphStyle.BODY)
                    exported++
                }
                writer.finish(request.outputFile)
            } finally {
                writer.close()
            }
            val note = if (lineCount > MAX_LINES) {
                Note.TextTruncated(MAX_LINES)
            } else {
                null
            }
            ConversionResult(listOf(request.outputFile), note)
        }

    private companion object {
        const val MAX_LINES = 50_000
    }
}

private class MarkdownToPdf : Converter {
    override val id: String = "text-md-to-pdf"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("md"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("pdf"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val markdown = readInput(request)
            val paragraphs = MarkdownParser.paragraphs(markdown)
            if (paragraphs.isEmpty()) throw ConversionException("The Markdown file has no text.")
            request.onProgress(0.05f)
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
                Note.MarkdownSimplified
            )
        }
}

private class HtmlToText : Converter {
    override val id: String = "text-html-to-txt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("html"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val html = readInput(request)
            request.onProgress(0.35f)
            val text = HtmlTextExtractor.toText(html)
            request.onProgress(0.8f)
            if (text.isBlank()) {
                throw ConversionException("No readable text was found in the HTML file.")
            }
            TextIo.writeText(request.outputFile, text + "\n")
            request.onProgress(1f)
            ConversionResult(listOf(request.outputFile))
        }
}

private fun readInput(request: ConversionRequest): String {
    val stream = request.context.contentResolver.openInputStream(request.input)
        ?: throw ConversionException("Cannot read the selected file.")
    return stream.use { TextIo.readText(it) }
}
