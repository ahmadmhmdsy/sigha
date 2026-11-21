package com.sigha.converter.data

import com.sigha.converter.core.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {

    private fun entry(
        timestamp: Long,
        uri: String = "content://media/downloads/1",
        name: String = "out.pdf"
    ) = HistoryEntry(
        inputName = "in.jpg",
        outputName = name,
        fromId = "jpg",
        toId = "pdf",
        outputUri = uri,
        sizeBytes = 2048L,
        timestamp = timestamp
    )

    @Test
    fun roundTripRestoresEveryField() {
        val encoded = HistoryCodec.encode(listOf(entry(100L)))
        val decoded = HistoryCodec.decode(encoded)
        assertEquals(1, decoded.size)
        assertEquals(entry(100L), decoded.first())
    }

    @Test
    fun blankAndGarbageDecodeToEmptyList() {
        assertTrue(HistoryCodec.decode(null).isEmpty())
        assertTrue(HistoryCodec.decode("").isEmpty())
        assertTrue(HistoryCodec.decode("   ").isEmpty())
        assertTrue(HistoryCodec.decode("{not json").isEmpty())
        assertTrue(HistoryCodec.decode("""{"version":1,"entries":[{"inputName":42}]}""").isEmpty())
    }

    @Test
    fun legacyBareArrayIsAccepted() {
        val raw = """[{"inputName":"a.jpg","outputName":"b.pdf","fromId":"jpg","toId":"pdf"}]"""
        val decoded = HistoryCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("b.pdf", decoded.first().outputName)
        assertEquals("", decoded.first().outputUri)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val raw = """{"version":1,"entries":[{"inputName":"a","outputName":"b","fromId":"png","toId":"jpg","futureField":true}],"other":{}}"""
        assertEquals(1, HistoryCodec.decode(raw).size)
    }

    @Test
    fun oneCorruptEntryDoesNotDiscardTheRest() {
        // A row written by a different app version must cost one row, not the whole list.
        val raw = """{"version":2,"entries":[""" +
            """{"inputName":"a","outputName":"b","fromId":"jpg","toId":"pdf","timestamp":1},""" +
            """{"inputName":42,"outputName":"broken"},""" +
            """{"inputName":"c","outputName":"d","fromId":"png","toId":"jpg","timestamp":2}""" +
            """]}"""
        val decoded = HistoryCodec.decode(raw)
        assertEquals(2, decoded.size)
        assertEquals(listOf(1L, 2L), decoded.map { it.timestamp }.sorted())
    }

    @Test
    fun addPutsNewestEntryFirst() {
        val entries = listOf(entry(1L), entry(2L), entry(3L))
        val updated = HistoryCodec.add(entries, entry(99L))
        assertEquals(listOf(99L, 1L, 2L, 3L), updated.map { it.timestamp })
    }

    @Test
    fun listIsCappedAtFifty() {
        val entries = (1L..60L).map { entry(it) }
        val capped = HistoryCodec.add(entries, entry(1000L))
        assertEquals(HistoryCodec.MAX_ENTRIES, capped.size)
        assertEquals(1000L, capped.first().timestamp)
        assertEquals(49L, capped.last().timestamp)
        assertEquals(50, HistoryCodec.decode(HistoryCodec.encode(entries)).size)
    }

    @Test
    fun removeNeedsTimestampAndUri() {
        val entries = listOf(entry(7L, uri = "a"), entry(7L, uri = "b"))
        val remaining = HistoryCodec.remove(entries, 7L, "a")
        assertEquals(1, remaining.size)
        assertEquals("b", remaining.first().outputUri)
    }

    @Test
    fun typedNotesSurviveRoundTrip() {
        val noted = entry(5L).copy(note = Note.TextTruncated(100))
        val decoded = HistoryCodec.decode(HistoryCodec.encode(listOf(noted)))
        assertEquals(Note.TextTruncated(100), decoded.single().note)
    }

    @Test
    fun legacyStringNotesAreDroppedInsteadOfKillingTheEntry() {
        // 0.2.0 wrote user-facing English into "note" as a plain string. That JSON must still
        // decode (without the stale note), otherwise an upgrade would wipe the history list.
        val raw = """{"version":2,"entries":[{"inputName":"a","outputName":"b","fromId":"png","toId":"jpg","note":"Only the first 50 of 120 pages were read."}]}"""
        val decoded = HistoryCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("b", decoded.single().outputName)
        assertNull(decoded.single().note)
    }

    @Test
    fun entryWithDefaultFieldsSurvivesRoundTrip() {
        val minimal = HistoryEntry(inputName = "a", outputName = "b", fromId = "png", toId = "jpg")
        val decoded = HistoryCodec.decode(HistoryCodec.encode(listOf(minimal)))
        assertEquals(minimal, decoded.single())
    }
}
