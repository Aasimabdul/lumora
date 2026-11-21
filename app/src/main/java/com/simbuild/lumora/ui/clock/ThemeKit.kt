package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.battery.BatteryState
import com.simbuild.lumora.core.battery.ChargeStatus
import kotlin.math.cos
import kotlin.math.sin

/** A theme renderer: draws one clock face from a live [ClockFrame]. */
internal typealias ThemeContent = @Composable (ClockFrame, Modifier) -> Unit

internal val LumoraCyan = Color(0xFF00E5FF)
internal val LumoraNavy = Color(0xFF0D1B2A)

/**
 * Layout numbers derived from the available space and the user's size setting.
 * [unit] is a base font size (in sp/dp) for the main time text; the user's clock-size
 * multiplier is already applied to it, so anything sized from [unit] scales automatically.
 */
internal class Metrics(
    val unit: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
) {
    /** Diameter (dp) for a round or square element such as an analog dial, scaled by the user setting. */
    val dial: Float get() = (minOf(width, height * 0.72f) * scale).coerceIn(40f, minOf(width, height))
}

/**
 * The common stage every theme is drawn on.
 *
 * - Backgrounds MUST be drawn only through [background] / [backdrop]. When the clock is shown on top
 *   of a user photo ([ClockFrame.overlay]) both are skipped so the photo shows through.
 * - The content column honours the user's size, position and font settings automatically.
 * - Inside [content], use [ink] / [accent] / [weight] on the frame for colours and weights so the
 *   user's text colour and weight overrides apply.
 */
@Composable
internal fun Stage(
    frame: ClockFrame,
    modifier: Modifier = Modifier,
    background: Color = Color.Black,
    backdrop: (DrawScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(Metrics) -> Unit,
) {
    val base = if (frame.overlay) {
        modifier.fillMaxSize()
    } else {
        val filled = modifier.fillMaxSize().background(background)
        if (backdrop != null) filled.drawBehind(backdrop) else filled
    }
    BoxWithConstraints(
        modifier = base,
        contentAlignment = BiasAlignment(frame.positionX, frame.positionY),
    ) {
        val metrics = Metrics(
            unit = minOf(maxWidth.value / 3.3f, maxHeight.value / 1.7f) * frame.scale,
            width = maxWidth.value,
            height = maxHeight.value,
            scale = frame.scale,
        )
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = frame.font)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { content(metrics) }
        }
    }
}

internal fun batteryText(b: BatteryState): String {
    val level = b.percent?.let { "$it%" } ?: "--%"
    return when (b.status) {
        ChargeStatus.CHARGING -> "$level · Charging"
        ChargeStatus.FULL -> "$level · Fully charged"
        else -> level
    }
}

/** Weekday + date line and battery line, honouring the user's show/hide and info-size settings. */
@Composable
internal fun InfoLines(f: ClockFrame, m: Metrics, color: Color = Color.White) {
    val size = ((m.unit / f.scale) * 0.17f * f.infoScale).coerceIn(7f, 40f).sp
    val c = f.ink(color)
    if (f.showDate) {
        Text("${f.weekday} · ${f.date}", color = c.copy(alpha = 0.75f), fontSize = size)
    }
    if (f.showBattery) {
        Text(batteryText(f.battery), color = c.copy(alpha = 0.75f), fontSize = size)
    }
}

/** AM/PM marker (nothing in 24-hour mode). */
@Composable
internal fun AmPm(f: ClockFrame, m: Metrics, color: Color, sizeFactor: Float = 0.22f) {
    f.time.amPm?.let {
        Text(it, color = f.ink(color), fontSize = (m.unit * sizeFactor).sp, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Font size in sp for the info text (date/battery) when a theme lays it out itself. */
internal fun infoFontSize(f: ClockFrame, m: Metrics, factor: Float = 0.17f): Float =
    ((m.unit / f.scale) * factor * f.infoScale).coerceIn(7f, 40f)

// ---- geometry helpers -------------------------------------------------------------------

/** Point at [degrees] clockwise from 12 o'clock, [r] away from [c]. */
internal fun polar(c: Offset, degrees: Double, r: Float): Offset {
    val a = Math.toRadians(degrees - 90.0)
    return Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
}

/** Hour-hand angle in degrees clockwise from 12. */
internal val ClockFrame.hourDegrees: Double get() = (zoned.hour % 12) * 30.0 + zoned.minute * 0.5

/** Minute-hand angle in degrees clockwise from 12. */
internal val ClockFrame.minuteDegrees: Double get() = zoned.minute * 6.0

/** Hours as digits: `"09"` style in 24-hour mode, otherwise as in [FormattedTimeParts]. */
internal val ClockFrame.hourText: String get() = time.digits.substringBefore(':')
internal val ClockFrame.minuteText: String get() = time.digits.substringAfter(':')

internal fun DrawScope.drawHand(center: Offset, degrees: Double, length: Float, width: Float, color: Color, tail: Float = 0f) {
    drawLine(
        color = color,
        start = if (tail > 0f) polar(center, degrees + 180.0, tail) else center,
        end = polar(center, degrees, length),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}

/** Soft glowing disc (radial gradient fading to transparent). */
internal fun DrawScope.glowCircle(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/**
 * Deterministic pseudo-random points in the unit square: (x, y, size) with all values in 0..1.
 * The same [seed] always gives the same field, so stars and particles never "jump" between frames.
 */
internal fun scatter(seed: Int, count: Int): List<Triple<Float, Float, Float>> {
    val rnd = java.util.Random(seed.toLong())
    return List(count) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
}

/** `FontWeight` for a bold digit style, replaced by the user's weight choice when one is set. */
internal fun ClockFrame.digitWeight(default: FontWeight = FontWeight.Light): FontWeight = weight(default)

internal fun Dp.coerceAtLeast1(): Dp = if (value < 1f) 1.dp else this
