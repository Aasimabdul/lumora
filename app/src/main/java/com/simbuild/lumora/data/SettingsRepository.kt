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
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.settings.RawSettings
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.CustomThemeCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.lumoraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lumora_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Persists [AppSettings] and the user's saved custom themes. Only preferences are stored —
 * never notification content. Reads go through [AppSettings.fromRaw] and [CustomThemeCodec],
 * so missing or corrupted values fall back to safe defaults.
 */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.lumoraDataStore

    private object K {
        val theme = stringPreferencesKey("theme_id")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val dateStyle = stringPreferencesKey("date_style")
        val showDate = booleanPreferencesKey("show_date")
        val showBattery = booleanPreferencesKey("show_battery")
        val clockScale = floatPreferencesKey("clock_scale")
        val infoScale = floatPreferencesKey("info_scale")
        val positionX = floatPreferencesKey("position_x")
        val positionY = floatPreferencesKey("position_y")
        val textColor = stringPreferencesKey("text_color")
        val font = stringPreferencesKey("clock_font")
        val weight = stringPreferencesKey("clock_weight")
        val favorites = stringSetPreferencesKey("favorite_themes")
        val chargingAutomation = booleanPreferencesKey("charging_automation")
        val chargingEnd = stringPreferencesKey("charging_end")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val previewEnabled = booleanPreferencesKey("preview_enabled")
        val previewSeconds = intPreferencesKey("preview_duration_seconds")
        val showText = booleanPreferencesKey("preview_show_text")
        val showSender = booleanPreferencesKey("preview_show_sender")
        val privacyMode = booleanPreferencesKey("preview_privacy_mode")
        val allowedPackages = stringSetPreferencesKey("preview_allowed_packages")
        val customThemes = stringPreferencesKey("custom_themes_json")
    }

    private val data: Flow<Preferences> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val settings: Flow<AppSettings> = data.map { read(it) }

    val customThemes: Flow<List<CustomTheme>> = data.map { CustomThemeCodec.decode(it[K.customThemes]) }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    /** Applies [transform] to the saved custom themes atomically. */
    suspend fun updateCustomThemes(transform: (List<CustomTheme>) -> List<CustomTheme>) {
        store.edit { prefs ->
            val next = transform(CustomThemeCodec.decode(prefs[K.customThemes]))
            prefs[K.customThemes] = CustomThemeCodec.encode(next)
        }
    }

    /** Applies both transforms in one atomic write (e.g. save a theme and select it). */
    suspend fun updateBoth(
        themes: (List<CustomTheme>) -> List<CustomTheme>,
        settings: (AppSettings) -> AppSettings,
    ) {
        store.edit { prefs ->
            prefs[K.customThemes] = CustomThemeCodec.encode(themes(CustomThemeCodec.decode(prefs[K.customThemes])))
            write(prefs, settings(read(prefs)))
        }
    }

    private fun read(p: Preferences): AppSettings = AppSettings.fromRaw(
        RawSettings(
            themeId = p[K.theme],
            use24Hour = p[K.use24Hour],
            dateStyle = p[K.dateStyle],
            showDate = p[K.showDate],
            showBattery = p[K.showBattery],
            clockScale = p[K.clockScale],
            infoScale = p[K.infoScale],
            positionX = p[K.positionX],
            positionY = p[K.positionY],
            textColor = p[K.textColor],
            font = p[K.font],
            weight = p[K.weight],
            favorites = p[K.favorites],
            chargingAutomation = p[K.chargingAutomation],
            chargingEnd = p[K.chargingEnd],
            onboardingDone = p[K.onboardingDone],
            previewEnabled = p[K.previewEnabled],
            previewDurationSeconds = p[K.previewSeconds],
            showText = p[K.showText],
            showSender = p[K.showSender],
            privacyMode = p[K.privacyMode],
            allowedPackages = p[K.allowedPackages],
        ),
    )

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[K.theme] = s.themeId
        p[K.use24Hour] = s.use24Hour
        p[K.dateStyle] = s.dateStyle.name
        p[K.showDate] = s.showDate
        p[K.showBattery] = s.showBattery
        p[K.clockScale] = s.clockScale
        p[K.infoScale] = s.infoScale
        p[K.positionX] = s.positionX
        p[K.positionY] = s.positionY
        val color = s.textColor
        if (color == null) p.remove(K.textColor) else p[K.textColor] = color
        p[K.font] = s.font.name
        p[K.weight] = s.weight.name
        p[K.favorites] = s.favorites
        p[K.chargingAutomation] = s.chargingAutomation
        p[K.chargingEnd] = s.chargingEnd.name
        p[K.onboardingDone] = s.onboardingDone
        p[K.previewEnabled] = s.preview.enabled
        p[K.previewSeconds] = s.preview.duration.seconds
        p[K.showText] = s.preview.showText
        p[K.showSender] = s.preview.showSender
        p[K.privacyMode] = s.preview.privacyMode
        p[K.allowedPackages] = s.preview.allowedPackages
    }
}
