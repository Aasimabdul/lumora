package com.simbuild.lumora.core.theme

import com.simbuild.lumora.core.settings.ClockFont
import com.simbuild.lumora.core.settings.ClockWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemeTest {
    @Test
    fun sanitizeClampsNumbersAndFixesColours() {
        val t = CustomTheme(
            id = "custom:a", name = "  ", baseThemeId = "nope", background = BackgroundKind.PHOTO,
            backgroundColor = "red", photoFile = "../evil.jpg", dim = 9f, blur = -2f, zoom = 100f,
            panX = Float.NaN, textColor = "#zzz", clockScale = 50f, positionY = -9f,
        ).sanitized()
        assertEquals("My theme", t.name)
        assertEquals(ThemeCatalog.DEFAULT_ID, t.baseThemeId)
        assertEquals(BackgroundKind.COLOR, t.background)
        assertNull(t.photoFile)
        assertEquals("#000000", t.backgroundColor)
        assertEquals(1f, t.dim)
        assertEquals(0f, t.blur)
        assertEquals(CustomTheme.MAX_ZOOM, t.zoom)
        assertEquals(0f, t.panX)
        assertNull(t.textColor)
        assertEquals(1.6f, t.clockScale)
        assertEquals(-0.8f, t.positionY)
    }

    @Test
    fun templateIdsCannotBeBaseThemes() {
        assertEquals(ThemeCatalog.DEFAULT_ID, CustomTheme(id = "custom:a", baseThemeId = "custom_photo_clock").sanitized().baseThemeId)
        assertEquals("wolf", CustomTheme(id = "custom:a", baseThemeId = "wolf").sanitized().baseThemeId)
    }

    @Test
    fun photoBackgroundNeedsSafeFileName() {
        val ok = CustomTheme(id = "custom:a", background = BackgroundKind.PHOTO, photoFile = "bg_123.jpg").sanitized()
        assertEquals(BackgroundKind.PHOTO, ok.background)
        val bad = CustomTheme(id = "custom:a", background = BackgroundKind.PHOTO, photoFile = "a/b.jpg").sanitized()
        assertEquals(BackgroundKind.COLOR, bad.background)
        assertNull(bad.photoFile)
        val missing = CustomTheme(id = "custom:a", background = BackgroundKind.PHOTO, photoFile = null).sanitized()
        assertEquals(BackgroundKind.COLOR, missing.background)
    }

    @Test
    fun invalidIdIsReplaced() {
        assertTrue(CustomTheme(id = "plain").sanitized().id.startsWith("custom:"))
    }

    @Test
    fun codecRoundTripsEveryField() {
        val t = CustomTheme(
            id = "custom:1", name = "Night", baseThemeId = "dragon", background = BackgroundKind.PHOTO,
            backgroundColor = "#112233", photoFile = "bg_1.jpg", dim = 0.5f, blur = 8f, zoom = 2f, panX = 0.3f,
            panY = -0.2f, textColor = "#00E5FF", font = ClockFont.SERIF, weight = ClockWeight.BOLD,
            clockScale = 1.2f, infoScale = 0.9f, positionX = 0.1f, positionY = -0.3f,
            showDate = false, showBattery = false, favorite = true,
        )
        val decoded = CustomThemeCodec.decode(CustomThemeCodec.encode(listOf(t)))
        assertEquals(listOf(t), decoded)
    }

    @Test
    fun corruptedJsonYieldsEmptyList() {
        for (raw in listOf(null, "", "   ", "not json", "{", "[]", "{\"version\":1}", "{\"themes\":5}", "{\"version\":99,\"themes\":[]}")) {
            assertEquals("raw=$raw", emptyList<CustomTheme>(), CustomThemeCodec.decode(raw))
        }
    }

    @Test
    fun invalidEntriesAreDroppedValidOnesKept() {
        val raw = """{"version":1,"themes":[
            {"id":"custom:ok","name":"Good","baseThemeId":"wolf"},
            "garbage",
            {"id":"custom:ok","name":"Duplicate id"},
            {"id":"custom:two","name":"Two","dim":"high","font":"COMIC","clockScale":99,"extra":1}
        ]}"""
        val list = CustomThemeCodec.decode(raw)
        assertEquals(listOf("custom:ok", "custom:two"), list.map { it.id })
        assertEquals("wolf", list[0].baseThemeId)
        assertEquals(ClockFont.DEFAULT, list[1].font)
        assertEquals(1.6f, list[1].clockScale)
    }

    @Test
    fun upsertDuplicateRenameDeleteFavoriteReset() {
        var list = emptyList<CustomTheme>()
        val a = CustomTheme.create("A", "wolf")
        list = CustomThemes.upsert(list, a)
        list = CustomThemes.upsert(list, a.copy(name = "A2"))
        assertEquals(1, list.size)
        assertEquals("A2", list[0].name)

        list = CustomThemes.duplicate(list, a.id)
        assertEquals(2, list.size)
        assertNotEquals(list[0].id, list[1].id)
        assertEquals("A2 copy", list[1].name)

        list = CustomThemes.rename(list, a.id, "  Renamed  ")
        assertEquals("Renamed", list[0].name)

        list = CustomThemes.toggleFavorite(list, a.id)
        assertTrue(list[0].favorite)

        list = CustomThemes.upsert(list, list[0].copy(clockScale = 1.5f, textColor = "#FF0000"))
        list = CustomThemes.resetCustomisation(list, a.id)
        assertEquals(1f, list[0].clockScale)
        assertNull(list[0].textColor)
        assertEquals(a.id, list[0].id)
        assertEquals("wolf", list[0].baseThemeId)
        assertTrue(list[0].favorite)

        list = CustomThemes.delete(list, a.id)
        assertEquals(1, list.size)
        assertFalse(list.any { it.id == a.id })
    }

    @Test
    fun listIsCappedAtMaximum() {
        var list = emptyList<CustomTheme>()
        repeat(CustomThemes.MAX_THEMES + 5) { list = CustomThemes.upsert(list, CustomTheme.create("T$it")) }
        assertEquals(CustomThemes.MAX_THEMES, list.size)
    }
}
