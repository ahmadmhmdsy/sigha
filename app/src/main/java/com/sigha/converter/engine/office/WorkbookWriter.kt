package com.sigha.converter.engine.office

import com.sigha.converter.core.ConversionException
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, valid XLSX workbook from a sequence of rows.
 *
 * The first row becomes the header row. Values that look like plain numbers
 * are written as numbers; everything else uses inline strings, so no shared
 * string table or style list is needed.
 */
internal object WorkbookWriter {

    private const val MAX_ROWS = 1_048_576L
    private const val MAX_COLUMNS = 16_384

    fun write(
        output: File,
        rows: Sequence<List<String>>,
        isCancelled: () -> Boolean,
        onRow: (Int) -> Unit
    ): Int {
        output.parentFile?.mkdirs()
        var rowNumber = 0
        BufferedOutputStream(FileOutputStream(output), OfficeIo.BUFFER_SIZE).use { fileOut ->
            ZipOutputStream(fileOut).use { zip ->
                writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
                writeEntry(zip, "_rels/.rels", ROOT_RELATIONSHIPS)
                writeEntry(zip, "xl/workbook.xml", WORKBOOK)
                writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELATIONSHIPS)
                writeEntry(zip, "xl/styles.xml", STYLES)
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                val writer = BufferedWriter(OutputStreamWriter(zip, Charsets.UTF_8), OfficeIo.BUFFER_SIZE)
                try {
                    writer.write(SHEET_HEADER)
                    for (row in rows) {
                        if (isCancelled()) throw CancellationException("Conversion cancelled")
                        rowNumber++
                        if (rowNumber > MAX_ROWS) {
                            throw ConversionException("The file has too many rows for an Excel workbook.")
                        }
                        if (row.size > MAX_COLUMNS) {
                            throw ConversionException("The file has more than 16384 columns.")
                        }
                        writer.write("<row r=\"")
                        writer.write(rowNumber.toString())
                        writer.write("\">")
                        for (index in row.indices) {
                            writeCell(writer, rowNumber, index, row[index])
                        }
                        writer.write("</row>")
                        if (rowNumber % 256 == 0) onRow(rowNumber)
                    }
                    writer.write(SHEET_FOOTER)
                    writer.flush()
                } finally {
                    runCatching { writer.flush() }
                }
                zip.closeEntry()
            }
        }
        if (rowNumber == 0) {
            throw ConversionException("The file has no rows to convert.")
        }
        return rowNumber
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeCell(writer: Writer, row: Int, column: Int, value: String) {
        val reference = columnName(column) + row
        if (value.isEmpty()) {
            writer.write("<c r=\"")
            writer.write(reference)
            writer.write("\"/>")
            return
        }
        if (isPlainNumber(value)) {
            writer.write("<c r=\"")
            writer.write(reference)
            writer.write("\"><v>")
            writer.write(value)
            writer.write("</v></c>")
            return
        }
        writer.write("<c r=\"")
        writer.write(reference)
        writer.write("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
        writer.write(OoxmlXml.escape(value))
        writer.write("</t></is></c>")
    }

    /** Positive or negative decimal with at most 15 digits, no leading zeros. */
    private fun isPlainNumber(value: String): Boolean {
        if (value.isEmpty() || value.length > 16) return false
        var index = 0
        if (value[0] == '-') index++
        if (index >= value.length) return false
        if (value[index] == '0' && index + 1 < value.length && value[index + 1] != '.') return false
        var digits = 0
        var dotSeen = false
        for (position in index until value.length) {
            val character = value[position]
            if (character == '.') {
                if (dotSeen) return false
                dotSeen = true
                continue
            }
            if (character !in '0'..'9') return false
            digits++
        }
        return digits in 1..15
    }

    private fun columnName(index: Int): String {
        var value = index
        val letters = StringBuilder(3)
        while (true) {
            letters.append(('A' + value % 26))
            value = value / 26 - 1
            if (value < 0) break
        }
        return letters.reverse().toString()
    }

    private const val SHEET_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<sheetData>"

    private const val SHEET_FOOTER = "</sheetData></worksheet>"

    private const val CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            "</Types>"

    private const val ROOT_RELATIONSHIPS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private const val WORKBOOK =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
            "</workbook>"

    private const val WORKBOOK_RELATIONSHIPS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"

    private const val STYLES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border/></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>" +
            "</styleSheet>"
}
