package com.sigha.converter.engine.media

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.CancellationException

/**
 * Converts video files to mp4, webm, mkv or an animated gif with FFmpegKit. MP4 and MKV prefer
 * hardware H.264 (MediaCodec) and retry with the LGPL MPEG-4 encoder when the device rejects it;
 * WebM always uses VP9 + Opus, and GIF adds the documented 30 second cap.
 */
internal class VideoConverter(app: Application) : Converter {

    private val runner = FfmpegRunner(app)

    override val id: String = "ffmpeg-video"

    override val inputs: Set<FileFormat> = MediaFormats.videoInputs

    override val outputs: Set<FileFormat> = MediaFormats.videoOutputs

    override suspend fun convert(request: ConversionRequest): ConversionResult {
        val target = request.to.id
        if (target !in TARGET_IDS) {
            throw ConversionException("Converting to ${request.to.label} is not supported by the video engine.")
        }
        val baseParams = request.options.toMediaParams()
        return runner.withMedia(request) { media ->
            if (media.probe.hasVideo == false) {
                throw ConversionException("This file has no video track that can be converted.")
            }
            if (request.isCancelled()) throw CancellationException("Conversion cancelled.")
            val needsEvenDimensions = media.probe.width?.let { width -> width % 2 != 0 } == true ||
                media.probe.height?.let { height -> height % 2 != 0 } == true
            val gifPlan = if (target == "gif") FfmpegArgs.gifPlan(media.probe.durationMs, baseParams) else null
            val params = (gifPlan?.params ?: baseParams).copy(evenDimensions = needsEvenDimensions)
            val plannedDuration = FfmpegArgs.plannedDurationMs(media.probe.durationMs, params)
                ?: if (target == "gif") FfmpegArgs.GIF_MAX_DURATION_MS else null
            val inputPath = media.file.absolutePath
            val outputPath = request.outputFile.absolutePath

            var note: String? = if (gifPlan?.capped == true) GIF_CAP_NOTE else null
            var result = runner.execute(
                request,
                FfmpegArgs.videoArgs(inputPath, outputPath, target, params),
                plannedDuration
            )
            if (!result.success && !result.cancelled && target in SOFTWARE_FALLBACK_TARGETS) {
                if (request.isCancelled()) throw CancellationException("Conversion cancelled.")
                request.outputFile.delete()
                result = runner.execute(
                    request,
                    FfmpegArgs.videoArgs(inputPath, outputPath, target, params, softwareEncoder = true),
                    plannedDuration
                )
                if (result.success) note = SOFTWARE_ENCODER_NOTE
            }
            runner.finish(result, request, request.to.label)
            ConversionResult(outputs = listOf(request.outputFile), note = note)
        }
    }

    private companion object {
        val TARGET_IDS = setOf("mp4", "webm", "mkv", "gif")
        val SOFTWARE_FALLBACK_TARGETS = setOf("mp4", "mkv")
        const val GIF_CAP_NOTE = "GIFs are limited to 30 seconds, so the rest of the video was skipped."
        const val SOFTWARE_ENCODER_NOTE =
            "Hardware video encoding was unavailable, so the software encoder was used."
    }
}
