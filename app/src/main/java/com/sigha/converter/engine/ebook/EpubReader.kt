package com.sigha.converter.engine.ebook

import android.net.Uri
import com.sigha.converter.core.ConversionException
import com.sigha.converter.engine.archive.ArchiveGuards
import com.sigha.converter.engine.office.OoxmlXml
import com.sigha.converter.engine.office.SafePackage
import com.sigha.converter.engine.text.HtmlTextExtractor
import com.sigha.converter.engine.text.TextIo
import org.xmlpull.v1.XmlPullParser

/** One spine item of an EPUB, already reduced to text and cleaned body HTML. */
internal data class EpubChapter(
    val title: String,
    val text: String,
    val html: String
)

internal class EpubBook(
    val title: String,
    val chapters: List<EpubChapter>
)

/**
 * Minimal EPUB 2/3 reader: META-INF/container.xml gives the package document,
 * its manifest and spine give the reading order, and each spine item is
 * stripped to text with HtmlTextExtractor.
 *
 * Only local XHTML documents are read; images, fonts and remote resources are
 * ignored, as are the navigation document and page-list titles.
 */
internal object EpubReader {

    private const val MAX_CHAPTERS = 2_000
    private const val MAX_CHAPTER_BYTES = 16L * 1024L * 1024L

    fun read(zip: SafePackage): EpubBook {
        val rootfile = findRootfile(zip)
        val baseDir = rootfile.substringBeforeLast('/', "")
        val manifest = LinkedHashMap<String, String>()
        val spine = ArrayList<String>()
        var title = "EPUB"
        val opfStream = zip.open(rootfile)
            ?: throw ConversionException("This EPUB file has no package document.")
        opfStream.use { stream ->
            val parser = OoxmlXml.parser(stream)
            val titleText = StringBuilder()
            var inTitle = false
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                when (event) {
                    XmlPullParser.START_TAG -> when {
                        parser.name == "item" -> {
                            val id = OoxmlXml.attribute(parser, "id")
                            val href = OoxmlXml.attribute(parser, "href")
                            val media = OoxmlXml.attribute(parser, "media-type")
                            if (id != null && href != null && isDocument(media, href)) {
                                manifest[id] = resolve(baseDir, href)
                            }
                        }
                        parser.name == "itemref" -> {
                            val idref = OoxmlXml.attribute(parser, "idref")
                            if (idref != null) spine.add(idref)
                        }
                        parser.name != null && parser.name.endsWith(":title") -> {
                            inTitle = true
                            titleText.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> if (inTitle) titleText.append(parser.text ?: "")
                    XmlPullParser.END_TAG -> if (parser.name != null && parser.name.endsWith(":title")) {
                        inTitle = false
                        val value = titleText.toString().trim()
                        if (value.isNotEmpty()) title = value
                    }
                }
            }
        }
        if (spine.isEmpty()) throw ConversionException("This EPUB file has no reading order.")
        val chapters = ArrayList<EpubChapter>()
        var number = 0
        for (idref in spine) {
            if (chapters.size >= MAX_CHAPTERS) {
                throw ConversionException("The book has too many chapters to convert.")
            }
            val path = manifest[idref] ?: continue
            if (!zip.has(path)) continue
            val stream = zip.open(path) ?: continue
            val html = stream.use { TextIo.readText(it, MAX_CHAPTER_BYTES) }
            val text = HtmlTextExtractor.toText(html).trim()
            if (text.isEmpty()) continue
            number++
            chapters.add(
                EpubChapter(
                    "Chapter " + number,
                    text,
                    HtmlTextExtractor.bodyHtmlFragment(html)
                )
            )
        }
        if (chapters.isEmpty()) {
            throw ConversionException("No readable text was found in the book.")
        }
        return EpubBook(title, chapters)
    }

    private fun isDocument(mediaType: String?, href: String): Boolean {
        if (href.contains("://")) return false
        if (mediaType == null) return href.endsWith(".xhtml") || href.endsWith(".html") || href.endsWith(".htm")
        return mediaType.contains("html") || mediaType == "application/x-dtbook+xml"
    }

    private fun findRootfile(zip: SafePackage): String {
        zip.open("META-INF/container.xml")?.use { stream ->
            val parser = OoxmlXml.parser(stream)
            while (true) {
                val event = parser.next()
                if (event == XmlPullParser.END_DOCUMENT) break
                if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    val path = OoxmlXml.attribute(parser, "full-path")
                    if (!path.isNullOrBlank()) {
                        return ArchiveGuards.normalizeEntryName(path)
                            ?: throw ConversionException("This EPUB file has an unsafe root path.")
                    }
                }
            }
        }
        return zip.names("", ".opf").firstOrNull()
            ?: throw ConversionException("This EPUB file has no package document.")
    }

    /** Resolves a manifest href against the package directory. */
    private fun resolve(baseDir: String, href: String): String {
        val withoutFragment = href.substringBefore('#')
        val decoded = runCatching { Uri.decode(withoutFragment) }.getOrDefault(withoutFragment)
        val raw = if (baseDir.isEmpty()) decoded else baseDir + "/" + decoded
        val segments = ArrayList<String>()
        for (segment in raw.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(segment)
            }
        }
        return segments.joinToString("/")
    }
}
