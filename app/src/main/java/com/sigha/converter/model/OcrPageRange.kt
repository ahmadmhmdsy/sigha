package com.sigha.converter.model

import com.sigha.converter.core.Note

/**
 * Turns the optional "pages" conversion option into the list of PDF pages to OCR.
 *
 * Users may type "1-5", "3" or "1-5,8". Unparseable input falls back to the default window,
 * while a valid range that lies outside the document selects nothing - both cases say so in
 * the typed [Note], so a typo never silently reads a different page. The engine never writes
 * user-facing text; the UI localizes the note.
 */
object OcrPageRange {

    /** Default ceiling for a scanned document, so one tap cannot OCR a 500 page book. */
    const val DEFAULT_MAX_PAGES: Int = 50

    /** Absolute ceiling, even for an explicit selection. */
    const val HARD_MAX_PAGES: Int = 500

    /** [pages] are 1-based; [note] explains a skipped window and is null when nothing was skipped. */
    data class Selection(val pages: List<Int> = emptyList(), val note: Note? = null)

    fun select(
        spec: String?,
        pageCount: Int,
        defaultMax: Int = DEFAULT_MAX_PAGES
    ): Selection {
        if (pageCount <= 0) return Selection(emptyList())
        if (spec.isNullOrBlank()) {
            val max = defaultMax.coerceIn(1, pageCount)
            return Selection(
                pages = (1..max).toList(),
                note = if (pageCount > max) {
                    Note.PagesLimited(read = max, total = pageCount)
                } else {
                    null
                }
            )
        }
        val requested = parse(spec, pageCount)
        if (requested == null) {
            val max = defaultMax.coerceIn(1, pageCount)
            return Selection(
                pages = (1..max).toList(),
                note = Note.PagesUnparsed(read = max)
            )
        }
        if (requested.isEmpty()) {
            return Selection(
                pages = emptyList(),
                note = Note.PagesMissing
            )
        }
        if (requested.size > HARD_MAX_PAGES) {
            return Selection(
                pages = requested.take(HARD_MAX_PAGES),
                note = Note.PagesCapped(read = HARD_MAX_PAGES)
            )
        }
        return Selection(requested)
    }

    /** Returns the requested pages, or null when no token was a usable page number. */
    private fun parse(spec: String, pageCount: Int): List<Int>? {
        val pages = sortedSetOf<Int>()
        var recognised = false
        for (token in spec.split(',', ';', ' ')) {
            val part = token.trim()
            if (part.isEmpty()) continue
            val dash = part.indexOf('-')
            try {
                if (dash > 0) {
                    val start = part.substring(0, dash).trim().toInt()
                    val end = part.substring(dash + 1).trim().toInt()
                    if (start <= 0 || end <= 0) continue
                    recognised = true
                    val low = minOf(start, end)
                    if (low > pageCount) continue
                    val to = maxOf(start, end).coerceAtMost(pageCount)
                    for (page in low..to) pages += page
                } else {
                    val page = part.toInt()
                    recognised = true
                    if (page in 1..pageCount) pages += page
                }
            } catch (_: NumberFormatException) {
                // Not a page number; fall through to the next token.
            }
        }
        return if (recognised) pages.toList() else null
    }
}
