package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.HexColor

/**
 * The manual clock customisations the user can change, independent of where they are stored
 * (global settings or one saved custom theme). The customise screen edits a [ClockLook].
 */
data class ClockLook(
    val clockScale: Float = 1f,
    val infoScale: Float = 1f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val textColor: String? = null,
    val font: ClockFont = ClockFont.DEFAULT,
    val weight: ClockWeight = ClockWeight.THEME,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
) {
    /** Clamps every value into its allowed range and drops an invalid colour. */
    fun sanitized(): ClockLook {
        fun f(v: Float, d: Float, min: Float, max: Float) = if (v.isFinite()) v.coerceIn(min, max) else d
        return copy(
            clockScale = f(clockScale, 1f, AppSettings.SCALE_MIN, AppSettings.SCALE_MAX),
            infoScale = f(infoScale, 1f, AppSettings.INFO_SCALE_MIN, AppSettings.INFO_SCALE_MAX),
            positionX = f(positionX, 0f, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT),
            positionY = f(positionY, 0f, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT),
            textColor = HexColor.normalize(textColor),
        )
    }

    /** Same look with size, position, colour, font and weight back to defaults (show flags kept). */
    fun reset(): ClockLook = ClockLook(showDate = showDate, showBattery = showBattery)

    val isDefault: Boolean get() = this == ClockLook(showDate = showDate, showBattery = showBattery)
}

fun AppSettings.look(): ClockLook = ClockLook(
    clockScale, infoScale, positionX, positionY, textColor, font, weight, showDate, showBattery,
)

fun AppSettings.withLook(look: ClockLook): AppSettings {
    val l = look.sanitized()
    return copy(
        clockScale = l.clockScale, infoScale = l.infoScale, positionX = l.positionX, positionY = l.positionY,
        textColor = l.textColor, font = l.font, weight = l.weight, showDate = l.showDate, showBattery = l.showBattery,
    )
}

fun CustomTheme.look(): ClockLook = ClockLook(
    clockScale, infoScale, positionX, positionY, textColor, font, weight, showDate, showBattery,
)

fun CustomTheme.withLook(look: ClockLook): CustomTheme {
    val l = look.sanitized()
    return copy(
        clockScale = l.clockScale, infoScale = l.infoScale, positionX = l.positionX, positionY = l.positionY,
        textColor = l.textColor, font = l.font, weight = l.weight, showDate = l.showDate, showBattery = l.showBattery,
    ).sanitized()
}
