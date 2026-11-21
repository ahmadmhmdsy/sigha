package com.sigha.converter.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Catalogue of formats the app knows about, plus URI detection.
 * Kept deliberately data-first: routing and UI both read from here.
 */
class FormatRegistry {
    private val byId = LinkedHashMap<String, FileFormat>()
    private val byExtension = HashMap<String, FileFormat>()
    private val byMime = HashMap<String, FileFormat>()

    init {
        DATA.forEach { add(it) }
    }

    private fun add(format: FileFormat) {
        byId[format.id] = format
        byExtension.putIfAbsent(format.extension, format)
        if (format.mime.isNotBlank()) byMime.putIfAbsent(format.mime, format)
    }

    fun get(id: String): FileFormat? = byId[id]

    fun all(): List<FileFormat> = byId.values.toList()

    fun byCategory(category: FormatCategory): List<FileFormat> =
        byId.values.filter { it.category == category }

    fun fromExtension(extension: String): FileFormat? =
        byExtension[extension.trim().lowercase().removePrefix(".")]

    fun fromMime(mime: String?): FileFormat? {
        if (mime.isNullOrBlank()) return null
        byMime[mime]?.let { return it }
        // Common aliases that Android uses differently per OEM.
        // Deliberately absent: application/vnd.ms-excel and application/msword. Legacy
        // .xls/.doc are not in the catalogue, and aliasing them to a missing format would
        // advertise a conversion the engine cannot perform.
        return when (mime) {
            "image/jpg", "image/pjpeg" -> byExtension["jpg"]
            "audio/mp4", "audio/x-m4a" -> byExtension["m4a"]
            "audio/mpeg" -> byExtension["mp3"]
            "video/quicktime" -> byExtension["mov"]
            else -> null
        }
    }

    /**
     * Best-effort detection: MIME first (authoritative from the content provider), then file name.
     */
    fun detect(context: Context, uri: Uri): DetectedFile? {
        val resolver = context.contentResolver
        var mime: String? = null
        var name: String? = null
        var size: Long? = null
        try {
            mime = resolver.getType(uri)
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            // Non-fatal: fall through to extension-only detection.
        }
        val extension = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        // Only catalogue formats are detected. An earlier version invented an "unknown"
        // DATA format for unknown MIME types; that id then leaked into targets, history and
        // failure messages, and could never be routed anywhere. Skipping the file produces
        // the honest "can't read this file" message instead.
        val format = fromMime(mime) ?: fromExtension(extension)
        return if (format == null) null else DetectedFile(uri, name ?: "file", size, format)
    }

    companion object {
        private fun f(id: String, extension: String, mime: String, label: String, category: FormatCategory) =
            FileFormat(id, extension, mime, label, category)

        /** Formats the app starts with. Order is the order users see inside a category. */
        val DATA: List<FileFormat> = listOf(
            // Images
            f("jpg", "jpg", "image/jpeg", "JPG", FormatCategory.IMAGE),
            f("png", "png", "image/png", "PNG", FormatCategory.IMAGE),
            f("webp", "webp", "image/webp", "WebP", FormatCategory.IMAGE),
            f("bmp", "bmp", "image/bmp", "BMP", FormatCategory.IMAGE),
            f("gif", "gif", "image/gif", "GIF", FormatCategory.IMAGE),
            f("tiff", "tiff", "image/tiff", "TIFF", FormatCategory.IMAGE),
            f("heic", "heic", "image/heic", "HEIC", FormatCategory.IMAGE),
            f("avif", "avif", "image/avif", "AVIF", FormatCategory.IMAGE),
            // PDF
            f("pdf", "pdf", "application/pdf", "PDF", FormatCategory.PDF),
            // Text / documents
            f("txt", "txt", "text/plain", "TXT", FormatCategory.TEXT),
            f("md", "md", "text/markdown", "Markdown", FormatCategory.TEXT),
            f("html", "html", "text/html", "HTML", FormatCategory.TEXT),
            f("docx", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word", FormatCategory.DOCUMENT),
            f("odt", "odt", "application/vnd.oasis.opendocument.text", "ODT", FormatCategory.DOCUMENT),
            f("rtf", "rtf", "application/rtf", "RTF", FormatCategory.DOCUMENT),
            f("xlsx", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel", FormatCategory.SPREADSHEET),
            f("ods", "ods", "application/vnd.oasis.opendocument.spreadsheet", "ODS", FormatCategory.SPREADSHEET),
            f("csv", "csv", "text/csv", "CSV", FormatCategory.SPREADSHEET),
            f("tsv", "tsv", "text/tab-separated-values", "TSV", FormatCategory.SPREADSHEET),
            f("pptx", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint", FormatCategory.PRESENTATION),
            // Audio
            f("mp3", "mp3", "audio/mpeg", "MP3", FormatCategory.AUDIO),
            f("m4a", "m4a", "audio/mp4", "M4A", FormatCategory.AUDIO),
            f("aac", "aac", "audio/aac", "AAC", FormatCategory.AUDIO),
            f("wav", "wav", "audio/wav", "WAV", FormatCategory.AUDIO),
            f("flac", "flac", "audio/flac", "FLAC", FormatCategory.AUDIO),
            f("ogg", "ogg", "audio/ogg", "OGG", FormatCategory.AUDIO),
            f("opus", "opus", "audio/opus", "OPUS", FormatCategory.AUDIO),
            f("amr", "amr", "audio/amr", "AMR", FormatCategory.AUDIO),
            // Video
            f("mp4", "mp4", "video/mp4", "MP4", FormatCategory.VIDEO),
            f("mkv", "mkv", "video/x-matroska", "MKV", FormatCategory.VIDEO),
            f("mov", "mov", "video/quicktime", "MOV", FormatCategory.VIDEO),
            f("avi", "avi", "video/x-msvideo", "AVI", FormatCategory.VIDEO),
            f("webm", "webm", "video/webm", "WebM", FormatCategory.VIDEO),
            f("flv", "flv", "video/x-flv", "FLV", FormatCategory.VIDEO),
            // Archives
            f("zip", "zip", "application/zip", "ZIP", FormatCategory.ARCHIVE),
            f("7z", "7z", "application/x-7z-compressed", "7Z", FormatCategory.ARCHIVE),
            f("tar", "tar", "application/x-tar", "TAR", FormatCategory.ARCHIVE),
            f("gz", "gz", "application/gzip", "GZ", FormatCategory.ARCHIVE),
            f("bz2", "bz2", "application/x-bzip2", "BZ2", FormatCategory.ARCHIVE),
            f("xz", "xz", "application/x-xz", "XZ", FormatCategory.ARCHIVE),
            // Data
            f("json", "json", "application/json", "JSON", FormatCategory.DATA),
            f("yaml", "yaml", "application/yaml", "YAML", FormatCategory.DATA),
            f("xml", "xml", "application/xml", "XML", FormatCategory.DATA),
            // Subtitles
            f("srt", "srt", "application/x-subrip", "SRT", FormatCategory.SUBTITLE),
            f("vtt", "vtt", "text/vtt", "VTT", FormatCategory.SUBTITLE),
            // E-books
            f("epub", "epub", "application/epub+zip", "EPUB", FormatCategory.EBOOK),
            f("mobi", "mobi", "application/x-mobipocket-ebook", "MOBI", FormatCategory.EBOOK),
            f("fb2", "fb2", "application/x-fictionbook+xml", "FB2", FormatCategory.EBOOK)
        )
    }
}

data class DetectedFile(
    val uri: Uri,
    val displayName: String,
    val size: Long?,
    val format: FileFormat
)
