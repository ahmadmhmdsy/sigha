package com.sigha.converter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrPageRangeTest {

    @Test
    fun noSpecReadsTheDefaultWindow() {
        val selection = OcrPageRange.select(null, 120)
        assertEquals((1..50).toList(), selection.pages)
        assertNotNull(selection.note)

        val short = OcrPageRange.select(null, 10)
        assertEquals((1..10).toList(), short.pages)
        assertNull(short.note)
    }

    @Test
    fun explicitRangesAndSinglesAreMerged() {
        val selection = OcrPageRange.select("1-3, 5", 10)
        assertEquals(listOf(1, 2, 3, 5), selection.pages)
        assertNull(selection.note)
    }

    @Test
    fun rangesAreClampedAndDeduplicated() {
        val selection = OcrPageRange.select("9-12,10", 10)
        assertEquals(listOf(9, 10), selection.pages)
    }

    @Test
    fun aRangePastTheEndSelectsNothingWithAnExplanation() {
        val selection = OcrPageRange.select("20-30", 10)
        assertTrue(selection.pages.isEmpty())
        assertNotNull(selection.note)
    }

    @Test
    fun unparseableInputFallsBackToTheDefaultWindow() {
        val selection = OcrPageRange.select("abc", 10)
        assertEquals((1..10).toList(), selection.pages)
        assertNotNull(selection.note)
        assertTrue(selection.note!!.contains("not understood"))
    }

    @Test
    fun theHardCeilingAppliesToExplicitSelections() {
        val selection = OcrPageRange.select("1-600", 1000)
        assertEquals(OcrPageRange.HARD_MAX_PAGES, selection.pages.size)
        assertEquals(1, selection.pages.first())
        assertEquals(OcrPageRange.HARD_MAX_PAGES, selection.pages.last())
        assertNotNull(selection.note)
    }

    @Test
    fun aDocumentWithoutPagesReadsNothing() {
        val selection = OcrPageRange.select(null, 0)
        assertTrue(selection.pages.isEmpty())
        assertNull(selection.note)
    }
}
