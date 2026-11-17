package com.sigha.converter.model

/**
 * The OCR languages Sigha ships as traineddata files. Pure logic so the parsing and the
 * installed-language reporting can be unit-tested without a device.
 */
object OcrLanguages {

    const val DEFAULT: String = "eng"

    /** Order used everywhere: catalogue, settings and the Tesseract language string. */
    val SUPPORTED: List<String> = listOf("eng", "ara")

    private const val SUFFIX = ".traineddata"

    /** Maps installed file names to supported language codes, in [SUPPORTED] order. */
    fun fromFileNames(names: Collection<String>): List<String> {
        val present = names.mapNotNull { name ->
            val lower = name.trim().lowercase()
            if (lower.endsWith(SUFFIX)) lower.removeSuffix(SUFFIX) else null
        }.toSet()
        return SUPPORTED.filter { it in present }
    }

    /**
     * Parses the user's request ("eng", "ara" or "eng+ara", forgiving about separators and
     * case). Unknown languages are dropped; callers report unsupported when nothing is left.
     */
    fun requested(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return listOf(DEFAULT)
        val tokens = raw.lowercase()
            .split('+', ',', ';', '&', '|', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        return SUPPORTED.filter { it in tokens }
    }

    /** "eng+ara" for the Tesseract API; empty when no supported language is selected. */
    fun code(languages: Collection<String>): String =
        SUPPORTED.filter { it in languages }.joinToString("+")
}
