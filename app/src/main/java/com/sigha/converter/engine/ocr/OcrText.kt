package com.sigha.converter.engine.ocr

/**
 * Cleans raw Tesseract output before it is written: one line ending, no trailing spaces and
 * no page-break control characters. Pure, so it is unit-testable without Android.
 */
internal object OcrText {

    fun normalize(raw: String): String = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace('\u000C', '\n')
        .lines()
        .joinToString("\n") { line -> line.trimEnd() }
        .trim()
}
