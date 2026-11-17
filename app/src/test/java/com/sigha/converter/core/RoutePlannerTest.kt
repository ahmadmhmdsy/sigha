package com.sigha.converter.core

import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlannerTest {

    private val formats = FormatRegistry()

    private fun converter(id: String, vararg pairs: Pair<String, String>): Converter = object : Converter {
        override val id = id
        override val inputs: Set<FileFormat> = pairs.map { formats.get(it.first)!! }.toSet()
        override val outputs: Set<FileFormat> = pairs.map { formats.get(it.second)!! }.toSet()
        override suspend fun convert(request: ConversionRequest) =
            ConversionResult(listOf(request.outputFile))
    }

    @Test
    fun directEdgeIsFound() {
        val registry = ConverterRegistry(formats)
        registry.register(converter("img", "png" to "jpg"))
        val planner = RoutePlanner(registry)
        val route = planner.plan(formats.get("png")!!, formats.get("jpg")!!)
        assertEquals(1, route?.size)
        assertEquals("img", route?.get(0)?.id)
    }

    @Test
    fun twoHopChainIsDiscovered() {
        val registry = ConverterRegistry(formats)
        registry.register(converter("docx-to-pdf", "docx" to "pdf"))
        registry.register(converter("pdf-to-png", "pdf" to "png"))
        val planner = RoutePlanner(registry)
        val route = planner.plan(formats.get("docx")!!, formats.get("png")!!)
        assertEquals(listOf("docx-to-pdf", "pdf-to-png"), route?.map { it.id })
    }

    @Test
    fun unreachableTargetReturnsNull() {
        val registry = ConverterRegistry(formats)
        registry.register(converter("img", "png" to "jpg"))
        val planner = RoutePlanner(registry)
        assertNull(planner.plan(formats.get("png")!!, formats.get("pdf")!!))
    }

    @Test
    fun hopLimitIsRespected() {
        val registry = ConverterRegistry(formats)
        registry.register(converter("a", "docx" to "pdf"))
        registry.register(converter("b", "pdf" to "png"))
        registry.register(converter("c", "png" to "jpg"))
        val planner = RoutePlanner(registry)
        assertNull(planner.plan(formats.get("docx")!!, formats.get("jpg")!!, maxHops = 2))
        assertTrue(planner.plan(formats.get("docx")!!, formats.get("jpg")!!, maxHops = 3)!!.size == 3)
    }
}
