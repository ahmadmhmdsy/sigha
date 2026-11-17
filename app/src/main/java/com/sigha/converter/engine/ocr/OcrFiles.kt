package com.sigha.converter.engine.ocr

import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Note

/** Writes recognised text as UTF-8 and rejects empty results with a useful message. */
internal object OcrFiles {

    fun writeText(
        request: ConversionRequest,
        raw: String,
        emptyMessage: String,
        note: Note? = null
    ): ConversionResult {
        val text = OcrText.normalize(raw)
        if (text.isBlank()) throw ConversionException(emptyMessage)
        val output = request.outputFile
        output.parentFile?.mkdirs()
        try {
            output.writeText(text + "\n", Charsets.UTF_8)
        } catch (error: Exception) {
            throw ConversionException("Could not save the recognised text", error)
        }
        return ConversionResult(outputs = listOf(output), note = note)
    }
}
