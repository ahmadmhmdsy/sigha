package com.sigha.converter.engine.text

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextExtractorTest {

    @Test
    fun stripsTagsAndDecodesEntities() {
        assertEquals(
            "Tom & Jerry <3",
            HtmlTextExtractor.toText("<p>Tom &amp; Jerry &lt;3</p>")
        )
    }

    @Test
    fun removesScriptsStylesAndHead() {
        val html = "<html><head><style>p{}</style><script>alert(1)</script></head>" +
            "<body><p>Hi</p></body></html>"
        assertEquals("Hi", HtmlTextExtractor.toText(html))
    }

    @Test
    fun keepsParagraphBreaks() {
        assertEquals("One\n\nTwo", HtmlTextExtractor.toText("<p>One</p><p>Two</p>"))
    }

    @Test
    fun separatesTableCellsWithOneTab() {
        assertEquals("A\tB", HtmlTextExtractor.toText("<td>A</td><td>B</td>"))
    }

    @Test
    fun decodesNumericEntities() {
        assertEquals("\u00E9", HtmlTextExtractor.decodeEntities("&#233;"))
        assertEquals("A", HtmlTextExtractor.decodeEntities("&#x41;"))
        assertEquals("&unknown;", HtmlTextExtractor.decodeEntities("&unknown;"))
    }

    @Test
    fun bodyFragmentKeepsMarkupButDropsScriptsAndHandlers() {
        val html = "<html><body><p onclick=\"x()\">Hi</p><script>bad()</script></body></html>"
        assertEquals("<p>Hi</p>", HtmlTextExtractor.bodyHtmlFragment(html))
    }

    @Test
    fun stripsTagsWithoutDecodingTwice() {
        assertEquals("a & b", HtmlTextExtractor.stripTags("<b>a</b> &amp; <i>b</i>"))
    }
}
