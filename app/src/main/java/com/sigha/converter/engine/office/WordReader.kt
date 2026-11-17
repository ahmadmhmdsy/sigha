package com.sigha.converter.engine.office

import com.sigha.converter.core.ConversionException
import com.sigha.converter.engine.text.ParagraphStyle
import com.sigha.converter.engine.text.TextParagraph
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Reads the body of a DOCX "word/document.xml" part into a paragraph list.
 *
 * Headings (by style name or outline level), lists and line breaks are kept;
 * character formatting, tables and images are flattened to plain text.
 */
internal object WordReader {

    private const val MAX_PARAGRAPHS = 200_000

    fun read(part: InputStream): List<TextParagraph> {
        val paragraphs = ArrayList<TextParagraph>()
        val text = StringBuilder()
        val parser = OoxmlXml.parser(part)
        var inParagraph = false
        var inProperties = false
        var inText = false
        var heading = 0
        var bullet = false
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:p" -> {
                        inParagraph = true
                        inProperties = false
                        inText = false
                        heading = 0
                        bullet = false
                        text.setLength(0)
                    }
                    "w:pPr" -> if (inParagraph) inProperties = true
                    "w:pStyle" -> if (inProperties) {
                        heading = headingLevel(OoxmlXml.attribute(parser, "w:val"))
                    }
                    "w:outlineLvl" -> if (inProperties) {
                        val level = outlineLevel(OoxmlXml.attribute(parser, "w:val"))
                        if (level > heading) heading = level
                    }
                    "w:numPr" -> if (inProperties) bullet = true
                    "w:t" -> if (inParagraph) inText = true
                    "w:tab" -> if (inParagraph && !inProperties) text.append('\t')
                    "w:br", "w:cr" -> if (inParagraph) text.append('\n')
                }
                XmlPullParser.TEXT -> if (inText) text.append(parser.text ?: "")
                XmlPullParser.END_TAG -> when (parser.name) {
                    "w:t" -> inText = false
                    "w:pPr" -> inProperties = false
                    "w:p" -> {
                        if (inParagraph) flush(paragraphs, text, heading, bullet)
                        inParagraph = false
                        inProperties = false
                        inText = false
                        if (paragraphs.size > MAX_PARAGRAPHS) {
                            throw ConversionException("The document has too many paragraphs to convert.")
                        }
                    }
                }
            }
        }
        return paragraphs
    }

    private fun flush(
        target: MutableList<TextParagraph>,
        text: StringBuilder,
        heading: Int,
        bullet: Boolean
    ) {
        val content = clean(text.toString())
        if (content.isEmpty()) return
        val style = when {
            heading >= 3 -> ParagraphStyle.H3
            heading == 2 -> ParagraphStyle.H2
            heading == 1 -> ParagraphStyle.H1
            bullet -> ParagraphStyle.BULLET
            else -> ParagraphStyle.BODY
        }
        target.add(TextParagraph(content, style))
    }

    private fun clean(value: String): String {
        val builder = StringBuilder(value.length)
        for (character in value) {
            when {
                character == '\u00A0' || character == '\u000B' -> builder.append(' ')
                character == '\n' || character == '\t' -> builder.append(character)
                character < ' ' -> Unit
                else -> builder.append(character)
            }
        }
        return builder.toString().trim()
    }

    private fun headingLevel(style: String?): Int {
        if (style == null) return 0
        val lowered = style.lowercase()
        if (lowered == "title") return 1
        if (lowered == "subtitle") return 2
        if (lowered.startsWith("heading")) {
            val digits = lowered.removePrefix("heading").trim()
            return digits.toIntOrNull()?.coerceIn(1, 3) ?: 1
        }
        if (lowered.startsWith("berschrift")) {
            val digits = lowered.removePrefix("berschrift").trim()
            return digits.toIntOrNull()?.coerceIn(1, 3) ?: 0
        }
        return 0
    }

    private fun outlineLevel(value: String?): Int {
        val level = value?.trim()?.toIntOrNull() ?: return 0
        return (level + 1).coerceIn(1, 3)
    }
}
