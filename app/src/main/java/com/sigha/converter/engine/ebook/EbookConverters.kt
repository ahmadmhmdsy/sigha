package com.sigha.converter.engine.ebook

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.engine.office.OfficeIo
import com.sigha.converter.engine.office.OoxmlXml
import com.sigha.converter.engine.office.SafePackage
import com.sigha.converter.engine.text.EngineFormats
import com.sigha.converter.engine.text.TextIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/** EPUB converters: the book to plain text or to one cleaned HTML document. */
fun ebookConverters(app: Application): List<Converter> = listOf(
    EpubToTxt(),
    EpubToHtml()
)

private class EpubToTxt : Converter {
    override val id: String = "ebook-epub-to-txt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("epub"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val book = withEpub(request) { zip -> EpubReader.read(zip) }
            request.onProgress(0.7f)
            val text = book.chapters.joinToString("\n\n") { it.text }
            TextIo.writeText(request.outputFile, text.trimEnd() + "\n")
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.combine(
                    listOf(
                        Note.EpubTextOnly,
                        if (book.truncated) Note.BookTruncated else null
                    )
                )
            )
        }
}

private class EpubToHtml : Converter {
    override val id: String = "ebook-epub-to-html"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("epub"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("html"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val book = withEpub(request) { zip -> EpubReader.read(zip) }
            request.onProgress(0.7f)
            val html = buildString {
                append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>")
                append(OoxmlXml.escape(book.title))
                append("</title>\n</head>\n<body>\n")
                for (chapter in book.chapters) {
                    append("<h1>").append(OoxmlXml.escape(chapter.title)).append("</h1>\n")
                    append(chapter.html).append('\n')
                }
                append("</body>\n</html>\n")
            }
            TextIo.writeText(request.outputFile, html)
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.combine(
                    listOf(
                        Note.EpubHtmlOnly,
                        if (book.truncated) Note.BookTruncated else null
                    )
                )
            )
        }
}

private fun <T> withEpub(request: ConversionRequest, block: (SafePackage) -> T): T {
    val temp = OfficeIo.copyToTemp(request, ".epub")
    try {
        SafePackage.open(temp).use { zip ->
            try {
                return block(zip)
            } catch (error: XmlPullParserException) {
                throw ConversionException("This EPUB file contains invalid XML.", error)
            } catch (error: IOException) {
                throw ConversionException("This EPUB file could not be read.", error)
            }
        }
    } finally {
        OfficeIo.deleteQuietly(temp)
    }
}
