package com.simbuild.lumora.core.theme

/** Validation helpers for `#RRGGBB` colour strings stored in settings. */
object HexColor {
    private val pattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun isValid(value: String?): Boolean = value != null && pattern.matches(value)

    /** Returns upper-case `#RRGGBB`, or null if [value] is not a valid colour. */
    fun normalize(value: String?): String? = value?.trim()?.takeIf { isValid(it) }?.uppercase()
}
