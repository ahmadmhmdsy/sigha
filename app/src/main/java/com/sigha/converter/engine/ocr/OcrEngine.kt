package com.sigha.converter.engine.ocr

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.sigha.converter.core.ConversionException
import java.io.File

/**
 * One initialised Tesseract instance. Loading language data costs a second or more, so one
 * instance recognises every page of a document and is then closed.
 *
 * Every native failure (missing library for the ABI, unreadable pack) becomes the same short
 * user message instead of a crash.
 */
internal class OcrEngine(datapath: File, language: String) : AutoCloseable {

    private val api: TessBaseAPI = try {
        TessBaseAPI()
    } catch (error: Throwable) {
        throw ConversionException(DEVICE_MESSAGE, error)
    }

    init {
        val started = try {
            // Tesseract appends "tessdata" itself; datapath is the pack root.
            api.init(datapath.absolutePath, language)
        } catch (error: Throwable) {
            runCatching { api.recycle() }
            throw ConversionException(DEVICE_MESSAGE, error)
        }
        if (!started) {
            runCatching { api.recycle() }
            throw ConversionException(DEVICE_MESSAGE)
        }
        try {
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
        } catch (error: Throwable) {
            runCatching { api.recycle() }
            throw ConversionException(DEVICE_MESSAGE, error)
        }
    }

    /** Recognises one software bitmap; the caller owns and recycles it. */
    fun recognize(bitmap: Bitmap): String = try {
        api.setImage(bitmap)
        api.getUTF8Text()?.trimEnd().orEmpty()
    } catch (error: Throwable) {
        throw ConversionException(DEVICE_MESSAGE, error)
    }

    override fun close() {
        runCatching { api.recycle() }
    }

    private companion object {
        const val DEVICE_MESSAGE = "OCR pack unavailable on this device"
    }
}
