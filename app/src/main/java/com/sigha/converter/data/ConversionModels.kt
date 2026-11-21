package com.sigha.converter.data

import android.net.Uri
import com.sigha.converter.core.ConversionFailureKind
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory
import com.sigha.converter.core.Note

/** A user-selected source file together with what detection worked out about it. */
data class ConversionInput(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val format: FileFormat,
    /**
     * For video sources: whether the container has an audio track. Null means "not probed or
     * unknown", which is treated optimistically so a failed probe never hides a working target.
     */
    val hasAudio: Boolean? = null
)

/**
 * True when this file can feed a target of [targetCategory].
 *
 * A video without an audio track cannot produce any audio file (the audio engine has nothing to
 * decode), so those targets are filtered out of the workbench instead of being offered and
 * failing. An unknown probe result keeps every target.
 */
fun ConversionInput.supportsTargetCategory(targetCategory: FormatCategory): Boolean =
    com.sigha.converter.core.supportsTargetCategory(format.category, hasAudio, targetCategory)

/** Outcome of converting one input file. Never thrown: the queue keeps going after failures. */
data class ConversionItemResult(
    val input: ConversionInput,
    val target: FileFormat,
    val success: Boolean,
    val outputs: List<PublishedOutput> = emptyList(),
    /** English developer diagnostic; the UI shows it only as expandable details. */
    val errorMessage: String? = null,
    /** Localized headline is derived from this by the UI. */
    val errorKind: ConversionFailureKind = ConversionFailureKind.UNKNOWN,
    /** Typed informational note from the engine; the UI localizes it (see ui/NoteUi.kt). */
    val note: Note? = null,
    val durationMs: Long = 0L
)
