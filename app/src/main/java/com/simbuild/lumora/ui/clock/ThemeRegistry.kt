package com.simbuild.lumora.ui.clock

/**
 * Every built-in theme renderer, keyed by catalog id. A unit test asserts that this has exactly
 * the same keys as `ThemeCatalog.all`, so a theme can never be listed without being drawable.
 */
internal val themeRegistry: Map<String, ThemeContent> by lazy {
    baseThemes + digitalThemes + analogThemes + futuristicThemes + natureThemes + creativeThemes + templateThemes
}

/** Ids that currently have a renderer. */
val implementedThemeIds: Set<String> get() = themeRegistry.keys
