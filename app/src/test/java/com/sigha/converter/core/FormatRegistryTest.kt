package com.sigha.converter.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogue is the single source of truth that every engine family resolves by id, so a
 * duplicate or blank id is a wiring defect, not a cosmetic one: it used to crash app start
 * through EngineFormats/ocrConverters before those lookups became defensive.
 */
class FormatRegistryTest {

    @Test
    fun everyCatalogueFormatRoundTripsById() {
        val registry = FormatRegistry()
        val ids = FormatRegistry.DATA.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue(id.isNotBlank())
            assertNotNull(registry.get(id))
        }
    }

    @Test
    fun extensionsAreUniqueAndNormalised() {
        val extensions = FormatRegistry.DATA.map { it.extension }
        assertEquals(extensions.size, extensions.toSet().size)
        extensions.forEach { extension ->
            assertTrue(extension.isNotBlank())
            assertEquals(extension.lowercase(), extension)
            assertTrue(!extension.startsWith("."))
        }
    }

    @Test
    fun everyFormatHasALabelAndCategory() {
        FormatRegistry.DATA.forEach { format ->
            assertTrue(format.label.isNotBlank())
            assertNotNull(format.category)
        }
    }
}
