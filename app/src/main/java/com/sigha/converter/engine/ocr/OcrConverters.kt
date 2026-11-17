package com.sigha.converter.engine.ocr

import android.app.Application
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatRegistry

/**
 * OCR edges: supported images and PDFs to plain text.
 *
 * The converters are always registered - the route stays visible so the picker can offer
 * "to TXT" - and a missing pack produces a short instruction instead of a generic failure.
 * Text recognition needs the downloadable OCR pack; see ModelPacks.
 */
@Suppress("UNUSED_PARAMETER")
fun ocrConverters(app: Application): List<Converter> {
    val formats = FormatRegistry.DATA.associateBy { it.id }
    fun format(id: String): FileFormat =
        formats[id] ?: error("Unknown format id: " + id)

    val text = format("txt")
    val images = listOf("jpg", "png", "webp", "bmp", "gif", "heic")
        .map { format(it) }
        .toSet()
    return listOf(
        ImageToTextConverter(images, text),
        PdfTextConverter(format("pdf"), text)
    )
}
