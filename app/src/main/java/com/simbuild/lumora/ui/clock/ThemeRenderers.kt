package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Draws one clock theme by id. Ids missing from [themeRegistry] show a plain message instead of
 * pretending to be a theme (the regression test asserts that no catalog id is missing).
 */
object ThemeRenderers {
    @Composable
    fun Render(themeId: String, frame: ClockFrame, modifier: Modifier = Modifier) {
        val content = themeRegistry[themeId]
        if (content != null) {
            content(frame, modifier)
        } else {
            Stage(frame, modifier) { Text("This theme isn't available", color = Color.Gray) }
        }
    }
}

// ---- Digital: Minimal Digital, Neon Cyan, Flip Clock, Vertical Clock ---------------------

@Composable
internal fun MinimalDigital(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    Row(verticalAlignment = Alignment.Bottom) {
        Text(f.time.digits, color = f.ink(Color.White), fontSize = m.unit.sp, fontWeight = f.weight(FontWeight.Light))
        AmPm(f, m, Color.White.copy(alpha = 0.7f))
    }
    InfoLines(f, m)
}

@Composable
internal fun NeonCyan(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    val c = f.accent(LumoraCyan)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            f.time.digits,
            style = TextStyle(
                color = c,
                fontSize = m.unit.sp,
                fontWeight = f.weight(FontWeight.Bold),
                shadow = Shadow(color = c.copy(alpha = 0.8f), offset = Offset.Zero, blurRadius = 28f),
            ),
        )
        AmPm(f, m, LumoraCyan)
    }
    InfoLines(f, m, LumoraCyan)
}

@Composable
internal fun FlipClock(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((m.unit * 0.05f).dp),
    ) {
        f.time.digits.forEach { ch ->
            if (ch == ':') {
                Text(":", color = f.ink(Color.White).copy(alpha = 0.6f), fontSize = (m.unit * 0.6f).sp)
            } else {
                Box(
                    modifier = Modifier
                        .size(width = (m.unit * 0.62f).dp, height = (m.unit * 0.9f).dp)
                        .clip(RoundedCornerShape((m.unit * 0.08f).dp))
                        .background(LumoraNavy),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ch.toString(), color = f.ink(Color.White), fontSize = (m.unit * 0.72f).sp, fontWeight = f.weight(FontWeight.Bold))
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height((m.unit * 0.02f).coerceAtLeast(1f).dp)
                            .background(Color.Black),
                    )
                }
            }
        }
        AmPm(f, m, Color.White.copy(alpha = 0.7f))
    }
    InfoLines(f, m)
}

@Composable
internal fun VerticalClock(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    val size = (m.unit * 0.55f).sp
    Text(f.hourText, color = f.ink(Color.White), fontSize = size, fontWeight = f.weight(FontWeight.Bold))
    Text(f.minuteText, color = f.accent(LumoraCyan), fontSize = size, fontWeight = f.weight(FontWeight.Light))
    f.time.amPm?.let { Text(it, color = f.ink(Color.White).copy(alpha = 0.7f), fontSize = (m.unit * 0.17f).sp) }
    InfoLines(f, m)
}

// ---- Analog: Analog Classic, Minimal Analog ----------------------------------------------

@Composable
internal fun AnalogClassic(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    AnalogDial(f, m.dial.dp, outline = true, allTicks = true, ink = f.ink(Color.White), accent = f.accent(LumoraCyan))
    InfoLines(f, m)
}

@Composable
internal fun MinimalAnalog(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    AnalogDial(f, m.dial.dp, outline = false, allTicks = false, ink = f.ink(Color.White).copy(alpha = 0.9f), accent = f.ink(Color.White))
    InfoLines(f, m)
}

@Composable
internal fun AnalogDial(
    f: ClockFrame,
    dial: androidx.compose.ui.unit.Dp,
    outline: Boolean,
    allTicks: Boolean,
    ink: Color,
    accent: Color,
) {
    Canvas(Modifier.size(dial)) {
        val c = center
        val r = this.size.minDimension / 2f
        if (outline) drawCircle(ink.copy(alpha = 0.35f), radius = r, style = Stroke(width = r * 0.01f))
        for (i in 0 until 60) {
            val major = i % 5 == 0
            if (!allTicks && i % 15 != 0) continue
            val outer = r * 0.96f
            val inner = outer - if (major) r * 0.10f else r * 0.04f
            drawLine(
                color = ink,
                start = polar(c, i * 6.0, inner),
                end = polar(c, i * 6.0, outer),
                strokeWidth = if (major) r * 0.02f else r * 0.008f,
                cap = StrokeCap.Round,
            )
        }
        drawHand(c, f.hourDegrees, r * 0.5f, r * 0.035f, ink)
        drawHand(c, f.minuteDegrees, r * 0.78f, r * 0.022f, accent)
        drawCircle(accent, radius = r * 0.04f, center = c)
    }
}

internal val baseThemes: Map<String, ThemeContent> = mapOf(
    "minimal_digital" to { f, m -> MinimalDigital(f, m) },
    "neon_cyan" to { f, m -> NeonCyan(f, m) },
    "flip_clock" to { f, m -> FlipClock(f, m) },
    "vertical_clock" to { f, m -> VerticalClock(f, m) },
    "analog_classic" to { f, m -> AnalogClassic(f, m) },
    "minimal_analog" to { f, m -> MinimalAnalog(f, m) },
)
