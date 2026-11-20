package com.simbuild.lumora.core

import com.simbuild.lumora.core.battery.BatteryState
import com.simbuild.lumora.core.battery.ChargeStatus
import com.simbuild.lumora.core.battery.ChargingEventDeduplicator
import com.simbuild.lumora.core.clock.ClockFormatter
import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.core.theme.ThemeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class ThemeCatalogTest {
    @Test
    fun hasExactlyFortyThemes() = assertEquals(40, ThemeCatalog.all.size)

    @Test
    fun idsAndNamesAreUniqueAndWellFormed() {
        assertEquals(40, ThemeCatalog.all.map { it.id }.toSet().size)
        assertEquals(40, ThemeCatalog.all.map { it.displayName }.toSet().size)
        ThemeCatalog.all.forEach { assertTrue(it.id, Regex("[a-z0-9]+(_[a-z0-9]+)*").matches(it.id)) }
    }

    @Test
    fun everyIdResolvesToItsDefinition() {
        ThemeCatalog.all.forEach { assertEquals(it, ThemeCatalog.byId(it.id)) }
    }

    @Test
    fun categorySizesMatchTheSpecification() {
        val sizes = ThemeCatalog.all.groupingBy { it.category }.eachCount()
        assertEquals(10, sizes[ThemeCategory.DIGITAL])
        assertEquals(8, sizes[ThemeCategory.ANALOG])
        assertEquals(8, sizes[ThemeCategory.FUTURISTIC])
        assertEquals(8, sizes[ThemeCategory.NATURE])
        assertEquals(6, sizes[ThemeCategory.CREATIVE])
    }

    @Test
    fun specifiedSlugsAreStable() {
        assertEquals("analog_calendar", ThemeCatalog.all[14].id)
        assertEquals("colorful_ios_inspired_clock", ThemeCatalog.all[34].id)
        assertEquals("my_custom_layout", ThemeCatalog.all.last().id)
    }

    @Test
    fun unknownOrNullIdFallsBackToDefault() {
        assertNull(ThemeCatalog.byId("nope"))
        assertEquals(ThemeCatalog.DEFAULT_ID, ThemeCatalog.resolveOrDefault("nope").id)
        assertEquals(ThemeCatalog.DEFAULT_ID, ThemeCatalog.resolveOrDefault(null).id)
        assertEquals("wolf", ThemeCatalog.resolveOrDefault("wolf").id)
    }

    @Test
    fun searchFiltersByNameAndCategory() {
        assertEquals(listOf("Aurora Glow", "Aurora Sky"), ThemeCatalog.search("aurora").map { it.displayName })
        assertEquals(8, ThemeCatalog.search("", ThemeCategory.ANALOG).size)
        assertTrue(ThemeCatalog.search("aurora", ThemeCategory.ANALOG).isEmpty())
        assertEquals(40, ThemeCatalog.search("  ").size)
    }
}

class ClockFormatterTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")
    private val us = Locale.US
    private val instant = Instant.parse("2026-11-20T03:48:00Z")

    @Test
    fun twentyFourHour() {
        val t = ClockFormatter.time(instant, kolkata, us, use24Hour = true)
        assertEquals("09:18", t.digits)
        assertNull(t.amPm)
    }

    @Test
    fun twelveHourPaddedAndUnpadded() {
        val padded = ClockFormatter.time(instant, kolkata, us, use24Hour = false)
        assertEquals("09:18", padded.digits)
        assertEquals("AM", padded.amPm)
        assertEquals("9:18", ClockFormatter.time(instant, kolkata, us, false, padHour = false).digits)
    }

    @Test
    fun midnightAndNoonIn12HourMode() {
        val midnight = ClockFormatter.time(Instant.parse("2026-11-20T00:00:00Z"), ZoneId.of("UTC"), us, false)
        assertEquals("12:00", midnight.digits)
        assertEquals("AM", midnight.amPm)
        val noon = ClockFormatter.time(Instant.parse("2026-11-20T12:00:00Z"), ZoneId.of("UTC"), us, false)
        assertEquals("12:00", noon.digits)
        assertEquals("PM", noon.amPm)
    }

    @Test
    fun sameInstantRendersPerTimeZone() {
        assertEquals("22:48", ClockFormatter.time(instant, newYork, us, true).digits)
        assertEquals("10:48", ClockFormatter.time(instant, newYork, us, false).digits)
        assertEquals("PM", ClockFormatter.time(instant, newYork, us, false).amPm)
    }

    @Test
    fun dateChangesWithTimeZone() {
        assertEquals("Friday", ClockFormatter.weekday(instant, kolkata, us))
        assertEquals("Thursday", ClockFormatter.weekday(instant, newYork, us))
        assertEquals("Friday, November 20, 2026", ClockFormatter.date(instant, kolkata, us, DateStyle.FULL))
        assertEquals("Thursday, November 19, 2026", ClockFormatter.date(instant, newYork, us, DateStyle.FULL))
        assertEquals("Nov 20, 2026", ClockFormatter.date(instant, kolkata, us, DateStyle.MEDIUM))
    }

    @Test
    fun localeChangeChangesWeekdayName() {
        assertEquals("vendredi", ClockFormatter.weekday(instant, kolkata, Locale.FRANCE))
    }

    @Test
    fun daylightSavingTransitionIsHandled() {
        val before = Instant.parse("2026-03-08T06:59:00Z")
        val after = Instant.parse("2026-03-08T07:00:00Z")
        assertEquals("01:59", ClockFormatter.time(before, newYork, us, true).digits)
        assertEquals("03:00", ClockFormatter.time(after, newYork, us, true).digits)
    }

    @Test
    fun millisUntilNextMinuteStaysInRange() {
        assertEquals(60_000L, ClockFormatter.millisUntilNextMinute(Instant.parse("2026-11-20T03:48:00Z"), kolkata))
        assertEquals(1_000L, ClockFormatter.millisUntilNextMinute(Instant.parse("2026-11-20T03:48:59Z"), kolkata))
        assertEquals(29_500L, ClockFormatter.millisUntilNextMinute(Instant.parse("2026-11-20T03:48:30.500Z"), kolkata))
    }
}

class BatteryStateTest {
    @Test
    fun percentIsRoundedAndClamped() {
        assertEquals(50, BatteryState.from(50, 100, 3).percent)
        assertEquals(33, BatteryState.from(1, 3, 3).percent)
        assertEquals(67, BatteryState.from(2, 3, 3).percent)
        assertEquals(100, BatteryState.from(120, 100, 3).percent)
        assertEquals(0, BatteryState.from(0, 100, 3).percent)
    }

    @Test
    fun invalidReadingsYieldNullPercent() {
        assertNull(BatteryState.from(-1, 100, 3).percent)
        assertNull(BatteryState.from(50, 0, 3).percent)
        assertNull(BatteryState.UNKNOWN.percent)
    }

    @Test
    fun statusMappingAndPluggedIn() {
        assertEquals(ChargeStatus.CHARGING, ChargeStatus.fromAndroid(2))
        assertEquals(ChargeStatus.DISCHARGING, ChargeStatus.fromAndroid(3))
        assertEquals(ChargeStatus.NOT_CHARGING, ChargeStatus.fromAndroid(4))
        assertEquals(ChargeStatus.FULL, ChargeStatus.fromAndroid(5))
        assertEquals(ChargeStatus.UNKNOWN, ChargeStatus.fromAndroid(1))
        assertEquals(ChargeStatus.UNKNOWN, ChargeStatus.fromAndroid(99))
        assertTrue(BatteryState.from(80, 100, 2).isPluggedIn)
        assertTrue(BatteryState.from(100, 100, 5).isPluggedIn)
        assertFalse(BatteryState.from(80, 100, 3).isPluggedIn)
        assertFalse(BatteryState.from(80, 100, 4).isPluggedIn)
    }
}

class ChargingEventDeduplicatorTest {
    @Test
    fun firstObservationOnlySeedsBaseline() {
        val d = ChargingEventDeduplicator()
        assertFalse("sticky broadcast on registration must not launch", d.shouldLaunch(true, 0))
    }

    @Test
    fun launchesOnceOnTransitionAndIgnoresDuplicates() {
        val d = ChargingEventDeduplicator()
        d.shouldLaunch(false, 0)
        assertTrue(d.shouldLaunch(true, 1_000))
        assertFalse(d.shouldLaunch(true, 1_100))
        assertFalse(d.shouldLaunch(true, 1_200))
    }

    @Test
    fun reconnectAfterCooldownLaunchesAgain() {
        val d = ChargingEventDeduplicator(cooldownMillis = 5_000)
        d.shouldLaunch(false, 0)
        assertTrue(d.shouldLaunch(true, 1_000))
        assertFalse(d.shouldLaunch(false, 2_000))
        assertFalse("within cooldown", d.shouldLaunch(true, 3_000))
        assertFalse(d.shouldLaunch(false, 7_000))
        assertTrue(d.shouldLaunch(true, 7_500))
    }

    @Test
    fun disconnectNeverLaunches() {
        val d = ChargingEventDeduplicator()
        d.shouldLaunch(true, 0)
        assertFalse(d.shouldLaunch(false, 10_000))
    }
}
