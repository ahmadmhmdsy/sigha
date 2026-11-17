package com.sigha.converter.model

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class DigestTest {

    @Test
    fun knownDigestsMatch() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Digest.sha256("abc".toByteArray())
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Digest.sha256(ByteArray(0))
        )
    }

    @Test
    fun hexIsLowercaseAndZeroPadded() {
        assertEquals("0001ff", Digest.toHex(byteArrayOf(0, 1, -1)))
    }

    @Test
    fun fileAndStreamProduceTheSameDigest() {
        val file = File.createTempFile("sigha-digest", ".bin")
        try {
            file.writeBytes("abc".toByteArray())
            val expected = Digest.sha256("abc".toByteArray())
            assertEquals(expected, Digest.sha256(file))
            file.inputStream().use { input ->
                assertEquals(expected, Digest.sha256(input))
            }
        } finally {
            file.delete()
        }
    }
}
