package com.sigha.converter.model

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * SHA-256 helpers used to verify downloaded packs. Pure JVM, so the digest format is covered
 * by a normal unit test.
 */
object Digest {

    private const val BUFFER_SIZE = 64 * 1024
    private const val HEX = "0123456789abcdef"

    fun sha256(file: File): String = file.inputStream().use { sha256(it) }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return toHex(digest.digest())
    }

    fun sha256(bytes: ByteArray): String =
        toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    /** Lowercase hex, matching the constants in [PackCatalog]. */
    fun toHex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            out.append(HEX[value ushr 4]).append(HEX[value and 0x0F])
        }
        return out.toString()
    }
}
