package com.sigha.converter.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UniqueNamesTest {

    @Test
    fun keepsNameWhenFree() {
        assertEquals("report.pdf", UniqueNames.unique("report.pdf") { false })
    }

    @Test
    fun appendsCounterBeforeExtension() {
        val taken = mutableSetOf("report.pdf")
        assertEquals("report (1).pdf", UniqueNames.unique("report.pdf") { it in taken })
        taken += "report (1).pdf"
        assertEquals("report (2).pdf", UniqueNames.unique("report.pdf") { it in taken })
    }

    @Test
    fun handlesNamesWithoutExtension() {
        val taken = setOf("notes", "notes (1)")
        assertEquals("notes (2)", UniqueNames.unique("notes") { it in taken })
    }

    @Test
    fun trimsWhitespace() {
        assertEquals("report.pdf", UniqueNames.unique("  report.pdf  ") { false })
    }

    @Test
    fun blankNameFallsBackToFile() {
        assertEquals("file", UniqueNames.unique("   ") { false })
        assertEquals("file (1)", UniqueNames.unique("") { it == "file" })
    }

    @Test
    fun manyCollisionsTerminate() {
        val taken = (1 until 500).map { index -> "photo (" + index + ").jpg" }.toSet() + "photo.jpg"
        assertEquals("photo (500).jpg", UniqueNames.unique("photo.jpg") { it in taken })
    }

    @Test
    fun dotFileIsTreatedAsPlainName() {
        val taken = setOf(".env")
        assertEquals(".env (1)", UniqueNames.unique(".env") { it in taken })
    }
}
