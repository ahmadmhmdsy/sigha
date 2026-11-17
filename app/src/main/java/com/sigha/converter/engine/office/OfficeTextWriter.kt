package com.sigha.converter.engine.office

import com.sigha.converter.engine.text.ParagraphStyle
import com.sigha.converter.engine.text.TextParagraph

/** Renders paragraph lists as plain text or as a small standalone HTML file. */
internal object OfficeTextWriter {

    fun plain(paragraphs: List<TextParagraph>): String {
        val builder = StringBuilder()
        for (paragraph in paragraphs) {
            if (paragraph.style == ParagraphStyle.BULLET) builder.append("\u2022 ")
            builder.append(paragraph.text).append("\n\n")
        }
        return builder.toString().trimEnd() + "\n"
    }

    fun html(title: String, paragraphs: List<TextParagraph>): String {
        val body = StringBuilder()
        var inList = false
        fun closeList() {
            if (inList) {
                body.append("</ul>\n")
                inList = false
            }
        }
        for (paragraph in paragraphs) {
            when (paragraph.style) {
                ParagraphStyle.BULLET -> {
                    if (!inList) {
                        body.append("<ul>\n")
                        inList = true
                    }
                    body.append("<li>").append(inline(paragraph.text)).append("</li>\n")
                }
                ParagraphStyle.H1 -> {
                    closeList()
                    body.append("<h1>").append(inline(paragraph.text)).append("</h1>\n")
                }
                ParagraphStyle.H2 -> {
                    closeList()
                    body.append("<h2>").append(inline(paragraph.text)).append("</h2>\n")
                }
                ParagraphStyle.H3 -> {
                    closeList()
                    body.append("<h3>").append(inline(paragraph.text)).append("</h3>\n")
                }
                ParagraphStyle.MONO -> {
                    closeList()
                    body.append("<pre>").append(inline(paragraph.text)).append("</pre>\n")
                }
                ParagraphStyle.QUOTE -> {
                    closeList()
                    body.append("<blockquote>").append(inline(paragraph.text)).append("</blockquote>\n")
                }
                ParagraphStyle.BODY -> {
                    closeList()
                    body.append("<p>").append(inline(paragraph.text)).append("</p>\n")
                }
            }
        }
        closeList()
        return buildString {
            append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>")
            append(OoxmlXml.escape(title))
            append("</title>\n</head>\n<body>\n")
            append(body)
            append("</body>\n</html>\n")
        }
    }

    private fun inline(text: String): String =
        OoxmlXml.escape(text).replace("\n", "<br>\n")
}
