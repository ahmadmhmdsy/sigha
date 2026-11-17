package com.sigha.converter.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One finished (or failed) conversion, kept so users can reopen their output later. */
@Serializable
data class HistoryEntry(
    val inputName: String,
    val outputName: String,
    val fromId: String,
    val toId: String,
    val outputUri: String = "",
    val outputPath: String? = null,
    val sizeBytes: Long = 0L,
    val timestamp: Long = 0L,
    val success: Boolean = true,
    val error: String? = null
)

/**
 * JSON persistence for the history list. Deliberately defensive: a corrupted or partially
 * written payload must never crash the app - it degrades to an empty list.
 */
object HistoryCodec {

    const val MAX_ENTRIES = 50

    @Serializable
    private data class Payload(
        val version: Int = 1,
        val entries: List<HistoryEntry> = emptyList()
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<Payload>(raw).entries.take(MAX_ENTRIES)
        } catch (_: Exception) {
            // Older or unexpected shape: accept a bare array, otherwise give up quietly.
            try {
                json.decodeFromString<List<HistoryEntry>>(raw).take(MAX_ENTRIES)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun encode(entries: List<HistoryEntry>): String =
        json.encodeToString(Payload(version = 1, entries = entries.take(MAX_ENTRIES)))

    /** Newest first, capped at [MAX_ENTRIES]. */
    fun add(entries: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + entries).take(MAX_ENTRIES)

    fun remove(entries: List<HistoryEntry>, timestamp: Long, outputUri: String): List<HistoryEntry> =
        entries.filterNot { it.timestamp == timestamp && it.outputUri == outputUri }
}
