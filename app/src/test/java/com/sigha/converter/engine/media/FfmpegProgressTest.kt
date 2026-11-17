package com.sigha.converter.engine.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The FFmpegKit statistics time unit is inferred rather than trusted: these tests pin the
 * millisecond and second interpretations to the same progress value.
 */
class FfmpegProgressTest {

    @Test
    fun `millisecond and second statistics produce the same progress`() {
        assertEquals(0.5f, FfmpegProgress.fromStatistics(30_000L, 60_000L), 0.001f)
        assertEquals(0.5f, FfmpegProgress.fromStatistics(30L, 60_000L), 0.001f)
        assertEquals(0.25f, FfmpegProgress.fromStatistics(15_000L, 60_000L), 0.001f)
        assertEquals(0.25f, FfmpegProgress.fromStatistics(15L, 60_000L), 0.001f)
    }

    @Test
    fun `progress never reaches one before the output is verified`() {
        assertEquals(0.99f, FfmpegProgress.fromStatistics(59_999L, 60_000L), 0.001f)
        assertEquals(0.99f, FfmpegProgress.fromStatistics(120L, 60_000L), 0.001f)
    }

    @Test
    fun `missing or unknown duration keeps progress at zero`() {
        assertEquals(0f, FfmpegProgress.fromStatistics(0L, 60_000L), 0f)
        assertEquals(0f, FfmpegProgress.fromStatistics(10_000L, 0L), 0f)
        assertEquals(0f, FfmpegProgress.fromStatistics(-5L, 60_000L), 0f)
    }
}
