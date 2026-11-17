package com.sigha.converter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where finished files are written. */
enum class OutputMode {
    /** Public Downloads/Sigha folder (MediaStore on API 29+, direct file on 26-28). */
    DOWNLOADS,

    /** A folder the user picked with the system folder picker (persisted perms). */
    FOLDER
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val outputMode: OutputMode = OutputMode.DOWNLOADS,
    val outputFolderUri: String? = null,
    /** Tesseract language tag, e.g. "eng", "ara", "eng+ara". */
    val ocrLanguage: String = DEFAULT_OCR_LANGUAGE
) {
    companion object {
        const val DEFAULT_OCR_LANGUAGE = "eng"
    }
}

// A truncated or corrupted preferences file must not brick Settings: replace it with the
// defaults instead of throwing on every read.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sigha_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/** User preferences backed by DataStore, exposed as a hot StateFlow. */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    val settings: StateFlow<AppSettings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> read(preferences) }
        .stateIn(AppScope, SharingStarted.Eagerly, AppSettings())

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setOutputMode(mode: OutputMode) {
        store.edit { it[KEY_OUTPUT_MODE] = mode.name }
    }

    suspend fun setOutputFolder(uri: String?) {
        store.edit { preferences ->
            if (uri.isNullOrBlank()) preferences.remove(KEY_OUTPUT_FOLDER) else preferences[KEY_OUTPUT_FOLDER] = uri
        }
    }

    /**
     * Durable marker for a batch in progress. If the process dies mid-batch the flag stays set,
     * and the next launch can tell the user the last batch was interrupted instead of silently
     * forgetting it.
     */
    suspend fun markBatchActive(active: Boolean) {
        store.edit { it[KEY_BATCH_ACTIVE] = active }
    }

    /** Reads and clears the interrupted-batch marker; true means the previous process died. */
    suspend fun takeInterruptedBatch(): Boolean {
        val preferences = store.data.first()
        val interrupted = preferences[KEY_BATCH_ACTIVE] == true
        if (interrupted) store.edit { it[KEY_BATCH_ACTIVE] = false }
        return interrupted
    }

    suspend fun setOcrLanguage(language: String) {
        store.edit { it[KEY_OCR_LANGUAGE] = language.trim().lowercase().ifBlank { AppSettings.DEFAULT_OCR_LANGUAGE } }
    }

    private fun read(preferences: Preferences): AppSettings {
        val theme = ThemeMode.entries.firstOrNull { it.name == preferences[KEY_THEME] }
        val mode = OutputMode.entries.firstOrNull { it.name == preferences[KEY_OUTPUT_MODE] }
        return AppSettings(
            themeMode = theme ?: ThemeMode.SYSTEM,
            outputMode = mode ?: OutputMode.DOWNLOADS,
            outputFolderUri = preferences[KEY_OUTPUT_FOLDER]?.takeIf { it.isNotBlank() },
            ocrLanguage = preferences[KEY_OCR_LANGUAGE]
                ?.takeIf { it.isNotBlank() }
                ?: AppSettings.DEFAULT_OCR_LANGUAGE
        )
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_OUTPUT_MODE = stringPreferencesKey("output_mode")
        val KEY_OUTPUT_FOLDER = stringPreferencesKey("output_folder_uri")
        val KEY_OCR_LANGUAGE = stringPreferencesKey("ocr_language")
        val KEY_BATCH_ACTIVE = booleanPreferencesKey("batch_active")
    }
}
