package com.simbuild.lumora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simbuild.lumora.core.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.lumoraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lumora_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Persists [AppSettings]. Only preferences are stored — never notification content.
 * Reads go through [AppSettings.fromRaw], so missing or corrupted values fall back to defaults.
 */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.lumoraDataStore

    private object K {
        val theme = stringPreferencesKey("theme_id")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val dateStyle = stringPreferencesKey("date_style")
        val showDate = booleanPreferencesKey("show_date")
        val showBattery = booleanPreferencesKey("show_battery")
        val previewEnabled = booleanPreferencesKey("preview_enabled")
        val previewSeconds = intPreferencesKey("preview_duration_seconds")
        val showText = booleanPreferencesKey("preview_show_text")
        val showSender = booleanPreferencesKey("preview_show_sender")
        val privacyMode = booleanPreferencesKey("preview_privacy_mode")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { read(it) }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun read(p: Preferences): AppSettings = AppSettings.fromRaw(
        themeId = p[K.theme],
        use24Hour = p[K.use24Hour],
        dateStyle = p[K.dateStyle],
        showDate = p[K.showDate],
        showBattery = p[K.showBattery],
        previewEnabled = p[K.previewEnabled],
        previewDurationSeconds = p[K.previewSeconds],
        showText = p[K.showText],
        showSender = p[K.showSender],
        privacyMode = p[K.privacyMode],
    )

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[K.theme] = s.themeId
        p[K.use24Hour] = s.use24Hour
        p[K.dateStyle] = s.dateStyle.name
        p[K.showDate] = s.showDate
        p[K.showBattery] = s.showBattery
        p[K.previewEnabled] = s.preview.enabled
        p[K.previewSeconds] = s.preview.duration.seconds
        p[K.showText] = s.preview.showText
        p[K.showSender] = s.preview.showSender
        p[K.privacyMode] = s.preview.privacyMode
    }
}
