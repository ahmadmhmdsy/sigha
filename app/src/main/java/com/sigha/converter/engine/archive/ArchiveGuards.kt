package com.sigha.converter.engine.archive

/**
 * Safety limits and path normalization shared by every archive converter.
 *
 * Entry names are normalized to forward slashes with "." and empty segments
 * removed. Anything that could escape the extraction directory ("..", an
 * absolute path or a Windows drive letter) is rejected. Pure Kotlin, so the
 * rules are unit-testable on the JVM.
 */
object ArchiveGuards {

    const val BUFFER_SIZE = 64 * 1024
    const val MAX_TOTAL_BYTES = 2L * 1024L * 1024L * 1024L
    const val MAX_ENTRIES = 50_000
    const val MAX_SEVEN_ZIP_MEMORY_KB = 64 * 1024

    private const val MAX_NAME_LENGTH = 4096
    private const val MAX_SEGMENT_LENGTH = 255

    /**
     * Returns the normalized, safe entry name or null when the name is unsafe
     * or has no usable segment (for example "/", "." or "./").
     */
    fun normalizeEntryName(name: String): String? {
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) return null
        if (name.contains('\u0000')) return null
        val unified = name.replace('\\', '/')
        if (unified.startsWith("/")) return null
        if (unified.length >= 2 && unified[1] == ':' && unified[0].isLetter()) return null
        val segments = ArrayList<String>(4)
        for (segment in unified.split('/')) {
            when (segment) {
                "", "." -> continue
                ".." -> return null
                else -> {
                    if (segment.length > MAX_SEGMENT_LENGTH) return null
                    segments.add(segment)
                }
            }
        }
        if (segments.isEmpty()) return null
        return segments.joinToString("/")
    }

    fun isSafeEntryName(name: String): Boolean = normalizeEntryName(name) != null
}
