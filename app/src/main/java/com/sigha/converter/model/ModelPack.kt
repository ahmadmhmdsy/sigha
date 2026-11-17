package com.sigha.converter.model

import androidx.annotation.StringRes

/**
 * A model pack: what the model packs screen shows, plus the exact files that make it work.
 *
 * Files install below `files/models/<id>/` and never leave app-private storage. When
 * [subdirectory] is set the files live one level deeper; OCR uses `tessdata` because
 * Tesseract requires that folder name next to its data path.
 *
 * The card copy is resource-backed: the catalogue is keyed by [id], so every language resolves
 * its own title and description from resources instead of an English literal.
 */
data class ModelPack(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val license: String,
    val licenseUrl: String,
    val files: List<PackFile>,
    val subdirectory: String? = null
) {
    /** Bytes the user downloads, shown on the pack card before installing. */
    val sizeBytes: Long = files.sumOf { it.sizeBytes }

    fun info(): ModelPacks.PackInfo = ModelPacks.PackInfo(
        id = id,
        titleRes = titleRes,
        descriptionRes = descriptionRes,
        sizeBytes = sizeBytes,
        license = license,
        licenseUrl = licenseUrl
    )
}
