package com.sigha.converter.engine.subtitle

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.engine.text.EngineFormats
import com.sigha.converter.engine.text.TextIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Subtitle converters: SRT to WebVTT and back. */
fun subtitleConverters(app: Application): List<Converter> = listOf(
    SrtToVtt(),
    VttToSrt()
)

private class SrtToVtt : Converter {
    override val id: String = "subtitle-srt-to-vtt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("srt"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("vtt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val cues = SubtitleFormat.parseSrt(readInput(request))
            request.onProgress(0.6f)
            if (cues.isEmpty()) throw ConversionException("No subtitle cues were found in the file.")
            TextIo.writeText(request.outputFile, SubtitleFormat.formatVtt(cues))
            request.onProgress(1f)
            ConversionResult(listOf(request.outputFile))
        }
}

private class VttToSrt : Converter {
    override val id: String = "subtitle-vtt-to-srt"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("vtt"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("srt"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val parsed = SubtitleFormat.parseVtt(readInput(request))
            request.onProgress(0.6f)
            if (parsed.isEmpty()) throw ConversionException("No subtitle cues were found in the file.")
            val cues = parsed.mapNotNull { cue ->
                val lines = cue.lines
                    .map { SubtitleFormat.stripVttTags(it).trim() }
                    .filter { it.isNotEmpty() }
                if (lines.isEmpty()) null else SubtitleCue(cue.startMs, cue.endMs, lines)
            }
            if (cues.isEmpty()) throw ConversionException("The WebVTT file has no readable cue text.")
            TextIo.writeText(request.outputFile, SubtitleFormat.formatSrt(cues))
            request.onProgress(1f)
            ConversionResult(listOf(request.outputFile))
        }
}

private fun readInput(request: ConversionRequest): String {
    val stream = request.context.contentResolver.openInputStream(request.input)
        ?: throw ConversionException("Cannot read the selected file.")
    return stream.use { TextIo.readText(it) }
}
