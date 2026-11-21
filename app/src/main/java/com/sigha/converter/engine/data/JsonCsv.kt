package com.sigha.converter.engine.data

import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionFailureKind
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * CSV to JSON and JSON to CSV.
 *
 * CSV to JSON treats the first row as the header and writes every value as a
 * JSON string (CSV carries no type information). JSON to CSV only accepts a
 * flat array of objects; nested values fail with a clear message instead of
 * silently flattening or dropping data.
 */
internal object JsonCsv {

    const val MAX_ROWS = 1_000_000
    private const val MAX_NESTING = 256

    fun csvToJson(parser: Csv.Parser, target: Appendable, onRow: (Int) -> Unit): Int {
        val header = parser.readRow()
        if (header == null) {
            target.append("[]\n")
            return 0
        }
        val keys = uniqueKeys(header)
        target.append("[\n")
        var count = 0
        var first = true
        while (true) {
            val row = parser.readRow() ?: break
            if (row.size == 1 && row[0].isEmpty()) continue
            if (!first) target.append(",\n")
            first = false
            target.append("  {")
            for (index in keys.indices) {
                if (index > 0) target.append(", ")
                target.append(JsonPrimitive(keys[index]).toString())
                target.append(": ")
                val value = if (index < row.size) row[index] else ""
                target.append(JsonPrimitive(value).toString())
            }
            target.append('}')
            count++
            onRow(count)
            if (count > MAX_ROWS) {
                throw ConversionException("The CSV file has too many rows for JSON export.")
            }
        }
        target.append("\n]\n")
        return count
    }

    fun jsonToCsv(text: String, target: Appendable): Int {
        requireReasonableNesting(text)
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            throw ConversionException("The file is not valid JSON.")
        } catch (_: IllegalArgumentException) {
            throw ConversionException("The file is not valid JSON.")
        } catch (_: StackOverflowError) {
            // Hostile nesting makes the recursive parser overflow the thread stack; report it
            // as a too-large input instead of letting an Error escape the engine.
            throw ConversionException(
                "The JSON file is nested too deeply.",
                kind = ConversionFailureKind.FILE_TOO_LARGE
            )
        }
        val array = root as? JsonArray
            ?: throw ConversionException("Only a JSON array of objects can be converted to CSV.")
        if (array.isEmpty()) {
            throw ConversionException("The JSON array is empty; there is nothing to convert.")
        }
        val headers = LinkedHashSet<String>()
        val rows = ArrayList<Map<String, String>>(array.size)
        for (element in array) {
            val row = readRow(element, headers)
            rows.add(row)
            if (rows.size > MAX_ROWS) {
                throw ConversionException("The JSON file has too many objects for CSV export.")
            }
        }
        if (headers.isEmpty()) {
            throw ConversionException("The JSON objects have no fields.")
        }
        Csv.appendRow(target, headers.toList(), ',', "\r\n")
        for (row in rows) {
            val values = ArrayList<String>(headers.size)
            for (header in headers) values.add(row[header].orEmpty())
            Csv.appendRow(target, values, ',', "\r\n")
        }
        return rows.size
    }

    /**
     * Counts the deepest []/{} nesting before parsing. The kotlinx parser is recursive, so a
     * file made of thousands of nested arrays would otherwise overflow the stack inside a
     * conversion (and, on old devices, look like a crash rather than a rejected file).
     */
    private fun requireReasonableNesting(text: String) {
        var depth = 0
        var maxDepth = 0
        var inString = false
        var escaped = false
        for (character in text) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{', '[' -> {
                    depth++
                    if (depth > maxDepth) maxDepth = depth
                    if (maxDepth > MAX_NESTING) {
                        throw ConversionException(
                            "The JSON file is nested too deeply (limit " + MAX_NESTING + " levels).",
                            kind = ConversionFailureKind.FILE_TOO_LARGE
                        )
                    }
                }
                '}', ']' -> if (depth > 0) depth--
            }
        }
    }

    private fun readRow(element: JsonElement, headers: MutableSet<String>): Map<String, String> {
        val obj = element as? JsonObject
            ?: throw ConversionException(
                "Only a JSON array of objects can be converted to CSV; the array contains " +
                    describe(element) + "."
            )
        val row = LinkedHashMap<String, String>(obj.size)
        for ((key, value) in obj) {
            row[key] = when (value) {
                is JsonNull -> ""
                is JsonPrimitive -> value.content
                is JsonArray, is JsonObject -> throw ConversionException(
                    "The JSON value for \"" + key + "\" is nested; only flat values can be converted to CSV."
                )
                else -> ""
            }
            headers.add(key)
        }
        return row
    }

    private fun describe(element: JsonElement): String = when (element) {
        is JsonArray -> "an array"
        is JsonObject -> "an object"
        is JsonNull -> "null"
        is JsonPrimitive -> if (element.isString) "a string" else "a number or boolean"
        else -> "an unsupported value"
    }

    private fun uniqueKeys(header: List<String>): List<String> {
        val seen = HashMap<String, Int>()
        val keys = ArrayList<String>(header.size)
        for (raw in header) {
            val base = raw.ifEmpty { "column" }
            val count = (seen[base] ?: 0) + 1
            seen[base] = count
            keys.add(if (count == 1) base else base + " (" + count + ")")
        }
        return keys
    }
}
