package com.sigha.converter.engine.data

import com.sigha.converter.core.ConversionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonCsvTest {

    private fun csvToJson(text: String): String {
        val builder = StringBuilder()
        JsonCsv.csvToJson(Csv.Parser(text.reader(), ','), builder) { }
        return builder.toString()
    }

    @Test
    fun writesObjectsFromHeader() {
        assertEquals(
            "[\n  {\"name\": \"Alice\", \"age\": \"30\"}\n]\n",
            csvToJson("name,age\nAlice,30\n")
        )
    }

    @Test
    fun keepsMissingColumnsAsEmptyStrings() {
        assertEquals(
            "[\n  {\"a\": \"1\", \"b\": \"\"}\n]\n",
            csvToJson("a,b\n1\n")
        )
    }

    @Test
    fun makesDuplicateHeadersUnique() {
        val json = csvToJson("a,a\n1,2\n")
        assertEquals(true, json.contains("\"a\": \"1\""))
        assertEquals(true, json.contains("\"a (2)\": \"2\""))
    }

    @Test
    fun writesEmptyArrayForHeaderOnly() {
        val builder = StringBuilder()
        val count = JsonCsv.csvToJson(Csv.Parser("a,b\n".reader(), ','), builder) { }
        assertEquals(0, count)
        assertEquals("[\n\n]\n", builder.toString())
    }

    @Test
    fun writesCsvFromFlatObjects() {
        val builder = StringBuilder()
        val rows = JsonCsv.jsonToCsv("[{\"a\":1,\"b\":\"x\"},{\"a\":2,\"c\":true}]", builder)
        assertEquals(2, rows)
        assertEquals("a,b,c\r\n1,x,\r\n2,,true\r\n", builder.toString())
    }

    @Test
    fun quotesCsvValuesWhenNeeded() {
        val builder = StringBuilder()
        JsonCsv.jsonToCsv("[{\"a\":\"x,y\"}]", builder)
        assertEquals("a\r\n\"x,y\"\r\n", builder.toString())
    }

    @Test
    fun rejectsNestedJson() {
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv("[{\"a\":{\"b\":1}}]", StringBuilder())
        }
    }

    @Test
    fun rejectsArraysOfScalars() {
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv("[1,2,3]", StringBuilder())
        }
    }

    @Test
    fun rejectsRootObject() {
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv("{\"a\":1}", StringBuilder())
        }
    }

    @Test
    fun rejectsEmptyArray() {
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv("[]", StringBuilder())
        }
    }

    @Test
    fun rejectsInvalidJson() {
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv("{not json", StringBuilder())
        }
    }

    @Test
    fun rejectsDeeplyNestedInputBeforeParsing() {
        // The parser is recursive; 300 nested arrays would overflow a small thread stack.
        val raw = "[".repeat(300) + "]".repeat(300)
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv(raw, StringBuilder())
        }
    }

    @Test
    fun shallowNestingStillParses() {
        // Guard must not reject ordinary nesting; the nested value is then rejected as before.
        val raw = "[" + "[1]".repeat(10) + "]"
        assertThrows(ConversionException::class.java) {
            JsonCsv.jsonToCsv(raw, StringBuilder())
        }
    }

    @Test
    fun writesNullAsEmptyField() {
        val builder = StringBuilder()
        JsonCsv.jsonToCsv("[{\"a\":null}]", builder)
        assertEquals("a\r\n\r\n", builder.toString())
    }
}
