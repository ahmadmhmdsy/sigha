package com.sigha.converter.engine.pdf

import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory

/** Formats the PDF engine understands; value-equal to the instances in FormatRegistry. */
internal object PdfFormats {
    val PDF = FileFormat("pdf", "pdf", "application/pdf", "PDF", FormatCategory.PDF)
    val PNG = FileFormat("png", "png", "image/png", "PNG", FormatCategory.IMAGE)
    val JPG = FileFormat("jpg", "jpg", "image/jpeg", "JPG", FormatCategory.IMAGE)
    val TXT = FileFormat("txt", "txt", "text/plain", "TXT", FormatCategory.TEXT)
}
