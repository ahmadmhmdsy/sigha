package com.sigha.converter.engine.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleFormatTest {

    @Test
    fun parsesSrtCues() {
        val cues = SubtitleFormat.parseSrt(
            "1\n00:00:01,000 --> 00:00:02,500\nHello\nworld\n\n" +
                "2\n00:01:02,000 --> 00:01:03,000\nAgain\n"
        )
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(2500L, cues[0].endMs)
        assertEquals(listOf("Hello", "world"), cues[0].lines)
        assertEquals(62000L, cues[1].startMs)
    }

    @Test
    fun parsesCuesWithoutBlankSeparator() {
        val cues = SubtitleFormat.parseSrt(
            "00:00:01,000 --> 00:00:02,000\nA\n00:00:03,000 --> 00:00:04,000\nB\n"
        )
        assertEquals(2, cues.size)
        assertEquals(listOf("A"), cues[0].lines)
        assertEquals(listOf("B"), cues[1].lines)
    }

    @Test
    fun formatsSrtWithNumbering() {
        val text = SubtitleFormat.formatSrt(listOf(SubtitleCue(1000L, 2500L, listOf("Hi"))))
        assertEquals("1\n00:00:01,000 --> 00:00:02,500\nHi\n\n", text)
    }

    @Test
    fun parsesVttWithHeaderNotesAndSettings() {
        val text = "WEBVTT\n\nNOTE a comment\nsecond line\n\n" +
            "intro\n00:00:01.000 --> 00:00:02.000 align:start\n<v Bob>Hi</v>\n"
        val cues = SubtitleFormat.parseVtt(text)
        assertEquals(1, cues.size)
        assertEquals(listOf("<v Bob>Hi</v>"), cues[0].lines)
        assertEquals(1000L, cues[0].startMs)
    }

    @Test
    fun formatsVttWithDotSeparator() {
        val text = SubtitleFormat.formatVtt(listOf(SubtitleCue(1000L, 2500L, listOf("Hi"))))
        assertEquals("WEBVTT\n\n00:00:01.000 --> 00:00:02.500\nHi\n\n", text)
    }

    @Test
    fun skipsVttStyleBlock() {
        val text = "WEBVTT\n\nSTYLE\n::cue { color: red }\n\n" +
            "00:00:01.000 --> 00:00:02.000\nText\n"
        val cues = SubtitleFormat.parseVtt(text)
        assertEquals(1, cues.size)
        assertEquals(listOf("Text"), cues[0].lines)
    }

    @Test
    fun parsesTimecodes() {
        assertEquals(3723004L, SubtitleFormat.parseTimecode("01:02:03.004"))
        assertEquals(62000L, SubtitleFormat.parseTimecode("01:02,000"))
        assertNull(SubtitleFormat.parseTimecode("nope"))
    }

    @Test
    fun formatsTimecodes() {
        assertEquals("01:02:03,004", SubtitleFormat.formatTimecode(3723004L, ','))
        assertEquals("00:00:00,000", SubtitleFormat.formatTimecode(-5L, ','))
    }

    @Test
    fun stripsVttTagsAndEntities() {
        assertEquals(
            "Hi & bye",
            SubtitleFormat.stripVttTags("<c.yellow>Hi</c> &amp; <i>bye</i>")
        )
    }

    @Test
    fun normalizesCrLfInput() {
        val cues = SubtitleFormat.parseSrt("1\r\n00:00:01,000 --> 00:00:02,000\r\nLine\r\n")
        assertEquals(1, cues.size)
        assertEquals(listOf("Line"), cues[0].lines)
    }
}
