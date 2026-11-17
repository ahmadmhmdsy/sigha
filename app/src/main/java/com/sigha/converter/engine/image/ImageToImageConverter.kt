package com.sigha.converter.engine.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.concurrent.CancellationException

/** image -> JPG / PNG / WebP: decode once, apply EXIF and the size limit, re-encode. */
internal class ImageToImageConverter(private val context: Context) : Converter {

    override val id: String = "image-to-image"
    override val inputs: Set<FileFormat> = ImageFormats.inputs
    override val outputs: Set<FileFormat> = ImageFormats.outputs

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            try {
                convertNow(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConversionException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw ConversionException(
                    "Not enough memory to convert this image. Try a smaller maximum dimension.",
                    e
                )
            } catch (e: Exception) {
                throw ConversionException(
                    "Could not convert this image. The file may be damaged or use a format this device cannot decode.",
                    e
                )
            }
        }

    private fun convertNow(request: ConversionRequest): ConversionResult {
        ImageDecoding.checkNotCancelled(request.isCancelled)
        request.onProgress(0.02f)
        val bitmap = ImageDecoding.decode(
            context = context,
            uri = request.input,
            formatId = request.from.id,
            maxDimension = request.options.maxDimension,
            isCancelled = request.isCancelled
        )
        try {
            ImageDecoding.checkNotCancelled(request.isCancelled)
            request.onProgress(0.7f)
            writeImage(bitmap, request)
            request.onProgress(1f)
            return ConversionResult(outputs = listOf(request.outputFile))
        } catch (e: Throwable) {
            request.outputFile.delete()
            throw e
        } finally {
            bitmap.recycle()
        }
    }

    private fun writeImage(bitmap: Bitmap, request: ConversionRequest) {
        val format = when (request.to.id) {
            "jpg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> webpFormat()
            else -> throw ConversionException("Images cannot be converted to " + request.to.label + ".")
        }
        val quality = request.options.quality.coerceIn(1, 100)
        // JPEG has no alpha channel: transparent pixels would be written as black, so paint them white.
        val source = if (format == Bitmap.CompressFormat.JPEG) flattenOnWhite(bitmap) else bitmap
        try {
            val output = request.outputFile
            output.parentFile?.let { parent ->
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw ConversionException("Could not create the output folder. Check the device's free storage.")
                }
            }
            FileOutputStream(output).use { stream ->
                if (!source.compress(format, quality, stream)) {
                    throw ConversionException("Could not write the converted image. The device may be out of storage.")
                }
            }
        } finally {
            if (source !== bitmap) source.recycle()
        }
    }

    /** WEBP is deprecated from API 30 on, where the lossy variant has an explicit constant. */
    @Suppress("DEPRECATION", "NewApi")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    private fun flattenOnWhite(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flattened
    }
}
