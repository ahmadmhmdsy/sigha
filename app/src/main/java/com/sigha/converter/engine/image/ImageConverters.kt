package com.sigha.converter.engine.image

import android.app.Application
import com.sigha.converter.core.Converter

/**
 * Every edge this engine family contributes: image -> JPG/PNG/WebP and image -> PDF.
 * Decoding uses only platform codecs (BitmapFactory and ImageDecoder); no native library needed.
 */
fun imageConverters(app: Application): List<Converter> = listOf(
    ImageToImageConverter(app.applicationContext),
    ImageToPdfConverter(app.applicationContext)
)
