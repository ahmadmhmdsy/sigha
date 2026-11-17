package com.sigha.converter.engine.pdf

import java.io.File

/**
 * One entry of a user page selection. Page numbers are 1-based and inclusive;
 * a null [end] means "through the last page".
 *
 * This file deliberately depends on nothing from Android so the parsing and naming rules can be
 * covered by plain JUnit tests.
 */
internal data class PageSpan(val start: Int, val end: Int?)

/**
 * Parses selections such as "1-3,5,8-" or "2".
 *
 * A blank selection means every page and returns an empty list. A malformed selection throws
 * [IllegalArgumentException]; the converters turn that into a readable ConversionException.
 */
internal fun parsePageSpans(raw: String?): List<PageSpan> {
    val selection = raw?.trim().orEmpty()
    if (selection.isEmpty()) return emptyList()
    val spans = ArrayList<PageSpan>()
    for (part in selection.split(',')) {
        val token = part.trim()
        if (token.isEmpty()) continue
        val dash = token.indexOf('-')
        if (dash < 0) {
            val page = token.toIntOrNull() ?: throw invalidSelection(token)
            requirePageNumber(page)
            spans += PageSpan(page, page)
        } else {
            if (token.indexOf('-', dash + 1) >= 0) throw invalidSelection(token)
            val start = token.substring(0, dash).trim().toIntOrNull() ?: throw invalidSelection(token)
            requirePageNumber(start)
            val endText = token.substring(dash + 1).trim()
            val end = if (endText.isEmpty()) {
                null
            } else {
                val page = endText.toIntOrNull() ?: throw invalidSelection(token)
                requirePageNumber(page)
                if (page < start) throw invalidSelection(token)
                page
            }
            spans += PageSpan(start, end)
        }
    }
    return spans
}

/**
 * Expands [spans] into 1-based page numbers clamped to a document of [pageCount] pages,
 * sorted and without duplicates. An empty [spans] list means every page.
 */
internal fun resolvePageIndices(spans: List<PageSpan>, pageCount: Int): List<Int> {
    if (pageCount <= 0) return emptyList()
    if (spans.isEmpty()) return (1..pageCount).toList()
    val pages = sortedSetOf<Int>()
    for (span in spans) {
        if (span.start > pageCount) continue
        val end = minOf(span.end ?: pageCount, pageCount)
        if (span.start > end) continue
        for (page in span.start..end) pages += page
    }
    return pages.toList()
}

/**
 * The file that holds one rendered page: "report.png" page 3 becomes "report-page3.png" next to
 * it. Every page keeps its own name, so a multi-page conversion can never overwrite itself.
 */
internal fun pageOutputFile(base: File, pageNumber: Int, extension: String): File =
    File(base.parentFile, base.nameWithoutExtension + "-page" + pageNumber + "." + extension)

private fun requirePageNumber(page: Int) {
    if (page < 1) throw IllegalArgumentException("Page numbers start at 1.")
}

private fun invalidSelection(token: String): IllegalArgumentException =
    IllegalArgumentException("Not a page or page range: " + token)
