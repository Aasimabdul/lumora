package com.simbuild.lumora.ui.clock

/**
 * Ids of catalog themes that have a real renderer in [ThemeRenderers].
 * Everything else in the catalog is shown as "not available yet" and cannot be selected.
 * Keep in sync with the `when` in [ThemeRenderers.Render].
 */
val implementedThemeIds: Set<String> = setOf(
    "minimal_digital",
    "neon_cyan",
    "flip_clock",
    "vertical_clock",
    "analog_classic",
    "minimal_analog",
)
