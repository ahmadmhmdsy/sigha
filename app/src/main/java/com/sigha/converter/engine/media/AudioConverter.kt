package com.sigha.converter.engine.media

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import kotlinx.coroutines.CancellationException

/**
 * Converts audio files - and the audio track of video files - to mp3, m4a, aac, wav, flac, ogg
 * and opus with FFmpegKit. Video inputs make this converter the "extract audio" edge.
 */
internal class AudioConverter(app: Application) : Converter {

    private val runner = FfmpegRunner(app)

    override val id: String = "ffmpeg-audio"

    override val inputs: Set<FileFormat> = MediaFormats.audioInputs

    override val outputs: Set<FileFormat> = MediaFormats.audioOutputs

    override suspend fun convert(request: ConversionRequest): ConversionResult {
        val target = request.to.id
        if (target !in TARGET_IDS) {
            throw ConversionException("Converting to ${request.to.label} is not supported by the audio engine.")
        }
        val params = request.options.toMediaParams()
        return runner.withMedia(request) { media ->
            if (media.probe.hasAudio == false) {
                throw ConversionException(
                    "This file has no audio track that can be converted.",
                    kind = ConversionFailureKind.NO_AUDIO_TRACK
                )
            }
            if (request.isCancelled()) throw CancellationException("Conversion cancelled.")
            val plannedDuration = FfmpegArgs.plannedDurationMs(media.probe.durationMs, params)
            val result = runner.execute(
                request,
                FfmpegArgs.audioArgs(
                    inputPath = media.file.absolutePath,
                    outputPath = request.outputFile.absolutePath,
                    targetId = target,
                    params = params
                ),
                plannedDuration
            )
            runner.finish(result, request, request.to.label)
            ConversionResult(outputs = listOf(request.outputFile))
        }
    }

    private companion object {
        val TARGET_IDS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
    }
}
