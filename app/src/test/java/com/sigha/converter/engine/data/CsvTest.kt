package com.sigha.converter.engine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {

    @Test
    fun parsesPlainRows() {
        assertEquals(
            listOf(listOf("a", "b"), listOf("c", "d")),
            Csv.parse("a,b\nc,d\n")
        )
    }

    @Test
    fun parsesQuotedFields() {
        assertEquals(
            listOf(listOf("a,b", "say \"hi\"")),
            Csv.parse("\"a,b\",\"say \"\"hi\"\"\"\n")
        )
    }

    @Test
    fun parsesEmbeddedLineBreaks() {
        assertEquals(
            listOf(listOf("line1\nline2", "x")),
            Csv.parse("\"line1\nline2\",x\n")
        )
    }

    @Test
    fun handlesCrLfAndTrailingEmptyField() {
        assertEquals(listOf(listOf("a", "")), Csv.parse("a,\r\n"))
    }

    @Test
    fun keepsEmptyRowsAsSingleEmptyField() {
        assertEquals(listOf(listOf("")), Csv.parse("\n"))
    }

    @Test
    fun escapesOnlyWhenNeeded() {
        assertEquals("plain", Csv.escape("plain"))
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
    }

    @Test
    fun roundTripsEscapedRows() {
        val row = listOf("a,b", "with \"quotes\"", "multi\nline", "plain")
        val text = buildString { Csv.appendRow(this, row, ',', "\n") }
        assertEquals(listOf(row), Csv.parse(text))
    }

    @Test
    fun parsesTabDelimitedInput() {
        assertEquals(listOf(listOf("a", "b\tc")), Csv.parse("a\t\"b\tc\"\n", '\t'))
    }

    @Test
    fun keepsSurroundingSpacesInUnquotedFields() {
        assertEquals(listOf(listOf("  x  ", "y")), Csv.parse("  x  ,y\n"))
    }

    @Test
    fun parsesEmptyInputToNoRows() {
        assertTrue(Csv.parse("").isEmpty())
    }

    @Test
    fun stripsLeadingByteOrderMark() {
        assertEquals(listOf(listOf("name", "age")), Csv.parse("\uFEFFname,age\n"))
    }
}
