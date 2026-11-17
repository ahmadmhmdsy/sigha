package com.sigha.converter.data

import android.net.Uri
import com.sigha.converter.core.FileFormat

/** A user-selected source file together with what detection worked out about it. */
data class ConversionInput(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val format: FileFormat
)

/** Outcome of converting one input file. Never thrown: the queue keeps going after failures. */
data class ConversionItemResult(
    val input: ConversionInput,
    val target: FileFormat,
    val success: Boolean,
    val outputs: List<PublishedOutput> = emptyList(),
    val errorMessage: String? = null,
    val durationMs: Long = 0L
)
