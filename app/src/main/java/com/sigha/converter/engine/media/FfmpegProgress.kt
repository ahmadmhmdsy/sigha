package com.sigha.converter.engine.media

/**
 * FFmpegKit reports [com.arthenica.ffmpegkit.Statistics.getTime] in a unit that is not part of
 * the public contract and has differed between builds of the library: some report milliseconds,
 * others seconds. The number alone cannot be trusted, so the unit is inferred from its relation
 * to the known output duration:
 *
 * - a value within twice the duration in seconds is read as seconds (a real milliseconds value
 *   for the same moment would be 1000x larger);
 * - anything larger is read as milliseconds;
 * - a seconds value past the end is clamped, never treated as a millisecond value.
 *
 * Progress is clamped to 0.0..0.99 so the UI never shows 100% before the output is verified.
 */
object FfmpegProgress {

    fun fromStatistics(rawTime: Long, totalMs: Long): Float {
        if (rawTime <= 0L || totalMs <= 0L) return 0f
        val totalSeconds = totalMs / 1000.0
        val seconds = if (rawTime.toDouble() <= totalSeconds * 2.0) {
            rawTime.toDouble()
        } else {
            rawTime / 1000.0
        }
        return (seconds / totalSeconds).coerceIn(0.0, 0.99).toFloat()
    }
}
