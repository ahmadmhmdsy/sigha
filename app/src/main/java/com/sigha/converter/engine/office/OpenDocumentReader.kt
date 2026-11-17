package com.sigha.converter.engine.office

import com.sigha.converter.core.ConversionException
import com.sigha.converter.engine.text.ParagraphStyle
import com.sigha.converter.engine.text.TextParagraph
import org.xmlpull.v1.XmlPullParser

/**
 * Minimal OpenDocument reader built on the ODF "content.xml" part.
 *
 * ODT text keeps headings, lists and line breaks; ODS reads the first table
 * and emits its displayed cell text (which is what a spreadsheet shows).
 */
internal object OpenDocumentReader {

    private const val MAX_PARAGRAPHS = 100_000

    fun readText(zip: SafePackage): List<TextParagraph> {
        val stream = zip.open("content.xml")
            ?: throw ConversionException("This OpenDocument file has no content.")
        val paragraphs = ArrayList<TextParagraph>()
        stream.use {
            val parser = OoxmlXml.parser(it)
            val text = StringBuilder()
            var inParagraph = false
            var heading = 0
            var listDepth = 0
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "text:h" -> {
                            inParagraph = true
                            heading = OoxmlXml.attribute(parser, "text:outline-level")
                                ?.trim()?.toIntOrNull()?.coerceIn(1, 3) ?: 1
                            text.setLength(0)
                        }
                        "text:p" -> if (!inParagraph) {
                            inParagraph = true
                            heading = 0
                            text.setLength(0)
                        } else {
                            text.append(' ')
                        }
                        "text:list" -> listDepth++
                        "text:s" -> if (inParagraph) {
                            val count = OoxmlXml.attribute(parser, "text:c")
                                ?.trim()?.toIntOrNull()?.coerceIn(1, 200) ?: 1
                            repeat(count) { text.append(' ') }
                        }
                        "text:tab" -> if (inParagraph) text.append('\t')
                        "text:line-break" -> if (inParagraph) text.append('\n')
                    }
                    XmlPullParser.TEXT -> if (inParagraph) text.append(parser.text ?: "")
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "text:p", "text:h" -> if (inParagraph) {
                            addParagraph(paragraphs, text.toString(), heading, listDepth > 0)
                            inParagraph = false
                            heading = 0
                        }
                        "text:list" -> if (listDepth > 0) listDepth--
                    }
                }
            }
        }
        return paragraphs
    }

    /** Streams rows of the first table; returns the number of non-empty rows. */
    fun readSheetRows(zip: SafePackage, onRow: (List<String>) -> Unit): Int {
        val stream = zip.open("content.xml")
            ?: throw ConversionException("This OpenDocument file has no content.")
        var rows = 0
        var inTable = false
        var tableSeen = false
        var inRow = false
        var inCell = false
        val cellText = StringBuilder()
        var cells = ArrayList<String>()
        var columnRepeat = 1
        var rowRepeat = 1
        var cellValue: String? = null
        stream.use {
            val parser = OoxmlXml.parser(it)
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "table:table" -> if (!tableSeen) {
                            tableSeen = true
                            inTable = true
                        }
                        "table:table-row" -> if (inTable) {
                            inRow = true
                            cells = ArrayList()
                            rowRepeat = repeatCount(parser, "table:number-rows-repeated")
                        }
                        "table:table-cell", "table:covered-table-cell" -> if (inRow) {
                            inCell = true
                            cellText.setLength(0)
                            columnRepeat = repeatCount(parser, "table:number-columns-repeated")
                            cellValue = OoxmlXml.attribute(parser, "office:value")
                        }
                        "text:p" -> if (inCell && cellText.isNotEmpty()) cellText.append(' ')
                        "text:s" -> if (inCell) {
                            val count = OoxmlXml.attribute(parser, "text:c")
                                ?.trim()?.toIntOrNull()?.coerceIn(1, 200) ?: 1
                            repeat(count) { cellText.append(' ') }
                        }
                        "text:tab" -> if (inCell) cellText.append('\t')
                        "text:line-break" -> if (inCell) cellText.append('\n')
                    }
                    XmlPullParser.TEXT -> if (inCell) cellText.append(parser.text ?: "")
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "table:table-cell", "table:covered-table-cell" -> if (inCell) {
                            val value = cellText.toString().trim().ifEmpty {
                                cellValue?.trim().orEmpty()
                            }
                            for (index in 0 until columnRepeat) {
                                if (cells.size >= WorkbookReader.MAX_COLUMNS) break
                                cells.add(value)
                            }
                            inCell = false
                            cellValue = null
                        }
                        "table:table-row" -> if (inRow) {
                            inRow = false
                            if (cells.any { it.isNotEmpty() }) {
                                for (index in 0 until rowRepeat) {
                                    if (rows >= WorkbookReader.MAX_ROWS) {
                                        throw ConversionException("The spreadsheet has too many rows to convert.")
                                    }
                                    rows++
                                    onRow(cells.toList())
                                }
                            }
                        }
                        "table:table" -> if (inTable) {
                            inTable = false
                            break
                        }
                    }
                }
            }
        }
        return rows
    }

    private fun addParagraph(
        target: MutableList<TextParagraph>,
        raw: String,
        heading: Int,
        bullet: Boolean
    ) {
        val content = raw.replace('\u00A0', ' ').trim()
        if (content.isEmpty()) return
        val style = when {
            heading >= 3 -> ParagraphStyle.H3
            heading == 2 -> ParagraphStyle.H2
            heading == 1 -> ParagraphStyle.H1
            bullet -> ParagraphStyle.BULLET
            else -> ParagraphStyle.BODY
        }
        target.add(TextParagraph(content, style))
        if (target.size > MAX_PARAGRAPHS) {
            throw ConversionException("The document has too many paragraphs to convert.")
        }
    }

    private fun repeatCount(parser: XmlPullParser, name: String): Int =
        OoxmlXml.attribute(parser, name)?.trim()?.toIntOrNull()?.coerceIn(1, 10_000) ?: 1
}
