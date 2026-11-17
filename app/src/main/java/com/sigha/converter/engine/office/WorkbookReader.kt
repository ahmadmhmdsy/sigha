package com.sigha.converter.engine.office

import com.sigha.converter.core.ConversionException
import org.xmlpull.v1.XmlPullParser

/**
 * Streaming reader for the first worksheet of an XLSX workbook.
 *
 * Cell values come from cached values, so formulas show their last result.
 * Dates stay as Excel serial numbers because style-driven number formats are
 * not interpreted (the converter reports that in its note).
 */
internal object WorkbookReader {

    const val MAX_ROWS = 1_000_000
    const val MAX_COLUMNS = 16_384
    private const val MAX_SHARED_STRINGS = 500_000
    private const val MAX_SHARED_CHARS = 32 * 1024 * 1024

    data class Sheet(val names: List<String>, val path: String)

    fun locateFirstSheet(zip: SafePackage): Sheet {
        val names = ArrayList<String>()
        var firstId: String? = null
        zip.open("xl/workbook.xml")?.use { stream ->
            val parser = OoxmlXml.parser(stream)
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                    names.add(OoxmlXml.attribute(parser, "name") ?: ("Sheet " + (names.size + 1)))
                    if (firstId == null) {
                        firstId = OoxmlXml.attribute(parser, "r:id")
                            ?: OoxmlXml.attribute(parser, "id")
                    }
                }
            }
        }
        if (firstId != null) {
            val target = relationshipTarget(zip, firstId)
            if (target != null) {
                val path = resolvePart("xl", target)
                if (zip.has(path)) return Sheet(names, path)
            }
        }
        val fallback = zip.names("xl/worksheets/", ".xml").minOrNull()
            ?: throw ConversionException("This spreadsheet has no worksheets.")
        return Sheet(names, fallback)
    }

    fun sharedStrings(zip: SafePackage): List<String> {
        val strings = ArrayList<String>()
        zip.open("xl/sharedStrings.xml")?.use { stream ->
            val parser = OoxmlXml.parser(stream)
            val builder = StringBuilder()
            var inItem = false
            var inText = false
            var chars = 0
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "si" -> {
                            inItem = true
                            builder.setLength(0)
                        }
                        "t" -> if (inItem) inText = true
                    }
                    XmlPullParser.TEXT -> if (inText) {
                        val value = parser.text ?: ""
                        chars += value.length
                        if (chars > MAX_SHARED_CHARS) {
                            throw ConversionException("The workbook has too much shared text to convert.")
                        }
                        builder.append(value)
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "t" -> inText = false
                        "si" -> if (inItem) {
                            if (strings.size >= MAX_SHARED_STRINGS) {
                                throw ConversionException("The workbook has too many shared strings to convert.")
                            }
                            strings.add(builder.toString())
                            inItem = false
                        }
                    }
                }
            }
        }
        return strings
    }

    /** Streams rows; returns the number of non-empty rows. */
    fun forEachRow(
        zip: SafePackage,
        path: String,
        shared: List<String>,
        onRow: (List<String>) -> Unit
    ): Int {
        val stream = zip.open(path)
            ?: throw ConversionException("The workbook is missing a worksheet part.")
        var rows = 0
        stream.use {
            val parser = OoxmlXml.parser(it)
            val cells = ArrayList<String>()
            val value = StringBuilder()
            var inRow = false
            var inCell = false
            var inValue = false
            var type: String? = null
            var column = 0
            var inSheetData = false
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "sheetData" -> inSheetData = true
                        "row" -> if (inSheetData) {
                            inRow = true
                            cells.clear()
                            column = 0
                        }
                        "c" -> if (inRow) {
                            inCell = true
                            inValue = false
                            type = OoxmlXml.attribute(parser, "t")
                            val reference = OoxmlXml.attribute(parser, "r")
                            if (reference != null) column = columnIndex(reference)
                            if (column >= MAX_COLUMNS) {
                                throw ConversionException("The worksheet has more than 16384 columns.")
                            }
                            while (cells.size <= column) cells.add("")
                            value.setLength(0)
                        }
                        "v", "t" -> if (inCell) inValue = true
                    }
                    XmlPullParser.TEXT -> if (inValue) {
                        value.append(parser.text ?: "")
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "v", "t" -> inValue = false
                        "c" -> if (inCell) {
                            val resolved = resolve(type, value.toString(), shared)
                            if (column < cells.size) cells[column] = resolved else cells.add(resolved)
                            column++
                            inCell = false
                            inValue = false
                            type = null
                        }
                        "row" -> if (inRow) {
                            inRow = false
                            while (cells.isNotEmpty() && cells[cells.size - 1].isEmpty()) {
                                cells.removeAt(cells.size - 1)
                            }
                            if (cells.isNotEmpty()) {
                                rows++
                                if (rows > MAX_ROWS) {
                                    throw ConversionException("The worksheet has too many rows to convert.")
                                }
                                onRow(cells.toList())
                            }
                        }
                        "sheetData" -> inSheetData = false
                    }
                }
            }
        }
        return rows
    }

    private fun resolve(type: String?, raw: String, shared: List<String>): String =
        when (type) {
            "s" -> {
                val index = raw.trim().toIntOrNull()
                if (index == null) "" else shared.getOrNull(index).orEmpty()
            }
            "b" -> if (raw.trim() == "1") "TRUE" else "FALSE"
            else -> raw
        }

    /** "AB12" becomes 27 (zero based). */
    private fun columnIndex(reference: String): Int {
        var result = 0
        var letters = 0
        for (character in reference) {
            if (character in 'A'..'Z') {
                result = result * 26 + (character - 'A' + 1)
                letters++
            } else if (character in 'a'..'z') {
                result = result * 26 + (character - 'a' + 1)
                letters++
            } else {
                break
            }
        }
        return if (letters == 0) 0 else result - 1
    }

    private fun relationshipTarget(zip: SafePackage, id: String): String? {
        val stream = zip.open("xl/_rels/workbook.xml.rels") ?: return null
        var target: String? = null
        stream.use {
            val parser = OoxmlXml.parser(it)
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    if (OoxmlXml.attribute(parser, "Id") == id) {
                        target = OoxmlXml.attribute(parser, "Target")
                        if (target != null) break
                    }
                }
            }
        }
        return target
    }

    private fun resolvePart(base: String, target: String): String {
        val raw = if (target.startsWith("/")) {
            target.trimStart('/')
        } else {
            base + "/" + target
        }
        val segments = ArrayList<String>()
        for (segment in raw.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(segment)
            }
        }
        return segments.joinToString("/")
    }
}
