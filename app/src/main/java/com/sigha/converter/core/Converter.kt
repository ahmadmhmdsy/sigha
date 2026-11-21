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

/**
 * One produced file. Most steps produce exactly one.
 *
 * [note] is a language-independent [Note]: engines report what happened, the UI localizes it.
 * Engines must never put user-facing English copy here.
 */
data class ConversionResult(val outputs: List<File>, val note: Note? = null)

/**
 * Why a conversion failed, independent of any language. Engines and the runner classify a
 * failure here; the UI turns the kind into a localized headline and shows the exception
 * message only as expandable technical details.
 */
enum class ConversionFailureKind {
    UNKNOWN,
    TARGET_SAME,
    NO_ROUTE,
    EMPTY_OUTPUT,
    UNSUPPORTED_FORMAT,
    CORRUPT_INPUT,
    PASSWORD_PROTECTED,
    NO_TEXT_LAYER,
    MODEL_PACK_MISSING,
    TIMEOUT,
    NO_STORAGE,
    SAVE_FAILED,
    STORAGE_PERMISSION,
    OUTPUT_FOLDER_UNAVAILABLE,
    FILE_TOO_LARGE,
    DRM_PROTECTED,
    NO_AUDIO_TRACK,
    NO_VIDEO_TRACK
}

/**
 * Thrown by engines for failures. [message] is a developer-facing English diagnostic:
 * the UI shows a localized headline (from [kind]) and offers this text only as expandable
 * details, so engines must never assume the message is user-facing copy.
 */
class ConversionException(
    message: String,
    cause: Throwable? = null,
    val kind: ConversionFailureKind = ConversionFailureKind.UNKNOWN
) : Exception(message, cause)

/**
 * A single edge in the conversion graph. Engines implement exactly one arrow
 * (optionally several, if the same code path serves them) and never orchestrate routes.
 */
interface Converter {
    val id: String
    val inputs: Set<FileFormat>
    val outputs: Set<FileFormat>

    /**
     * True when one input may produce several files (for example PDF page export).
     * Such a converter may only be the LAST hop of a route: using it mid-route would
     * silently drop every output except the first one.
     */
    val producesMultipleOutputs: Boolean get() = false

    /** True when this edge needs an optional model pack that may not be installed yet. */
    val requiresModelPack: Boolean get() = false

    suspend fun convert(request: ConversionRequest): ConversionResult
}
