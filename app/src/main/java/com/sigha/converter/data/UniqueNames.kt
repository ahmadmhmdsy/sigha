package com.sigha.converter.data

/**
 * Builds a display name that does not collide with an existing file:
 * "report.pdf" -> "report (1).pdf" -> "report (2).pdf".
 *
 * Pure logic (the caller supplies the existence check) so it is unit-testable on the JVM.
 */
object UniqueNames {

    private const val MAX_ATTEMPTS = 10_000

    fun unique(desired: String, exists: (String) -> Boolean): String {
        val safe = desired.trim().ifBlank { "file" }
        if (!exists(safe)) return safe
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val extension = if (dot > 0) safe.substring(dot) else ""
        var index = 1
        while (index < MAX_ATTEMPTS) {
            val candidate = "$base ($index)$extension"
            if (!exists(candidate)) return candidate
            index++
        }
        return "$base ($index)$extension"
    }
}
