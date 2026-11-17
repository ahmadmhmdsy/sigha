package com.sigha.converter.engine.text

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    private val fence = "\u0060\u0060\u0060"

    @Test
    fun parsesHeadingsAndParagraphs() {
        val paragraphs = MarkdownParser.paragraphs("# Title\n\nSome **bold** text.")
        assertEquals(2, paragraphs.size)
        assertEquals(ParagraphStyle.H1, paragraphs[0].style)
        assertEquals("Title", paragraphs[0].text)
        assertEquals(ParagraphStyle.BODY, paragraphs[1].style)
        assertEquals("Some bold text.", paragraphs[1].text)
    }

    @Test
    fun parsesSetextHeadings() {
        val paragraphs = MarkdownParser.paragraphs("Title\n=====\n\nSub\n---\n")
        assertEquals(2, paragraphs.size)
        assertEquals(ParagraphStyle.H1, paragraphs[0].style)
        assertEquals("Title", paragraphs[0].text)
        assertEquals(ParagraphStyle.H2, paragraphs[1].style)
        assertEquals("Sub", paragraphs[1].text)
    }

    @Test
    fun parsesBullets() {
        val paragraphs = MarkdownParser.paragraphs("- one\n- two\n")
        assertEquals(2, paragraphs.size)
        assertEquals(ParagraphStyle.BULLET, paragraphs[0].style)
        assertEquals("one", paragraphs[0].text)
        assertEquals("two", paragraphs[1].text)
    }

    @Test
    fun keepsNumberedListsAsBullets() {
        val paragraphs = MarkdownParser.paragraphs("1. first\n2. second\n")
        assertEquals("1. first", paragraphs[0].text)
        assertEquals(ParagraphStyle.BULLET, paragraphs[1].style)
    }

    @Test
    fun keepsFencedCodeAsMono() {
        val markdown = fence + "\nval x = 1\n" + fence + "\n"
        val paragraphs = MarkdownParser.paragraphs(markdown)
        assertEquals(1, paragraphs.size)
        assertEquals(ParagraphStyle.MONO, paragraphs[0].style)
        assertEquals("val x = 1", paragraphs[0].text)
    }

    @Test
    fun stripsInlineCodeMarkers() {
        val text = "Use \u0060x\u0060 here."
        assertEquals("Use x here.", MarkdownParser.paragraphs(text).single().text)
    }

    @Test
    fun parsesQuotes() {
        val paragraphs = MarkdownParser.paragraphs("> quoted\n> lines\n")
        assertEquals(1, paragraphs.size)
        assertEquals(ParagraphStyle.QUOTE, paragraphs[0].style)
        assertEquals("quoted lines", paragraphs[0].text)
    }

    @Test
    fun convertsTablesToMonoRows() {
        val paragraphs = MarkdownParser.paragraphs("| a | b |\n|---|---|\n| 1 | 2 |\n")
        assertEquals(2, paragraphs.size)
        assertEquals(ParagraphStyle.MONO, paragraphs[0].style)
        assertEquals("a  |  b", paragraphs[0].text)
        assertEquals("1  |  2", paragraphs[1].text)
    }

    @Test
    fun dropsHorizontalRules() {
        val paragraphs = MarkdownParser.paragraphs("One\n\n---\n\nTwo\n")
        assertEquals(2, paragraphs.size)
        assertEquals("One", paragraphs[0].text)
        assertEquals("Two", paragraphs[1].text)
    }

    @Test
    fun keepsImageAltText() {
        val paragraphs = MarkdownParser.paragraphs("![a cat](cat.png)\n")
        assertEquals("a cat", paragraphs.single().text)
    }
}
