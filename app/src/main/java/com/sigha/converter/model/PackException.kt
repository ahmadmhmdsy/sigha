package com.sigha.converter.model

/**
 * A model pack could not be downloaded or installed. The message is short and safe to show
 * to the user as-is; the model packs screen already falls back to a generic string.
 */
class PackException(message: String, cause: Throwable? = null) : Exception(message, cause)
