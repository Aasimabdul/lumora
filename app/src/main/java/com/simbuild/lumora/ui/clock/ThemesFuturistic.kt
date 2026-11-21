package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.battery.ChargeStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ---- shared palette ---------------------------------------------------------------------

private val CyberBg = Color(0xFF0A0314)
private val CyberMagenta = Color(0xFFFF2BD6)
private val CyberCyan = Color(0xFF00F0FF)

private val HudBg = Color(0xFF030B12)
private val HudCyan = Color(0xFF29E6FF)
private val HudOrange = Color(0xFFFF9F1C)

// ============================================================================================
// 1. Cyberpunk: perspective grid floor, striped sun, chamfered neon frame
// ============================================================================================

private fun DrawScope.futuristicCyberBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val horizon = h * 0.6f
    val cx = w / 2f

    val sky = Brush.verticalGradient(
        colors = listOf(Color(0xFF07020F), Color(0xFF1A0536), Color(0xFF3A0B4F)),
        startY = 0f,
        endY = horizon,
    )
    drawRect(sky, topLeft = Offset.Zero, size = Size(w, horizon))

    scatter(5, 26).forEach { (x, y, s) ->
        drawCircle(
            Color.White.copy(alpha = 0.25f + 0.5f * s),
            radius = md * 0.002f * (1f + 1.5f * s),
            center = Offset(x * w, y * horizon * 0.8f),
        )
    }

    // Striped sun, half sunk into the horizon.
    val r = md * 0.24f
    val cy = horizon - r * 0.45f
    val sunCenter = Offset(cx, cy)
    glowCircle(sunCenter, r * 1.7f, CyberMagenta.copy(alpha = 0.28f))
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFE14D), Color(0xFFFF4FA3), Color(0xFFB4189C)),
            startY = cy - r,
            endY = cy + r,
        ),
        radius = r,
        center = sunCenter,
    )
    for (i in 0 until 6) {
        val top = cy + r * (-0.05f + 0.15f * i)
        val thick = r * (0.015f + 0.022f * i)
        drawRect(sky, topLeft = Offset(cx - r, top), size = Size(r * 2f, thick))
    }

    // Floor with perspective grid.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2A0A4A), Color(0xFF0B0218)),
            startY = horizon,
            endY = h,
        ),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    for (i in 1..9) {
        val t = i / 9f
        val y = horizon + (h - horizon) * t * t
        drawLine(
            CyberMagenta.copy(alpha = 0.15f + 0.5f * t),
            Offset(0f, y),
            Offset(w, y),
            strokeWidth = md * 0.003f * (0.6f + t),
        )
    }
    for (k in -12..12) {
        drawLine(
            CyberCyan.copy(alpha = 0.45f),
            Offset(cx, horizon),
            Offset(cx + k * w * 0.16f, h),
            strokeWidth = md * 0.0028f,
        )
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, CyberMagenta.copy(alpha = 0.35f)),
            startY = horizon - md * 0.08f,
            endY = horizon,
        ),
        topLeft = Offset(0f, horizon - md * 0.08f),
        size = Size(w, md * 0.08f),
    )
    drawLine(CyberCyan, Offset(0f, horizon), Offset(w, horizon), strokeWidth = md * 0.004f)
}

private fun futuristicChamferPath(l: Float, t: Float, r: Float, b: Float, cut: Float): Path = Path().apply {
    moveTo(l + cut, t)
    lineTo(r, t)
    lineTo(r, b - cut)
    lineTo(r - cut, b)
    lineTo(l, b)
    lineTo(l, t + cut)
    close()
}

private fun DrawScope.futuristicCyberFrame(magenta: Color, cyan: Color) {
    val w = size.width
    val h = size.height
    val sw = minOf(w, h) * 0.028f
    val cut = minOf(w, h) * 0.24f
    val outer = futuristicChamferPath(sw * 2.5f, sw * 2.5f, w - sw * 2.5f, h - sw * 2.5f, cut)
    drawPath(outer, magenta.copy(alpha = 0.16f), style = Stroke(width = sw * 5f, join = StrokeJoin.Round))
    drawPath(outer, magenta.copy(alpha = 0.32f), style = Stroke(width = sw * 2.4f, join = StrokeJoin.Round))
    drawPath(outer, magenta, style = Stroke(width = sw * 0.9f, join = StrokeJoin.Round))
    val g = sw * 5.5f
    val inner = futuristicChamferPath(g, g, w - g, h - g, cut * 0.65f)
    drawPath(inner, cyan.copy(alpha = 0.16f), style = Stroke(width = sw * 3.6f, join = StrokeJoin.Round))
    drawPath(inner, cyan.copy(alpha = 0.34f), style = Stroke(width = sw * 1.6f, join = StrokeJoin.Round))
    drawPath(inner, cyan, style = Stroke(width = sw * 0.5f, join = StrokeJoin.Round))
}

@Composable
private fun ThemeCyberpunk(f: ClockFrame, modifier: Modifier) {
    val magenta = f.accent(CyberMagenta)
    val cyan = f.accent(CyberCyan)
    Stage(f, modifier, background = CyberBg, backdrop = { futuristicCyberBackdrop() }) { m ->
        val labelSp = infoFontSize(f, m, 0.1f).sp
        Row(
            modifier = Modifier.padding(bottom = (m.unit * 0.08f).dp),
            horizontalArrangement = Arrangement.spacedBy((m.unit * 0.22f).dp),
        ) {
            Text("SYS//CLK", color = cyan.copy(alpha = 0.9f), fontSize = labelSp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text("NET:ONLINE", color = magenta.copy(alpha = 0.9f), fontSize = labelSp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text("MODE:NEON", color = cyan.copy(alpha = 0.9f), fontSize = labelSp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
        Box(
            modifier = Modifier
                .drawBehind { futuristicCyberFrame(magenta, cyan) }
                .padding(horizontal = (m.unit * 0.36f).dp, vertical = (m.unit * 0.18f).dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f.time.digits,
                    color = f.ink(Color.White),
                    fontSize = (m.unit * 0.74f).sp,
                    fontWeight = f.weight(FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(magenta.copy(alpha = 0.9f), Offset(m.unit * 0.025f, 0f), m.unit * 0.2f),
                    ),
                )
                AmPm(f, m, cyan, 0.16f)
            }
        }
        Box(Modifier.padding(top = (m.unit * 0.08f).dp)) { }
        InfoLines(f, m, cyan)
    }
}

// ============================================================================================
// 2. Aurora Glow: overlapping soft radial blobs, thin spaced digits
// ============================================================================================

private fun DrawScope.futuristicAuroraBackdrop(hour: Int) {
    val w = size.width
    val h = size.height
    val big = maxOf(w, h)
    val palette = listOf(
        Color(0xFF1DE9A6), Color(0xFF14B8C4), Color(0xFF8B5CF6),
        Color(0xFF22D3A0), Color(0xFF6D4AFF), Color(0xFF0EA5C6),
    )
    val phase = (hour % 24) / 24f * 2f * PI.toFloat()
    for (i in 0 until 6) {
        val a = phase * (1 + i % 3) + i * 1.05f
        val cx = w * (0.5f + 0.34f * cos(a))
        val cy = h * (0.5f + 0.32f * sin(a * 0.8f + i))
        val r = big * (0.38f + 0.07f * (i % 3))
        glowCircle(Offset(cx, cy), r, palette[i].copy(alpha = 0.42f))
    }
}

@Composable
private fun ThemeAuroraGlow(f: ClockFrame, modifier: Modifier) {
    val line = f.accent(Color(0xFF7CFFD9))
    val hour = f.zoned.hour
    Stage(f, modifier, background = Color(0xFF050B14), backdrop = { futuristicAuroraBackdrop(hour) }) { m ->
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = f.ink(Color(0xFFF2FFFB)),
                fontSize = (m.unit * 0.85f).sp,
                fontWeight = f.weight(FontWeight.ExtraLight),
                letterSpacing = (m.unit * 0.07f).sp,
                maxLines = 1,
                softWrap = false,
            )
            AmPm(f, m, Color(0xFFF2FFFB).copy(alpha = 0.8f), 0.14f)
        }
        Box(
            Modifier
                .padding(vertical = (m.unit * 0.1f).dp)
                .size(width = (m.unit * 1.4f).dp, height = 1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, line, Color.Transparent))),
        )
        InfoLines(f, m, Color(0xFFDFFFF5))
    }
}

// ============================================================================================
// 3. Wave Flow: layered translucent waves in the lower half
// ============================================================================================

private fun futuristicWavePath(w: Float, h: Float, baseY: Float, amp: Float, cycles: Float, phase: Float, close: Boolean): Path {
    val steps = 48
    val path = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val a = phase + cycles * 2f * PI.toFloat() * t
        val y = baseY + amp * sin(a) + amp * 0.35f * sin(a * 2.3f + 1f)
        if (i == 0) path.moveTo(0f, y) else path.lineTo(w * t, y)
    }
    if (close) {
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
    }
    return path
}

private fun DrawScope.futuristicWaveBackdrop(minute: Int) {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF040B24), Color(0xFF0A2A5E)),
            startY = 0f,
            endY = h,
        ),
    )
    val colors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF1D4ED8))
    val base = minute / 60f * 2f * PI.toFloat()
    for (i in 0 until 5) {
        val baseY = h * (0.6f + 0.075f * i)
        val amp = h * (0.045f - 0.004f * i)
        val cycles = 1.3f + 0.35f * i
        val phase = base * (1 + i % 2) + i * 1.3f
        val fill = futuristicWavePath(w, h, baseY, amp, cycles, phase, true)
        drawPath(
            fill,
            Brush.verticalGradient(
                colors = listOf(colors[i].copy(alpha = 0.5f), colors[i].copy(alpha = 0.12f)),
                startY = baseY - amp,
                endY = h,
            ),
        )
        if (i % 2 == 0) {
            val crest = futuristicWavePath(w, h, baseY, amp, cycles, phase, false)
            drawPath(crest, colors[i].copy(alpha = 0.85f), style = Stroke(width = minOf(w, h) * 0.003f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ThemeWaveFlow(f: ClockFrame, modifier: Modifier) {
    val glow = f.accent(Color(0xFF22D3EE))
    val minute = f.zoned.minute
    Stage(f, modifier, background = Color(0xFF040B24), backdrop = { futuristicWaveBackdrop(minute) }) { m ->
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = f.ink(Color.White),
                fontSize = (m.unit * 0.9f).sp,
                fontWeight = f.weight(FontWeight.Light),
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(shadow = Shadow(glow.copy(alpha = 0.55f), Offset.Zero, m.unit * 0.15f)),
            )
            AmPm(f, m, Color.White.copy(alpha = 0.8f), 0.16f)
        }
        InfoLines(f, m, Color(0xFFBDE9FF))
    }
}

// ============================================================================================
// 4. Geometric Clock: low-poly triangle mesh, bold hexagon frame
// ============================================================================================

private fun futuristicTriPath(a: Offset, b: Offset, c: Offset): Path = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    close()
}

private fun DrawScope.futuristicTriangle(a: Offset, b: Offset, c: Offset, color: Color) {
    drawPath(futuristicTriPath(a, b, c), color)
}

private fun DrawScope.futuristicPolyBackdrop() {
    val w = size.width
    val h = size.height
    val cols = 8
    val cellW = w / cols
    val rows = (h / cellW).toInt().coerceIn(4, 12)
    val cellH = h / rows
    val rnd = java.util.Random(42L)
    val pts = Array(rows + 1) { r ->
        Array(cols + 1) { c ->
            val jx = if (c == 0 || c == cols) 0f else (rnd.nextFloat() - 0.5f) * cellW * 0.6f
            val jy = if (r == 0 || r == rows) 0f else (rnd.nextFloat() - 0.5f) * cellH * 0.6f
            Offset(c * cellW + jx, r * cellH + jy)
        }
    }
    val dark = Color(0xFF0B1330)
    val light = Color(0xFF1F5A9A)
    fun tone(a: Offset, b: Offset, c: Offset): Color {
        val t = ((a.x + b.x + c.x) / (3f * w) + (a.y + b.y + c.y) / (3f * h)) / 2f
        val k = (t + (rnd.nextFloat() - 0.5f) * 0.28f).coerceIn(0f, 1f)
        return lerp(dark, light, k)
    }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val p00 = pts[r][c]
            val p10 = pts[r][c + 1]
            val p01 = pts[r + 1][c]
            val p11 = pts[r + 1][c + 1]
            if ((r + c) % 2 == 0) {
                futuristicTriangle(p00, p10, p11, tone(p00, p10, p11))
                futuristicTriangle(p00, p11, p01, tone(p00, p11, p01))
            } else {
                futuristicTriangle(p00, p10, p01, tone(p00, p10, p01))
                futuristicTriangle(p10, p11, p01, tone(p10, p11, p01))
            }
        }
    }
    glowCircle(center, minOf(w, h) * 0.6f, Color(0xFF3B82F6).copy(alpha = 0.22f))
}

private fun futuristicHexPath(l: Float, t: Float, r: Float, b: Float, cut: Float): Path = Path().apply {
    val midY = (t + b) / 2f
    moveTo(l + cut, t)
    lineTo(r - cut, t)
    lineTo(r, midY)
    lineTo(r - cut, b)
    lineTo(l + cut, b)
    lineTo(l, midY)
    close()
}

private fun DrawScope.futuristicHexagon(accent: Color) {
    val w = size.width
    val h = size.height
    val cut = w * 0.2f
    val sw = h * 0.055f
    val hex = futuristicHexPath(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f, cut)
    drawPath(hex, Color(0xFF050B1E).copy(alpha = 0.72f))
    drawPath(hex, accent.copy(alpha = 0.25f), style = Stroke(width = sw * 2.6f, join = StrokeJoin.Round))
    drawPath(hex, accent, style = Stroke(width = sw, join = StrokeJoin.Round))
    val g = sw * 2.6f
    val inner = futuristicHexPath(g, g, w - g, h - g, cut * 0.8f)
    drawPath(inner, accent.copy(alpha = 0.45f), style = Stroke(width = sw * 0.3f, join = StrokeJoin.Round))

    // Ornaments: outward-pointing triangles at the four extreme edges.
    val o = h * 0.14f
    val half = h * 0.07f
    val midY = h / 2f
    drawPath(futuristicTriPath(Offset(-o, midY), Offset(-o * 0.15f, midY - half), Offset(-o * 0.15f, midY + half)), accent)
    drawPath(futuristicTriPath(Offset(w + o, midY), Offset(w + o * 0.15f, midY - half), Offset(w + o * 0.15f, midY + half)), accent)
    drawPath(futuristicTriPath(Offset(w / 2f, -o * 0.6f), Offset(w / 2f - half, -o * 0.1f), Offset(w / 2f + half, -o * 0.1f)), accent.copy(alpha = 0.8f))
    drawPath(futuristicTriPath(Offset(w / 2f, h + o * 0.6f), Offset(w / 2f - half, h + o * 0.1f), Offset(w / 2f + half, h + o * 0.1f)), accent.copy(alpha = 0.8f))
    // Vertex dots.
    val dot = sw * 0.8f
    drawCircle(accent, dot, Offset(cut, 0f))
    drawCircle(accent, dot, Offset(w - cut, 0f))
    drawCircle(accent, dot, Offset(cut, h))
    drawCircle(accent, dot, Offset(w - cut, h))
}

@Composable
private fun ThemeGeometricClock(f: ClockFrame, modifier: Modifier) {
    val accent = f.accent(Color(0xFFFFC145))
    Stage(f, modifier, background = Color(0xFF0B1330), backdrop = { futuristicPolyBackdrop() }) { m ->
        Box(
            modifier = Modifier
                .size(width = (m.unit * 2.5f).dp, height = (m.unit * 1.2f).dp)
                .drawBehind { futuristicHexagon(accent) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f.time.digits,
                    color = f.ink(Color.White),
                    fontSize = (m.unit * 0.64f).sp,
                    fontWeight = f.weight(FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false,
                )
                AmPm(f, m, accent, 0.14f)
            }
        }
        Box(Modifier.padding(top = (m.unit * 0.06f).dp)) { }
        InfoLines(f, m, Color(0xFFDCE8FF))
    }
}

// ============================================================================================
// 5. Particle Glow: bokeh dots, glowing digits, thin ring
// ============================================================================================

private fun DrawScope.futuristicParticleBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFF16123A), Color(0xFF05060F)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.7f,
        ),
    )
    val colors = listOf(
        Color(0xFFFF5FA2), Color(0xFF38E8FF), Color(0xFFFFC857),
        Color(0xFFA78BFA), Color(0xFFFFFFFF), Color(0xFF5EF2B0),
    )
    scatter(7, 80).forEachIndexed { i, (x, y, s) ->
        val c = colors[i % colors.size]
        val center = Offset(x * w, y * h)
        val r = md * (0.012f + 0.055f * s * s)
        glowCircle(center, r * 2.2f, c.copy(alpha = 0.22f + 0.3f * s))
        if (s > 0.55f) drawCircle(c.copy(alpha = 0.3f), r, center, style = Stroke(width = r * 0.07f))
        drawCircle(c.copy(alpha = 0.5f), r * 0.18f, center)
    }
}

private fun DrawScope.futuristicGlowRing(color: Color, minuteDeg: Double) {
    val c = center
    val r = size.minDimension / 2f
    glowCircle(c, r * 0.95f, color.copy(alpha = 0.12f))
    drawCircle(color.copy(alpha = 0.7f), r * 0.97f, c, style = Stroke(width = maxOf(1f, r * 0.012f)))
    drawCircle(
        color.copy(alpha = 0.28f),
        r * 0.86f,
        c,
        style = Stroke(
            width = maxOf(1f, r * 0.006f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.03f, r * 0.06f), 0f),
        ),
    )
    drawCircle(color, r * 0.035f, polar(c, minuteDeg, r * 0.97f))
}

@Composable
private fun ThemeParticleGlow(f: ClockFrame, modifier: Modifier) {
    val glow = f.accent(Color(0xFFFF7AD9))
    val minuteDeg = f.minuteDegrees
    Stage(f, modifier, background = Color(0xFF05060F), backdrop = { futuristicParticleBackdrop() }) { m ->
        val d = m.dial * 0.86f
        Box(
            modifier = Modifier
                .size(d.dp)
                .drawBehind { futuristicGlowRing(glow, minuteDeg) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f.time.digits,
                    color = f.ink(Color.White),
                    fontSize = (d * 0.27f).sp,
                    fontWeight = f.weight(FontWeight.Light),
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(shadow = Shadow(glow, Offset.Zero, d * 0.1f)),
                )
                f.time.amPm?.let {
                    Text(it, color = f.ink(Color.White).copy(alpha = 0.8f), fontSize = (d * 0.07f).sp, modifier = Modifier.padding(start = 3.dp))
                }
            }
        }
        InfoLines(f, m, Color(0xFFF1E9FF))
    }
}

// ============================================================================================
// 6. Speed Lines: radial warp streaks, bold italic digits, accent streak
// ============================================================================================

private fun DrawScope.futuristicSpeedBackdrop(accent: Color) {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val c = center
    val rMax = sqrt(w * w + h * h) / 2f
    glowCircle(c, rMax * 0.6f, accent.copy(alpha = 0.18f))
    scatter(31, 120).forEachIndexed { i, (a, b, s) ->
        val deg = (a * 360f).toDouble()
        val start = rMax * (0.3f + 0.4f * b)
        val len = rMax * (0.1f + 0.45f * s * s)
        val col = if (i % 5 == 0) accent else Color.White
        drawLine(
            col.copy(alpha = 0.12f + 0.55f * s),
            polar(c, deg, start),
            polar(c, deg, start + len),
            strokeWidth = md * (0.002f + 0.006f * s),
            cap = StrokeCap.Round,
        )
    }
    glowCircle(c, rMax * 0.42f, Color(0xFF050814).copy(alpha = 0.85f))
}

private fun DrawScope.futuristicStreak(accent: Color) {
    val w = size.width
    val h = size.height
    val main = h * 0.45f
    val sk = main * 1.2f
    val p = Path().apply {
        moveTo(sk, 0f)
        lineTo(w, 0f)
        lineTo(w - sk, main)
        lineTo(0f, main)
        close()
    }
    drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, accent, accent, Color.Transparent), startX = 0f, endX = w))
    val y2 = h * 0.7f
    val h2 = h * 0.2f
    val sk2 = h2 * 1.2f
    val q = Path().apply {
        moveTo(w * 0.45f + sk2, y2)
        lineTo(w, y2)
        lineTo(w - sk2, y2 + h2)
        lineTo(w * 0.45f, y2 + h2)
        close()
    }
    drawPath(q, Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.6f), Color.Transparent), startX = w * 0.45f, endX = w))
}

@Composable
private fun ThemeSpeedLines(f: ClockFrame, modifier: Modifier) {
    val accent = f.accent(Color(0xFFFFB020))
    Stage(f, modifier, background = Color(0xFF050814), backdrop = { futuristicSpeedBackdrop(accent) }) { m ->
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = f.ink(Color.White),
                fontSize = (m.unit * 0.9f).sp,
                fontWeight = f.weight(FontWeight.Black),
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(accent.copy(alpha = 0.7f), Offset(m.unit * 0.035f, m.unit * 0.035f), 0f),
                ),
            )
            AmPm(f, m, accent, 0.16f)
        }
        Box(
            Modifier
                .padding(top = (m.unit * 0.03f).dp, bottom = (m.unit * 0.08f).dp)
                .size(width = (m.unit * 2.2f).dp, height = (m.unit * 0.16f).dp)
                .drawBehind { futuristicStreak(accent) },
        )
        InfoLines(f, m, Color.White)
    }
}

// ============================================================================================
// 7. Matrix Clock: digital rain from rects and lines, readable box
// ============================================================================================

private fun DrawScope.futuristicMatrixBackdrop() {
    val w = size.width
    val h = size.height
    val cs = maxOf(minOf(w, h) / 22f, w / 26f)
    val cols = (w / cs).toInt().coerceAtLeast(1)
    val pitch = cs * 1.35f
    val bw = cs * 0.46f
    val bh = cs * 0.78f
    val offX = (w - cols * cs) / 2f
    for (col in 0 until cols) {
        val rnd = java.util.Random(col * 7919L + 13L)
        val len = 4 + rnd.nextInt(8)
        val head = rnd.nextFloat() * (h + len * pitch)
        val dim = 0.55f + 0.45f * rnd.nextFloat()
        val x = offX + col * cs + (cs - bw) / 2f
        for (i in 0 until len) {
            val kind = rnd.nextInt(3)
            val skip = rnd.nextFloat() < 0.14f
            val y = head - i * pitch
            if (y < -pitch || y > h) continue
            if (skip && i > 0) continue
            val fade = 1f - i / len.toFloat()
            val base = if (i == 0) Color(0xFFD7FFE0) else Color(0xFF22E05A)
            val a = ((if (i == 0) 0.95f else 0.75f * fade * fade + 0.06f) * dim).coerceIn(0f, 1f)
            val color = base.copy(alpha = a)
            when (kind) {
                0 -> drawRect(color, topLeft = Offset(x, y), size = Size(bw, bh))
                1 -> drawLine(color, Offset(x + bw / 2f, y), Offset(x + bw / 2f, y + bh), strokeWidth = bw * 0.3f)
                else -> drawLine(color, Offset(x, y + bh / 2f), Offset(x + bw, y + bh / 2f), strokeWidth = bh * 0.3f)
            }
        }
    }
}

@Composable
private fun ThemeMatrixClock(f: ClockFrame, modifier: Modifier) {
    val green = f.accent(Color(0xFF4DFF88))
    val mono = if (f.font == FontFamily.Default) FontFamily.Monospace else f.font
    Stage(f, modifier, background = Color(0xFF010A04), backdrop = { futuristicMatrixBackdrop() }) { m ->
        val shape = RoundedCornerShape((m.unit * 0.14f).dp)
        Column(
            modifier = Modifier
                .background(Color(0xFF020A05).copy(alpha = 0.88f), shape)
                .border(1.dp, green.copy(alpha = 0.5f), shape)
                .padding(horizontal = (m.unit * 0.28f).dp, vertical = (m.unit * 0.16f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "> LOCAL_TIME",
                color = green.copy(alpha = 0.7f),
                fontSize = infoFontSize(f, m, 0.1f).sp,
                fontFamily = mono,
                letterSpacing = 2.sp,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f.time.digits,
                    color = f.ink(Color(0xFFB8FFCB)),
                    fontSize = (m.unit * 0.72f).sp,
                    fontWeight = f.weight(FontWeight.Medium),
                    fontFamily = mono,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(shadow = Shadow(green.copy(alpha = 0.8f), Offset.Zero, m.unit * 0.12f)),
                )
                AmPm(f, m, green, 0.14f)
            }
            InfoLines(f, m, green)
        }
    }
}

// ============================================================================================
// 8. HUD Dashboard: concentric arcs, tick ring, brackets, corner readouts
// ============================================================================================

private fun DrawScope.futuristicHudBackdrop(cyan: Color) {
    val w = size.width
    val h = size.height
    glowCircle(center, minOf(w, h) * 0.6f, cyan.copy(alpha = 0.10f))
    val step = minOf(w, h) / 10f
    var x = 0f
    while (x <= w) {
        drawLine(cyan.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= h) {
        drawLine(cyan.copy(alpha = 0.05f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.futuristicArc(c: Offset, r: Float, start: Float, sweep: Float, color: Color, sw: Float, cap: StrokeCap = StrokeCap.Butt) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = sw, cap = cap),
    )
}

private fun DrawScope.futuristicBracket(x: Float, y: Float, dx: Float, dy: Float, len: Float, sw: Float, color: Color) {
    drawLine(color, Offset(x, y), Offset(x + dx * len, y), strokeWidth = sw)
    drawLine(color, Offset(x, y), Offset(x, y + dy * len), strokeWidth = sw)
}

private fun DrawScope.futuristicHudRings(cyan: Color, orange: Color, minute: Int, hourDeg: Double, battery: Int) {
    val c = center
    val md = size.minDimension
    val rb = md * 0.40f

    // Battery gauge (270 degree arc, gap at the bottom).
    val pct = battery.coerceIn(0, 100)
    futuristicArc(c, rb, 135f, 270f, cyan.copy(alpha = 0.18f), rb * 0.045f)
    futuristicArc(c, rb, 135f, 270f * pct / 100f, orange, rb * 0.045f, StrokeCap.Round)

    // Minute tick ring.
    val outer = rb * 0.9f
    for (i in 0 until 60) {
        val major = i % 5 == 0
        val now = i == minute
        val len = rb * (if (now) 0.12f else if (major) 0.09f else 0.045f)
        val color = when {
            now -> orange
            major -> cyan.copy(alpha = 0.85f)
            else -> cyan.copy(alpha = 0.4f)
        }
        drawLine(
            color,
            polar(c, i * 6.0, outer - len),
            polar(c, i * 6.0, outer),
            strokeWidth = rb * (if (major || now) 0.02f else 0.01f),
        )
    }

    // Concentric thin arcs.
    futuristicArc(c, rb * 0.76f, 200f, 100f, cyan.copy(alpha = 0.7f), rb * 0.02f)
    futuristicArc(c, rb * 0.76f, 320f, 60f, cyan.copy(alpha = 0.7f), rb * 0.02f)
    futuristicArc(c, rb * 0.76f, 60f, 110f, cyan.copy(alpha = 0.35f), rb * 0.02f)
    futuristicArc(c, rb * 0.70f, 20f, 200f, orange.copy(alpha = 0.5f), rb * 0.012f)
    futuristicArc(c, rb * 0.70f, 240f, 80f, orange.copy(alpha = 0.5f), rb * 0.012f)
    drawCircle(orange, rb * 0.03f, polar(c, hourDeg, rb * 0.78f))

    // Side crosshair stubs.
    drawLine(cyan.copy(alpha = 0.5f), Offset(c.x - rb * 1.18f, c.y), Offset(c.x - rb * 1.06f, c.y), strokeWidth = rb * 0.012f)
    drawLine(cyan.copy(alpha = 0.5f), Offset(c.x + rb * 1.06f, c.y), Offset(c.x + rb * 1.18f, c.y), strokeWidth = rb * 0.012f)

    // Corner brackets.
    val w = size.width
    val h = size.height
    val len = md * 0.09f
    val sw = md * 0.012f
    val p = sw / 2f
    futuristicBracket(p, p, 1f, 1f, len, sw, orange)
    futuristicBracket(w - p, p, -1f, 1f, len, sw, orange)
    futuristicBracket(p, h - p, 1f, -1f, len, sw, orange)
    futuristicBracket(w - p, h - p, -1f, -1f, len, sw, orange)
}

@Composable
private fun FuturisticHudReadout(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    labelSp: Float,
    valueSp: Float,
    align: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = align) {
        Text(label, color = labelColor, fontSize = labelSp.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Text(value, color = valueColor, fontSize = valueSp.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun ThemeHudDashboard(f: ClockFrame, modifier: Modifier) {
    val cyan = f.accent(HudCyan)
    val orange = f.accent(HudOrange)
    val minute = f.zoned.minute
    val hourDeg = f.hourDegrees
    val percent = f.battery.percent ?: 0
    Stage(f, modifier, background = HudBg, backdrop = { futuristicHudBackdrop(cyan) }) { m ->
        val boxH = m.dial
        val boxW = minOf(m.width, m.dial * 1.6f)
        val labelSp = infoFontSize(f, m, 0.085f)
        val valueSp = infoFontSize(f, m, 0.12f)
        val pad = (boxH * 0.05f).dp
        val ink = f.ink(Color(0xFFE6FBFF))
        Box(
            modifier = Modifier
                .size(width = boxW.dp, height = boxH.dp)
                .drawBehind { futuristicHudRings(cyan, orange, minute, hourDeg, percent) },
        ) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        f.time.digits,
                        color = ink,
                        fontSize = (boxH * 0.17f).sp,
                        fontWeight = f.weight(FontWeight.Light),
                        maxLines = 1,
                        softWrap = false,
                    )
                    f.time.amPm?.let {
                        Text(it, color = orange, fontSize = (boxH * 0.055f).sp, modifier = Modifier.padding(start = 3.dp))
                    }
                }
                Text("LOCAL TIME", color = cyan.copy(alpha = 0.7f), fontSize = labelSp.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            if (f.showDate) {
                FuturisticHudReadout(
                    "DAY", f.weekday.uppercase(), orange.copy(alpha = 0.85f), cyan, labelSp, valueSp,
                    Alignment.Start, Modifier.align(Alignment.TopStart).padding(pad),
                )
                FuturisticHudReadout(
                    "DATE", f.date, orange.copy(alpha = 0.85f), cyan, labelSp, valueSp,
                    Alignment.End, Modifier.align(Alignment.TopEnd).padding(pad),
                )
            }
            if (f.showBattery) {
                val level = f.battery.percent?.let { "$it%" } ?: "--%"
                val value = if (f.battery.status == ChargeStatus.CHARGING) "$level +" else level
                FuturisticHudReadout(
                    "BAT", value, orange.copy(alpha = 0.85f), cyan, labelSp, valueSp,
                    Alignment.Start, Modifier.align(Alignment.BottomStart).padding(pad),
                )
            }
            FuturisticHudReadout(
                "YEAR DAY", "${f.zoned.dayOfYear}", orange.copy(alpha = 0.85f), cyan, labelSp, valueSp,
                Alignment.End, Modifier.align(Alignment.BottomEnd).padding(pad),
            )
        }
    }
}

// ============================================================================================

internal val futuristicThemes: Map<String, ThemeContent> = mapOf(
    "cyberpunk" to { f, m -> ThemeCyberpunk(f, m) },
    "aurora_glow" to { f, m -> ThemeAuroraGlow(f, m) },
    "wave_flow" to { f, m -> ThemeWaveFlow(f, m) },
    "geometric_clock" to { f, m -> ThemeGeometricClock(f, m) },
    "particle_glow" to { f, m -> ThemeParticleGlow(f, m) },
    "speed_lines" to { f, m -> ThemeSpeedLines(f, m) },
    "matrix_clock" to { f, m -> ThemeMatrixClock(f, m) },
    "hud_dashboard" to { f, m -> ThemeHudDashboard(f, m) },
)
