package com.sigha.converter.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An informational, language-independent note about a finished conversion.
 *
 * Engines report *what* happened as a typed value; the UI turns it into a localized sentence
 * (see ui/NoteUi.kt). This mirrors [ConversionFailureKind]: no engine may hand user-facing
 * English copy to a screen, because an engine has no locale.
 *
 * Every kind is serializable so the history can persist the note and restore it later. Kinds
 * document the facts, not the wording: the same kind may read differently in English and Arabic.
 */
@Serializable
sealed interface Note {

    /** Only a page window was read: the first [read] of [total] pages (no page range given). */
    @Serializable
    @SerialName("pages_limited")
    data class PagesLimited(val read: Int, val total: Int) : Note

    /** The typed page range was not understood, so the first [read] pages were read instead. */
    @Serializable
    @SerialName("pages_unparsed")
    data class PagesUnparsed(val read: Int) : Note

    /** None of the requested pages exist in the document, so nothing was read. */
    @Serializable
    @SerialName("pages_missing")
    data object PagesMissing : Note

    /** An explicit selection was larger than the hard cap: the first [read] pages were read. */
    @Serializable
    @SerialName("pages_capped")
    data class PagesCapped(val read: Int) : Note

    /** Text output was cut after [maxLines] lines. */
    @Serializable
    @SerialName("text_truncated")
    data class TextTruncated(val maxLines: Int) : Note

    /** The chosen output folder was unavailable, so the file went to Downloads instead. */
    @Serializable
    @SerialName("folder_fallback")
    data object FolderFallback : Note

    /** A GIF was trimmed to [maxSeconds] seconds, so the rest of the video was skipped. */
    @Serializable
    @SerialName("gif_trimmed")
    data class GifTrimmed(val maxSeconds: Int) : Note

    /** Hardware video encoding failed; the software encoder produced the output. */
    @Serializable
    @SerialName("software_encoder")
    data object SoftwareEncoder : Note

    /** [count] duplicate archive entries were skipped. */
    @Serializable
    @SerialName("archive_duplicates")
    data class ArchiveDuplicates(val count: Int) : Note

    /** [count] symbolic links were skipped (a ZIP cannot represent them faithfully). */
    @Serializable
    @SerialName("archive_symlinks")
    data class ArchiveSymlinks(val count: Int) : Note

    /** [count] deleted (0-byte placeholder) archive entries were skipped. */
    @Serializable
    @SerialName("archive_deleted_entries")
    data class ArchiveDeletedEntries(val count: Int) : Note

    /** The ".gz" output is really a gzip-compressed tar archive (.tar.gz). */
    @Serializable
    @SerialName("tar_gz_output")
    data object TarGzOutput : Note

    /** A document office format was exported as text only: tables and images are not carried over. */
    @Serializable
    @SerialName("office_text_only")
    data object OfficeTextOnly : Note

    /** DOCX -> PDF keeps text, headings and lists, but images and tables are not rendered. */
    @Serializable
    @SerialName("office_images_omitted")
    data object OfficeImagesOmitted : Note

    /** XLSX -> PDF produced a simple text table: styles, formulas and images are not rendered. */
    @Serializable
    @SerialName("office_simple_table")
    data object OfficeSimpleTable : Note

    /** CSV -> XLSX wrote [rows] rows; the first row became the header. */
    @Serializable
    @SerialName("csv_rows_written")
    data class CsvRowsWritten(val rows: Int) : Note

    /** The CSV had a header row but no data rows, so the JSON array is empty. */
    @Serializable
    @SerialName("csv_header_only")
    data object CsvHeaderOnly : Note

    /** Every JSON value is written as a string: numbers, booleans and nesting are not kept. */
    @Serializable
    @SerialName("json_values_as_strings")
    data object JsonValuesAsStrings : Note

    /** [count] top-level JSON objects were exported; nested values are not supported. */
    @Serializable
    @SerialName("json_objects_exported")
    data class JsonObjectsExported(val count: Int) : Note

    /** PPTX -> text exported slide text only; images, speaker notes and layout were dropped. */
    @Serializable
    @SerialName("pptx_text_only")
    data object PptxTextOnly : Note

    /** EPUB -> text: chapters are separated by a blank line; images and styling are not exported. */
    @Serializable
    @SerialName("epub_text_only")
    data object EpubTextOnly : Note

    /** EPUB -> HTML: one file with each chapter as a heading; images and styles are omitted. */
    @Serializable
    @SerialName("epub_html_only")
    data object EpubHtmlOnly : Note

    /** A book was very long, so only its first part was converted. */
    @Serializable
    @SerialName("book_truncated")
    data object BookTruncated : Note

    /** A spreadsheet was exported from its first sheet only. */
    @Serializable
    @SerialName("first_sheet_only")
    data object FirstSheetOnly : Note

    /** Cell values are the displayed text, not the underlying formulas. */
    @Serializable
    @SerialName("displayed_cell_text")
    data object DisplayedCellText : Note

    /** Dates stay as Excel serial numbers (number formats are not interpreted). */
    @Serializable
    @SerialName("dates_as_serial")
    data object DatesAsSerial : Note

    /** Markdown specifics (images, raw HTML) are omitted from the PDF. */
    @Serializable
    @SerialName("markdown_simplified")
    data object MarkdownSimplified : Note

    /** The PDF had no text layer, so its pages were read with OCR. */
    @Serializable
    @SerialName("ocr_fallback")
    data object OcrFallback : Note

    /**
     * Several notes for one conversion (for example OCR fallback plus a page window, or a
     * multi-hop route where every engine reported something). The UI renders each part.
     */
    @Serializable
    @SerialName("composite")
    data class Composite(val parts: List<Note>) : Note

    companion object {
        /**
         * Merges notes from every step of one conversion into a single value: null when there
         * is nothing to say, the note itself when there is one, otherwise a deduplicated
         * [Composite] in first-seen order. Nested composites are flattened.
         */
        fun combine(notes: List<Note?>): Note? {
            val flat = ArrayList<Note>(notes.size)
            fun add(note: Note) {
                when (note) {
                    is Composite -> note.parts.forEach { part -> add(part) }
                    else -> if (note !in flat) flat += note
                }
            }
            notes.forEach { note -> if (note != null) add(note) }
            return when (flat.size) {
                0 -> null
                1 -> flat.first()
                else -> Composite(flat)
            }
        }
    }
}
