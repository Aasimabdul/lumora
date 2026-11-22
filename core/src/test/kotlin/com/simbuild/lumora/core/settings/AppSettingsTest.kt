package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.astro.MoonPhase
import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.notifications.NotificationDisplayDuration
import com.simbuild.lumora.core.theme.HexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AppSettingsTest {
    @Test
    fun emptyStoreYieldsDefaults() {
        assertEquals(AppSettings(), AppSettings.fromRaw(RawSettings()))
    }

    @Test
    fun previewsAndChargingAutomationAreOffByDefault() {
        val s = AppSettings.fromRaw(RawSettings())
        assertFalse(s.preview.enabled)
        assertFalse(s.chargingAutomation)
        assertFalse(s.onboardingDone)
        assertEquals(NotificationDisplayDuration.S15, s.preview.duration)
        assertFalse(s.isCustomised)
    }

    @Test
    fun validValuesArePreserved() {
        val s = AppSettings.fromRaw(
            RawSettings(
                themeId = "wolf", use24Hour = true, dateStyle = "SHORT", showDate = false, showBattery = false,
                clockScale = 1.3f, infoScale = 0.8f, positionX = -0.4f, positionY = 0.5f,
                textColor = "#00e5ff", font = "SERIF", weight = "BOLD",
                favorites = setOf("wolf", "dragon", "custom:abc"),
                chargingAutomation = true, chargingEnd = "EXIT", onboardingDone = true,
                previewEnabled = true, previewDurationSeconds = 30, showText = false, showSender = false,
                privacyMode = true, allowedPackages = setOf("com.chat"),
            ),
        )
        assertEquals("wolf", s.themeId)
        assertTrue(s.use24Hour)
        assertEquals(DateStyle.SHORT, s.dateStyle)
        assertEquals(1.3f, s.clockScale)
        assertEquals(0.8f, s.infoScale)
        assertEquals(-0.4f, s.positionX)
        assertEquals(0.5f, s.positionY)
        assertEquals("#00E5FF", s.textColor)
        assertEquals(ClockFont.SERIF, s.font)
        assertEquals(ClockWeight.BOLD, s.weight)
        assertEquals(setOf("wolf", "dragon", "custom:abc"), s.favorites)
        assertTrue(s.chargingAutomation)
        assertEquals(ChargingEndBehavior.EXIT, s.chargingEnd)
        assertTrue(s.onboardingDone)
        assertTrue(s.preview.enabled)
        assertEquals(NotificationDisplayDuration.S30, s.preview.duration)
        assertEquals(setOf("com.chat"), s.preview.allowedPackages)
        assertTrue(s.isCustomised)
    }

    @Test
    fun everyFontNameRoundTripsThroughStoredValue() {
        ClockFont.entries.forEach { f ->
            assertEquals(f, AppSettings.fromRaw(RawSettings(font = f.name)).font)
        }
        assertEquals(16, ClockFont.entries.size)
    }

    @Test
    fun corruptedValuesFallBackToSafeDefaults() {
        val s = AppSettings.fromRaw(
            RawSettings(
                themeId = "deleted_theme", dateStyle = "WEIRD", previewDurationSeconds = 12,
                textColor = "red", font = "COMIC", weight = "HEAVY", chargingEnd = "???",
                favorites = setOf("nope", "wolf"),
            ),
        )
        assertEquals("minimal_digital", s.themeId)
        assertEquals(DateStyle.FULL, s.dateStyle)
        assertEquals(NotificationDisplayDuration.S15, s.preview.duration)
        assertNull(s.textColor)
        assertEquals(ClockFont.DEFAULT, s.font)
        assertEquals(ClockWeight.THEME, s.weight)
        assertEquals(ChargingEndBehavior.STAY, s.chargingEnd)
        assertEquals(setOf("wolf"), s.favorites)
    }

    @Test
    fun numericSettingsAreClampedAndNanIsReplaced() {
        val s = AppSettings.fromRaw(
            RawSettings(clockScale = 99f, infoScale = -3f, positionX = Float.NaN, positionY = 5f),
        )
        assertEquals(AppSettings.SCALE_MAX, s.clockScale)
        assertEquals(AppSettings.INFO_SCALE_MIN, s.infoScale)
        assertEquals(0f, s.positionX)
        assertEquals(AppSettings.POSITION_LIMIT, s.positionY)
    }

    @Test
    fun customThemeIdsAreKept() {
        assertEquals("custom:123", AppSettings.fromRaw(RawSettings(themeId = "custom:123")).themeId)
        assertEquals("minimal_digital", AppSettings.fromRaw(RawSettings(themeId = "custom:")).themeId)
    }

    @Test
    fun everySupportedDurationRoundTrips() {
        for (d in NotificationDisplayDuration.entries) {
            assertEquals(d, AppSettings.fromRaw(RawSettings(previewDurationSeconds = d.seconds)).preview.duration)
        }
    }

    @Test
    fun resetCustomisationKeepsThemeAndFormats() {
        val s = AppSettings(themeId = "wolf", use24Hour = true, clockScale = 1.4f, textColor = "#FFFFFF", font = ClockFont.MONO)
        val r = s.withDefaultCustomisation()
        assertFalse(r.isCustomised)
        assertEquals("wolf", r.themeId)
        assertTrue(r.use24Hour)
    }

    @Test
    fun hexColorValidation() {
        assertEquals("#ABCDEF", HexColor.normalize(" #abcdef "))
        assertNull(HexColor.normalize("#abc"))
        assertNull(HexColor.normalize(null))
        assertFalse(HexColor.isValid("#GGGGGG"))
    }
}

class MoonPhaseTest {
    @Test
    fun knownNewMoonIsNearZero() {
        // New moon 2024-11-01 12:47 UTC.
        val p = MoonPhase.phase(Instant.parse("2024-11-01T12:47:00Z"))
        assertTrue("phase=$p", p < 0.03 || p > 0.97)
    }

    @Test
    fun knownFullMoonIsNearHalf() {
        // Full moon 2024-11-15 21:28 UTC.
        val p = MoonPhase.phase(Instant.parse("2024-11-15T21:28:00Z"))
        assertEquals(0.5, p, 0.03)
        assertEquals("Full moon", MoonPhase.name(0.5))
    }

    @Test
    fun phaseStaysInRangeAndIlluminationMatches() {
        for (day in 0 until 60) {
            val p = MoonPhase.phase(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(day * 86_400L))
            assertTrue(p >= 0.0 && p < 1.0)
        }
        assertEquals(0.0, MoonPhase.illumination(0.0), 1e-9)
        assertEquals(1.0, MoonPhase.illumination(0.5), 1e-9)
        assertEquals(0.5, MoonPhase.illumination(0.25), 1e-9)
        assertTrue(MoonPhase.isWaxing(0.2))
        assertFalse(MoonPhase.isWaxing(0.7))
    }
}
