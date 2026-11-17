package com.sigha.converter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackCatalogTest {

    @Test
    fun idsAreUniqueAndOcrIsListed() {
        val ids = PackCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.contains(ModelPacks.OCR_PACK_ID))
    }

    @Test
    fun unknownPackIsNull() {
        assertNull(PackCatalog.byId("missing"))
    }

    @Test
    fun ocrPackShipsEnglishAndArabicIntoATessdataFolder() {
        val pack = PackCatalog.OCR
        assertEquals(
            setOf("eng.traineddata", "ara.traineddata"),
            pack.files.map { it.fileName }.toSet()
        )
        assertEquals("tessdata", pack.subdirectory)
        assertEquals("Apache-2.0", pack.license)
        assertTrue(pack.licenseUrl.startsWith("https://"))
    }

    @Test
    fun sizesAreTheSumOfTheirFiles() {
        for (pack in PackCatalog.ALL) {
            assertEquals(pack.files.sumOf { it.sizeBytes }, pack.sizeBytes)
            assertEquals(pack.sizeBytes, pack.info().sizeBytes)
            assertTrue(pack.sizeBytes > 0L)
        }
    }

    @Test
    fun everyFileIsPinnedHttpsWithALowercaseDigest() {
        val digest = Regex("^[0-9a-f]{64}$")
        for (pack in PackCatalog.ALL) {
            for (file in pack.files) {
                assertTrue(file.url, file.url.startsWith("https://"))
                assertTrue(file.fileName, digest.matches(file.sha256))
                assertTrue(file.fileName, file.sizeBytes > 0L)
                assertTrue(file.fileName, file.fileName.endsWith(".traineddata"))
            }
        }
    }

    @Test
    fun urlsStayPinnedToTheReviewedCommit() {
        val pinned =
            "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/" +
                "e12c65a915945e4c28e237a9b52bc4a8f39a0cec/"
        for (file in PackCatalog.OCR.files) {
            assertTrue(file.url, file.url.startsWith(pinned))
        }
    }
}
