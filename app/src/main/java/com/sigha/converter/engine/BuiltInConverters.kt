package com.sigha.converter.engine

import android.app.Application
import android.util.Log
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
    // Each family is wired independently. A drifted format id inside one family used to throw
    // at Application.onCreate and take the whole app down; now only that family disappears
    // from the planner, the rest of the catalogue keeps working, and the log names the cause.
    fun family(name: String, build: () -> List<Converter>) {
        val converters = runCatching(build)
            .onFailure { Log.e("Sigha", "Converter family skipped: " + name, it) }
            .getOrDefault(emptyList())
        addAll(converters)
    }
    family("image") { imageConverters(app) }
    family("pdf") { pdfConverters(app) }
    family("office") { officeConverters(app) }
    family("text") { textConverters(app) }
    family("data") { dataConverters(app) }
    family("subtitle") { subtitleConverters(app) }
    family("ebook") { ebookConverters(app) }
    family("media") { mediaConverters(app) }
    family("ocr") { ocrConverters(app) }
    family("archive") { archiveConverters(app) }
    family("upscale") { upscaleConverters(app) }
}
