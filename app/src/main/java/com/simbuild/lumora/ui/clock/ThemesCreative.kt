package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ---- shared helpers ---------------------------------------------------------------------

private fun creativeUnit(v: Offset): Offset {
    val d = v.getDistance()
    return if (d < 0.001f) Offset(1f, 0f) else Offset(v.x / d, v.y / d)
}

private fun creativePoly(points: List<Offset>): Path = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

/** Four-pointed sparkle star centred on [c] with outer radius [r]. */
private fun creativeStar(c: Offset, r: Float): Path {
    val pts = List(8) { i ->
        val a = (i * 45.0 - 90.0) * PI / 180.0
        val rad = if (i % 2 == 0) r else r * 0.26f
        Offset(c.x + cos(a).toFloat() * rad, c.y + sin(a).toFloat() * rad)
    }
    return creativePoly(pts)
}

// ============================================================================================
// 1. Colorful soft-gradient mesh with a glass card
// ============================================================================================

private val MeshBase = Color(0xFF0B0B1F)

private fun DrawScope.creativeMeshBackdrop() {
    val w = size.width
    val h = size.height
    val big = maxOf(w, h)
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color(0xFF140A33), Color(0xFF0A1030)),
            start = Offset.Zero,
            end = Offset(w, h),
        ),
    )
    glowCircle(Offset(w * 0.12f, h * 0.10f), big * 0.58f, Color(0xFFFF4D9D).copy(alpha = 0.85f))
    glowCircle(Offset(w * 0.92f, h * 0.18f), big * 0.52f, Color(0xFFFF9A3C).copy(alpha = 0.75f))
    glowCircle(Offset(w * 0.18f, h * 0.88f), big * 0.56f, Color(0xFF3B82FF).copy(alpha = 0.80f))
    glowCircle(Offset(w * 0.88f, h * 0.82f), big * 0.52f, Color(0xFF9B5CFF).copy(alpha = 0.80f))
    glowCircle(Offset(w * 0.50f, h * 0.52f), big * 0.36f, Color(0xFFFF6B8A).copy(alpha = 0.35f))
    glowCircle(Offset(w * 0.62f, h * 0.30f), big * 0.30f, Color(0xFF4FC3FF).copy(alpha = 0.30f))
    glowCircle(Offset(w * 0.34f, h * 0.70f), big * 0.30f, Color(0xFFFFB36B).copy(alpha = 0.28f))
}

@Composable
private fun ThemeColorfulIos(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = MeshBase, backdrop = { creativeMeshBackdrop() }) { m ->
        val landscape = m.width > m.height
        val k = if (landscape) 0.56f else 0.72f
        val shape = RoundedCornerShape((m.unit * 0.26f).dp)
        val textColor = f.ink(Color.White)
        val infoSp = infoFontSize(f, m, 0.15f).sp
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.16f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.40f), shape)
                .padding(horizontal = (m.unit * 0.22f).dp, vertical = (m.unit * 0.14f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (f.showDate) {
                Text(
                    "${f.weekday} · ${f.date}",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = infoSp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f.time.digits,
                    color = textColor,
                    fontSize = (m.unit * k).sp,
                    fontWeight = f.weight(FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false,
                )
                AmPm(f, m, Color.White.copy(alpha = 0.8f), 0.16f)
            }
            if (f.showBattery) {
                Text(
                    batteryText(f.battery),
                    color = textColor.copy(alpha = 0.80f),
                    fontSize = infoSp,
                    maxLines = 1,
                )
            }
        }
    }
}

// ============================================================================================
// 2. Anime-inspired poster: dusk sky, big sun, skyline, petals, sparkles
// ============================================================================================

private val AnimeBase = Color(0xFF2A1B6B)
private val AnimeInk = Color(0xFF2A1560)

private fun DrawScope.creativeSkyline(
    seed: Int,
    count: Int,
    baseY: Float,
    maxH: Float,
    color: Color,
    lit: Boolean,
) {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val step = w / count
    val cells = scatter(seed, count)
    cells.forEachIndexed { i, (a, b, c) ->
        val bw = step * (0.9f + 0.8f * a)
        val bh = maxH * (0.35f + 0.65f * b)
        val x = i * step
        val top = baseY - bh
        drawRect(color, topLeft = Offset(x, top), size = Size(bw, h - top))
        if (c > 0.78f) {
            drawRect(color, topLeft = Offset(x + bw * 0.45f, top - md * 0.03f), size = Size(md * 0.006f, md * 0.03f))
        }
        if (lit) {
            val win = md * 0.012f
            val cols = maxOf(1, (bw / (win * 2.6f)).toInt())
            val rows = maxOf(1, (bh / (win * 3f)).toInt())
            for (r in 0 until rows) {
                for (cc in 0 until cols) {
                    if ((i * 7 + r * 3 + cc * 5) % 4 == 0) {
                        drawRect(
                            Color(0xFFFFE9A8).copy(alpha = 0.9f),
                            topLeft = Offset(x + win * (0.9f + 2.6f * cc), top + win * (1.2f + 3f * r)),
                            size = Size(win, win * 1.4f),
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.creativeAnimeBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val horizon = h * 0.74f

    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1B6B), Color(0xFF5B3A9E), Color(0xFFFF77A9), Color(0xFFFFB38A)),
            startY = 0f,
            endY = horizon,
        ),
        topLeft = Offset.Zero,
        size = Size(w, horizon),
    )

    scatter(11, 30).forEach { (x, y, s) ->
        drawCircle(
            Color.White.copy(alpha = 0.3f + 0.5f * s),
            radius = md * 0.003f * (1f + 1.6f * s),
            center = Offset(x * w, y * horizon * 0.5f),
        )
    }

    // Big pastel sun / moon disc.
    val sun = Offset(w * 0.70f, horizon * 0.50f)
    val r = md * 0.24f
    glowCircle(sun, r * 2.1f, Color(0xFFFF9CC8).copy(alpha = 0.55f))
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF3C2), Color(0xFFFFB6D5)),
            startY = sun.y - r,
            endY = sun.y + r,
        ),
        radius = r,
        center = sun,
    )

    // Soft cloud pills.
    val pill = md * 0.022f
    listOf(
        Triple(0.06f, 0.50f, 0.34f),
        Triple(0.52f, 0.58f, 0.30f),
        Triple(0.20f, 0.64f, 0.26f),
    ).forEach { (cx, cy, cw) ->
        drawRoundRect(
            Color.White.copy(alpha = 0.30f),
            topLeft = Offset(w * cx, horizon * cy),
            size = Size(w * cw, pill),
            cornerRadius = CornerRadius(pill / 2f, pill / 2f),
        )
    }

    // Speed lines.
    scatter(51, 7).forEach { (x, y, s) ->
        val start = Offset(x * w, y * horizon * 0.9f)
        val len = md * 0.35f * (0.5f + s)
        drawLine(
            Color.White.copy(alpha = 0.30f),
            start = start,
            end = Offset(start.x - len * 0.94f, start.y + len * 0.34f),
            strokeWidth = md * 0.003f,
            cap = StrokeCap.Round,
        )
    }

    // Layered city skyline.
    creativeSkyline(21, 9, horizon, h * 0.22f, Color(0xFF4A2F8C).copy(alpha = 0.92f), false)
    creativeSkyline(22, 12, horizon + h * 0.03f, h * 0.20f, Color(0xFF2B1D66), false)
    creativeSkyline(23, 15, horizon + h * 0.06f, h * 0.17f, Color(0xFF160F3F), true)

    // Sakura-like petals.
    scatter(31, 26).forEachIndexed { i, (x, y, s) ->
        val c = Offset(x * w, y * h)
        val pw = md * 0.024f * (0.6f + s)
        rotate(degrees = s * 180f, pivot = c) {
            drawOval(
                color = (if (i % 3 == 0) Color(0xFFFFE3EE) else Color(0xFFFFB3D1)).copy(alpha = 0.75f),
                topLeft = Offset(c.x - pw / 2f, c.y - pw * 0.28f),
                size = Size(pw, pw * 0.56f),
            )
        }
    }

    // Sparkles.
    scatter(41, 8).forEach { (x, y, s) ->
        val c = Offset(x * w, y * horizon)
        drawPath(creativeStar(c, md * 0.032f * (0.5f + s)), Color.White.copy(alpha = 0.9f))
    }
}

private val CreativeOutlineDirs = listOf(
    -1f to -1f, 0f to -1f, 1f to -1f,
    -1f to 0f, 1f to 0f,
    -1f to 1f, 0f to 1f, 1f to 1f,
)

/** Time text with a thin outline made by drawing the same text offset in eight directions. */
@Composable
private fun CreativeOutlinedText(
    text: String,
    sizeSp: Float,
    color: Color,
    outline: Color,
    weight: FontWeight,
    d: Float,
) {
    Box {
        CreativeOutlineDirs.forEach { (dx, dy) ->
            Text(
                text,
                color = outline,
                fontSize = sizeSp.sp,
                fontWeight = weight,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = (dx * d).dp, y = (dy * d).dp),
            )
        }
        Text(
            text,
            color = color,
            fontSize = sizeSp.sp,
            fontWeight = weight,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ThemeAnimeInspired(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = AnimeBase, backdrop = { creativeAnimeBackdrop() }) { m ->
        val tinted = f.tint != null
        val main = f.ink(AnimeInk)
        val outline = if (tinted) AnimeInk else Color.White
        Row(verticalAlignment = Alignment.Bottom) {
            CreativeOutlinedText(
                text = f.time.digits,
                sizeSp = m.unit * 0.78f,
                color = main,
                outline = outline,
                weight = f.weight(FontWeight.Black),
                d = (m.unit * 0.012f).coerceAtLeast(1f),
            )
            AmPm(f, m, Color.White, 0.16f)
        }
        if (f.showDate || f.showBattery) {
            Column(
                modifier = Modifier
                    .padding(top = (m.unit * 0.08f).dp)
                    .background(Color(0xFF1E1B6B).copy(alpha = 0.50f), RoundedCornerShape(50))
                    .padding(horizontal = (m.unit * 0.14f).dp, vertical = (m.unit * 0.04f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InfoLines(f, m, Color.White)
            }
        }
    }
}

// ============================================================================================
// 3. Dragon: tapered serpentine body, scale arcs, horns, ember particles
// ============================================================================================

private val DragonBase = Color(0xFF0C0204)
private val DragonGold = Color(0xFFE8B84A)
private val DragonCrimson = Color(0xFF9B1220)

private fun creativeBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val a = u * u * u
    val b = 3f * u * u * t
    val c = 3f * u * t * t
    val d = t * t * t
    return Offset(
        a * p0.x + b * p1.x + c * p2.x + d * p3.x,
        a * p0.y + b * p1.y + c * p2.y + d * p3.y,
    )
}

/** Spine of the dragon: head top-right, along the top edge, down the left edge, along the bottom, curling tail. */
private fun creativeDragonSpine(w: Float, h: Float): List<Offset> {
    val segments = listOf(
        listOf(0.80f to 0.11f, 0.52f to 0.03f, 0.20f to 0.08f, 0.10f to 0.26f),
        listOf(0.10f to 0.26f, 0.00f to 0.42f, 0.01f to 0.62f, 0.10f to 0.76f),
        listOf(0.10f to 0.76f, 0.20f to 0.92f, 0.55f to 0.99f, 0.76f to 0.90f),
        listOf(0.76f to 0.90f, 0.90f to 0.92f, 0.97f to 0.80f, 0.89f to 0.78f),
    )
    val steps = 24
    val out = ArrayList<Offset>()
    segments.forEachIndexed { si, seg ->
        val p = seg.map { Offset(it.first * w, it.second * h) }
        val from = if (si == 0) 0 else 1
        for (i in from..steps) out.add(creativeBezier(p[0], p[1], p[2], p[3], i / steps.toFloat()))
    }
    return out
}

private fun DrawScope.creativeDragon() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val spine = creativeDragonSpine(w, h)
    val n = spine.size
    val maxW = md * 0.06f
    val widths = FloatArray(n) { i ->
        val t = i / (n - 1f)
        val prof = if (t < 0.12f) 0.75f + 0.25f * (t / 0.12f) else (1f - (t - 0.12f) / 0.88f).pow(1.2f)
        maxW * maxOf(prof, 0.03f)
    }
    val tangents = List(n) { i ->
        creativeUnit(spine[minOf(i + 1, n - 1)] - spine[maxOf(i - 1, 0)])
    }
    fun normal(i: Int) = Offset(-tangents[i].y, tangents[i].x)

    // Body: one closed polygon along both sides of the spine.
    val body = Path().apply {
        for (i in 0 until n) {
            val p = spine[i] + normal(i) * (widths[i] / 2f)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        for (i in n - 1 downTo 0) {
            val p = spine[i] - normal(i) * (widths[i] / 2f)
            lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(body, DragonCrimson)
    drawPath(body, DragonGold.copy(alpha = 0.9f), style = Stroke(width = md * 0.004f, join = StrokeJoin.Round))

    // Scale arcs across the body.
    var i = 4
    while (i < n - 4) {
        val p = spine[i]
        val nrm = normal(i)
        val half = widths[i] * 0.5f
        val a = p + nrm * half
        val b = p - nrm * half
        val ctrl = p + tangents[i] * (widths[i] * 0.4f)
        val arc = Path().apply {
            moveTo(a.x, a.y)
            quadraticBezierTo(ctrl.x, ctrl.y, b.x, b.y)
        }
        drawPath(arc, DragonGold.copy(alpha = 0.5f), style = Stroke(width = md * 0.0028f, cap = StrokeCap.Round))
        i += 3
    }

    // Dorsal spikes along one side.
    i = 6
    while (i < (n * 0.85f).toInt()) {
        val p = spine[i]
        val q = spine[i + 1]
        val b1 = p + normal(i) * (widths[i] / 2f)
        val b2 = q + normal(i + 1) * (widths[i + 1] / 2f)
        val tip = p + normal(i) * (widths[i] * 0.95f) + tangents[i] * (widths[i] * 0.35f)
        drawPath(creativePoly(listOf(b1, tip, b2)), DragonGold.copy(alpha = 0.9f))
        i += 5
    }

    // Head: pointed snout, horns, whiskers, fire-orange eye.
    val head = spine[0]
    val d = creativeUnit(spine[0] - spine[4])
    val nn = Offset(-d.y, d.x)
    val len = md * 0.12f
    val hw = md * 0.045f
    val headPath = creativePoly(
        listOf(
            head + nn * hw,
            head + d * (len * 0.55f) + nn * (hw * 0.8f),
            head + d * len + nn * (hw * 0.15f),
            head + d * (len * 0.95f) - nn * (hw * 0.35f),
            head + d * (len * 0.45f) - nn * (hw * 0.8f),
            head - nn * hw,
        ),
    )
    drawPath(headPath, DragonCrimson)
    drawPath(headPath, DragonGold, style = Stroke(width = md * 0.004f, join = StrokeJoin.Round))

    for (side in listOf(1f, -1f)) {
        val hornBase1 = head + d * (len * 0.10f) + nn * (hw * 0.9f * side)
        val hornBase2 = head - d * (len * 0.15f) + nn * (hw * 0.9f * side)
        val hornTip = head - d * (len * 0.60f) + nn * (hw * 2.1f * side)
        drawPath(creativePoly(listOf(hornBase1, hornTip, hornBase2)), DragonGold)

        val ws = head + d * (len * 0.9f) + nn * (hw * 0.1f * side)
        val wc = ws + nn * (hw * 1.6f * side) + d * (len * 0.2f)
        val we = ws + d * (len * 0.45f) + nn * (hw * 2.5f * side)
        val whisker = Path().apply {
            moveTo(ws.x, ws.y)
            quadraticBezierTo(wc.x, wc.y, we.x, we.y)
        }
        drawPath(whisker, DragonGold.copy(alpha = 0.85f), style = Stroke(width = md * 0.003f, cap = StrokeCap.Round))
    }

    val eye = head + d * (len * 0.30f) + nn * (hw * 0.35f)
    glowCircle(eye, md * 0.045f, Color(0xFFFF7A1A).copy(alpha = 0.9f))
    drawCircle(Color(0xFFFF8A1F), radius = md * 0.013f, center = eye)
    drawCircle(Color(0xFF2A0405), radius = md * 0.005f, center = eye)
}

private fun DrawScope.creativeDragonBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val big = maxOf(w, h)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFF3A0A10), DragonBase),
            center = Offset(w / 2f, h / 2f),
            radius = big * 0.7f,
        ),
    )
    drawCircle(Color(0xFF7A0C14).copy(alpha = 0.22f), radius = md * 0.32f, center = Offset(w / 2f, h / 2f))

    // Ember particles.
    scatter(61, 40).forEachIndexed { i, (x, y, s) ->
        val c = Offset(x * w, y * h)
        val col = if (i % 2 == 0) Color(0xFFFF7A1A) else Color(0xFFFFC857)
        if (s > 0.7f) glowCircle(c, md * 0.03f, col.copy(alpha = 0.5f))
        drawCircle(col.copy(alpha = 0.3f + 0.6f * s), radius = md * 0.004f * (1f + 2f * s), center = c)
    }

    creativeDragon()
}

@Composable
private fun ThemeDragon(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = DragonBase, backdrop = { creativeDragonBackdrop() }) { m ->
        val gold = f.ink(Color(0xFFFFC857))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = gold,
                fontSize = (m.unit * 0.78f).sp,
                fontWeight = f.weight(FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color(0xFFFF6A1A).copy(alpha = 0.6f), Offset.Zero, m.unit * 0.18f),
                ),
            )
            AmPm(f, m, Color(0xFFFFC857), 0.16f)
        }
        InfoLines(f, m, Color(0xFFE8C98A))
    }
}

// ============================================================================================
// 4. Wolf: howling silhouette on a rock ridge, big moon, pines and stars
// ============================================================================================

private val WolfBase = Color(0xFF050A18)
private val WolfSilhouette = Color(0xFF04070D)

/** Wolf outline in local units: x forward, y up, wolf about 1.0 tall, origin at the base of the paws. */
private val WolfOutline = listOf(
    -0.30f to 0.00f, -0.38f to 0.08f, -0.56f to 0.04f, -0.54f to 0.14f, -0.36f to 0.22f,
    -0.22f to 0.38f, -0.04f to 0.52f, 0.02f to 0.64f, 0.05f to 0.74f,
    0.02f to 0.93f, 0.10f to 0.85f, 0.12f to 0.97f, 0.18f to 0.87f,
    0.35f to 1.02f, 0.40f to 0.99f, 0.30f to 0.90f, 0.24f to 0.81f,
    0.26f to 0.64f, 0.22f to 0.40f, 0.20f to 0.20f, 0.19f to 0.00f,
    0.04f to 0.00f, 0.05f to 0.20f, 0.02f to 0.18f, -0.10f to 0.08f, -0.10f to 0.00f,
)

private fun DrawScope.creativePine(x: Float, baseY: Float, hgt: Float, color: Color) {
    drawRect(color, topLeft = Offset(x - hgt * 0.03f, baseY - hgt * 0.1f), size = Size(hgt * 0.06f, hgt * 0.12f))
    for (t in 0 until 3) {
        val top = baseY - hgt + t * hgt * 0.28f
        val hw = hgt * (0.16f + 0.10f * t)
        val bottom = top + hgt * 0.42f
        drawPath(creativePoly(listOf(Offset(x, top), Offset(x + hw, bottom), Offset(x - hw, bottom))), color)
    }
}

private fun DrawScope.creativeWolfBackdrop() {
    val w = size.width
    val h = size.height
    val md = minOf(w, h)
    val portrait = h >= w

    // Layout: wolf on the right facing the moon in portrait, on the left in landscape.
    val moon = if (portrait) Offset(w * 0.36f, h * 0.22f) else Offset(w * 0.86f, h * 0.34f)
    val moonR = if (portrait) md * 0.30f else h * 0.22f
    val ax = if (portrait) w * 0.70f else w * 0.17f
    val ay = if (portrait) h * 0.88f else h * 0.92f
    val s = if (portrait) md * 0.50f else h * 0.54f
    val sx = if (portrait) -1f else 1f

    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF050A18), Color(0xFF0E1B38), Color(0xFF1B3358)),
            startY = 0f,
            endY = h,
        ),
    )

    scatter(71, 45).forEach { (x, y, sz) ->
        drawCircle(
            Color(0xFFDDE8FF).copy(alpha = 0.3f + 0.6f * sz),
            radius = md * 0.003f * (1f + 1.6f * sz),
            center = Offset(x * w, y * h * 0.7f),
        )
    }

    // Big pale moon with a few craters.
    glowCircle(moon, moonR * 2.2f, Color(0xFFBBD0FF).copy(alpha = 0.35f))
    drawCircle(Color(0xFFEAF1FF), radius = moonR, center = moon)
    listOf(
        Triple(-0.30f, -0.20f, 0.16f),
        Triple(0.25f, 0.10f, 0.22f),
        Triple(-0.10f, 0.40f, 0.12f),
        Triple(0.35f, -0.35f, 0.09f),
    ).forEach { (cx, cy, cr) ->
        drawCircle(
            Color(0xFFC9D8F2).copy(alpha = 0.6f),
            radius = moonR * cr,
            center = Offset(moon.x + moonR * cx, moon.y + moonR * cy),
        )
    }

    // Distant pine row.
    val distant = Color(0xFF0B1524)
    for (i in 0 until 9) {
        creativePine((i + 0.5f) / 9f * w, ay + s * 0.05f, s * 0.30f * (0.8f + 0.4f * ((i * 5) % 3) / 2f), distant)
    }

    // Mist above the ridge.
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFF2A4470).copy(alpha = 0.40f)),
            startY = ay - s * 0.3f,
            endY = ay + s * 0.1f,
        ),
        topLeft = Offset(0f, ay - s * 0.3f),
        size = Size(w, s * 0.4f),
    )

    // Rock ridge.
    val ridge = Path().apply {
        moveTo(0f, ay + s * 0.10f)
        lineTo(ax - s * 0.9f, ay + s * 0.06f)
        lineTo(ax - s * 0.50f, ay + s * 0.02f)
        lineTo(ax - s * 0.42f, ay)
        lineTo(ax + s * 0.45f, ay)
        lineTo(ax + s * 0.52f, ay + s * 0.05f)
        lineTo(ax + s * 1.0f, ay + s * 0.12f)
        lineTo(w, ay + s * 0.18f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(ridge, WolfSilhouette)

    // Wolf silhouette.
    val pts = WolfOutline.map { (x, y) -> Offset(ax + x * s * sx, ay + s * 0.01f - y * s) }
    drawPath(creativePoly(pts), WolfSilhouette)
    drawCircle(
        Color(0xFFFFE08A).copy(alpha = 0.9f),
        radius = s * 0.007f,
        center = Offset(ax + 0.2f * s * sx, ay - 0.88f * s),
    )

    // Foreground pines.
    val xs = if (portrait) listOf(0.06f, 0.17f, 0.96f) else listOf(0.93f, 0.985f, 0.60f)
    xs.forEachIndexed { i, fx ->
        creativePine(w * fx, ay + s * 0.10f, s * (0.62f - 0.10f * (i % 2)), WolfSilhouette)
    }
}

@Composable
private fun ThemeWolf(f: ClockFrame, modifier: Modifier) {
    Stage(f, modifier, background = WolfBase, backdrop = { creativeWolfBackdrop() }) { m ->
        val silver = f.ink(Color(0xFFC7D8F5))
        val k = if (m.width > m.height) 0.62f else 0.74f
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = silver,
                fontSize = (m.unit * k).sp,
                fontWeight = f.weight(FontWeight.Light),
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color(0xFF6C8CD8).copy(alpha = 0.7f), Offset.Zero, m.unit * 0.16f),
                ),
            )
            AmPm(f, m, Color(0xFFC7D8F5), 0.16f)
        }
        InfoLines(f, m, Color(0xFFAFC4E8))
    }
}

// ============================================================================================

internal val creativeThemes: Map<String, ThemeContent> = mapOf(
    "colorful_ios_inspired_clock" to { f, m -> ThemeColorfulIos(f, m) },
    "anime_inspired_clock" to { f, m -> ThemeAnimeInspired(f, m) },
    "dragon" to { f, m -> ThemeDragon(f, m) },
    "wolf" to { f, m -> ThemeWolf(f, m) },
)
