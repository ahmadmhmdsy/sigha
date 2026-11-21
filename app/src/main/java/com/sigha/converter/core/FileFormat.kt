package com.sigha.converter.core

/** Broad families used for grouping the format picker and for routing hints. */
enum class FormatCategory {
    IMAGE, PDF, DOCUMENT, SPREADSHEET, PRESENTATION, TEXT, AUDIO, VIDEO,
    ARCHIVE, DATA, SUBTITLE, EBOOK, AI
}

/**
 * One known file format. Pure data - no Android dependencies, so it is unit-testable.
 */
data class FileFormat(
    val id: String,
    val extension: String,
    val mime: String,
    val label: String,
    val category: FormatCategory
) {
    override fun toString(): String = id
}

/**
 * True when a source of [sourceCategory] with an audio-track fact of [hasAudio] can feed a target
 * of [targetCategory].
 *
 * A video without an audio track cannot produce any audio file: the audio engine has nothing to
 * decode, so offering those targets would guarantee a failure. [hasAudio] == null means the probe
 * could not tell, and unknown is treated optimistically so a failed probe never hides a target
 * that might work.
 */
fun supportsTargetCategory(
    sourceCategory: FormatCategory,
    hasAudio: Boolean?,
    targetCategory: FormatCategory
): Boolean = !(sourceCategory == FormatCategory.VIDEO &&
    hasAudio == false &&
    targetCategory == FormatCategory.AUDIO)

/**
 * True when the selection contains a video without an audio track, so the workbench should hide
 * audio targets for the whole batch and explain why. Unknown audio facts never hide anything.
 */
fun hidesAudioTargets(files: List<Pair<FormatCategory, Boolean?>>): Boolean =
    files.any { (category, hasAudio) ->
        category == FormatCategory.VIDEO && hasAudio == false
    }
