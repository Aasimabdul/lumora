package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.astro.MoonPhase
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================================
// Shared helpers (all private to this file)
// ============================================================================================

private const val NATURE_TAU = 2f * PI.toFloat()

/**
 * Time font size in sp. Bounded by the available height so a wide landscape screen does not
 * produce giant digits that collide with the scenery; the user's size multiplier applies to both.
 */
private fun natureSp(m: Metrics, k: Float): Float = minOf(m.unit * k, m.height * m.scale * 0.33f * k)

/** Deterministic star field between [top] and [bottom]; every [glowEvery]-th star gets a soft halo. */
private fun DrawScope.natureStars(
    seed: Int,
    count: Int,
    top: Float,
    bottom: Float,
    maxR: Float,
    color: Color,
    glowEvery: Int = 0,
) {
    val w = size.width
    scatter(seed, count).forEachIndexed { i, (x, y, s) ->
        val c = Offset(x * w, top + y * (bottom - top))
        val r = maxR * (0.35f + 0.65f * s * s)
        if (glowEvery > 0 && i % glowEvery == 0) glowCircle(c, r * 7f, color.copy(alpha = 0.28f))
        drawCircle(color.copy(alpha = 0.35f + 0.6f * s), radius = r, center = c)
    }
}

/** Plain glowing moon disc. */
private fun DrawScope.natureMoonDisc(center: Offset, r: Float, tint: Color, body: Color = Color(0xFFF3F1E4)) {
    glowCircle(center, r * 5f, tint.copy(alpha = 0.16f))
    glowCircle(center, r * 2.4f, tint.copy(alpha = 0.30f))
    drawCircle(body, radius = r, center = center)
    drawCircle(Color.Black.copy(alpha = 0.08f), radius = r * 0.22f, center = Offset(center.x - r * 0.3f, center.y - r * 0.2f))
    drawCircle(Color.Black.copy(alpha = 0.07f), radius = r * 0.14f, center = Offset(center.x + r * 0.35f, center.y + r * 0.3f))
}

/** Sharp-peaked mountain ridge polygon, filled down to [bottom]. */
private fun natureRidge(w: Float, bottom: Float, baseY: Float, amp: Float, f1: Float, f2: Float, phase: Float): Path {
    val steps = 96
    val path = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val a = t * f1 * NATURE_TAU + phase
        val b = t * f2 * NATURE_TAU + phase * 1.7f
        val y = baseY - amp * (0.65f * (1f - abs(sin(a))) + 0.35f * (1f - abs(sin(b))))
        if (i == 0) path.moveTo(0f, y) else path.lineTo(w * t, y)
    }
    path.lineTo(w, bottom)
    path.lineTo(0f, bottom)
    path.close()
    return path
}

/** Smooth sine wave; filled down to [bottom] when [close] is true, otherwise an open crest line. */
private fun natureWave(w: Float, bottom: Float, baseY: Float, amp: Float, cycles: Float, phase: Float, close: Boolean): Path {
    val steps = 64
    val path = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val a = phase + cycles * NATURE_TAU * t
        val y = baseY + amp * sin(a) + amp * 0.3f * sin(a * 2.1f + 1f)
        if (i == 0) path.moveTo(0f, y) else path.lineTo(w * t, y)
    }
    if (close) {
        path.lineTo(w, bottom)
        path.lineTo(0f, bottom)
        path.close()
    }
    return path
}

/** A row of stacked-triangle pine trees whose feet stand on [baseY]; they occupy x in [from]..[to] of the width. */
private fun DrawScope.naturePines(
    baseY: Float,
    treeHeight: Float,
    count: Int,
    seed: Int,
    color: Color,
    from: Float = 0f,
    to: Float = 1f,
) {
    val w = size.width
    scatter(seed, count).forEachIndexed { i, (jx, _, s) ->
        val x = (from + (to - from) * (i + 0.5f + (jx - 0.5f) * 0.8f) / count) * w
        val t = treeHeight * (0.75f + 0.5f * s)
        for (k in 0..2) {
            val apexY = baseY - t + k * t * 0.26f
            val bottomY = apexY + t * 0.4f
            val hw = t * (0.13f + 0.07f * k)
            val tier = Path().apply {
                moveTo(x, apexY)
                lineTo(x + hw, bottomY)
                lineTo(x - hw, bottomY)
                close()
            }
            drawPath(tier, color)
        }
        drawRect(color, topLeft = Offset(x - t * 0.025f, baseY - t * 0.1f), size = Size(t * 0.05f, t * 0.1f))
    }
}

/** The time as one line, with optional soft glow and AM/PM. */
@Composable
private fun NatureTime(
    f: ClockFrame,
    m: Metrics,
    k: Float,
    color: Color,
    weight: FontWeight,
    letterSpacing: Float = 0f,
    glow: Color? = null,
    family: FontFamily? = null,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            f.time.digits,
            color = f.ink(color),
            fontSize = natureSp(m, k).sp,
            fontWeight = f.weight(weight),
            letterSpacing = letterSpacing.sp,
            fontFamily = family,
            maxLines = 1,
            softWrap = false,
            style = if (glow != null) {
                LocalTextStyle.current.copy(shadow = Shadow(glow, Offset.Zero, m.unit * 0.12f))
            } else {
                LocalTextStyle.current
            },
        )
        AmPm(f, m, color.copy(alpha = 0.8f), 0.16f)
    }
}

// ============================================================================================
// 1. Moon Phase: today's real phase drawn as a lit crescent / gibbous on a dark disc
// ============================================================================================

/**
 * Lit part of the moon as a path: one limb semicircle plus the terminator (half ellipse).
 * Waxing = right side lit, waning = left side lit.
 */
private fun natureMoonLitPath(center: Offset, r: Float, phase: Double): Path {
    val waxing = MoonPhase.isWaxing(phase)
    val c = kotlin.math.cos(2.0 * PI * phase).toFloat()
    val s = if (waxing) 1f else -1f
    val sgn = if (c >= 0f) 1f else -1f
    val rx = (r * abs(c)).coerceAtLeast(0.5f)
    return Path().apply {
        moveTo(center.x, center.y - r)
        arcTo(Rect(center.x - r, center.y - r, center.x + r, center.y + r), -90f, 180f * s, false)
        arcTo(Rect(center.x - rx, center.y - r, center.x + rx, center.y + r), 90f, -180f * s * sgn, false)
        close()
    }
}

private fun DrawScope.natureMoonPhaseBackdrop(phase: Double) {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val land = w > h
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF03050B), Color(0xFF0A1226), Color(0xFF111A33)),
            startY = 0f,
            endY = h,
        ),
    )
    natureStars(11, 90, 0f, h, md * 0.0032f, Color.White)

    val r = if (land) h * 0.24f else minOf(w * 0.24f, h * 0.12f)
    val center = if (land) Offset(w * 0.15f, h * 0.5f) else Offset(w / 2f, h * 0.27f)
    val illum = MoonPhase.illumination(phase).toFloat()
    glowCircle(center, r * 2.6f, Color(0xFFB8CCFF).copy(alpha = 0.05f + 0.16f * illum))

    // Dark side with a faint rim so a new moon is still visible.
    drawCircle(Color(0xFF1A2236), radius = r, center = center)
    drawCircle(Color(0xFF3A4668).copy(alpha = 0.55f), radius = r, center = center, style = Stroke(width = r * 0.02f))
    // Lit part.
    drawPath(natureMoonLitPath(center, r, phase), Color(0xFFF4EFD6))
    // Subtle craters.
    scatter(3, 9).forEach { (x, y, s) ->
        val dx = (x - 0.5f) * 1.5f * r
        val dy = (y - 0.5f) * 1.5f * r
        if (dx * dx + dy * dy < (0.8f * r) * (0.8f * r)) {
            drawCircle(Color.Black.copy(alpha = 0.10f), radius = r * (0.05f + 0.09f * s), center = Offset(center.x + dx, center.y + dy))
        }
    }
}

@Composable
private fun ThemeMoonPhase(f: ClockFrame, modifier: Modifier) {
    val phase = MoonPhase.phase(f.zoned.toInstant())
    val illum = MoonPhase.illumination(phase)
    val label = "${MoonPhase.name(phase)} · ${(illum * 100.0).roundToInt()}% lit"
    Stage(f, modifier, background = Color(0xFF04060D), backdrop = { natureMoonPhaseBackdrop(phase) }) { m ->
        NatureTime(f, m, 0.9f, Color(0xFFF2F4FF), FontWeight.Light, letterSpacing = m.unit * 0.02f)
        Text(
            label,
            color = f.accent(Color(0xFFC9D6FF)),
            fontSize = infoFontSize(f, m, 0.2f).sp,
            modifier = Modifier.padding(top = (m.unit * 0.04f).dp),
        )
        InfoLines(f, m, Color(0xFFC9D6FF))
    }
}

// ============================================================================================
// 2. Starry Night: blue-violet sky, many stars, sparkles and shooting stars
// ============================================================================================

private fun DrawScope.natureStarryBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val big = maxOf(w, h)
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF060824), Color(0xFF1A1458), Color(0xFF3B1F72)),
            startY = 0f,
            endY = h,
        ),
    )
    glowCircle(Offset(w * 0.3f, h * 0.72f), big * 0.55f, Color(0xFF7C3AED).copy(alpha = 0.18f))
    glowCircle(Offset(w * 0.82f, h * 0.25f), big * 0.45f, Color(0xFF3B82F6).copy(alpha = 0.16f))

    natureStars(31, 150, 0f, h * 0.96f, md * 0.0045f, Color(0xFFF4F1FF), glowEvery = 9)

    // A few bright four-point sparkles.
    scatter(32, 8).forEach { (x, y, s) ->
        val c = Offset(x * w, y * h * 0.9f)
        val len = md * (0.012f + 0.016f * s)
        glowCircle(c, len * 2.6f, Color(0xFFBFB2FF).copy(alpha = 0.35f))
        val sw = md * 0.0018f
        drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x - len, c.y), Offset(c.x + len, c.y), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x, c.y - len), Offset(c.x, c.y + len), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(Color.White, radius = md * 0.003f, center = c)
    }

    // Thin shooting-star streaks: head position (fractions) and length.
    val streaks = listOf(
        Triple(0.78f, 0.10f, 0.20f),
        Triple(0.34f, 0.20f, 0.14f),
        Triple(0.92f, 0.46f, 0.12f),
    )
    streaks.forEach { (fx, fy, fl) ->
        val head = Offset(w * fx, h * fy)
        val len = md * fl * 1.6f
        val tail = Offset(head.x + len * 0.87f, head.y - len * 0.5f)
        drawLine(
            brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f)), start = tail, end = head),
            start = tail,
            end = head,
            strokeWidth = md * 0.0035f,
            cap = StrokeCap.Round,
        )
        glowCircle(head, md * 0.018f, Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ThemeStarryNight(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF060824), backdrop = { natureStarryBackdrop() }) { m ->
        NatureTime(
            f, m, 0.92f, Color(0xFFEDEBFF), FontWeight.ExtraLight,
            letterSpacing = m.unit * 0.04f,
            glow = Color(0xFF9D8CFF).copy(alpha = 0.5f),
        )
        Box(Modifier.padding(vertical = (m.unit * 0.06f).dp))
        InfoLines(f, m, Color(0xFFD9D4FF))
    }
}

// ============================================================================================
// 3. Mountain Night: three layered blue-grey ridges, moon, mist
// ============================================================================================

private fun DrawScope.natureMountainBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val land = w > h
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF070D1F), Color(0xFF16233F), Color(0xFF2D4264)),
            startY = 0f,
            endY = h * 0.85f,
        ),
    )
    natureStars(71, 80, 0f, h * 0.5f, md * 0.0032f, Color.White)
    natureMoonDisc(if (land) Offset(w * 0.82f, h * 0.2f) else Offset(w * 0.78f, h * 0.14f), md * 0.055f, Color(0xFFBFD4FF))

    val top = if (land) 0.62f else 0.58f
    // Far ridge (lightest blue-grey).
    drawPath(natureRidge(w, h, h * 0.82f, h * (0.82f - top), 2.5f, 5.3f, 0.4f), Color(0xFF33466A))
    // Mist between far and mid ridge.
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFFB7C8E6).copy(alpha = 0.22f), Color.Transparent),
            startY = h * 0.74f,
            endY = h * 0.88f,
        ),
        topLeft = Offset(0f, h * 0.74f),
        size = Size(w, h * 0.14f),
    )
    // Mid ridge.
    drawPath(natureRidge(w, h, h * 0.9f, h * (if (land) 0.13f else 0.17f), 3.2f, 6.1f, 2.1f), Color(0xFF223250))
    // Thin low mist band.
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFFB7C8E6).copy(alpha = 0.16f), Color.Transparent),
            startY = h * 0.86f,
            endY = h * 0.96f,
        ),
        topLeft = Offset(0f, h * 0.86f),
        size = Size(w, h * 0.10f),
    )
    // Near ridge (darkest).
    drawPath(natureRidge(w, h, h * 0.99f, h * 0.1f, 1.8f, 4.4f, 3.3f), Color(0xFF111A2D))
}

@Composable
private fun ThemeMountainNight(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF070D1F), backdrop = { natureMountainBackdrop() }) { m ->
        val land = m.width > m.height
        NatureTime(f, m, 0.88f, Color(0xFFE6EEFA), FontWeight.Light, glow = Color(0xFF8FB0E8).copy(alpha = 0.35f))
        Box(
            Modifier
                .padding(vertical = (m.unit * 0.05f).dp)
                .size(width = (m.unit * 1.2f).dp, height = 1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, f.accent(Color(0xFF9DB8E8)), Color.Transparent))),
        )
        InfoLines(f, m, Color(0xFFCFDCF2))
        // Pushes the readout up so it sits above the peaks.
        Spacer(Modifier.height((m.height * (if (land) 0.3f else 0.14f)).dp))
    }
}

// ============================================================================================
// 4. Forest: pine silhouettes, fireflies, moon glow
// ============================================================================================

private fun DrawScope.natureForestBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val land = w > h
    val lh = if (land) 0.72f else 1f
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF040E0B), Color(0xFF0A2219), Color(0xFF123B2C)),
            startY = 0f,
            endY = h * 0.85f,
        ),
    )
    natureStars(81, 40, 0f, h * 0.4f, md * 0.0028f, Color(0xFFE6FFF2))
    natureMoonDisc(if (land) Offset(w * 0.82f, h * 0.2f) else Offset(w * 0.8f, h * 0.15f), md * 0.06f, Color(0xFFBFE8D0), Color(0xFFE6F5E8))

    // Back, middle and front pine rows, each in a darker green.
    val back = Color(0xFF0E3326)
    val mid = Color(0xFF0A2619)
    val front = Color(0xFF05140D)
    naturePines(h * 0.80f, h * 0.26f * lh, 10, 91, back)
    drawRect(back, topLeft = Offset(0f, h * 0.80f), size = Size(w, h * 0.2f))
    naturePines(h * 0.89f, h * 0.30f * lh, 7, 92, mid)
    drawRect(mid, topLeft = Offset(0f, h * 0.89f), size = Size(w, h * 0.11f))
    naturePines(h * 1.0f, h * 0.36f * lh, 5, 93, front)

    // Fireflies.
    scatter(21, 24).forEach { (x, y, s) ->
        val c = Offset(x * w, h * (0.38f + 0.55f * y))
        glowCircle(c, md * (0.018f + 0.022f * s), Color(0xFFD8F36A).copy(alpha = 0.55f))
        drawCircle(Color(0xFFF4FFB0), radius = md * 0.0028f, center = c)
    }
}

@Composable
private fun ThemeForest(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF040E0B), backdrop = { natureForestBackdrop() }) { m ->
        val land = m.width > m.height
        val size = natureSp(m, 0.88f).sp
        val mint = Color(0xFFC8F7DE)
        val lime = Color(0xFFE4F59A)
        val glow = Shadow(Color(0xFF3DDC97).copy(alpha = 0.35f), Offset.Zero, m.unit * 0.12f)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.hourText,
                color = f.ink(mint),
                fontSize = size,
                fontWeight = f.weight(FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(shadow = glow),
            )
            Text(
                ":",
                color = f.ink(mint).copy(alpha = 0.6f),
                fontSize = size,
                fontWeight = f.weight(FontWeight.Light),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                f.minuteText,
                color = f.accent(lime),
                fontSize = size,
                fontWeight = f.weight(FontWeight.Light),
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(shadow = glow),
            )
            AmPm(f, m, mint.copy(alpha = 0.8f), 0.16f)
        }
        InfoLines(f, m, Color(0xFFB4E8CE))
        Spacer(Modifier.height((m.height * (if (land) 0.26f else 0.1f)).dp))
    }
}

// ============================================================================================
// 5. Sunset: purple-to-gold sky, half sun on the horizon, reflection stripes, dark foreground
// ============================================================================================

private fun DrawScope.natureSunsetBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val land = w > h
    val horizon = h * (if (land) 0.74f else 0.68f)
    val cx = w / 2f
    val r = md * (if (land) 0.14f else 0.17f)

    drawRect(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E0B3F), Color(0xFF6A1B7A), Color(0xFFE8504A), Color(0xFFFF9A3C), Color(0xFFFFD36B),
            ),
            startY = 0f,
            endY = horizon,
        ),
        topLeft = Offset.Zero,
        size = Size(w, horizon),
    )
    natureStars(41, 24, 0f, horizon * 0.35f, md * 0.0024f, Color.White)
    glowCircle(Offset(cx, horizon), r * 3.4f, Color(0xFFFFB347).copy(alpha = 0.5f))
    clipRect(bottom = horizon) {
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF1B0), Color(0xFFFFC145), Color(0xFFFF8A3D)),
                startY = horizon - r,
                endY = horizon,
            ),
            radius = r,
            center = Offset(cx, horizon),
        )
    }

    // Water below the horizon.
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFFB5473F), Color(0xFF4A1650), Color(0xFF1A0A2E)),
            startY = horizon,
            endY = h,
        ),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    // Reflection stripes: shorter and thicker towards the viewer.
    for (i in 0 until 8) {
        val t = i / 7f
        val y = horizon + (h - horizon) * (0.02f + 0.36f * t * t)
        val thick = md * (0.005f + 0.004f * i)
        val width = r * (2f - 0.8f * t)
        drawRect(
            Color(0xFFFFC46B).copy(alpha = 0.78f - 0.08f * i),
            topLeft = Offset(cx - width / 2f, y),
            size = Size(width, thick),
        )
    }
    drawLine(Color(0xFFFFD79A).copy(alpha = 0.7f), Offset(0f, horizon), Offset(w, horizon), strokeWidth = md * 0.003f)

    // Dark foreground: rolling shore with a few pines at the sides.
    val dark = Color(0xFF0C0516)
    naturePines(h * 0.95f, h * 0.12f, 3, 43, dark, from = 0.02f, to = 0.3f)
    naturePines(h * 0.95f, h * 0.10f, 2, 44, dark, from = 0.82f, to = 0.98f)
    drawPath(natureWave(w, h, h * 0.955f, h * 0.025f, 1.2f, 0.6f, true), dark)
}

@Composable
private fun ThemeSunset(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF1E0B3F), backdrop = { natureSunsetBackdrop() }) { m ->
        val land = m.width > m.height
        NatureTime(f, m, 0.95f, Color(0xFFFFF1DC), FontWeight.Light, glow = Color(0xFF3A0A4A).copy(alpha = 0.6f))
        InfoLines(f, m, Color(0xFFFFE3C2))
        Spacer(Modifier.height((m.height * (if (land) 0.26f else 0.08f)).dp))
    }
}

// ============================================================================================
// 6. Space Explorer: ringed planet, satellite, nebulae, HUD-style readout
// ============================================================================================

private fun DrawScope.natureSpaceBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val big = maxOf(w, h)
    val land = w > h
    drawRect(Color(0xFF000308))

    glowCircle(Offset(w * 0.2f, h * 0.3f), big * 0.5f, Color(0xFF6A2BD9).copy(alpha = 0.22f))
    glowCircle(Offset(w * 0.85f, h * 0.78f), big * 0.45f, Color(0xFF1E88E5).copy(alpha = 0.18f))
    glowCircle(Offset(w * 0.5f, h * 0.95f), big * 0.4f, Color(0xFFD946A8).copy(alpha = 0.12f))
    natureStars(51, 130, 0f, h, md * 0.0035f, Color(0xFFDDE8FF), glowEvery = 12)

    // Ringed planet: back half of ring, sphere, front half of ring.
    val pc = if (land) Offset(w * 0.82f, h * 0.5f) else Offset(w * 0.66f, h * 0.2f)
    val r = md * 0.15f
    val ringColor = Color(0xFFE8CFA6)
    rotate(degrees = -18f, pivot = pc) {
        for (k in 0..1) {
            val rx = r * (1.7f + 0.5f * k)
            val ry = rx * 0.28f
            drawArc(
                color = ringColor.copy(alpha = 0.8f - 0.3f * k),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pc.x - rx, pc.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = r * (0.18f - 0.07f * k)),
            )
        }
    }
    glowCircle(pc, r * 2.2f, Color(0xFFFFB070).copy(alpha = 0.12f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF6C58B), Color(0xFFB5683A), Color(0xFF4A2418), Color(0xFF1A0B08)),
            center = Offset(pc.x - r * 0.35f, pc.y - r * 0.35f),
            radius = r * 1.6f,
        ),
        radius = r,
        center = pc,
    )
    rotate(degrees = -18f, pivot = pc) {
        for (k in 0..1) {
            val rx = r * (1.7f + 0.5f * k)
            val ry = rx * 0.28f
            drawArc(
                color = ringColor.copy(alpha = 0.9f - 0.3f * k),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pc.x - rx, pc.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = r * (0.18f - 0.07f * k)),
            )
        }
    }

    // Small satellite with solar panels and antenna.
    val sp = if (land) Offset(w * 0.2f, h * 0.75f) else Offset(w * 0.22f, h * 0.72f)
    val s = md * 0.05f
    rotate(degrees = -20f, pivot = sp) {
        val panel = Color(0xFF2D5BA8)
        val grid = Color(0xFF8FB4F0).copy(alpha = 0.6f)
        drawRect(panel, topLeft = Offset(sp.x - s * 2.1f, sp.y - s * 0.25f), size = Size(s * 1.5f, s * 0.5f))
        drawRect(panel, topLeft = Offset(sp.x + s * 0.6f, sp.y - s * 0.25f), size = Size(s * 1.5f, s * 0.5f))
        for (j in 1..3) {
            val lx = sp.x - s * 2.1f + s * 0.375f * j
            val rxl = sp.x + s * 0.6f + s * 0.375f * j
            drawLine(grid, Offset(lx, sp.y - s * 0.25f), Offset(lx, sp.y + s * 0.25f), strokeWidth = s * 0.03f)
            drawLine(grid, Offset(rxl, sp.y - s * 0.25f), Offset(rxl, sp.y + s * 0.25f), strokeWidth = s * 0.03f)
        }
        drawLine(Color(0xFF9AA5B5), Offset(sp.x - s * 0.6f, sp.y), Offset(sp.x - s * 0.5f, sp.y), strokeWidth = s * 0.06f)
        drawLine(Color(0xFF9AA5B5), Offset(sp.x + s * 0.5f, sp.y), Offset(sp.x + s * 0.6f, sp.y), strokeWidth = s * 0.06f)
        drawRoundRect(
            Color(0xFFCBD5E1),
            topLeft = Offset(sp.x - s * 0.5f, sp.y - s * 0.35f),
            size = Size(s, s * 0.7f),
            cornerRadius = CornerRadius(s * 0.1f),
        )
        drawCircle(Color(0xFF1E293B), radius = s * 0.16f, center = sp)
        drawLine(Color(0xFFCBD5E1), Offset(sp.x, sp.y - s * 0.35f), Offset(sp.x, sp.y - s * 0.9f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
        glowCircle(Offset(sp.x, sp.y - s * 0.9f), s * 0.35f, Color(0xFFFF5A5A).copy(alpha = 0.7f))
        drawCircle(Color(0xFFFF8A8A), radius = s * 0.06f, center = Offset(sp.x, sp.y - s * 0.9f))
    }
}

@Composable
private fun ThemeSpaceExplorer(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF000308), backdrop = { natureSpaceBackdrop() }) { m ->
        val hudFont = if (f.font == FontFamily.Default) FontFamily.Monospace else f.font
        val blue = Color(0xFF8FD8FF)
        val line = f.accent(Color(0xFF29B6F6))
        val tsz = natureSp(m, 0.8f)
        val labelSp = infoFontSize(f, m, 0.1f).sp
        Text(
            "// ORBITAL CLOCK //",
            color = line.copy(alpha = 0.85f),
            fontSize = labelSp,
            fontFamily = hudFont,
            letterSpacing = 2.sp,
            maxLines = 1,
        )
        Box(
            Modifier
                .padding(vertical = (tsz * 0.1f).dp)
                .size(width = (tsz * 2.9f).dp, height = 1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, line, Color.Transparent))),
        )
        NatureTime(f, m, 0.8f, blue, FontWeight.Light, glow = Color(0xFF29B6F6).copy(alpha = 0.55f), family = hudFont)
        Box(
            Modifier
                .padding(vertical = (tsz * 0.1f).dp)
                .size(width = (tsz * 2.9f).dp, height = 1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, line, Color.Transparent))),
        )
        Row(horizontalArrangement = Arrangement.spacedBy((tsz * 0.3f).dp)) {
            listOf("NAV LOCK", "SYS OK", "RCS 100%").forEach {
                Text(it, color = line.copy(alpha = 0.7f), fontSize = labelSp, fontFamily = hudFont, letterSpacing = 1.sp, maxLines = 1)
            }
        }
        Box(Modifier.padding(top = (tsz * 0.1f).dp))
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = hudFont)),
        ) {
            InfoLines(f, m, blue)
        }
    }
}

// ============================================================================================
// 7. Ocean Waves: night sea with four sine-wave layers and a moon reflection
// ============================================================================================

private fun DrawScope.natureOceanBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val land = w > h
    val horizon = h * 0.46f
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF020B14), Color(0xFF06222E), Color(0xFF0B4350)),
            startY = 0f,
            endY = h,
        ),
    )
    natureStars(61, 50, 0f, horizon * 0.9f, md * 0.003f, Color.White)
    val moonX = w * 0.72f
    val moonC = Offset(moonX, h * (if (land) 0.22f else 0.2f))
    natureMoonDisc(moonC, md * 0.06f, Color(0xFFBFE9F2))

    val baseFr = listOf(0.50f, 0.60f, 0.71f, 0.83f)
    val ampFr = listOf(0.018f, 0.024f, 0.030f, 0.036f)
    val cycles = listOf(2.2f, 1.8f, 1.5f, 1.2f)
    val phases = listOf(0f, 1.3f, 2.6f, 3.9f)
    val colors = listOf(Color(0xFF0B4A58), Color(0xFF08394A), Color(0xFF062B3A), Color(0xFF031A26))
    for (i in 0 until 4) {
        drawPath(natureWave(w, h, h * baseFr[i], h * ampFr[i], cycles[i], phases[i], true), colors[i])
        drawPath(
            natureWave(w, h, h * baseFr[i], h * ampFr[i], cycles[i], phases[i], false),
            Color(0xFF7FE0EE).copy(alpha = 0.30f),
            style = Stroke(width = md * 0.003f, cap = StrokeCap.Round),
        )
        if (i == 1) {
            // Moon reflection glimmer over the first two layers.
            for (j in 0 until 6) {
                val y = h * (0.485f + 0.03f * j)
                val bw = md * (0.14f - 0.012f * j) * (1f + 0.15f * (j % 2))
                drawRect(
                    Color(0xFFD6F6FF).copy(alpha = 0.55f - 0.07f * j),
                    topLeft = Offset(moonX - bw / 2f, y),
                    size = Size(bw, md * (0.004f + 0.0015f * j)),
                )
            }
        }
    }
}

@Composable
private fun ThemeOceanWaves(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF020B14), backdrop = { natureOceanBackdrop() }) { m ->
        InfoLines(f, m, Color(0xFFCFF3FA))
        NatureTime(f, m, 0.92f, Color(0xFFE6FBFF), FontWeight.Light, glow = Color(0xFF5EEAD4).copy(alpha = 0.4f))
    }
}

// ============================================================================================
// 8. Aurora Sky: flowing ribbons over a snowy ridge
// ============================================================================================

private fun DrawScope.natureAuroraBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF01030A), Color(0xFF04101A), Color(0xFF07202A)),
            startY = 0f,
            endY = h,
        ),
    )
    natureStars(101, 90, 0f, h * 0.7f, md * 0.0032f, Color.White, glowEvery = 14)

    // Ribbons: colour, base y, band height, cycles, phase, strength.
    class Ribbon(val c: Color, val base: Float, val band: Float, val cycles: Float, val phase: Float, val a: Float)
    val ribbons = listOf(
        Ribbon(Color(0xFF9B5CFF), 0.22f, 0.22f, 1.1f, 3.7f, 0.8f),
        Ribbon(Color(0xFF2BFF9A), 0.28f, 0.28f, 1.3f, 0.5f, 1.0f),
        Ribbon(Color(0xFF22D3EE), 0.34f, 0.24f, 1.9f, 2.0f, 0.8f),
        Ribbon(Color(0xFF5CFFB8), 0.40f, 0.20f, 2.4f, 1.2f, 0.6f),
    )
    val steps = 64
    val ampY = h * 0.07f
    for (rb in ribbons) {
        val tops = FloatArray(steps + 1)
        val bots = FloatArray(steps + 1)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val a = rb.phase + rb.cycles * NATURE_TAU * t
            tops[i] = h * rb.base + ampY * sin(a)
            bots[i] = tops[i] + h * rb.band * (0.7f + 0.3f * sin(a * 2.3f + 0.8f))
        }
        val path = Path()
        for (i in 0..steps) {
            if (i == 0) path.moveTo(0f, tops[i]) else path.lineTo(w * i / steps, tops[i])
        }
        for (i in steps downTo 0) path.lineTo(w * i / steps, bots[i])
        path.close()
        val startY = h * rb.base - ampY
        val endY = h * rb.base + ampY + h * rb.band
        drawPath(
            path,
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    rb.c.copy(alpha = 0.35f * rb.a),
                    rb.c.copy(alpha = 0.50f * rb.a),
                    Color.Transparent,
                ),
                startY = startY,
                endY = endY,
            ),
        )
        // Curtain rays.
        for (j in 0..40) {
            val i = j * steps / 40
            val x = w * i / steps
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, rb.c.copy(alpha = 0.22f * rb.a)),
                    startY = tops[i],
                    endY = bots[i],
                ),
                start = Offset(x, tops[i]),
                end = Offset(x, bots[i]),
                strokeWidth = md * 0.004f,
            )
        }
    }

    // Distant dark ridge, then the snowy near ridge.
    drawPath(natureRidge(w, h, h * 0.93f, h * 0.12f, 2.1f, 4.6f, 0.9f), Color(0xFF0C1827))
    val ridgeTop = h * 0.85f
    drawPath(
        natureRidge(w, h, h * 0.96f, h * 0.11f, 1.6f, 3.1f, 1f),
        Brush.verticalGradient(
            colors = listOf(Color(0xFFDDE9F4), Color(0xFF8FA6BF), Color(0xFF0A111B)),
            startY = ridgeTop,
            endY = h,
        ),
    )
}

@Composable
private fun ThemeAuroraSky(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = Color(0xFF01030A), backdrop = { natureAuroraBackdrop() }) { m ->
        val size = natureSp(m, 0.7f).sp
        val shadow = Shadow(Color(0xFF042A22).copy(alpha = 0.8f), Offset.Zero, m.unit * 0.1f)
        Text(
            f.hourText,
            color = f.ink(Color(0xFFF2FFF8)),
            fontSize = size,
            fontWeight = f.weight(FontWeight.Light),
            maxLines = 1,
            softWrap = false,
            style = LocalTextStyle.current.copy(shadow = shadow),
        )
        Text(
            f.minuteText,
            color = f.accent(Color(0xFF7CFFD0)),
            fontSize = size,
            fontWeight = f.weight(FontWeight.Light),
            maxLines = 1,
            softWrap = false,
            style = LocalTextStyle.current.copy(shadow = shadow),
        )
        AmPm(f, m, Color(0xFFF2FFF8).copy(alpha = 0.8f), 0.12f)
        InfoLines(f, m, Color(0xFFD5FFF0))
    }
}

// ============================================================================================

internal val natureThemes: Map<String, ThemeContent> = mapOf(
    "moon_phase" to { f, m -> ThemeMoonPhase(f, m) },
    "starry_night" to { f, m -> ThemeStarryNight(f, m) },
    "mountain_night" to { f, m -> ThemeMountainNight(f, m) },
    "forest" to { f, m -> ThemeForest(f, m) },
    "sunset" to { f, m -> ThemeSunset(f, m) },
    "space_explorer" to { f, m -> ThemeSpaceExplorer(f, m) },
    "ocean_waves" to { f, m -> ThemeOceanWaves(f, m) },
    "aurora_sky" to { f, m -> ThemeAuroraSky(f, m) },
)
