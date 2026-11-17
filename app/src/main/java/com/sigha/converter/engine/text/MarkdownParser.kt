package com.sigha.converter.engine.text

/**
 * Turns Markdown into plain paragraphs for the PDF renderer.
 *
 * It is a pragmatic subset: headings, lists, quotes, fenced code, simple tables
 * and inline emphasis are handled; inline images keep their alt text and raw
 * HTML is stripped. Pure Kotlin, so it is unit-testable on the JVM.
 */
object MarkdownParser {

    const val MAX_PARAGRAPHS = 50_000

    private val HEADING = Regex("""(#{1,6})\s+(.*)""")
    private val SETEXT_ONE = Regex("={2,}")
    private val SETEXT_TWO = Regex("-{2,}")
    private val HORIZONTAL_RULE = Regex("""([-*_])(?:\s*\1){2,}""")
    private val BULLET = Regex("""[-*+]\s+(.*)""")
    private val NUMBERED = Regex("""(\d{1,9})[.)]\s+(.*)""")
    private val QUOTE = Regex(""">\s?(.*)""")
    private val FENCE = Regex("""(\x60{3,}|~{3,}).*""")
    private val TABLE_SEPARATOR = Regex(""":?-{1,}:?""")
    private val CODE_SPAN = Regex("""\x60([^\x60\n]+)\x60""")
    private val IMAGE = Regex("""!\[([^\]]*)]\(([^)\s]*)(?:\s+"[^"]*")?\)""")
    private val LINK = Regex("""\[([^\]]+)]\(([^)\s]*)(?:\s+"[^"]*")?\)""")
    private val AUTOLINK = Regex("""<((?:https?://|mailto:)[^>\s]+)>""")
    private val BOLD = Regex("""\*\*([^*]+)\*\*""")
    private val UNDERSCORE_BOLD = Regex("""__([^_]+)__""")
    private val STRIKE = Regex("""~~([^~]+)~~""")
    private val ITALIC = Regex("""\*([^*\n]+)\*""")
    private val UNDERSCORE_ITALIC = Regex("""(?<![\w])_([^_\n]+)_(?![\w])""")

    fun paragraphs(markdown: String, maxParagraphs: Int = MAX_PARAGRAPHS): List<TextParagraph> {
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = ArrayList<TextParagraph>()
        var index = 0
        while (index < lines.size && out.size < maxParagraphs) {
            val trimmed = lines[index].trim()
            if (trimmed.isEmpty()) {
                index++
                continue
            }

            val fence = FENCE.matchEntire(trimmed)
            if (fence != null) {
                val marker = fence.groupValues[1].take(3)
                index++
                while (index < lines.size && !lines[index].trim().startsWith(marker)) {
                    if (out.size >= maxParagraphs) break
                    out.add(TextParagraph(lines[index].trimEnd(), ParagraphStyle.MONO))
                    index++
                }
                index++
                continue
            }

            val heading = HEADING.matchEntire(trimmed)
            if (heading != null) {
                val level = heading.groupValues[1].length
                val text = heading.groupValues[2].trim().trimEnd('#').trim()
                out.add(TextParagraph(inline(text), headingStyle(level)))
                index++
                continue
            }

            if (index + 1 < lines.size) {
                val underline = lines[index + 1].trim()
                if (SETEXT_ONE.matches(underline) || SETEXT_TWO.matches(underline)) {
                    val style = if (SETEXT_ONE.matches(underline)) ParagraphStyle.H1 else ParagraphStyle.H2
                    out.add(TextParagraph(inline(trimmed), style))
                    index += 2
                    continue
                }
            }

            if (HORIZONTAL_RULE.matches(trimmed)) {
                index++
                continue
            }

            val bullet = BULLET.matchEntire(trimmed)
            if (bullet != null) {
                out.add(TextParagraph(inline(bullet.groupValues[1]), ParagraphStyle.BULLET))
                index++
                continue
            }

            val numbered = NUMBERED.matchEntire(trimmed)
            if (numbered != null) {
                val text = numbered.groupValues[1] + ". " + inline(numbered.groupValues[2])
                out.add(TextParagraph(text, ParagraphStyle.BULLET))
                index++
                continue
            }

            if (trimmed.startsWith("|")) {
                val cells = trimmed.trim('|').split('|').map { it.trim() }
                val isSeparator = cells.isNotEmpty() && cells.all { it.isEmpty() || TABLE_SEPARATOR.matches(it) }
                if (!isSeparator) {
                    out.add(TextParagraph(inline(cells.joinToString("  |  ")), ParagraphStyle.MONO))
                }
                index++
                continue
            }

            if (QUOTE.matchEntire(trimmed) != null) {
                val pieces = ArrayList<String>()
                while (index < lines.size) {
                    val quoted = QUOTE.matchEntire(lines[index].trim()) ?: break
                    pieces.add(quoted.groupValues[1])
                    index++
                }
                out.add(TextParagraph(inline(pieces.joinToString(" ")), ParagraphStyle.QUOTE))
                continue
            }

            val paragraph = StringBuilder()
            while (index < lines.size) {
                val current = lines[index]
                if (current.isBlank()) break
                val currentTrimmed = current.trim()
                if (isBlockStart(currentTrimmed)) break
                if (paragraph.isNotEmpty()) {
                    val previous = lines[index - 1]
                    if (previous.endsWith("  ")) paragraph.append('\n') else paragraph.append(' ')
                }
                paragraph.append(currentTrimmed)
                index++
                if (out.size >= maxParagraphs) break
            }
            if (paragraph.isNotEmpty()) {
                out.add(TextParagraph(inline(paragraph.toString()), ParagraphStyle.BODY))
            }
        }
        return out
    }

    private fun isBlockStart(line: String): Boolean =
        HEADING.matchEntire(line) != null ||
            FENCE.matchEntire(line) != null ||
            HORIZONTAL_RULE.matches(line) ||
            BULLET.matchEntire(line) != null ||
            NUMBERED.matchEntire(line) != null ||
            QUOTE.matchEntire(line) != null ||
            line.startsWith("|")

    private fun headingStyle(level: Int): ParagraphStyle = when (level) {
        1 -> ParagraphStyle.H1
        2 -> ParagraphStyle.H2
        else -> ParagraphStyle.H3
    }

    private fun inline(raw: String): String {
        if (raw.isEmpty()) return ""
        var text = raw
        val codeSpans = ArrayList<String>()
        text = CODE_SPAN.replace(text) { match ->
            codeSpans.add(match.groupValues[1])
            "\u0001" + (codeSpans.size - 1) + "\u0001"
        }
        text = AUTOLINK.replace(text) { match ->
            val target = match.groupValues[1]
            if (target.startsWith("mailto:", ignoreCase = true)) target.substring(7) else target
        }
        text = IMAGE.replace(text) { it.groupValues[1] }
        text = LINK.replace(text) { match ->
            val label = match.groupValues[1]
            val target = match.groupValues[2]
            if (target.isEmpty() || label == target) label else label + " (" + target + ")"
        }
        text = HtmlTextExtractor.stripTags(text)
        text = BOLD.replace(text) { it.groupValues[1] }
        text = UNDERSCORE_BOLD.replace(text) { it.groupValues[1] }
        text = STRIKE.replace(text) { it.groupValues[1] }
        text = ITALIC.replace(text) { it.groupValues[1] }
        text = UNDERSCORE_ITALIC.replace(text) { it.groupValues[1] }
        for (i in codeSpans.indices) {
            text = text.replace("\u0001" + i + "\u0001", codeSpans[i])
        }
        return text.trim()
    }
}
