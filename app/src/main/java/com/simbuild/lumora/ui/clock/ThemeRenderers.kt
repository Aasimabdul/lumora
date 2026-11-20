package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.battery.BatteryState
import com.simbuild.lumora.core.battery.ChargeStatus
import kotlin.math.cos
import kotlin.math.sin

private val Cyan = Color(0xFF00E5FF)
private val Navy = Color(0xFF0D1B2A)

private class Metrics(val unit: Float, val width: Float, val height: Float)

/**
 * Draws one clock theme. Only ids in [implementedThemeIds] have a renderer; any other id
 * shows a plain "not available" message instead of pretending to be a theme.
 */
object ThemeRenderers {
    @Composable
    fun Render(themeId: String, frame: ClockFrame, modifier: Modifier = Modifier) {
        when (themeId) {
            "minimal_digital" -> MinimalDigital(frame, modifier)
            "neon_cyan" -> NeonCyan(frame, modifier)
            "flip_clock" -> FlipClock(frame, modifier)
            "vertical_clock" -> VerticalClock(frame, modifier)
            "analog_classic" -> AnalogClassic(frame, modifier)
            "minimal_analog" -> MinimalAnalog(frame, modifier)
            else -> Stage(modifier) { Text("This theme isn't available yet", color = Color.Gray) }
        }
    }
}

/** Pure-black stage; [Metrics.unit] is a base text size derived from the available space. */
@Composable
private fun Stage(modifier: Modifier, content: @Composable ColumnScope.(Metrics) -> Unit) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val metrics = Metrics(
            unit = minOf(maxWidth.value / 3.3f, maxHeight.value / 1.7f),
            width = maxWidth.value,
            height = maxHeight.value,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { content(this, metrics) }
    }
}

private fun batteryText(b: BatteryState): String {
    val level = b.percent?.let { "$it%" } ?: "--%"
    return when (b.status) {
        ChargeStatus.CHARGING -> "$level · Charging"
        ChargeStatus.FULL -> "$level · Fully charged"
        else -> level
    }
}

@Composable
private fun InfoLines(f: ClockFrame, m: Metrics, color: Color = Color.White) {
    val size = (m.unit * 0.17f).coerceIn(7f, 22f).sp
    if (f.showDate) {
        Text("${f.weekday} · ${f.date}", color = color.copy(alpha = 0.75f), fontSize = size)
    }
    if (f.showBattery) {
        Text(batteryText(f.battery), color = color.copy(alpha = 0.75f), fontSize = size)
    }
}

@Composable
private fun AmPm(f: ClockFrame, m: Metrics, color: Color) {
    f.time.amPm?.let {
        Text(it, color = color, fontSize = (m.unit * 0.22f).sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun MinimalDigital(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    Row(verticalAlignment = Alignment.Bottom) {
        Text(f.time.digits, color = Color.White, fontSize = m.unit.sp, fontWeight = FontWeight.Light)
        AmPm(f, m, Color.White.copy(alpha = 0.7f))
    }
    InfoLines(f, m)
}

@Composable
private fun NeonCyan(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            f.time.digits,
            style = TextStyle(
                color = Cyan,
                fontSize = m.unit.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Cyan.copy(alpha = 0.8f), offset = Offset.Zero, blurRadius = 28f),
            ),
        )
        AmPm(f, m, Cyan)
    }
    InfoLines(f, m, Cyan)
}

@Composable
private fun FlipClock(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((m.unit * 0.05f).dp),
    ) {
        f.time.digits.forEach { ch ->
            if (ch == ':') {
                Text(":", color = Color.White.copy(alpha = 0.6f), fontSize = (m.unit * 0.6f).sp)
            } else {
                Box(
                    modifier = Modifier
                        .size(width = (m.unit * 0.62f).dp, height = (m.unit * 0.9f).dp)
                        .clip(RoundedCornerShape((m.unit * 0.08f).dp))
                        .background(Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ch.toString(), color = Color.White, fontSize = (m.unit * 0.72f).sp, fontWeight = FontWeight.Bold)
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
private fun VerticalClock(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    val (hour, minute) = f.time.digits.split(":").let { it[0] to it.getOrElse(1) { "00" } }
    val size = (m.unit * 0.55f).sp
    Text(hour, color = Color.White, fontSize = size, fontWeight = FontWeight.Bold)
    Text(minute, color = Cyan, fontSize = size, fontWeight = FontWeight.Light)
    f.time.amPm?.let { Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = (m.unit * 0.17f).sp) }
    InfoLines(f, m)
}

@Composable
private fun AnalogClassic(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    AnalogDial(
        hour = f.zoned.hour,
        minute = f.zoned.minute,
        dial = minOf(m.width, m.height * 0.72f).dp,
        outline = true,
        allTicks = true,
        ink = Color.White,
        accent = Cyan,
    )
    InfoLines(f, m)
}

@Composable
private fun MinimalAnalog(f: ClockFrame, modifier: Modifier) = Stage(modifier) { m ->
    AnalogDial(
        hour = f.zoned.hour,
        minute = f.zoned.minute,
        dial = minOf(m.width, m.height * 0.72f).dp,
        outline = false,
        allTicks = false,
        ink = Color.White.copy(alpha = 0.9f),
        accent = Color.White,
    )
    InfoLines(f, m)
}

@Composable
private fun AnalogDial(
    hour: Int,
    minute: Int,
    dial: androidx.compose.ui.unit.Dp,
    outline: Boolean,
    allTicks: Boolean,
    ink: Color,
    accent: Color,
) {
    Canvas(Modifier.size(dial)) {
        val c = center
        val r = this.size.minDimension / 2f
        if (outline) {
            drawCircle(ink.copy(alpha = 0.35f), radius = r, style = Stroke(width = r * 0.01f))
        }
        for (i in 0 until 60) {
            val major = i % 5 == 0
            if (!allTicks && i % 15 != 0) continue
            val angle = Math.toRadians(i * 6.0 - 90.0)
            val outer = r * 0.96f
            val inner = outer - if (major) r * 0.10f else r * 0.04f
            drawLine(
                color = ink,
                start = Offset(c.x + cos(angle).toFloat() * inner, c.y + sin(angle).toFloat() * inner),
                end = Offset(c.x + cos(angle).toFloat() * outer, c.y + sin(angle).toFloat() * outer),
                strokeWidth = if (major) r * 0.02f else r * 0.008f,
                cap = StrokeCap.Round,
            )
        }
        val hourAngle = Math.toRadians((hour % 12) * 30.0 + minute * 0.5 - 90.0)
        val minuteAngle = Math.toRadians(minute * 6.0 - 90.0)
        drawHand(c, hourAngle, r * 0.5f, r * 0.035f, ink)
        drawHand(c, minuteAngle, r * 0.78f, r * 0.022f, accent)
        drawCircle(accent, radius = r * 0.04f, center = c)
    }
}

private fun DrawScope.drawHand(center: Offset, angle: Double, length: Float, width: Float, color: Color) {
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x + cos(angle).toFloat() * length, center.y + sin(angle).toFloat() * length),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}
