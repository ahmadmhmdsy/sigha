package com.sigha.converter.engine

import android.app.Application
import com.sigha.converter.core.Converter
import com.sigha.converter.engine.archive.archiveConverters
import com.sigha.converter.engine.data.dataConverters
import com.sigha.converter.engine.ebook.ebookConverters
import com.sigha.converter.engine.image.imageConverters
import com.sigha.converter.engine.media.mediaConverters
import com.sigha.converter.engine.ocr.ocrConverters
import com.sigha.converter.engine.office.officeConverters
import com.sigha.converter.engine.pdf.pdfConverters
import com.sigha.converter.engine.subtitle.subtitleConverters
import com.sigha.converter.engine.text.textConverters
import com.sigha.converter.engine.upscale.upscaleConverters

/**
 * Single wiring point for every engine family. Each family contributes its own list; this
 * file is owned by the Lead so parallel work never edits the same lines.
 *
 * Order matters only for readability: the registry keys converters by source format and
 * the route planner picks the cheapest path regardless of registration order.
 */
fun buildBuiltInConverters(app: Application): List<Converter> = buildList {
    addAll(imageConverters(app))
    addAll(pdfConverters(app))
    addAll(officeConverters(app))
    addAll(textConverters(app))
    addAll(dataConverters(app))
    addAll(subtitleConverters(app))
    addAll(ebookConverters(app))
    addAll(mediaConverters(app))
    addAll(ocrConverters(app))
    addAll(archiveConverters(app))
    addAll(upscaleConverters(app))
}
