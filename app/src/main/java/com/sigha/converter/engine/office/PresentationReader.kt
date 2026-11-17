package com.sigha.converter.engine.office

import com.sigha.converter.core.ConversionException
import com.sigha.converter.engine.text.ParagraphStyle
import com.sigha.converter.engine.text.TextParagraph
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Collects the text of every slide in a PPTX package, one paragraph per slide.
 * Slide order follows the file names (slide1, slide2, ...); notes, images and
 * layout are not exported.
 */
internal object PresentationReader {

    private const val MAX_SLIDES = 5_000
    private const val MAX_TEXT_CHARS = 16 * 1024 * 1024
    private val SLIDE_NAME = Regex("""ppt/slides/slide(\d+)\.xml""")

    fun read(zip: SafePackage): List<TextParagraph> {
        val slides = zip.names("ppt/slides/", ".xml")
            .mapNotNull { name ->
                SLIDE_NAME.matchEntire(name)?.let { match ->
                    name to (match.groupValues[1].toIntOrNull() ?: 0)
                }
            }
            .sortedBy { it.second }
        if (slides.isEmpty()) throw ConversionException("This presentation has no slides.")
        if (slides.size > MAX_SLIDES) {
            throw ConversionException("The presentation has too many slides to convert.")
        }
        val paragraphs = ArrayList<TextParagraph>()
        var characters = 0
        for ((name, _) in slides) {
            val stream = zip.open(name) ?: continue
            val text = stream.use { readSlide(it) }
            if (text.isEmpty()) continue
            characters += text.length
            if (characters > MAX_TEXT_CHARS) {
                throw ConversionException("The presentation has too much text to convert.")
            }
            paragraphs.add(TextParagraph(text, ParagraphStyle.BODY))
        }
        return paragraphs
    }

    private fun readSlide(stream: InputStream): String {
        val parser = OoxmlXml.parser(stream)
        val builder = StringBuilder()
        var inText = false
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "a:t" -> inText = true
                    "a:br" -> builder.append('\n')
                }
                XmlPullParser.TEXT -> if (inText) builder.append(parser.text ?: "")
                XmlPullParser.END_TAG -> when (parser.name) {
                    "a:t" -> inText = false
                    "a:p" -> builder.append('\n')
                }
            }
        }
        val lines = ArrayList<String>()
        for (line in builder.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) lines.add(trimmed)
        }
        return lines.joinToString("\n")
    }
}
