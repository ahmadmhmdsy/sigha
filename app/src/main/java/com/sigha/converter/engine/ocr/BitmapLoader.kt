package com.sigha.converter.engine.ocr

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.sigha.converter.core.ConversionException
import java.io.IOException

/**
 * Decodes an image URI into a software ARGB_8888 bitmap that Tesseract can read.
 *
 * The platform decoder (API 28+) handles HEIC and EXIF rotation for us; on API 26-27 the app
 * still supports the common formats through BitmapFactory plus an EXIF pass. Bitmaps are
 * bounded so a 108 MP photo cannot exhaust the heap during OCR.
 */
internal object BitmapLoader {

    const val DEFAULT_MAX_DIMENSION: Int = 3000

    /** Hard ceiling so a hostile option can never ask for a heap-sized bitmap. */
    private const val MAX_DIMENSION: Int = 8192

    fun load(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val limit = maxDimension.coerceIn(64, MAX_DIMENSION)
        return try {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(context, uri, limit)
            } else {
                decodeWithBitmapFactory(context, uri, limit)
            }
            ensureArgb(decoded)
        } catch (error: ConversionException) {
            throw error
        } catch (error: OutOfMemoryError) {
            throw ConversionException("This image is too large to process", error)
        } catch (error: IOException) {
            throw ConversionException("Could not read this image", error)
        } catch (error: Exception) {
            throw ConversionException("Could not read this image", error)
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Tesseract reads pixels through native code, so a hardware bitmap is not usable.
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            decoder.setMutableRequired(false)
            val width = info.size.width
            val height = info.size.height
            val longest = maxOf(width, height)
            if (longest > maxDimension && longest > 0) {
                val scale = maxDimension.toFloat() / longest
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw ConversionException("Could not open this image")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ConversionException("This image format is not supported on this device")
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw ConversionException("Could not open this image")
        val scaled = scaleDown(decoded, maxDimension)
        return applyExifRotation(context, uri, scaled)
    }

    /** Keeps the decoded bitmap close to the target without holding a huge intermediate. */
    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        val longest = maxOf(width, height)
        var sample = 1
        while (longest / sample > maxDimension + maxDimension / 2) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    private fun ensureArgb(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.ARGB_8888) return bitmap
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return bitmap
        bitmap.recycle()
        return copy
    }
}
