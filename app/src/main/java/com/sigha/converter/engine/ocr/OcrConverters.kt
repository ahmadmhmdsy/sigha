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
    // A catalogue edit that drops or renames an id must not crash app start: the affected
    // OCR edge simply disappears from the planner until the ids agree again.
    val text = formats["txt"] ?: return emptyList()
    val images = listOf("jpg", "png", "webp", "bmp", "gif", "heic")
        .mapNotNull { formats[it] }
        .toSet()
    val pdf = formats["pdf"]
    return buildList {
        if (images.isNotEmpty()) add(ImageToTextConverter(images, text))
        if (pdf != null) add(PdfTextConverter(pdf, text))
    }
}
