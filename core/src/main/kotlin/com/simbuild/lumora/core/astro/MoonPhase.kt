package com.simbuild.lumora.core.astro

import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos

/**
 * Approximate lunar phase from the mean synodic month. Accurate to roughly half a day,
 * which is plenty for a decorative clock face. Not suitable for astronomy.
 */
object MoonPhase {
    private const val SYNODIC_DAYS = 29.530588853

    // Reference new moon: 2000-01-06 18:14 UTC.
    private val REFERENCE_NEW_MOON: Instant = Instant.parse("2000-01-06T18:14:00Z")

    /** Phase in `[0, 1)`: 0 = new moon, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter. */
    fun phase(instant: Instant): Double {
        val days = (instant.epochSecond - REFERENCE_NEW_MOON.epochSecond) / 86_400.0
        val cycles = days / SYNODIC_DAYS
        val frac = cycles - Math.floor(cycles)
        return if (frac >= 1.0) 0.0 else frac
    }

    /** Illuminated fraction of the disc in `[0, 1]`. */
    fun illumination(phase: Double): Double = (1 - cos(2 * PI * phase)) / 2

    /** True while the moon is waxing (growing), i.e. the first half of the cycle. */
    fun isWaxing(phase: Double): Boolean = phase < 0.5

    fun name(phase: Double): String = when {
        phase < 0.03 || phase >= 0.97 -> "New moon"
        phase < 0.22 -> "Waxing crescent"
        phase < 0.28 -> "First quarter"
        phase < 0.47 -> "Waxing gibbous"
        phase < 0.53 -> "Full moon"
        phase < 0.72 -> "Waning gibbous"
        phase < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }
}
