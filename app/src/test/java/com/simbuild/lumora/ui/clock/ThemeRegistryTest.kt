package com.simbuild.lumora.ui.clock

import com.simbuild.lumora.core.theme.ThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {
    @Test
    fun everyCatalogThemeHasARendererAndNothingElse() {
        assertEquals(ThemeCatalog.all.map { it.id }.toSet(), themeRegistry.keys)
    }

    @Test
    fun allFortyThemesAreImplemented() {
        assertEquals(40, implementedThemeIds.size)
        assertTrue("minimal_digital" in implementedThemeIds)
    }
}
