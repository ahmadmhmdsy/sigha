package com.sigha.converter.engine.ocr

import android.content.Context
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.model.ModelPacks
import com.sigha.converter.model.OcrLanguages
import java.io.File
import kotlinx.coroutines.CancellationException

/** Language and data-path handling shared by the OCR converters. */
internal object OcrSupport {

    const val INSTALL_MESSAGE = "Install the OCR pack (eng/ara) first"

    private const val UNSUPPORTED_MESSAGE =
        "Unsupported OCR language. Choose English, Arabic, or both."

    /**
     * Validates the request against the installed pack and returns Tesseract's language
     * string ("eng", "ara" or "eng+ara").
     */
    fun languageCode(context: Context, request: ConversionRequest): String {
        val requested = OcrLanguages.requested(request.options.ocrLanguage)
        if (requested.isEmpty()) throw ConversionException(UNSUPPORTED_MESSAGE)
        val installed = ModelPacks.installedOcrLanguages(context)
        if (!installed.containsAll(requested)) throw ConversionException(INSTALL_MESSAGE)
        return OcrLanguages.code(requested)
    }

    /** Tesseract data path: the pack root, whose `tessdata` child holds the language files. */
    fun datapath(context: Context): File {
        val dir = ModelPacks.installDir(context, ModelPacks.OCR_PACK_ID)
            ?: throw ConversionException(INSTALL_MESSAGE)
        if (!File(dir, "tessdata").isDirectory) throw ConversionException(INSTALL_MESSAGE)
        return dir
    }

    /** Honours the job's cancellation flag; the runner rethrows this as a cancelled run. */
    fun checkCancelled(request: ConversionRequest) {
        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
    }
}
