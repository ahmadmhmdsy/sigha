package com.sigha.converter.data

import com.sigha.converter.core.Note
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

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
    val error: String? = null,
    /** Language-independent failure kind name; the UI localizes it. */
    val errorKind: String? = null,
    /** Typed engine note (page cap, OCR fallback, truncation, ...); the UI localizes it. */
    val note: Note? = null
)

/**
 * JSON persistence for the history list. Deliberately defensive: a corrupted or partially
 * written payload must never crash the app - it degrades to an empty list.
 */
object HistoryCodec {

    const val MAX_ENTRIES = 50

    @Serializable
    private data class Payload(
        val version: Int = 2,
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
        // Notes used to be free-text English strings; drop those before decoding so a legacy
        // history file keeps its entries instead of failing into an empty list.
        val sanitized = stripLegacyNotes(raw)
        // Decode entry by entry: one row written by a different app version (a renamed field,
        // a note shape we no longer model) must not wipe the other 49 conversions the user
        // expected to keep. Rows that still fail are dropped silently.
        val root = runCatching { json.parseToJsonElement(sanitized) }.getOrNull()
            ?: return emptyList()
        val array = when (root) {
            is JsonObject -> root["entries"] as? JsonArray
            is JsonArray -> root
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching {
                json.decodeFromJsonElement<HistoryEntry>(stripLegacyNote(element))
            }.getOrNull()
        }.filter { it.inputName.isNotBlank() && it.outputName.isNotBlank() }.take(MAX_ENTRIES)
    }

    /**
     * Version 2 and earlier stored `"note"` as a plain string. [HistoryEntry.note] is now a
     * [Note] object, so unknown string notes are removed from the JSON tree and the rest of the
     * entry survives. A JSON object/array note (the new shape) is left untouched.
     */
    private fun stripLegacyNotes(raw: String): String {
        val root = try {
            json.parseToJsonElement(raw)
        } catch (_: Exception) {
            return raw
        }
        val cleaned = when (root) {
            is JsonObject -> {
                val entries = root["entries"] as? JsonArray
                if (entries == null) {
                    root
                } else {
                    JsonObject(root + ("entries" to JsonArray(entries.map(::stripLegacyNote))))
                }
            }
            is JsonArray -> JsonArray(root.map(::stripLegacyNote))
            else -> root
        }
        return cleaned.toString()
    }

    private fun stripLegacyNote(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        val note = element["note"] ?: return element
        val typed = note is JsonObject || note is JsonNull
        return if (typed) element else JsonObject(element - "note")
    }

    fun encode(entries: List<HistoryEntry>): String =
        json.encodeToString(Payload(version = 2, entries = entries.take(MAX_ENTRIES)))

    /** Newest first, capped at [MAX_ENTRIES]. */
    fun add(entries: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + entries).take(MAX_ENTRIES)

    fun remove(entries: List<HistoryEntry>, timestamp: Long, outputUri: String): List<HistoryEntry> =
        entries.filterNot { it.timestamp == timestamp && it.outputUri == outputUri }
}
