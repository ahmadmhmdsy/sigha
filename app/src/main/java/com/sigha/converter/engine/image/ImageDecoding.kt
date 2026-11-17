package com.sigha.converter.engine.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.sigha.converter.core.ConversionException
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decodes every supported image input into a software ARGB bitmap that is already upright
 * (EXIF orientation applied for JPEG) and already limited to the requested maximum dimension.
 *
 * BitmapFactory covers the formats the platform has always decoded; HEIC (API 28+) and AVIF
 * (API 31+) go through ImageDecoder because only it has those codecs. Encoded bytes are streamed
 * from the content resolver, never buffered whole in memory.
 */
internal object ImageDecoding {

    /** Bigger bitmaps cannot be converted on a phone; reject them before allocating. */
    private const val MAX_PIXELS = 100_000_000L

    /** Hard limit for the encoded input, checked when the content provider reports the size. */
    private const val MAX_BYTES = 200L * 1024L * 1024L

    fun decode(
        context: Context,
        uri: Uri,
        formatId: String,
        maxDimension: Int?,
        isCancelled: () -> Boolean
    ): Bitmap {
        rejectOversizedFile(context, uri)
        checkNotCancelled(isCancelled)
        return when (formatId) {
            "heic" -> decodeHeic(context, uri, maxDimension)
            "avif" -> decodeAvif(context, uri, maxDimension)
            else -> decodeWithBitmapFactory(context, uri, formatId, maxDimension, isCancelled)
        }
    }

    fun checkNotCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw CancellationException("Conversion cancelled.")
    }

    private fun decodeWithBitmapFactory(
        context: Context,
        uri: Uri,
        formatId: String,
        maxDimension: Int?,
        isCancelled: () -> Boolean
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openStream(context, uri) ?: throw cannotRead()
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) throw cannotRead()
        if (width.toLong() * height.toLong() > MAX_PIXELS) throw tooManyPixels()
        checkNotCancelled(isCancelled)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(width, height, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = openStream(context, uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw cannotRead()
        val upright = if (formatId == "jpg") applyExifOrientation(context, uri, decoded) else decoded
        return scaleToMaxDimension(upright, maxDimension)
    }

    private fun decodeHeic(context: Context, uri: Uri, maxDimension: Int?): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw ConversionException("HEIC images need Android 9 or newer. This device cannot read them.")
        }
        return decodeWithImageDecoder(context, uri, maxDimension)
    }

    private fun decodeAvif(context: Context, uri: Uri, maxDimension: Int?): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw ConversionException("AVIF images need Android 12 or newer. This device cannot read them.")
        }
        return decodeWithImageDecoder(context, uri, maxDimension)
    }

    @Suppress("NewApi")
    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxDimension: Int?): Bitmap {
        var tooLarge = false
        var decoded: Bitmap? = null
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val size = info.size
                if (size.width <= 0 || size.height <= 0) return@decodeBitmap
                if (size.width.toLong() * size.height.toLong() > MAX_PIXELS) {
                    tooLarge = true
                    // Shrink to one pixel so the bitmap we are about to reject is never allocated.
                    decoder.setTargetSize(1, 1)
                } else if (maxDimension != null && maxDimension > 0) {
                    val longest = max(size.width, size.height)
                    if (longest > maxDimension) {
                        val ratio = maxDimension.toFloat() / longest
                        decoder.setTargetSize(
                            max(1, (size.width * ratio).roundToInt()),
                            max(1, (size.height * ratio).roundToInt())
                        )
                    }
                }
            }
        } catch (e: IOException) {
            throw cannotRead(e)
        } catch (e: Exception) {
            throw cannotRead(e)
        }
        if (tooLarge) {
            decoded?.recycle()
            throw tooManyPixels()
        }
        return decoded ?: throw cannotRead()
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val matrix = orientationMatrix(readExifOrientation(context, uri)) ?: return bitmap
        val upright = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            throw e
        } catch (_: Exception) {
            return bitmap
        }
        if (upright !== bitmap) bitmap.recycle()
        return upright
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                return ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            // Some providers hand out a pipe; the stream fallback below handles those.
        }
        return try {
            val stream = openStream(context, uri) ?: return ExifInterface.ORIENTATION_NORMAL
            stream.use { raw ->
                val buffered = if (raw.markSupported()) raw else BufferedInputStream(raw)
                ExifInterface(buffered).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply {
            setRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
            setRotate(90f)
            postScale(1f, -1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(270f) }
        else -> null
    }

    /**
     * Largest power of two that keeps the decoded bitmap at or above [maxDimension] on its long
     * side, so the exact scale afterwards only ever shrinks by less than half.
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int?): Int {
        if (maxDimension == null || maxDimension <= 0) return 1
        val longest = max(width, height)
        var sample = 1
        while (longest / (sample * 2) >= maxDimension) sample *= 2
        return sample
    }

    /** Scales the long side down to [maxDimension]; never upscales. */
    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int?): Bitmap {
        if (maxDimension == null || maxDimension <= 0) return bitmap
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        val targetWidth = max(1, (bitmap.width * ratio).roundToInt())
        val targetHeight = max(1, (bitmap.height * ratio).roundToInt())
        if (targetWidth == bitmap.width && targetHeight == bitmap.height) return bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun rejectOversizedFile(context: Context, uri: Uri) {
        val size = queryFileSize(context, uri) ?: return
        if (size > MAX_BYTES) {
            throw ConversionException("This image is too large to convert. The limit is 200 MB.")
        }
    }

    private fun queryFileSize(context: Context, uri: Uri): Long? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || !cursor.moveToFirst() || cursor.isNull(index)) null else cursor.getLong(index)
        }
    } catch (_: Exception) {
        null
    }

    private fun openStream(context: Context, uri: Uri): InputStream? = try {
        context.contentResolver.openInputStream(uri)
    } catch (_: Exception) {
        null
    }

    private fun tooManyPixels(): ConversionException =
        ConversionException("This image is too large to convert. The limit is 100 megapixels.")

    private fun cannotRead(cause: Throwable? = null): ConversionException =
        ConversionException(
            "Could not read this image. The file may be damaged or use a format this device cannot decode.",
            cause
        )
}
