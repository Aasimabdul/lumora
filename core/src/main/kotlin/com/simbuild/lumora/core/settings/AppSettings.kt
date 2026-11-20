package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.notifications.NotificationDisplayDuration
import com.simbuild.lumora.core.notifications.PreviewSettings
import com.simbuild.lumora.core.theme.ThemeCatalog

/**
 * User settings as the app sees them. Notification *content* is never part of this model.
 * Notification previews are OFF by default.
 */
data class AppSettings(
    val themeId: String = ThemeCatalog.DEFAULT_ID,
    val use24Hour: Boolean = false,
    val dateStyle: DateStyle = DateStyle.FULL,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val preview: PreviewSettings = PreviewSettings(),
) {
    companion object {
        /**
         * Builds settings from raw stored values. Missing, unknown or corrupted values
         * fall back to safe defaults, so a damaged store can never crash the app or
         * silently turn previews on.
         */
        fun fromRaw(
            themeId: String?,
            use24Hour: Boolean?,
            dateStyle: String?,
            showDate: Boolean?,
            showBattery: Boolean?,
            previewEnabled: Boolean?,
            previewDurationSeconds: Int?,
            showText: Boolean?,
            showSender: Boolean?,
            privacyMode: Boolean?,
        ): AppSettings {
            val d = AppSettings()
            return AppSettings(
                themeId = ThemeCatalog.resolveOrDefault(themeId).id,
                use24Hour = use24Hour ?: d.use24Hour,
                dateStyle = DateStyle.entries.firstOrNull { it.name == dateStyle } ?: d.dateStyle,
                showDate = showDate ?: d.showDate,
                showBattery = showBattery ?: d.showBattery,
                preview = PreviewSettings(
                    enabled = previewEnabled ?: d.preview.enabled,
                    duration = NotificationDisplayDuration.fromSeconds(previewDurationSeconds),
                    showText = showText ?: d.preview.showText,
                    showSender = showSender ?: d.preview.showSender,
                    privacyMode = privacyMode ?: d.preview.privacyMode,
                ),
            )
        }
    }
}
