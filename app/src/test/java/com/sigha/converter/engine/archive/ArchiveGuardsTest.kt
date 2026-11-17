package com.sigha.converter.engine.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveGuardsTest {

    @Test
    fun keepsPlainRelativeNames() {
        assertEquals("folder/file.txt", ArchiveGuards.normalizeEntryName("folder/file.txt"))
        assertEquals("file.txt", ArchiveGuards.normalizeEntryName("file.txt"))
    }

    @Test
    fun rejectsParentTraversal() {
        assertNull(ArchiveGuards.normalizeEntryName("../evil.txt"))
        assertNull(ArchiveGuards.normalizeEntryName("a/../../evil.txt"))
        assertNull(ArchiveGuards.normalizeEntryName("a\\..\\evil.txt"))
        assertNull(ArchiveGuards.normalizeEntryName(".."))
        assertNull(ArchiveGuards.normalizeEntryName("a/b/.."))
    }

    @Test
    fun rejectsAbsolutePathsAndDriveLetters() {
        assertNull(ArchiveGuards.normalizeEntryName("/etc/passwd"))
        assertNull(ArchiveGuards.normalizeEntryName("C:\\Windows\\system32"))
        assertNull(ArchiveGuards.normalizeEntryName("\\\\server\\share\\file"))
    }

    @Test
    fun removesDotSegmentsAndRepeatedSeparators() {
        assertEquals("a/b", ArchiveGuards.normalizeEntryName("./a/./b"))
        assertEquals("a/b", ArchiveGuards.normalizeEntryName("a//b"))
        assertEquals("a/b", ArchiveGuards.normalizeEntryName("a\\b"))
    }

    @Test
    fun rejectsNamesWithoutUsableSegments() {
        assertNull(ArchiveGuards.normalizeEntryName(""))
        assertNull(ArchiveGuards.normalizeEntryName("/"))
        assertNull(ArchiveGuards.normalizeEntryName("."))
        assertNull(ArchiveGuards.normalizeEntryName("//"))
        assertNull(ArchiveGuards.normalizeEntryName("./"))
    }

    @Test
    fun rejectsOverlongNames() {
        val longSegment = "x".repeat(300)
        assertNull(ArchiveGuards.normalizeEntryName(longSegment))
        assertNull(ArchiveGuards.normalizeEntryName("folder/" + "y".repeat(5000)))
    }

    @Test
    fun checksSafetyPredicate() {
        assertTrue(ArchiveGuards.isSafeEntryName("ok/file.txt"))
        assertFalse(ArchiveGuards.isSafeEntryName("../bad"))
    }
}
