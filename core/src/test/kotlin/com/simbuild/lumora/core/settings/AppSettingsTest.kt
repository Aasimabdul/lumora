package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.notifications.NotificationDisplayDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private fun raw(
        themeId: String? = null,
        use24Hour: Boolean? = null,
        dateStyle: String? = null,
        showDate: Boolean? = null,
        showBattery: Boolean? = null,
        previewEnabled: Boolean? = null,
        seconds: Int? = null,
        showText: Boolean? = null,
        showSender: Boolean? = null,
        privacyMode: Boolean? = null,
    ) = AppSettings.fromRaw(
        themeId, use24Hour, dateStyle, showDate, showBattery,
        previewEnabled, seconds, showText, showSender, privacyMode,
    )

    @Test
    fun emptyStoreYieldsDefaults() {
        assertEquals(AppSettings(), raw())
    }

    @Test
    fun previewsAreOffByDefaultAndDurationIsFifteenSeconds() {
        val s = raw()
        assertFalse(s.preview.enabled)
        assertEquals(NotificationDisplayDuration.S15, s.preview.duration)
        assertFalse(s.preview.privacyMode)
    }

    @Test
    fun validValuesArePreserved() {
        val s = raw(
            themeId = "wolf", use24Hour = true, dateStyle = "SHORT", showDate = false, showBattery = false,
            previewEnabled = true, seconds = 30, showText = false, showSender = false, privacyMode = true,
        )
        assertEquals("wolf", s.themeId)
        assertTrue(s.use24Hour)
        assertEquals(DateStyle.SHORT, s.dateStyle)
        assertFalse(s.showDate)
        assertFalse(s.showBattery)
        assertTrue(s.preview.enabled)
        assertEquals(NotificationDisplayDuration.S30, s.preview.duration)
        assertFalse(s.preview.showText)
        assertFalse(s.preview.showSender)
        assertTrue(s.preview.privacyMode)
    }

    @Test
    fun corruptedValuesFallBackToSafeDefaults() {
        val s = raw(themeId = "deleted_theme", dateStyle = "WEIRD", seconds = 12)
        assertEquals("minimal_digital", s.themeId)
        assertEquals(DateStyle.FULL, s.dateStyle)
        assertEquals(NotificationDisplayDuration.S15, s.preview.duration)
    }

    @Test
    fun everySupportedDurationRoundTrips() {
        for (d in NotificationDisplayDuration.entries) {
            assertEquals(d, raw(seconds = d.seconds).preview.duration)
        }
    }
}
