package com.sigha.converter.core

import android.content.Context
import android.net.Uri
import java.io.File

/** User-tunable options. Every field must have a safe default. */
data class ConversionOptions(
    val quality: Int = 85,
    val maxDimension: Int? = null,
    val dpi: Int = 150,
    val pages: String? = null,
    val bitrateKbps: Int? = null,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val ocrLanguage: String? = null,
    val extra: Map<String, String> = emptyMap()
)

/** Everything an engine needs to run one conversion step. */
data class ConversionRequest(
    val context: Context,
    val input: Uri,
    val from: FileFormat,
    val to: FileFormat,
    /** Engine writes the finished file here (a private cache file). */
    val outputFile: File,
    val options: ConversionOptions = ConversionOptions(),
    val onProgress: (Float) -> Unit = {},
    val isCancelled: () -> Boolean = { false }
)

/** One produced file. Most steps produce exactly one. */
data class ConversionResult(val outputs: List<File>, val note: String? = null)

/** Thrown by engines for user-facing failures; message is shown as-is. */
class ConversionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * A single edge in the conversion graph. Engines implement exactly one arrow
 * (optionally several, if the same code path serves them) and never orchestrate routes.
 */
interface Converter {
    val id: String
    val inputs: Set<FileFormat>
    val outputs: Set<FileFormat>
    suspend fun convert(request: ConversionRequest): ConversionResult
}
