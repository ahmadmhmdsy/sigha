package com.sigha.converter.engine.text

import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatRegistry

/**
 * Resolves the shared [FileFormat] definitions by id.
 *
 * Converters only reference ids, so every engine family agrees on the same
 * formats without touching the registry wiring in AppGraph.
 */
internal object EngineFormats {
    private val registry = FormatRegistry()

    fun of(id: String): FileFormat =
        registry.get(id) ?: error("Unknown format id: " + id)
}
