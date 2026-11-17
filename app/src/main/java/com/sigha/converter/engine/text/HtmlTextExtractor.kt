package com.sigha.converter.engine.text

/**
 * Dependency-free HTML to text helper.
 *
 * This is deliberately not a full HTML parser: it keeps block boundaries,
 * table cells and line breaks, drops markup and hidden blocks, and decodes
 * the entities that appear in real-world documents. Pure Kotlin, so it is
 * unit-testable on the JVM (no Android classes).
 */
object HtmlTextExtractor {

    private val COMMENT = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))
    private val CDATA = Regex("<!\\[CDATA\\[(.*?)]]>", setOf(RegexOption.DOT_MATCHES_ALL))
    private val DOCTYPE = Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE)
    private val PROCESSING_INSTRUCTION = Regex("<\\?.*?\\?>", setOf(RegexOption.DOT_MATCHES_ALL))
    private val HIDDEN_BLOCK = Regex(
        """<(script|style|head|noscript|template|svg)\b[^>]*>.*?</\1\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HIDDEN_EMPTY = Regex(
        """<(script|style|noscript|template|svg)\b[^>]*/>""",
        RegexOption.IGNORE_CASE
    )
    private val LINE_BREAK = Regex("""<br\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val RULE = Regex("""<hr\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val TABLE_CELL = Regex("""</?(td|th)\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val TAB_RUN = Regex("\t{2,}")
    private val TABLE_ROW_END = Regex("""</tr\s*>""", RegexOption.IGNORE_CASE)
    private val BLOCK_BOUNDARY = Regex(
        """</?(p|div|section|article|header|footer|nav|main|aside|h[1-6]|ul|ol|li|dl|dt|dd|table|thead|tbody|tfoot|tr|blockquote|pre|figure|figcaption|address|fieldset|form|legend|details|summary|caption)\b[^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val ANY_TAG = Regex("<[^>]*>")
    private val EVENT_HANDLER = Regex("""\s+on[a-zA-Z]+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""")
    private val JAVASCRIPT_URL = Regex("javascript\\s*:", RegexOption.IGNORE_CASE)
    private val BODY_OPEN = Regex("""<body\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val HORIZONTAL_SPACE = Regex("[ \\u00A0\\u000B\\u000C]+")

    /** Extracts readable text from a full HTML document or fragment. */
    fun toText(html: String): String {
        var text = html.replace("\r\n", "\n").replace('\r', '\n')
        text = COMMENT.replace(text, "")
        text = CDATA.replace(text) { it.groupValues[1] }
        text = HIDDEN_BLOCK.replace(text, "")
        text = HIDDEN_EMPTY.replace(text, "")
        text = DOCTYPE.replace(text, "")
        text = PROCESSING_INSTRUCTION.replace(text, "")
        text = LINE_BREAK.replace(text, "\n")
        text = RULE.replace(text, "\n")
        text = TABLE_CELL.replace(text, "\t")
        text = TAB_RUN.replace(text, "\t")
        text = TABLE_ROW_END.replace(text, "\n")
        text = BLOCK_BOUNDARY.replace(text, "\n")
        text = ANY_TAG.replace(text, "")
        text = decodeEntities(text)
        return normalize(text)
    }

    /** Removes markup only, keeping inline text and decoding entities once. */
    fun stripTags(html: String): String {
        var text = COMMENT.replace(html, "")
        text = HIDDEN_BLOCK.replace(text, "")
        text = HIDDEN_EMPTY.replace(text, "")
        text = ANY_TAG.replace(text, "")
        return decodeEntities(text)
    }

    /** Decodes named and numeric HTML entities; unknown entities are left alone. */
    fun decodeEntities(text: String): String {
        if (text.indexOf('&') < 0) return text
        val builder = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val character = text[index]
            if (character != '&') {
                builder.append(character)
                index++
                continue
            }
            val semicolon = text.indexOf(';', index + 1)
            if (semicolon < 0 || semicolon - index > 10) {
                builder.append(character)
                index++
                continue
            }
            val decoded = decodeEntity(text.substring(index + 1, semicolon))
            if (decoded == null) {
                builder.append(character)
                index++
            } else {
                builder.append(decoded)
                index = semicolon + 1
            }
        }
        return builder.toString()
    }

    /**
     * Returns the inner body of an XHTML document with scripts, styles and
     * event handlers removed. Used by the EPUB to HTML converter.
     */
    fun bodyHtmlFragment(html: String): String {
        var fragment = html
        val bodyStart = BODY_OPEN.find(html)
        if (bodyStart != null) {
            val start = bodyStart.range.last + 1
            val end = html.lastIndexOf("</body", ignoreCase = true)
            fragment = html.substring(start, if (end > start) end else html.length)
        }
        fragment = COMMENT.replace(fragment, "")
        fragment = DOCTYPE.replace(fragment, "")
        fragment = PROCESSING_INSTRUCTION.replace(fragment, "")
        fragment = HIDDEN_BLOCK.replace(fragment, "")
        fragment = HIDDEN_EMPTY.replace(fragment, "")
        fragment = EVENT_HANDLER.replace(fragment, "")
        fragment = JAVASCRIPT_URL.replace(fragment, "")
        return fragment.trim()
    }

    private fun decodeEntity(body: String): String? {
        if (body.isEmpty()) return null
        if (body[0] == '#') {
            val code = if (body.length > 1 && (body[1] == 'x' || body[1] == 'X')) {
                body.substring(2).toIntOrNull(16)
            } else {
                body.substring(1).toIntOrNull(10)
            } ?: return null
            if (code <= 0 || code > 0x10FFFF || code in 0xD800..0xDFFF) return null
            return String(Character.toChars(code))
        }
        return ENTITIES[body.lowercase()]
    }

    private fun normalize(text: String): String {
        if (text.isEmpty()) return ""
        val builder = StringBuilder(text.length)
        var pendingBlank = false
        for (line in text.split('\n')) {
            val cleaned = cleanLine(line)
            if (cleaned.isEmpty()) {
                if (builder.isNotEmpty()) pendingBlank = true
                continue
            }
            if (pendingBlank) {
                builder.append('\n')
                pendingBlank = false
            }
            builder.append(cleaned).append('\n')
        }
        return builder.toString().trimEnd('\n')
    }

    private fun cleanLine(line: String): String {
        if (line.isEmpty()) return ""
        val cells = line.split('\t').map { cell ->
            cell.replace(HORIZONTAL_SPACE, " ").trim()
        }
        return cells.joinToString("\t").trim()
    }

    private val ENTITIES: Map<String, String> = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "copy" to "\u00A9",
        "reg" to "\u00AE",
        "trade" to "\u2122",
        "hellip" to "\u2026",
        "mdash" to "\u2014",
        "ndash" to "\u2013",
        "lsquo" to "\u2018",
        "rsquo" to "\u2019",
        "ldquo" to "\u201C",
        "rdquo" to "\u201D",
        "laquo" to "\u00AB",
        "raquo" to "\u00BB",
        "bull" to "\u2022",
        "middot" to "\u00B7",
        "deg" to "\u00B0",
        "plusmn" to "\u00B1",
        "times" to "\u00D7",
        "divide" to "\u00F7",
        "frac12" to "\u00BD",
        "frac14" to "\u00BC",
        "frac34" to "\u00BE",
        "sup2" to "\u00B2",
        "sup3" to "\u00B3",
        "micro" to "\u00B5",
        "sect" to "\u00A7",
        "para" to "\u00B6",
        "cent" to "\u00A2",
        "pound" to "\u00A3",
        "euro" to "\u20AC",
        "yen" to "\u00A5",
        "alpha" to "\u03B1",
        "beta" to "\u03B2",
        "gamma" to "\u03B3",
        "pi" to "\u03C0",
        "shy" to "\u00AD",
        "ensp" to " ",
        "emsp" to " ",
        "thinsp" to " "
    )
}
