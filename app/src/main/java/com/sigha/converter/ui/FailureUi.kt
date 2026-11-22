package com.sigha.converter.ui

import androidx.annotation.StringRes
import com.sigha.converter.R
import com.sigha.converter.core.ConversionFailureKind

/**
 * Maps a language-independent failure kind to a localized headline. The English engine
 * diagnostic is never shown as the headline; screens offer it as expandable details.
 */
@StringRes
fun ConversionFailureKind.headlineRes(): Int = when (this) {
    ConversionFailureKind.TARGET_SAME -> R.string.err_target_same
    ConversionFailureKind.NO_ROUTE, ConversionFailureKind.UNSUPPORTED_FORMAT -> R.string.err_no_route
    ConversionFailureKind.EMPTY_OUTPUT -> R.string.err_empty_output
    ConversionFailureKind.CORRUPT_INPUT -> R.string.err_read_file
    ConversionFailureKind.PASSWORD_PROTECTED -> R.string.ui_error_password
    ConversionFailureKind.NO_TEXT_LAYER -> R.string.ui_error_no_text_layer
    ConversionFailureKind.MODEL_PACK_MISSING -> R.string.ui_error_model_pack_missing
    ConversionFailureKind.TIMEOUT -> R.string.ui_error_timeout
    ConversionFailureKind.NO_STORAGE -> R.string.ui_error_no_storage
    ConversionFailureKind.SAVE_FAILED -> R.string.err_save_failed
    ConversionFailureKind.STORAGE_PERMISSION -> R.string.err_storage_permission
    ConversionFailureKind.OUTPUT_FOLDER_UNAVAILABLE -> R.string.err_output_folder
    ConversionFailureKind.FILE_TOO_LARGE -> R.string.ui_error_file_too_large
    ConversionFailureKind.DRM_PROTECTED -> R.string.ui_error_drm
    ConversionFailureKind.NO_AUDIO_TRACK -> R.string.ui_error_no_audio
    ConversionFailureKind.NO_VIDEO_TRACK -> R.string.ui_error_no_video
    ConversionFailureKind.FILE_ACCESS_LOST -> R.string.err_file_access_lost
    ConversionFailureKind.UNKNOWN -> R.string.ui_error_unknown
}
