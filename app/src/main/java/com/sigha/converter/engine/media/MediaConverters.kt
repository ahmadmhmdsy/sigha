package com.sigha.converter.engine.media

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionOptions
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatRegistry

/**
 * FFmpeg backed audio and video conversion edges.
 *
 * Input Uris are copied to private cache files before FFmpegKit sees them because the native
 * engine needs real paths. Commands come from [FfmpegArgs], execution and progress from
 * [FfmpegRunner], so this family contributes audio extraction, audio conversion, video
 * conversion and GIF export to the routing graph.
 */
fun mediaConverters(app: Application): List<Converter> = listOf(
    AudioConverter(app),
    VideoConverter(app)
)

/** Media formats resolved from the shared catalogue; unknown ids are skipped, never faked. */
internal object MediaFormats {
    private val byId: Map<String, FileFormat> = FormatRegistry.DATA.associateBy { format -> format.id }

    val audioInputs: Set<FileFormat> = resolve(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr",
        "mp4", "mkv", "mov", "avi", "webm", "flv"
    )
    val audioOutputs: Set<FileFormat> = resolve("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
    val videoInputs: Set<FileFormat> = resolve("mp4", "mkv", "mov", "avi", "webm", "flv")
    val videoOutputs: Set<FileFormat> = resolve("mp4", "webm", "mkv", "gif")

    private fun resolve(vararg ids: String): Set<FileFormat> =
        ids.mapNotNull { id -> byId[id] }.toSet()
}

/** Maps the shared option bag onto the pure FFmpeg options, rejecting an impossible trim range. */
internal fun ConversionOptions.toMediaParams(): FfmpegArgs.MediaParams {
    val start = startMs?.takeIf { value -> value > 0L }
    val end = endMs?.takeIf { value -> value > 0L }
    return try {
        FfmpegArgs.MediaParams(
            startMs = start,
            endMs = end,
            bitrateKbps = bitrateKbps,
            maxDimension = maxDimension?.takeIf { value -> value > 0 }
        )
    } catch (e: IllegalArgumentException) {
        throw ConversionException("The trim range is invalid: the end time must come after the start time.", e)
    }
}
