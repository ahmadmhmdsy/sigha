package com.sigha.converter.model

/**
 * One downloadable file inside a model pack.
 *
 * Pure data - no Android types - so the catalogue and its invariants stay unit-testable.
 * [sha256] is the lowercase hex digest of the whole file and is always verified before the
 * file is moved into its install location.
 */
data class PackFile(
    val fileName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long
)
