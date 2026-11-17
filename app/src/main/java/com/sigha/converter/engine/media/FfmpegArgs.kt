package com.sigha.converter.engine.media

/**
 * Builds the argument arrays passed to FFmpegKit.
 *
 * This file is deliberately pure Kotlin (no Android imports) so every command detail is
 * unit-testable on the JVM. FFmpegKit receives an argument array, never a shell string, so no
 * shell escaping happens here; filter expressions are single-quoted because FFmpeg's own filter
 * parser treats commas as filter separators.
 *
 * Only encoders present in the LGPL `ffmpeg-kit-full` build are used: libx264 / libx265 are GPL
 * and absent, so H.264 always goes through MediaCodec (`h264_mediacodec`) with the LGPL MPEG-4
 * encoder as the fallback.
 */
object FfmpegArgs {

    /** GIF output is capped so an accidental feature-length input cannot fill the storage. */
    const val GIF_MAX_DURATION_MS = 30_000L
    const val GIF_FPS = 12
    const val GIF_MAX_DIMENSION = 480

    /** Video bitrate used when the caller did not ask for a specific one. */
    const val DEFAULT_VIDEO_KBPS = 3_000

    private val AUDIO_TARGETS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
    private val VIDEO_TARGETS = setOf("mp4", "webm", "mkv", "gif")

    private const val HARDWARE_H264 = "h264_mediacodec"
    private const val SOFTWARE_MPEG4 = "mpeg4"

    /** Caller options translated into FFmpeg terms; all values are already sanitized. */
    data class MediaParams(
        val startMs: Long? = null,
        val endMs: Long? = null,
        val bitrateKbps: Int? = null,
        val maxDimension: Int? = null,
        /** True when the source has an odd width or height and needs an even-size filter. */
        val evenDimensions: Boolean = false
    ) {
        init {
            require(startMs == null || startMs >= 0L) { "startMs must not be negative" }
            require(endMs == null || endMs >= 0L) { "endMs must not be negative" }
            require(endMs == null || startMs == null || endMs > startMs) { "endMs must be after startMs" }
        }
    }

    /** GIF length cap decision: the effective params plus whether anything was skipped. */
    data class GifPlan(val params: MediaParams, val capped: Boolean)

    /**
     * Arguments that turn any supported audio/video [inputPath] into one of
     * mp3, m4a, aac, wav, flac, ogg or opus at [outputPath].
     */
    fun audioArgs(
        inputPath: String,
        outputPath: String,
        targetId: String,
        params: MediaParams = MediaParams()
    ): Array<String> {
        require(targetId in AUDIO_TARGETS) { "Unsupported audio target: $targetId" }
        val args = mutableListOf("-y")
        addTrimInput(args, params)
        args += listOf("-i", inputPath, "-vn")
        when (targetId) {
            "mp3" -> args += listOf("-c:a", "libmp3lame", "-b:a", "${audioKbps(params, 192)}k")
            "m4a" -> args += listOf("-c:a", "aac", "-b:a", "${audioKbps(params, 128)}k")
            "aac" -> args += listOf("-c:a", "aac", "-b:a", "${audioKbps(params, 128)}k")
            "wav" -> args += listOf("-c:a", "pcm_s16le")
            "flac" -> args += listOf("-c:a", "flac", "-compression_level", "5")
            "ogg" -> args += listOf("-c:a", "libvorbis", "-b:a", "${audioKbps(params, 128)}k")
            "opus" -> args += listOf("-c:a", "libopus", "-b:a", "${audioKbps(params, 96)}k")
        }
        args += listOf("-f", audioFormat(targetId), outputPath)
        return args.toTypedArray()
    }

    /**
     * Arguments that turn a video [inputPath] into mp4, webm, mkv or an animated gif at
     * [outputPath]. [softwareEncoder] selects software MPEG-4 instead of MediaCodec H.264 and is
     * used as the retry when the device rejects hardware encoding.
     */
    fun videoArgs(
        inputPath: String,
        outputPath: String,
        targetId: String,
        params: MediaParams = MediaParams(),
        softwareEncoder: Boolean = false
    ): Array<String> {
        require(targetId in VIDEO_TARGETS) { "Unsupported video target: $targetId" }
        val args = mutableListOf("-y")
        addTrimInput(args, params)
        args += listOf("-i", inputPath)
        when (targetId) {
            "mp4", "mkv" -> {
                args += listOf("-map", "0:v:0", "-map", "0:a:0?")
                args += listOf("-c:v", if (softwareEncoder) SOFTWARE_MPEG4 else HARDWARE_H264)
                args += listOf("-pix_fmt", "yuv420p", "-b:v", "${videoKbps(params)}k")
                scaleFilter(params)?.let { filter -> args += listOf("-vf", filter) }
                args += listOf("-c:a", "aac", "-b:a", "128k")
                if (targetId == "mp4") args += listOf("-movflags", "+faststart")
                args += listOf("-f", if (targetId == "mp4") "mp4" else "matroska", outputPath)
            }
            "webm" -> {
                args += listOf("-map", "0:v:0", "-map", "0:a:0?")
                args += listOf("-c:v", "libvpx-vp9", "-cpu-used", "4", "-crf", "32")
                args += listOf("-b:v", params.bitrateKbps?.let { kbps -> "${sanitizeVideoKbps(kbps)}k" } ?: "0")
                args += listOf("-pix_fmt", "yuv420p")
                scaleFilter(params)?.let { filter -> args += listOf("-vf", filter) }
                args += listOf("-c:a", "libopus", "-b:a", "96k")
                args += listOf("-f", "webm", outputPath)
            }
            "gif" -> {
                args += listOf("-filter_complex", gifFilter(params.maxDimension))
                args += listOf("-map", "[out]", "-an")
                // Also bounded here so a file with unreadable duration cannot grow without limit.
                args += listOf("-t", (GIF_MAX_DURATION_MS / 1_000L).toString())
                args += listOf("-loop", "0")
                args += listOf("-f", "gif", outputPath)
            }
        }
        return args.toTypedArray()
    }

    /**
     * Limits the longer edge to [MediaParams.maxDimension] without upscaling, keeping the aspect
     * ratio and even dimensions (H.264, MPEG-4 and VP9 all need even sizes). When no limit was
     * requested but the source is odd-sized, the filter only rounds the size down to even.
     */
    fun scaleFilter(params: MediaParams): String? {
        val max = params.maxDimension?.takeIf { it > 0 }
        if (max != null) {
            return "scale=w='min($max,iw)':h='min($max,ih)':" +
                "force_original_aspect_ratio=decrease:force_divisible_by=2"
        }
        if (params.evenDimensions) {
            return "scale=trunc(iw/2)*2:trunc(ih/2)*2"
        }
        return null
    }

    /** Palette based GIF filter graph; 12 fps, at most 480 px wide, loops forever. */
    fun gifFilter(maxDimension: Int?): String {
        val width = maxDimension?.takeIf { it > 0 }?.let { requested -> minOf(requested, GIF_MAX_DIMENSION) }
            ?: GIF_MAX_DIMENSION
        return "[0:v]fps=$GIF_FPS,scale='min($width,iw)':-1:flags=lanczos," +
            "split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse[out]"
    }

    /** Applies the 30 second GIF cap to an explicit trim range as well as to whole files. */
    fun gifPlan(sourceDurationMs: Long?, params: MediaParams): GifPlan {
        val start = params.startMs ?: 0L
        val availableMs = params.endMs?.let { end -> end - start }
            ?: sourceDurationMs?.let { duration -> duration - start }
        if (availableMs == null || availableMs <= GIF_MAX_DURATION_MS) return GifPlan(params, false)
        return GifPlan(params.copy(endMs = start + GIF_MAX_DURATION_MS), true)
    }

    /** Output length after trimming; drives the progress denominator. */
    fun plannedDurationMs(sourceDurationMs: Long?, params: MediaParams): Long? {
        val start = params.startMs ?: 0L
        val plannedMs = params.endMs?.let { end -> end - start }
            ?: sourceDurationMs?.let { duration -> duration - start }
        return plannedMs?.takeIf { it > 0L }
    }

    private fun addTrimInput(args: MutableList<String>, params: MediaParams) {
        params.startMs?.takeIf { it > 0L }?.let { start -> args += listOf("-ss", seconds(start)) }
        params.endMs?.let { end -> args += listOf("-to", seconds(end)) }
    }

    private fun seconds(milliseconds: Long): String {
        if (milliseconds <= 0L) return "0"
        val wholeSeconds = milliseconds / 1_000L
        val remainder = milliseconds % 1_000L
        if (remainder == 0L) return wholeSeconds.toString()
        return "$wholeSeconds.${remainder.toString().padStart(3, '0').trimEnd('0')}"
    }

    private fun audioKbps(params: MediaParams, defaultKbps: Int): Int =
        (params.bitrateKbps ?: defaultKbps).coerceIn(32, 512)

    private fun videoKbps(params: MediaParams): Int =
        sanitizeVideoKbps(params.bitrateKbps ?: DEFAULT_VIDEO_KBPS)

    private fun sanitizeVideoKbps(kbps: Int): Int = kbps.coerceIn(100, 20_000)

    private fun audioFormat(targetId: String): String = when (targetId) {
        "mp3" -> "mp3"
        "m4a" -> "ipod"
        "aac" -> "adts"
        "wav" -> "wav"
        "flac" -> "flac"
        "ogg" -> "ogg"
        "opus" -> "opus"
        else -> throw IllegalArgumentException("Unsupported audio target: $targetId")
    }
}
