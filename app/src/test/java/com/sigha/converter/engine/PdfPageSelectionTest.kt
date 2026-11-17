package com.sigha.converter.engine

import com.sigha.converter.engine.pdf.PageSpan
import com.sigha.converter.engine.pdf.pageOutputFile
import com.sigha.converter.engine.pdf.parsePageSpans
import com.sigha.converter.engine.pdf.resolvePageIndices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

/** Pure rules behind the PDF page selection and the per-page output naming; no Android classes. */
class PdfPageSelectionTest {

    @Test
    fun blankSelectionMeansEveryPage() {
        assertEquals(emptyList<PageSpan>(), parsePageSpans(null))
        assertEquals(emptyList<PageSpan>(), parsePageSpans("   "))
        assertEquals((1..4).toList(), resolvePageIndices(emptyList(), 4))
        assertEquals(emptyList<Int>(), resolvePageIndices(emptyList(), 0))
    }

    @Test
    fun parsesSinglePagesRangesAndOpenRanges() {
        assertEquals(
            listOf(PageSpan(1, 3), PageSpan(5, 5), PageSpan(8, null)),
            parsePageSpans("1-3, 5, 8-")
        )
        assertEquals(listOf(PageSpan(2, 2)), parsePageSpans("2"))
        assertEquals(listOf(PageSpan(1, 2)), parsePageSpans(" 1 - 2 "))
    }

    @Test
    fun ignoresEmptyTokensBetweenCommas() {
        assertEquals(listOf(PageSpan(1, 1), PageSpan(2, 2)), parsePageSpans("1,,2,"))
    }

    @Test
    fun rejectsMalformedSelections() {
        assertThrows(IllegalArgumentException::class.java) { parsePageSpans("0") }
        assertThrows(IllegalArgumentException::class.java) { parsePageSpans("-3") }
        assertThrows(IllegalArgumentException::class.java) { parsePageSpans("5-3") }
        assertThrows(IllegalArgumentException::class.java) { parsePageSpans("abc") }
        assertThrows(IllegalArgumentException::class.java) { parsePageSpans("1-2-3") }
    }

    @Test
    fun resolvesOpenRangesToTheDocumentEnd() {
        assertEquals(listOf(3, 4, 5), resolvePageIndices(parsePageSpans("3-"), 5))
        assertEquals(listOf(5, 6), resolvePageIndices(parsePageSpans("5-6"), 6))
    }

    @Test
    fun resolvesSortedAndDeduplicated() {
        assertEquals(listOf(3, 4, 5), resolvePageIndices(parsePageSpans("5,3-"), 5))
        assertEquals((1..5).toList(), resolvePageIndices(parsePageSpans("2,2,1-5"), 5))
    }

    @Test
    fun clampsRangesAndSkipsPagesPastTheEnd() {
        assertEquals(listOf(1, 2), resolvePageIndices(parsePageSpans("1-3"), 2))
        assertEquals(emptyList<Int>(), resolvePageIndices(parsePageSpans("20-30"), 5))
        assertEquals(listOf(5), resolvePageIndices(parsePageSpans("5-9"), 5))
    }

    @Test
    fun namesEveryPageNextToTheBaseFile() {
        assertEquals(
            File(File("/tmp/out"), "out-page3.png"),
            pageOutputFile(File(File("/tmp/out"), "out.png"), 3, "png")
        )
        assertEquals(
            File(File("/tmp/out"), "photo-page1.jpg"),
            pageOutputFile(File(File("/tmp/out"), "photo.jpg"), 1, "jpg")
        )
        assertEquals(File("scan-page2.png"), pageOutputFile(File("scan"), 2, "png"))
    }
}
