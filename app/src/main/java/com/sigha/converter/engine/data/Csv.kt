package com.sigha.converter.engine.data

import java.io.Reader

/**
 * RFC 4180 delimited-text reader and writer.
 *
 * The parser is streaming (one row at a time, 64 KB source buffer provided by
 * the caller) and accepts quoted fields with embedded delimiters, quotes and
 * line breaks. Pure Kotlin, so it is unit-testable on the JVM.
 */
object Csv {

    const val BUFFER_SIZE = 64 * 1024
    private const val NONE = -2
    private const val QUOTE = '"'.code

    class Parser(private val source: Reader, private val delimiter: Char) {

        private var pushedBack: Int = NONE

        /** Returns the next row, or null at end of input. */
        fun readRow(): List<String>? {
            var current = readChar()
            if (current == -1) return null
            val row = ArrayList<String>()
            val field = StringBuilder()
            var quoted = false
            while (current != -1) {
                if (quoted) {
                    if (current == QUOTE) {
                        val next = readChar()
                        if (next == QUOTE) {
                            field.append('"')
                            current = readChar()
                        } else {
                            quoted = false
                            current = next
                        }
                    } else {
                        field.append(current.toChar())
                        current = readChar()
                    }
                    continue
                }
                when (current) {
                    QUOTE -> {
                        if (field.isEmpty()) quoted = true else field.append('"')
                        current = readChar()
                    }
                    delimiter.code -> {
                        row.add(field.toString())
                        field.setLength(0)
                        current = readChar()
                    }
                    '\r'.code -> {
                        val next = readChar()
                        if (next != '\n'.code && next != -1) pushedBack = next
                        row.add(field.toString())
                        return row
                    }
                    '\n'.code -> {
                        row.add(field.toString())
                        return row
                    }
                    else -> {
                        field.append(current.toChar())
                        current = readChar()
                    }
                }
            }
            row.add(field.toString())
            return row
        }

        private fun readChar(): Int {
            if (pushedBack != NONE) {
                val value = pushedBack
                pushedBack = NONE
                return value
            }
            return source.read()
        }
    }

    /** Parses a whole document; intended for tests and small inputs. */
    fun parse(text: String, delimiter: Char = ','): List<List<String>> {
        val parser = Parser(text.removePrefix("\uFEFF").reader(), delimiter)
        val rows = ArrayList<List<String>>()
        while (true) {
            val row = parser.readRow() ?: break
            rows.add(row)
        }
        return rows
    }

    fun escape(value: String, delimiter: Char = ','): String {
        val needsQuotes = value.any { character ->
            character == delimiter || character == '"' || character == '\n' || character == '\r'
        }
        if (!needsQuotes) return value
        val builder = StringBuilder(value.length + 8)
        builder.append('"')
        for (character in value) {
            if (character == '"') builder.append('"')
            builder.append(character)
        }
        builder.append('"')
        return builder.toString()
    }

    fun appendRow(
        target: Appendable,
        row: List<String>,
        delimiter: Char,
        lineEnding: String
    ) {
        for (index in row.indices) {
            if (index > 0) target.append(delimiter)
            target.append(escape(row[index], delimiter))
        }
        target.append(lineEnding)
    }
}
