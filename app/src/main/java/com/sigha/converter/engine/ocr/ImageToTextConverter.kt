package com.sigha.converter.engine.ocr

import android.os.Build
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Image to text: decode once at a size Tesseract likes, recognise, write UTF-8.
 * Languages come from [ConversionRequest.options], defaulting to English.
 */
internal class ImageToTextConverter(
    inputFormats: Set<FileFormat>,
    outputFormat: FileFormat
) : Converter {

    override val id: String = "ocr.image-to-txt"
    override val inputs: Set<FileFormat> = inputFormats
    override val outputs: Set<FileFormat> = setOf(outputFormat)

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            val context = request.context
            if (request.from.id == HEIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                throw ConversionException("HEIC images need Android 9 or newer")
            }
            val language = OcrSupport.languageCode(context, request)
            val datapath = OcrSupport.datapath(context)
            OcrSupport.checkCancelled(request)
            request.onProgress(0.02f)
            val maxDimension = request.options.maxDimension?.takeIf { it > 0 }
                ?: BitmapLoader.DEFAULT_MAX_DIMENSION
            val bitmap = BitmapLoader.load(context, request.input, maxDimension)
            try {
                OcrSupport.checkCancelled(request)
                request.onProgress(0.25f)
                val text = OcrEngine(datapath, language).use { engine -> engine.recognize(bitmap) }
                OcrSupport.checkCancelled(request)
                request.onProgress(0.92f)
                val result = OcrFiles.writeText(request, text, EMPTY_MESSAGE)
                request.onProgress(1f)
                result
            } finally {
                bitmap.recycle()
            }
        }

    private companion object {
        const val HEIC = "heic"
        const val EMPTY_MESSAGE = "No text recognised in this image. Try a sharper, well-lit photo."
    }
}
