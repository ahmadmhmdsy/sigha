package com.sigha.converter.engine.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure FFmpeg command surface: codecs, containers, trim options, scaling, the GIF
 * cap and the absence of GPL encoders. Runs on the JVM - no Android, no FFmpegKit.
 */
class FfmpegArgsTest {

    private fun audioArgs(
        target: String,
        params: FfmpegArgs.MediaParams = FfmpegArgs.MediaParams()
    ): List<String> = FfmpegArgs.audioArgs(INPUT, outputPath(target), target, params).toList()

    private fun videoArgs(
        target: String,
        params: FfmpegArgs.MediaParams = FfmpegArgs.MediaParams(),
        software: Boolean = false
    ): List<String> = FfmpegArgs.videoArgs(INPUT, outputPath(target), target, params, software).toList()

    private fun outputPath(target: String): String = "/cache/media/out_$target.tmp"

    private fun List<String>.valueOf(option: String): String {
        val index = indexOf(option)
        assertTrue("Expected option $option in $this", index >= 0)
        return get(index + 1)
    }

    @Test
    fun `mp3 command uses the lame encoder at the default bitrate`() {
        assertEquals(
            listOf(
                "-y", "-i", INPUT, "-vn",
                "-c:a", "libmp3lame", "-b:a", "192k",
                "-map_metadata", "-1", "-map_chapters", "-1",
                "-f", "mp3", outputPath("mp3")
            ),
            audioArgs("mp3")
        )
    }

    @Test
    fun `audio targets use the right codecs and explicit containers`() {
        val m4a = audioArgs("m4a")
        assertEquals("aac", m4a.valueOf("-c:a"))
        assertEquals("128k", m4a.valueOf("-b:a"))
        assertEquals("ipod", m4a.valueOf("-f"))

        val aac = audioArgs("aac")
        assertEquals("aac", aac.valueOf("-c:a"))
        assertEquals("adts", aac.valueOf("-f"))

        val wav = audioArgs("wav")
        assertEquals("pcm_s16le", wav.valueOf("-c:a"))
        assertEquals("wav", wav.valueOf("-f"))
        assertFalse("-b:a" in wav)

        val flac = audioArgs("flac")
        assertEquals("flac", flac.valueOf("-c:a"))
        assertEquals("5", flac.valueOf("-compression_level"))
        assertEquals("flac", flac.valueOf("-f"))

        val ogg = audioArgs("ogg")
        assertEquals("libvorbis", ogg.valueOf("-c:a"))
        assertEquals("ogg", ogg.valueOf("-f"))

        val opus = audioArgs("opus")
        assertEquals("libopus", opus.valueOf("-c:a"))
        assertEquals("opus", opus.valueOf("-f"))
    }

    @Test
    fun `bitrate option is honoured and clamped to sane audio values`() {
        assertEquals("256k", audioArgs("mp3", FfmpegArgs.MediaParams(bitrateKbps = 256)).valueOf("-b:a"))
        assertEquals("32k", audioArgs("mp3", FfmpegArgs.MediaParams(bitrateKbps = 1)).valueOf("-b:a"))
        assertEquals("32k", audioArgs("opus", FfmpegArgs.MediaParams(bitrateKbps = 1)).valueOf("-b:a"))
    }

    @Test
    fun `trim options are placed before the input as input seek options`() {
        val args = audioArgs(
            "mp3",
            FfmpegArgs.MediaParams(startMs = 1_500L, endMs = 7_000L)
        )
        assertEquals("1.5", args.valueOf("-ss"))
        assertEquals("7", args.valueOf("-to"))
        assertTrue(args.indexOf("-ss") < args.indexOf("-i"))
        assertTrue(args.indexOf("-to") < args.indexOf("-i"))
    }

    @Test
    fun `trim seconds keep millisecond precision`() {
        assertEquals("0.5", audioArgs("wav", FfmpegArgs.MediaParams(startMs = 500L)).valueOf("-ss"))
        assertEquals("12.5", audioArgs("wav", FfmpegArgs.MediaParams(startMs = 12_500L)).valueOf("-ss"))
        assertEquals("2.25", audioArgs("wav", FfmpegArgs.MediaParams(startMs = 2_250L)).valueOf("-ss"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `trim end must come after trim start`() {
        FfmpegArgs.MediaParams(startMs = 5_000L, endMs = 4_000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative trim values are rejected`() {
        FfmpegArgs.MediaParams(startMs = -1L)
    }

    @Test
    fun `audio conversions ignore video scaling options`() {
        val args = audioArgs("mp3", FfmpegArgs.MediaParams(maxDimension = 720, evenDimensions = true))
        assertFalse("-vf" in args)
        assertFalse("-map" in args)
    }

    @Test
    fun `mp4 uses hardware h264 with aac audio and faststart`() {
        val args = videoArgs("mp4")
        assertEquals("h264_mediacodec", args.valueOf("-c:v"))
        assertEquals("yuv420p", args.valueOf("-pix_fmt"))
        assertEquals("3000k", args.valueOf("-b:v"))
        assertEquals("0:v:0", args.valueOf("-map"))
        assertTrue("0:a:0?" in args)
        assertEquals("aac", args.valueOf("-c:a"))
        assertEquals("128k", args.valueOf("-b:a"))
        assertTrue("+faststart" in args)
        assertEquals("mp4", args.valueOf("-f"))
        assertFalse(args.any { it.contains("libx264") || it.contains("libx265") })
    }

    @Test
    fun `mp4 software fallback switches to mpeg4`() {
        val args = videoArgs("mp4", software = true)
        assertEquals("mpeg4", args.valueOf("-c:v"))
        assertFalse("h264_mediacodec" in args)
        assertEquals("mp4", args.valueOf("-f"))
    }

    @Test
    fun `mkv uses matroska muxer without faststart`() {
        val args = videoArgs("mkv")
        assertEquals("h264_mediacodec", args.valueOf("-c:v"))
        assertEquals("matroska", args.valueOf("-f"))
        assertFalse("+faststart" in args)
    }

    @Test
    fun `webm uses vp9 at crf 32 and opus audio`() {
        val args = videoArgs("webm")
        assertEquals("libvpx-vp9", args.valueOf("-c:v"))
        assertEquals("4", args.valueOf("-cpu-used"))
        assertEquals("32", args.valueOf("-crf"))
        assertEquals("0", args.valueOf("-b:v"))
        assertEquals("libopus", args.valueOf("-c:a"))
        assertEquals("96k", args.valueOf("-b:a"))
        assertEquals("webm", args.valueOf("-f"))
    }

    @Test
    fun `webm honours a requested bitrate`() {
        val args = videoArgs("webm", FfmpegArgs.MediaParams(bitrateKbps = 1_500))
        assertEquals("1500k", args.valueOf("-b:v"))
    }

    @Test
    fun `video bitrate is honoured and clamped`() {
        val requested = videoArgs("mp4", FfmpegArgs.MediaParams(bitrateKbps = 1_200))
        assertEquals("1200k", requested.valueOf("-b:v"))
        val tooLow = videoArgs("mp4", FfmpegArgs.MediaParams(bitrateKbps = 1))
        assertEquals("100k", tooLow.valueOf("-b:v"))
    }

    @Test
    fun `max dimension scales the longer edge without upscaling`() {
        val args = videoArgs("mp4", FfmpegArgs.MediaParams(maxDimension = 720))
        assertEquals(
            "scale=w='min(720,iw)':h='min(720,ih)':force_original_aspect_ratio=decrease:force_divisible_by=2",
            args.valueOf("-vf")
        )
    }

    @Test
    fun `odd sized sources get an even size filter`() {
        val args = videoArgs("webm", FfmpegArgs.MediaParams(evenDimensions = true))
        assertEquals("scale=trunc(iw/2)*2:trunc(ih/2)*2", args.valueOf("-vf"))
    }

    @Test
    fun `no scale filter is added for even sized sources without a limit`() {
        assertFalse("-vf" in videoArgs("mp4"))
        assertFalse("-vf" in videoArgs("webm"))
    }

    @Test
    fun `only gif output is bounded by a hard duration limit`() {
        listOf("mp4", "webm", "mkv").forEach { target ->
            assertFalse("-t" in videoArgs(target))
        }
        assertTrue("-t" in videoArgs("gif"))
    }

    @Test
    fun `gif uses a palette filter, loops forever and stays under 480 px`() {
        val args = videoArgs("gif")
        val filter = args.valueOf("-filter_complex")
        assertTrue(filter.startsWith("[0:v]fps=12,"))
        assertTrue(filter.contains("scale='min(480,iw)':-1:flags=lanczos"))
        assertTrue(filter.contains("palettegen"))
        assertTrue(filter.contains("paletteuse[out]"))
        assertEquals("[out]", args.valueOf("-map"))
        assertEquals("30", args.valueOf("-t"))
        assertEquals("0", args.valueOf("-loop"))
        assertTrue("-an" in args)
        assertEquals("gif", args.valueOf("-f"))
    }

    @Test
    fun `gif follows a smaller max dimension but never exceeds 480`() {
        val small = videoArgs("gif", FfmpegArgs.MediaParams(maxDimension = 240))
        assertTrue(small.valueOf("-filter_complex").contains("scale='min(240,iw)'"))
        val large = videoArgs("gif", FfmpegArgs.MediaParams(maxDimension = 1_920))
        assertTrue(large.valueOf("-filter_complex").contains("scale='min(480,iw)'"))
    }

    @Test
    fun `gif plan caps whole files and trimmed ranges at thirty seconds`() {
        val whole = FfmpegArgs.gifPlan(60_000L, FfmpegArgs.MediaParams())
        assertTrue(whole.capped)
        assertEquals(30_000L, whole.params.endMs)
        assertEquals(30_000L, FfmpegArgs.plannedDurationMs(60_000L, whole.params))

        val short = FfmpegArgs.gifPlan(20_000L, FfmpegArgs.MediaParams())
        assertFalse(short.capped)
        assertEquals(null, short.params.endMs)

        val trimmed = FfmpegArgs.gifPlan(100_000L, FfmpegArgs.MediaParams(startMs = 40_000L))
        assertTrue(trimmed.capped)
        assertEquals(70_000L, trimmed.params.endMs)
        assertEquals(30_000L, FfmpegArgs.plannedDurationMs(100_000L, trimmed.params))

        val shortTrim = FfmpegArgs.gifPlan(100_000L, FfmpegArgs.MediaParams(startMs = 10_000L, endMs = 20_000L))
        assertFalse(shortTrim.capped)
        assertEquals(10_000L, FfmpegArgs.plannedDurationMs(100_000L, shortTrim.params))
    }

    @Test
    fun `planned duration respects trim and unknown sources`() {
        assertEquals(9_000L, FfmpegArgs.plannedDurationMs(9_000L, FfmpegArgs.MediaParams()))
        assertEquals(7_000L, FfmpegArgs.plannedDurationMs(9_000L, FfmpegArgs.MediaParams(startMs = 2_000L)))
        assertEquals(5_000L, FfmpegArgs.plannedDurationMs(9_000L, FfmpegArgs.MediaParams(endMs = 5_000L)))
        assertEquals(null, FfmpegArgs.plannedDurationMs(null, FfmpegArgs.MediaParams()))
        assertEquals(null, FfmpegArgs.plannedDurationMs(9_000L, FfmpegArgs.MediaParams(startMs = 9_000L)))
    }

    @Test
    fun `no gpl encoder is ever referenced`() {
        val audioTargets = listOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
        val videoTargets = listOf("mp4", "webm", "mkv", "gif")
        val commands = audioTargets.flatMap { target -> listOf(audioArgs(target)) } +
            videoTargets.flatMap { target -> listOf(videoArgs(target), videoArgs(target, software = true)) }
        commands.forEach { args ->
            assertFalse(args.toString(), args.any { arg -> arg.contains("libx264") || arg.contains("libx265") })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported video target is rejected`() {
        FfmpegArgs.videoArgs(INPUT, outputPath("flv"), "flv")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported audio target is rejected`() {
        FfmpegArgs.audioArgs(INPUT, outputPath("mp4"), "mp4")
    }

    @Test
    fun `metadata and chapters are stripped from every output`() {
        val audioTargets = listOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
        val videoTargets = listOf("mp4", "webm", "mkv", "gif")
        val commands = audioTargets.map { audioArgs(it) } +
            videoTargets.flatMap { listOf(videoArgs(it), videoArgs(it, software = true)) }
        commands.forEach { args ->
            val metadata = args.indexOf("-map_metadata")
            assertTrue(args.toString(), metadata >= 0)
            assertEquals("-1", args[metadata + 1])
            val chapters = args.indexOf("-map_chapters")
            assertTrue(args.toString(), chapters >= 0)
            assertEquals("-1", args[chapters + 1])
        }
    }

    private companion object {
        const val INPUT = "/cache/media/in_source.mp4"
    }
}
