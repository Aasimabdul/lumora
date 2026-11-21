package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.notifications.NotificationDisplayDuration
import com.simbuild.lumora.core.notifications.PreviewSettings
import com.simbuild.lumora.core.theme.HexColor
import com.simbuild.lumora.core.theme.ThemeCatalog

enum class ClockFont(val label: String) {
    DEFAULT("Default"),
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Monospace"),
    CURSIVE("Cursive"),
}

/** [THEME] keeps whatever weight the selected theme was designed with. */
enum class ClockWeight(val label: String) {
    THEME("Theme default"),
    LIGHT("Light"),
    NORMAL("Regular"),
    BOLD("Bold"),
    BLACK("Black"),
}

enum class ChargingEndBehavior(val label: String) {
    STAY("Stay in standby"),
    EXIT("Exit standby"),
}

/** Raw, possibly missing or corrupted stored values. Anything invalid falls back to a default. */
data class RawSettings(
    val themeId: String? = null,
    val use24Hour: Boolean? = null,
    val dateStyle: String? = null,
    val showDate: Boolean? = null,
    val showBattery: Boolean? = null,
    val clockScale: Float? = null,
    val infoScale: Float? = null,
    val positionX: Float? = null,
    val positionY: Float? = null,
    val textColor: String? = null,
    val font: String? = null,
    val weight: String? = null,
    val favorites: Set<String>? = null,
    val chargingAutomation: Boolean? = null,
    val chargingEnd: String? = null,
    val onboardingDone: Boolean? = null,
    val previewEnabled: Boolean? = null,
    val previewDurationSeconds: Int? = null,
    val showText: Boolean? = null,
    val showSender: Boolean? = null,
    val privacyMode: Boolean? = null,
    val allowedPackages: Set<String>? = null,
)

/**
 * User settings as the app sees them. Notification *content* is never part of this model.
 * Notification previews and charging automation are OFF by default.
 */
data class AppSettings(
    /** A built-in theme id, or a saved custom theme id (prefix `custom:`). */
    val themeId: String = ThemeCatalog.DEFAULT_ID,
    val use24Hour: Boolean = false,
    val dateStyle: DateStyle = DateStyle.FULL,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    /** Multiplier for the main clock, [SCALE_MIN]..[SCALE_MAX]. */
    val clockScale: Float = 1f,
    /** Multiplier for date / battery text, [INFO_SCALE_MIN]..[INFO_SCALE_MAX]. */
    val infoScale: Float = 1f,
    /** -1 (left) .. 1 (right), limited to ±[POSITION_LIMIT]. */
    val positionX: Float = 0f,
    /** -1 (top) .. 1 (bottom), limited to ±[POSITION_LIMIT]. */
    val positionY: Float = 0f,
    /** `#RRGGBB` text colour override, or null to use each theme's own colours. */
    val textColor: String? = null,
    val font: ClockFont = ClockFont.DEFAULT,
    val weight: ClockWeight = ClockWeight.THEME,
    val favorites: Set<String> = emptySet(),
    val chargingAutomation: Boolean = false,
    val chargingEnd: ChargingEndBehavior = ChargingEndBehavior.STAY,
    val onboardingDone: Boolean = false,
    val preview: PreviewSettings = PreviewSettings(),
) {
    /** True when any manual clock customisation differs from the defaults. */
    val isCustomised: Boolean
        get() = clockScale != 1f || infoScale != 1f || positionX != 0f || positionY != 0f ||
            textColor != null || font != ClockFont.DEFAULT || weight != ClockWeight.THEME

    /** Returns these settings with every manual clock customisation reset (theme and formats kept). */
    fun withDefaultCustomisation(): AppSettings = copy(
        clockScale = 1f, infoScale = 1f, positionX = 0f, positionY = 0f,
        textColor = null, font = ClockFont.DEFAULT, weight = ClockWeight.THEME,
    )

    companion object {
        const val SCALE_MIN = 0.5f
        const val SCALE_MAX = 1.6f
        const val INFO_SCALE_MIN = 0.6f
        const val INFO_SCALE_MAX = 1.8f
        const val POSITION_LIMIT = 0.8f
        const val MAX_FAVORITES = 200
        const val CUSTOM_PREFIX = "custom:"

        private fun clampFloat(v: Float?, default: Float, min: Float, max: Float): Float =
            if (v == null || !v.isFinite()) default else v.coerceIn(min, max)

        private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default

        /** Is [id] a plausible saved-custom-theme id? Existence is checked by the app layer. */
        fun isCustomId(id: String?): Boolean =
            id != null && id.startsWith(CUSTOM_PREFIX) && id.length in (CUSTOM_PREFIX.length + 1)..80

        fun fromRaw(raw: RawSettings): AppSettings {
            val d = AppSettings()
            val dp = d.preview
            val themeId = when {
                isCustomId(raw.themeId) -> raw.themeId!!
                else -> ThemeCatalog.resolveOrDefault(raw.themeId).id
            }
            return AppSettings(
                themeId = themeId,
                use24Hour = raw.use24Hour ?: d.use24Hour,
                dateStyle = enumOr(raw.dateStyle, d.dateStyle),
                showDate = raw.showDate ?: d.showDate,
                showBattery = raw.showBattery ?: d.showBattery,
                clockScale = clampFloat(raw.clockScale, d.clockScale, SCALE_MIN, SCALE_MAX),
                infoScale = clampFloat(raw.infoScale, d.infoScale, INFO_SCALE_MIN, INFO_SCALE_MAX),
                positionX = clampFloat(raw.positionX, d.positionX, -POSITION_LIMIT, POSITION_LIMIT),
                positionY = clampFloat(raw.positionY, d.positionY, -POSITION_LIMIT, POSITION_LIMIT),
                textColor = HexColor.normalize(raw.textColor),
                font = enumOr(raw.font, d.font),
                weight = enumOr(raw.weight, d.weight),
                favorites = raw.favorites.orEmpty()
                    .filter { ThemeCatalog.byId(it) != null || isCustomId(it) }
                    .take(MAX_FAVORITES).toSet(),
                chargingAutomation = raw.chargingAutomation ?: d.chargingAutomation,
                chargingEnd = enumOr(raw.chargingEnd, d.chargingEnd),
                onboardingDone = raw.onboardingDone ?: d.onboardingDone,
                preview = PreviewSettings(
                    enabled = raw.previewEnabled ?: dp.enabled,
                    duration = NotificationDisplayDuration.fromSeconds(raw.previewDurationSeconds),
                    showText = raw.showText ?: dp.showText,
                    showSender = raw.showSender ?: dp.showSender,
                    privacyMode = raw.privacyMode ?: dp.privacyMode,
                    allowedPackages = raw.allowedPackages.orEmpty().filter { it.isNotBlank() && it.length <= 200 }.take(500).toSet(),
                ),
            )
        }
    }
}
