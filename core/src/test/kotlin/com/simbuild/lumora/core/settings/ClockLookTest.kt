package com.simbuild.lumora.core.settings

import com.simbuild.lumora.core.theme.CustomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockLookTest {
    @Test
    fun `sanitized clamps values and drops bad colours`() {
        val l = ClockLook(clockScale = 9f, infoScale = -1f, positionX = Float.NaN, positionY = 5f, textColor = "red").sanitized()
        assertEquals(AppSettings.SCALE_MAX, l.clockScale, 0f)
        assertEquals(AppSettings.INFO_SCALE_MIN, l.infoScale, 0f)
        assertEquals(0f, l.positionX, 0f)
        assertEquals(AppSettings.POSITION_LIMIT, l.positionY, 0f)
        assertNull(l.textColor)
    }

    @Test
    fun `colour is normalised to upper case`() {
        assertEquals("#AABBCC", ClockLook(textColor = "#aabbcc").sanitized().textColor)
    }

    @Test
    fun `settings round trip through look`() {
        val look = ClockLook(1.2f, 0.8f, 0.3f, -0.2f, "#FFAA00", ClockFont.SERIF, ClockWeight.BOLD, showDate = false, showBattery = true)
        val s = AppSettings().withLook(look)
        assertEquals(look, s.look())
        assertTrue(s.isCustomised)
    }

    @Test
    fun `custom theme round trips through look`() {
        val look = ClockLook(0.7f, 1.5f, -0.4f, 0.4f, "#00FF88", ClockFont.MONO, ClockWeight.LIGHT)
        val t = CustomTheme.create("x").withLook(look)
        assertEquals(look, t.look())
    }

    @Test
    fun `reset restores defaults but keeps show flags`() {
        val look = ClockLook(clockScale = 1.4f, textColor = "#112233", showDate = false)
        val r = look.reset()
        assertEquals(ClockLook(showDate = false), r)
        assertTrue(r.isDefault)
        assertFalse(look.isDefault)
    }
}
