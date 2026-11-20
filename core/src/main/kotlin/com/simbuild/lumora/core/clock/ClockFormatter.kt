package com.simbuild.lumora.core.clock

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class DateStyle(internal val style: FormatStyle) {
    FULL(FormatStyle.FULL),
    LONG(FormatStyle.LONG),
    MEDIUM(FormatStyle.MEDIUM),
    SHORT(FormatStyle.SHORT),
}

/** Time split into digits and an optional AM/PM marker (null in 24-hour mode). */
data class FormattedTime(val digits: String, val amPm: String?)

/**
 * Stateless formatting. Everything is a function of (instant, zone, locale), so a
 * time-zone or locale change is handled by simply calling it again with the new values.
 */
object ClockFormatter {
    fun time(
        instant: Instant,
        zone: ZoneId,
        locale: Locale,
        use24Hour: Boolean,
        padHour: Boolean = true,
    ): FormattedTime {
        val local = instant.atZone(zone)
        return if (use24Hour) {
            FormattedTime(DateTimeFormatter.ofPattern("HH:mm", locale).format(local), null)
        } else {
            val pattern = if (padHour) "hh:mm" else "h:mm"
            FormattedTime(
                DateTimeFormatter.ofPattern(pattern, locale).format(local),
                DateTimeFormatter.ofPattern("a", locale).format(local),
            )
        }
    }

    fun weekday(instant: Instant, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofPattern("EEEE", locale).format(instant.atZone(zone))

    fun date(instant: Instant, zone: ZoneId, locale: Locale, style: DateStyle): String =
        DateTimeFormatter.ofLocalizedDate(style.style).withLocale(locale).format(instant.atZone(zone))

    /** Milliseconds until the next minute boundary in [zone]; always in 1..60_000. */
    fun millisUntilNextMinute(instant: Instant, zone: ZoneId): Long {
        val local = instant.atZone(zone)
        val intoMinute = local.second * 1000L + local.nano / 1_000_000L
        return 60_000L - intoMinute
    }
}
