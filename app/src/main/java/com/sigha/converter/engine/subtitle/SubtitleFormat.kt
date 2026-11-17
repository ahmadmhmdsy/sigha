package com.sigha.converter.engine.subtitle

import com.sigha.converter.engine.text.HtmlTextExtractor

/** One subtitle cue: timestamps in milliseconds plus the text lines. */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val lines: List<String>
)

/**
 * SRT and WebVTT parsing/formatting.
 *
 * The parsers are tolerant of the variations real files show: BOMs, CRLF,
 * comma or dot milliseconds, missing blank separators, cue identifiers and
 * WebVTT settings/tags. Pure Kotlin, so it is unit-testable on the JVM.
 */
object SubtitleFormat {

    private val TIMING = Regex(
        """(\d{1,3}:\d{2}:\d{2}[.,]\d{1,3}|\d{1,2}:\d{2}[.,]\d{1,3})\s*-->\s*(\d{1,3}:\d{2}:\d{2}[.,]\d{1,3}|\d{1,2}:\d{2}[.,]\d{1,3})"""
    )
    private val TIMECODE = Regex("""^(?:(\d{1,3}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})$""")
    private val TAG = Regex("<[^>]*>")

    fun parseSrt(text: String): List<SubtitleCue> {
        val cues = ArrayList<SubtitleCue>()
        val lines = ArrayList<String>()
        var start = -1L
        var end = -1L
        fun flush() {
            if (start >= 0L) cues.add(SubtitleCue(start, maxOf(end, start), trimmed(lines)))
            start = -1L
            end = -1L
            lines.clear()
        }
        for (raw in normalize(text).lineSequence()) {
            if (raw.isBlank()) {
                flush()
                continue
            }
            val timing = TIMING.find(raw)
            if (timing != null) {
                flush()
                start = parseTimecode(timing.groupValues[1]) ?: 0L
                end = parseTimecode(timing.groupValues[2]) ?: start
                continue
            }
            if (start >= 0L) lines.add(raw.trimEnd())
        }
        flush()
        return cues
    }

    fun parseVtt(text: String): List<SubtitleCue> {
        val cues = ArrayList<SubtitleCue>()
        val lines = ArrayList<String>()
        var start = -1L
        var end = -1L
        var inHeader = true
        var inNote = false
        fun flush() {
            if (start >= 0L) cues.add(SubtitleCue(start, maxOf(end, start), trimmed(lines)))
            start = -1L
            end = -1L
            lines.clear()
        }
        for (raw in normalize(text).lineSequence()) {
            var line = raw.trimEnd()
            if (inHeader) {
                if (line.isBlank()) {
                    inHeader = false
                    continue
                }
                if (TIMING.containsMatchIn(line)) {
                    inHeader = false
                } else {
                    continue
                }
            }
            if (inNote) {
                if (line.isBlank()) inNote = false
                continue
            }
            if (line.startsWith("NOTE") || line.startsWith("STYLE") || line.startsWith("REGION")) {
                flush()
                inNote = true
                continue
            }
            if (line.isBlank()) {
                flush()
                continue
            }
            val timing = TIMING.find(line)
            if (timing != null) {
                flush()
                start = parseTimecode(timing.groupValues[1]) ?: 0L
                end = parseTimecode(timing.groupValues[2]) ?: start
                continue
            }
            if (start >= 0L) lines.add(line)
        }
        flush()
        return cues
    }

    fun formatSrt(cues: List<SubtitleCue>): String {
        val builder = StringBuilder()
        for (index in cues.indices) {
            val cue = cues[index]
            builder.append(index + 1).append('\n')
            builder.append(formatTimecode(cue.startMs, ','))
            builder.append(" --> ")
            builder.append(formatTimecode(cue.endMs, ','))
            builder.append('\n')
            for (line in cue.lines) builder.append(line).append('\n')
            builder.append('\n')
        }
        return builder.toString()
    }

    fun formatVtt(cues: List<SubtitleCue>): String {
        val builder = StringBuilder("WEBVTT\n\n")
        for (cue in cues) {
            builder.append(formatTimecode(cue.startMs, '.'))
            builder.append(" --> ")
            builder.append(formatTimecode(cue.endMs, '.'))
            builder.append('\n')
            for (line in cue.lines) builder.append(line).append('\n')
            builder.append('\n')
        }
        return builder.toString()
    }

    /** Parses "HH:MM:SS,mmm", "HH:MM:SS.mmm" or "MM:SS.mmm". */
    fun parseTimecode(value: String): Long? {
        val match = TIMECODE.matchEntire(value.trim()) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toLongOrNull() ?: return null
        val millis = match.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: return null
        return hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
    }

    fun formatTimecode(milliseconds: Long, separator: Char): String {
        val total = if (milliseconds < 0L) 0L else milliseconds
        val hours = total / 3_600_000L
        val minutes = (total / 60_000L) % 60L
        val seconds = (total / 1_000L) % 60L
        val millis = total % 1_000L
        return buildString {
            append(hours.toString().padStart(2, '0'))
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
            append(separator)
            append(millis.toString().padStart(3, '0'))
        }
    }

    /** Removes WebVTT markup (tags, voice spans, inline timestamps) and entities. */
    fun stripVttTags(text: String): String =
        HtmlTextExtractor.decodeEntities(TAG.replace(text, ""))

    private fun normalize(text: String): String =
        text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')

    private fun trimmed(lines: MutableList<String>): List<String> {
        var from = 0
        var to = lines.size
        while (from < to && lines[from].isBlank()) from++
        while (to > from && lines[to - 1].isBlank()) to--
        return lines.subList(from, to).toList()
    }
}
