package com.sigha.converter.engine.office

import android.util.Xml
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.zip.ZipFile

/** Copies a content Uri into the cache and deletes it when the conversion ends. */
internal object OfficeIo {

    const val BUFFER_SIZE = 64 * 1024
    const val MAX_PACKAGE_BYTES = 512L * 1024L * 1024L
    private const val WORK_DIR_NAME = "sigha-work"
    private const val MAX_TEMP_AGE_MS = 6L * 60L * 60L * 1000L

    fun copyToTemp(request: ConversionRequest, suffix: String): File {
        val base = request.context.cacheDir ?: request.context.filesDir
        val dir = File(base, WORK_DIR_NAME)
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            throw ConversionException("Not enough storage space to start the conversion.")
        }
        pruneOldFiles(dir)
        val temp = File(dir, UUID.randomUUID().toString() + suffix)
        try {
            val stream = request.context.contentResolver.openInputStream(request.input)
                ?: throw ConversionException("Cannot read the selected file.")
            var total = 0L
            stream.use { input ->
                BufferedOutputStream(temp.outputStream(), BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        if (total > MAX_PACKAGE_BYTES) {
                            throw ConversionException("The document is larger than the 512 MB conversion limit.")
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            deleteQuietly(temp)
            if (error is IOException) throw ConversionException("Cannot copy the selected file.", error)
            throw error
        }
        return temp
    }

    fun deleteQuietly(file: File) {
        runCatching {
            if (file.exists()) file.delete()
        }
    }

    private fun pruneOldFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_TEMP_AGE_MS
        runCatching {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val stamp = file.lastModified()
                    if (stamp > 0L && stamp < cutoff) file.delete()
                }
            }
        }
    }
}

/**
 * Read-only view of an Office Open XML or OpenDocument package with zip-slip
 * and expansion guards. Part streams are capped so a malicious package cannot
 * stream unbounded data into a converter.
 */
internal class SafePackage private constructor(private val zip: ZipFile) : Closeable {

    companion object {
        private const val MAX_ENTRIES = 50_000
        private const val MAX_EXPANDED_BYTES = 2L * 1024L * 1024L * 1024L
        private const val MAX_PART_BYTES = 512L * 1024L * 1024L

        fun open(file: File): SafePackage {
            val zip = try {
                ZipFile(file)
            } catch (error: IOException) {
                throw ConversionException("This file is not a valid Office document.", error)
            }
            try {
                var count = 0
                var total = 0L
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    count++
                    if (count > MAX_ENTRIES) {
                        throw ConversionException("The document has too many internal parts.")
                    }
                    if (entry.name.startsWith("/") || entry.name.contains("..")) {
                        throw ConversionException("The document contains an unsafe part name.")
                    }
                    if (entry.size > 0L) total += entry.size
                    if (total > MAX_EXPANDED_BYTES) {
                        throw ConversionException("The document expands beyond the 2 GB limit.")
                    }
                }
            } catch (error: Throwable) {
                runCatching { zip.close() }
                throw error
            }
            return SafePackage(zip)
        }
    }

    fun has(name: String): Boolean = zip.getEntry(name) != null

    fun open(name: String, maxBytes: Long = MAX_PART_BYTES): InputStream? {
        val entry = zip.getEntry(name) ?: return null
        if (entry.isDirectory) return null
        if (entry.size > maxBytes) {
            throw ConversionException("The document part \"" + name + "\" is too large.")
        }
        val raw = zip.getInputStream(entry) ?: return null
        return BufferedInputStream(LimitedInputStream(raw, maxBytes), OfficeIo.BUFFER_SIZE)
    }

    /** Entry names under [prefix] ending with [suffix], in zip order. */
    fun names(prefix: String, suffix: String): List<String> {
        val result = ArrayList<String>()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val name = entries.nextElement().name
            if (name.startsWith(prefix) && name.endsWith(suffix)) result.add(name)
        }
        return result
    }

    override fun close() {
        runCatching { zip.close() }
    }
}

/** Throws as soon as the wrapped stream reads more than [limit] bytes. */
internal class LimitedInputStream(
    private val delegate: InputStream,
    private val limit: Long
) : InputStream() {

    private var count = 0L

    override fun read(): Int {
        val value = delegate.read()
        if (value >= 0) {
            count++
            checkLimit()
        }
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = delegate.read(buffer, offset, length)
        if (read > 0) {
            count += read
            checkLimit()
        }
        return read
    }

    override fun available(): Int = delegate.available()

    override fun close() {
        delegate.close()
    }

    private fun checkLimit() {
        if (count > limit) {
            throw ConversionException("The document contains an oversized part.")
        }
    }
}

/** Pull-parser setup and XML escaping shared by the Office readers. */
internal object OoxmlXml {

    fun parser(input: InputStream): XmlPullParser {
        val parser = Xml.newPullParser()
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        } catch (_: XmlPullParserException) {
            // KXmlParser supports namespaces, but keep going if a provider does not.
        }
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        } catch (_: XmlPullParserException) {
            // The parser then refuses DOCTYPE declarations instead of expanding them.
        }
        parser.setInput(input, null)
        return parser
    }

    fun attribute(parser: XmlPullParser, name: String): String? =
        parser.getAttributeValue(null, name)

    fun escape(text: String): String {
        if (text.none { it == '&' || it == '<' || it == '>' || it == '"' }) return text
        val builder = StringBuilder(text.length + 16)
        for (character in text) {
            when (character) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                else -> builder.append(character)
            }
        }
        return builder.toString()
    }
}
