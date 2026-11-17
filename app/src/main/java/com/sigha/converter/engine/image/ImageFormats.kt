package com.sigha.converter.engine.image

import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory

/**
 * Image formats this engine understands, kept in one place so the converters and the decoder
 * agree on ids. The instances are value-equal to the ones in FormatRegistry.
 */
internal object ImageFormats {
    val JPG = FileFormat("jpg", "jpg", "image/jpeg", "JPG", FormatCategory.IMAGE)
    val PNG = FileFormat("png", "png", "image/png", "PNG", FormatCategory.IMAGE)
    val WEBP = FileFormat("webp", "webp", "image/webp", "WebP", FormatCategory.IMAGE)
    val BMP = FileFormat("bmp", "bmp", "image/bmp", "BMP", FormatCategory.IMAGE)
    val GIF = FileFormat("gif", "gif", "image/gif", "GIF", FormatCategory.IMAGE)
    val HEIC = FileFormat("heic", "heic", "image/heic", "HEIC", FormatCategory.IMAGE)
    val AVIF = FileFormat("avif", "avif", "image/avif", "AVIF", FormatCategory.IMAGE)
    val PDF = FileFormat("pdf", "pdf", "application/pdf", "PDF", FormatCategory.PDF)

    /** Every image the decoder accepts. HEIC needs API 28, AVIF needs API 31; see ImageDecoding. */
    val inputs: Set<FileFormat> = linkedSetOf(JPG, PNG, WEBP, BMP, GIF, HEIC, AVIF)

    /** Targets Bitmap.compress can write for all of the inputs above. */
    val outputs: Set<FileFormat> = linkedSetOf(JPG, PNG, WEBP)
}
