package com.sigha.converter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "sigha_history")

/** Persists the last [HistoryCodec.MAX_ENTRIES] conversions as JSON inside DataStore. */
class HistoryStore(context: Context) {

    private val store = context.applicationContext.historyDataStore

    val entries: StateFlow<List<HistoryEntry>> = store.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> HistoryCodec.decode(preferences[KEY_ENTRIES]) }
        .stateIn(AppScope, SharingStarted.Eagerly, emptyList())

    suspend fun record(entry: HistoryEntry) {
        store.edit { preferences ->
            val current = HistoryCodec.decode(preferences[KEY_ENTRIES])
            preferences[KEY_ENTRIES] = HistoryCodec.encode(HistoryCodec.add(current, entry))
        }
    }

    suspend fun remove(timestamp: Long, outputUri: String) {
        store.edit { preferences ->
            val current = HistoryCodec.decode(preferences[KEY_ENTRIES])
            preferences[KEY_ENTRIES] = HistoryCodec.encode(HistoryCodec.remove(current, timestamp, outputUri))
        }
    }

    suspend fun clear() {
        store.edit { preferences -> preferences.remove(KEY_ENTRIES) }
    }

    private companion object {
        val KEY_ENTRIES = stringPreferencesKey("entries_json")
    }
}
