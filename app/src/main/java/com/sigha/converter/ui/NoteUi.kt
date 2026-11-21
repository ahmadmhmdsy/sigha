package com.sigha.converter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sigha.converter.R
import com.sigha.converter.core.Note

/**
 * Turns a language-independent [Note] into display text for the current locale.
 *
 * This is the note counterpart of [headlineRes] for failures: producers never write user copy,
 * and every counted sentence goes through <plurals> so Arabic gets its six categories.
 * A [Note.Composite] (several facts for one conversion) becomes one sentence per line.
 */
@Composable
fun noteText(note: Note?): String? = when (note) {
    null -> null
    is Note.PagesLimited -> pluralStringResource(
        R.plurals.note_pages_limited,
        // The sentence counts the pages that were read, not the pages in the document:
        // Arabic must pick one/two/few/many from note.read.
        note.read,
        note.read,
        note.total
    )
    is Note.PagesUnparsed -> pluralStringResource(
        R.plurals.note_pages_unparsed,
        note.read,
        note.read
    )
    Note.PagesMissing -> stringResource(R.string.note_pages_missing)
    is Note.PagesCapped -> pluralStringResource(
        R.plurals.note_pages_capped,
        note.read,
        note.read
    )
    is Note.TextTruncated -> pluralStringResource(
        R.plurals.note_text_truncated,
        note.maxLines,
        note.maxLines
    )
    Note.FolderFallback -> stringResource(R.string.ui_note_output_folder_fallback)
    is Note.GifTrimmed -> pluralStringResource(
        R.plurals.note_gif_trimmed,
        note.maxSeconds,
        note.maxSeconds
    )
    Note.SoftwareEncoder -> stringResource(R.string.note_software_encoder)
    is Note.ArchiveDuplicates -> pluralStringResource(
        R.plurals.note_archive_duplicates,
        note.count,
        note.count
    )
    is Note.ArchiveSymlinks -> pluralStringResource(
        R.plurals.note_archive_symlinks,
        note.count,
        note.count
    )
    is Note.ArchiveDeletedEntries -> pluralStringResource(
        R.plurals.note_archive_deleted_entries,
        note.count,
        note.count
    )
    Note.TarGzOutput -> stringResource(R.string.note_tar_gz_output)
    Note.OfficeTextOnly -> stringResource(R.string.note_office_text_only)
    Note.OfficeImagesOmitted -> stringResource(R.string.note_office_images_omitted)
    Note.OfficeSimpleTable -> stringResource(R.string.note_office_simple_table)
    is Note.CsvRowsWritten -> pluralStringResource(
        R.plurals.note_csv_rows_written,
        note.rows,
        note.rows
    )
    Note.CsvHeaderOnly -> stringResource(R.string.note_csv_header_only)
    Note.JsonValuesAsStrings -> stringResource(R.string.note_json_values_as_strings)
    is Note.JsonObjectsExported -> pluralStringResource(
        R.plurals.note_json_objects_exported,
        note.count,
        note.count
    )
    Note.PptxTextOnly -> stringResource(R.string.note_pptx_text_only)
    Note.EpubTextOnly -> stringResource(R.string.note_epub_text_only)
    Note.EpubHtmlOnly -> stringResource(R.string.note_epub_html_only)
    Note.BookTruncated -> stringResource(R.string.note_book_truncated)
    Note.FirstSheetOnly -> stringResource(R.string.note_first_sheet_only)
    Note.DisplayedCellText -> stringResource(R.string.note_displayed_cell_text)
    Note.DatesAsSerial -> stringResource(R.string.note_dates_as_serial)
    Note.MarkdownSimplified -> stringResource(R.string.note_markdown_simplified)
    Note.OcrFallback -> stringResource(R.string.note_ocr_fallback)
    is Note.PartialPublish -> pluralStringResource(
        R.plurals.note_partial_publish,
        note.failed,
        note.failed
    )
    is Note.Composite -> compositeNoteText(note.parts)
}

@Composable
private fun compositeNoteText(parts: List<Note>): String? {
    val lines = ArrayList<String>(parts.size)
    for (part in parts) {
        val text = noteText(part)
        if (!text.isNullOrBlank() && text !in lines) lines += text
    }
    return if (lines.isEmpty()) null else lines.joinToString("\n")
}
