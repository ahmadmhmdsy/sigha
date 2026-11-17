package com.sigha.converter.core

/** Broad families used for grouping the format picker and for routing hints. */
enum class FormatCategory {
    IMAGE, PDF, DOCUMENT, SPREADSHEET, PRESENTATION, TEXT, AUDIO, VIDEO,
    ARCHIVE, DATA, SUBTITLE, EBOOK, AI
}

/**
 * One known file format. Pure data - no Android dependencies, so it is unit-testable.
 */
data class FileFormat(
    val id: String,
    val extension: String,
    val mime: String,
    val label: String,
    val category: FormatCategory
) {
    override fun toString(): String = id
}
