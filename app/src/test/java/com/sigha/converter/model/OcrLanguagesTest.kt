package com.sigha.converter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrLanguagesTest {

    @Test
    fun blankRequestMeansEnglish() {
        assertEquals(listOf("eng"), OcrLanguages.requested(null))
        assertEquals(listOf("eng"), OcrLanguages.requested("   "))
    }

    @Test
    fun combinedRequestsKeepCatalogueOrder() {
        assertEquals(listOf("eng", "ara"), OcrLanguages.requested("ara+ENG"))
        assertEquals(listOf("eng", "ara"), OcrLanguages.requested(" ARA , eng "))
        assertEquals(listOf("ara"), OcrLanguages.requested("ara"))
    }

    @Test
    fun unknownLanguagesAreDropped() {
        assertEquals(emptyList<String>(), OcrLanguages.requested("fra"))
        assertEquals(listOf("eng"), OcrLanguages.requested("eng+fra"))
    }

    @Test
    fun tesseractCodeJoinsTheSelectedLanguages() {
        assertEquals("eng+ara", OcrLanguages.code(listOf("ara", "eng")))
        assertEquals("ara", OcrLanguages.code(listOf("ara")))
        assertEquals("", OcrLanguages.code(emptyList()))
        assertEquals("eng+ara", OcrLanguages.code(listOf("eng", "ara", "fra")))
    }

    @Test
    fun installedLanguagesComeFromTraineddataFiles() {
        assertEquals(
            listOf("eng", "ara"),
            OcrLanguages.fromFileNames(
                listOf("ara.traineddata", "ENG.TRAINEDDATA", "fra.traineddata", "notes.txt")
            )
        )
        assertEquals(emptyList<String>(), OcrLanguages.fromFileNames(emptyList()))
    }

    @Test
    fun theDefaultLanguageIsSupported() {
        assertTrue(OcrLanguages.DEFAULT in OcrLanguages.SUPPORTED)
    }
}
