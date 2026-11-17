package com.sigha.converter.engine.text

import android.content.Context
import android.net.Uri
import com.sigha.converter.core.ConversionException
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Text input/output shared by the text, data, subtitle, office and ebook engines.
 *
 * Encoding rules: BOM aware, strict UTF-8 first, then a conservative legacy
 * fallback (Windows-1256 when the bytes look like Arabic text, Latin-1
 * otherwise). All buffers are 64 KB and every read is bounded.
 */
internal object TextIo {

    const val BUFFER_SIZE = 64 * 1024
    const val MAX_TEXT_BYTES = 24L * 1024L * 1024L
    private const val SNIFF_BYTES = 32 * 1024

    private val CP1256: Charset? = runCatching { Charset.forName("windows-1256") }.getOrNull()

    /** Reads at most [maxBytes] and decodes with BOM/legacy detection. */
    fun readText(input: InputStream, maxBytes: Long = MAX_TEXT_BYTES): String =
        decode(readAll(input, maxBytes))

    fun readAll(input: InputStream, maxBytes: Long): ByteArray {
        val initial = minOf(maxBytes, 64L * 1024L).toInt().coerceAtLeast(32)
        val output = ByteArrayOutputStream(initial)
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw ConversionException("The file is too large for this conversion (limit 24 MB).")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): String {
        when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte() ->
                return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)

            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        if (bytes.isEmpty()) return ""
        decodeStrict(bytes, bytes.size, Charsets.UTF_8)?.let { return it }
        if (looksLikeLegacyArabic(bytes)) {
            CP1256?.let { charset ->
                decodeStrict(bytes, bytes.size, charset)?.let { return it }
            }
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    /** Buffered reader with BOM/sniffed encoding, for streaming converters. */
    fun reader(input: InputStream): BufferedReader {
        val buffered = BufferedInputStream(input, BUFFER_SIZE)
        val probe = ByteArray(SNIFF_BYTES)
        var total = 0
        try {
            while (total < probe.size) {
                val read = buffered.read(probe, total, probe.size - total)
                if (read < 0) break
                total += read
            }
        } catch (error: Exception) {
            runCatching { buffered.close() }
            throw error
        }
        val head = if (total == probe.size) probe else probe.copyOf(total)
        val sniffed = sniff(head)
        val prefix = head.copyOfRange(minOf(sniffed.second, head.size), head.size)
        val stream = PrefixInputStream(prefix, buffered)
        return BufferedReader(InputStreamReader(stream, sniffed.first), BUFFER_SIZE)
    }

    fun open(context: Context, uri: Uri): Source {
        val raw = context.contentResolver.openInputStream(uri)
            ?: throw ConversionException("Cannot read the selected file.")
        val size = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it > 0L }
        val counting = CountingInputStream(raw)
        return try {
            Source(reader(counting), counting, size)
        } catch (error: Throwable) {
            runCatching { counting.close() }
            throw error
        }
    }

    fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        writer(file.outputStream()).use { it.write(text) }
    }

    fun writer(output: OutputStream): BufferedWriter =
        BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8), BUFFER_SIZE)

    private fun sniff(head: ByteArray): Pair<Charset, Int> {
        if (head.size >= 3 &&
            head[0] == 0xEF.toByte() &&
            head[1] == 0xBB.toByte() &&
            head[2] == 0xBF.toByte()
        ) {
            return Charsets.UTF_8 to 3
        }
        if (head.size >= 2 && head[0] == 0xFF.toByte() && head[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE to 2
        }
        if (head.size >= 2 && head[0] == 0xFE.toByte() && head[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE to 2
        }
        if (looksLikeUtf8(head)) return Charsets.UTF_8 to 0
        if (looksLikeLegacyArabic(head)) return (CP1256 ?: Charsets.ISO_8859_1) to 0
        return Charsets.ISO_8859_1 to 0
    }

    private fun looksLikeUtf8(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        for (trim in 0..3) {
            val length = bytes.size - trim
            if (length <= 0) return true
            if (decodeStrict(bytes, length, Charsets.UTF_8) != null) return true
        }
        return false
    }

    /**
     * Legacy Arabic (Windows-1256) and Latin-1 both turn bytes above 0x7F into
     * letters, so counting decoded letters cannot tell them apart. Arabic words
     * produce runs of adjacent high bytes, while accented Latin text usually
     * has isolated high bytes: use that shape as the discriminator.
     */
    private fun looksLikeLegacyArabic(bytes: ByteArray): Boolean {
        var high = 0
        var neighbours = 0
        for (index in bytes.indices) {
            val value = bytes[index].toInt() and 0xFF
            if (value < 0x80) continue
            high++
            val before = index > 0 && (bytes[index - 1].toInt() and 0x80) != 0
            val after = index + 1 < bytes.size && (bytes[index + 1].toInt() and 0x80) != 0
            if (before || after) neighbours++
        }
        if (high < 4) return false
        return neighbours.toFloat() / high >= 0.6f
    }

    private fun decodeStrict(bytes: ByteArray, length: Int, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, 0, length))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    /** Current input for one conversion, with byte progress when the size is known. */
    class Source(
        val reader: BufferedReader,
        private val counter: CountingInputStream,
        val size: Long?
    ) : Closeable {

        val bytesRead: Long
            get() = counter.bytesRead

        fun progress(): Float = if (size != null && size > 0L) {
            (counter.bytesRead.toFloat() / size).coerceIn(0f, 0.99f)
        } else {
            0f
        }

        override fun close() {
            runCatching { reader.close() }
        }
    }
}

/** Counts bytes pulled through the stream so converters can report real progress. */
internal class CountingInputStream(delegate: InputStream) : FilterInputStream(delegate) {

    var bytesRead: Long = 0L
        private set

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) bytesRead++
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) bytesRead += count
        return count
    }
}

/** Replays sniffed bytes before delegating, so BOM sniffing needs no mark/reset. */
private class PrefixInputStream(
    private val prefix: ByteArray,
    private val delegate: InputStream
) : InputStream() {

    private var index = 0

    override fun read(): Int {
        if (index < prefix.size) return prefix[index++].toInt() and 0xFF
        return delegate.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (index < prefix.size) {
            val count = minOf(length, prefix.size - index)
            System.arraycopy(prefix, index, buffer, offset, count)
            index += count
            return count
        }
        return delegate.read(buffer, offset, length)
    }

    override fun available(): Int = (prefix.size - index) + delegate.available()

    override fun close() {
        delegate.close()
    }
}
