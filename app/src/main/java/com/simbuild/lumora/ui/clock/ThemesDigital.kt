package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- 1. Split Clock ----------------------------------------------------------------------

@Composable
private fun DigitalSplitClock(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF05070C),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF04050A))))
    },
) { m ->
    val hourInk = f.ink(Color(0xFFF4F7FF))
    val minuteInk = f.accent(Color(0xFFFF7A59))
    val glow = f.accent(Color(0xFF5EE7FF))

    // Tile height is bounded by the unit, the screen height and the screen width.
    val th = minOf(m.unit * 1.6f, m.height * 0.6f, m.width * 0.42f * 1.3f)
    val tw = th / 1.3f
    val fs = th * 0.55f
    val corner = th * 0.10f
    val lineW = (th * 0.012f).coerceAtLeast(2f)
    val glowW = lineW * 8f
    val leftShape = RoundedCornerShape(topStart = corner.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = corner.dp)
    val rightShape = RoundedCornerShape(topStart = 0.dp, topEnd = corner.dp, bottomEnd = corner.dp, bottomStart = 0.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = tw.dp, height = th.dp)
                .clip(leftShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2A4A), Color(0xFF0D1426)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                f.hourText,
                color = hourInk,
                fontSize = fs.sp,
                fontWeight = f.weight(FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
        }
        Canvas(Modifier.size(width = glowW.dp, height = th.dp)) {
            val layers = listOf(glowW to 0.10f, glowW * 0.6f to 0.20f, glowW * 0.3f to 0.40f, lineW to 1f)
            for ((w, a) in layers) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, glow.copy(alpha = a), glow.copy(alpha = a), Color.Transparent),
                    ),
                    topLeft = Offset((this.size.width - w) / 2f, 0f),
                    size = Size(w, this.size.height),
                )
            }
        }
        Box(
            Modifier
                .size(width = tw.dp, height = th.dp)
                .clip(rightShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF3A1A16), Color(0xFF1A0C0A)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                f.minuteText,
                color = minuteInk,
                fontSize = fs.sp,
                fontWeight = f.weight(FontWeight.Light),
                maxLines = 1,
                softWrap = false,
            )
            f.time.amPm?.let {
                Text(
                    it,
                    color = minuteInk.copy(alpha = 0.8f),
                    fontSize = (fs * 0.2f).sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = (th * 0.06f).dp, bottom = (th * 0.05f).dp),
                )
            }
        }
    }
    Spacer(Modifier.height((th * 0.08f).dp))
    InfoLines(f, m, hourInk)
}

// ---- 2. Retro Digital (7-segment LCD) ----------------------------------------------------

/** Which segments (a..g) are lit for each digit 0-9. */
private val DigitalSegments = arrayOf(
    "abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg",
)

/** Hexagonal segment starting at ([x0], [y0]) and running [len] along its axis, [t] thick. */
private fun digitalSegPath(horizontal: Boolean, x0: Float, y0: Float, len: Float, t: Float): Path = Path().apply {
    val h = t / 2f
    if (horizontal) {
        moveTo(x0, y0)
        lineTo(x0 + h, y0 - h)
        lineTo(x0 + len - h, y0 - h)
        lineTo(x0 + len, y0)
        lineTo(x0 + len - h, y0 + h)
        lineTo(x0 + h, y0 + h)
    } else {
        moveTo(x0, y0)
        lineTo(x0 + h, y0 + h)
        lineTo(x0 + h, y0 + len - h)
        lineTo(x0, y0 + len)
        lineTo(x0 - h, y0 + len - h)
        lineTo(x0 - h, y0 + h)
    }
    close()
}

/** One 7-segment digit in a [w] x [h] box at [origin]; unlit segments are drawn faintly in [off]. A null [digit] is blank. */
private fun DrawScope.segmentDigit(digit: Int?, origin: Offset, w: Float, h: Float, on: Color, off: Color) {
    val t = w * 0.17f
    val gap = t * 0.10f
    val left = origin.x + t / 2f
    val right = origin.x + w - t / 2f
    val top = origin.y + t / 2f
    val mid = origin.y + h / 2f
    val bot = origin.y + h - t / 2f
    val hLen = (right - left) - 2f * gap
    val vLen = (mid - top) - 2f * gap
    val segs = listOf(
        'a' to digitalSegPath(true, left + gap, top, hLen, t),
        'b' to digitalSegPath(false, right, top + gap, vLen, t),
        'c' to digitalSegPath(false, right, mid + gap, vLen, t),
        'd' to digitalSegPath(true, left + gap, bot, hLen, t),
        'e' to digitalSegPath(false, left, mid + gap, vLen, t),
        'f' to digitalSegPath(false, left, top + gap, vLen, t),
        'g' to digitalSegPath(true, left + gap, mid, hLen, t),
    )
    val lit = if (digit != null && digit in 0..9) DigitalSegments[digit] else ""
    for ((_, path) in segs) drawPath(path, off)
    for ((key, path) in segs) {
        if (key in lit) {
            drawPath(path, on.copy(alpha = 0.28f), style = Stroke(width = t * 0.9f))
            drawPath(path, on)
        }
    }
}

@Composable
private fun DigitalRetro(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF060604),
    backdrop = {
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF16130A), Color(0xFF050503)),
                center = center,
                radius = size.maxDimension * 0.7f,
            ),
        )
    },
) { m ->
    val on = f.accent(Color(0xFFFFB000))
    val off = on.copy(alpha = 0.09f)
    val dw = minOf(m.unit * 0.56f, m.width * 0.86f / 5.9f, m.height * 0.44f / 1.8f)
    val dh = dw * 1.8f
    val gap = dw * 0.22f
    val colonW = dw * 0.28f
    val totalW = dw * 4f + gap * 4f + colonW
    val pad = dw * 0.35f
    val shape = RoundedCornerShape((dw * 0.25f).dp)
    val digits = f.hourText.padStart(2, ' ').takeLast(2) + f.minuteText.padStart(2, '0').takeLast(2)

    Column(
        Modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF14110A), Color(0xFF080705))))
            .border((dw * 0.03f).coerceAtLeast(1f).dp, on.copy(alpha = 0.25f), shape)
            .padding(pad.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Canvas(Modifier.size(width = totalW.dp, height = dh.dp)) {
            val x0 = 0f
            val x1 = dw + gap
            val cx = 2f * dw + 2f * gap
            val x2 = cx + colonW + gap
            val x3 = x2 + dw + gap
            val xs = floatArrayOf(x0, x1, x2, x3)
            for (i in 0 until 4) {
                segmentDigit(digits[i].digitToIntOrNull(), Offset(xs[i], 0f), dw, dh, on, off)
            }
            for (yf in floatArrayOf(0.33f, 0.67f)) {
                val tl = Offset(cx, dh * yf - colonW / 2f)
                drawRect(on.copy(alpha = 0.25f), topLeft = Offset(tl.x - colonW * 0.25f, tl.y - colonW * 0.25f), size = Size(colonW * 1.5f, colonW * 1.5f))
                drawRect(on, topLeft = tl, size = Size(colonW, colonW))
            }
        }
        f.time.amPm?.let {
            Text(
                it,
                color = on.copy(alpha = 0.85f),
                fontSize = (dw * 0.34f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = (dw * 0.10f).dp),
            )
        }
    }
    Spacer(Modifier.height((dw * 0.25f).dp))
    InfoLines(f, m, on)
}

// ---- 3. Dot Matrix -----------------------------------------------------------------------

/** 5x7 bitmap font for the digits 0-9: seven rows of five columns, '#' = lit dot. */
private val DotFont: Array<Array<String>> = arrayOf(
    arrayOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
    arrayOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
    arrayOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
    arrayOf(".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."),
    arrayOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
    arrayOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
    arrayOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
    arrayOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    arrayOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    arrayOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
)

/** Draws one 5x7 glyph whose top-left cell is ([col0], [row0]) on a grid of pitch [p]. A null glyph is all unlit dots. */
private fun DrawScope.dotGlyph(glyph: Array<String>?, col0: Int, row0: Int, p: Float, on: Color, dim: Color) {
    for (r in 0 until 7) {
        val line = glyph?.get(r)
        for (c in 0 until 5) {
            val pt = Offset((col0 + c + 0.5f) * p, (row0 + r + 0.5f) * p)
            if (line != null && line[c] == '#') {
                drawCircle(on.copy(alpha = 0.18f), radius = p * 0.62f, center = pt)
                drawCircle(on, radius = p * 0.40f, center = pt)
            } else {
                drawCircle(dim, radius = p * 0.17f, center = pt)
            }
        }
    }
}

@Composable
private fun DigitalDotMatrix(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF04080C),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF07101A), Color(0xFF03060A))))
    },
) { m ->
    val on = f.accent(Color(0xFF4DFF9A))
    val dim = on.copy(alpha = 0.10f)
    val stacked = m.height > m.width
    val cols = if (stacked) 11 else 25
    val rows = if (stacked) 16 else 7
    val p = if (stacked) {
        minOf(m.unit * 0.24f, m.width * 0.88f / cols, m.height * 0.5f / rows)
    } else {
        minOf(m.unit * 0.18f, m.width * 0.90f / cols, m.height * 0.44f / rows)
    }
    val digits = f.hourText.padStart(2, ' ').takeLast(2) + f.minuteText.padStart(2, '0').takeLast(2)
    val glyphs = digits.map { ch -> ch.digitToIntOrNull()?.let { DotFont[it] } }

    Column(horizontalAlignment = Alignment.End) {
        Canvas(Modifier.size(width = (p * cols).dp, height = (p * rows).dp)) {
            if (stacked) {
                dotGlyph(glyphs[0], 0, 0, p, on, dim)
                dotGlyph(glyphs[1], 6, 0, p, on, dim)
                dotGlyph(glyphs[2], 0, 9, p, on, dim)
                dotGlyph(glyphs[3], 6, 9, p, on, dim)
            } else {
                dotGlyph(glyphs[0], 0, 0, p, on, dim)
                dotGlyph(glyphs[1], 6, 0, p, on, dim)
                dotGlyph(glyphs[2], 14, 0, p, on, dim)
                dotGlyph(glyphs[3], 20, 0, p, on, dim)
                for (r in intArrayOf(2, 4)) {
                    val pt = Offset(12.5f * p, (r + 0.5f) * p)
                    drawCircle(on.copy(alpha = 0.18f), radius = p * 0.62f, center = pt)
                    drawCircle(on, radius = p * 0.40f, center = pt)
                }
            }
        }
        f.time.amPm?.let {
            Text(
                it,
                color = on.copy(alpha = 0.85f),
                fontSize = (p * 1.1f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = (p * 0.4f).dp),
            )
        }
    }
    Spacer(Modifier.height((p * 1.0f).dp))
    InfoLines(f, m, on)
}

// ---- 4. Large Typography -----------------------------------------------------------------

/** One tightly-spaced line of poster type: a fixed-height box the oversized text is centred in. */
@Composable
private fun DigitalPosterLine(text: String, fs: Float, lineHeight: Float, color: Color, weight: FontWeight) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(lineHeight.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            color = color,
            fontSize = fs.sp,
            fontWeight = weight,
            letterSpacing = (-fs * 0.04f).sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
        )
    }
}

@Composable
private fun DigitalLargeTypography(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color.Black,
) { m ->
    val hourInk = f.ink(Color.White)
    val minuteInk = f.ink(Color(0xFFE6E0D4))
    val accent = f.accent(Color(0xFFFF3B30))
    val fs = minOf(m.unit * 2.3f, m.width * 0.92f / 1.3f, m.height * 0.38f)
    val colW = minOf(m.width * 0.94f, fs * 1.32f)
    val lineH = fs * 0.80f
    val weight = f.weight(FontWeight.Black)

    Column(Modifier.width(colW.dp), horizontalAlignment = Alignment.Start) {
        DigitalPosterLine(f.hourText, fs, lineH, hourInk, weight)
        DigitalPosterLine(f.minuteText, fs, lineH, minuteInk, weight)
        Spacer(Modifier.height((fs * 0.08f).dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width((colW * 0.22f).dp)
                    .height((fs * 0.045f).coerceAtLeast(2f).dp)
                    .background(accent),
            )
            f.time.amPm?.let {
                Text(
                    it,
                    color = hourInk,
                    fontSize = (fs * 0.12f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = (fs * 0.05f).dp),
                )
            }
        }
        Spacer(Modifier.height((fs * 0.04f).dp))
        InfoLines(f, m, hourInk)
    }
}

// ---- 5. Gradient Digits ------------------------------------------------------------------

@Composable
private fun DigitalGradient(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF07060F),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0716), Color(0xFF05040C))))
        val r = size.maxDimension
        glowCircle(Offset(size.width * 0.15f, size.height * 0.22f), r * 0.45f, Color(0xFFFF5FA2).copy(alpha = 0.32f))
        glowCircle(Offset(size.width * 0.88f, size.height * 0.45f), r * 0.50f, Color(0xFF7C4DFF).copy(alpha = 0.30f))
        glowCircle(Offset(size.width * 0.40f, size.height * 0.90f), r * 0.45f, Color(0xFF00B8D4).copy(alpha = 0.28f))
    },
) { m ->
    val tint = f.tint
    val budget = if (f.time.amPm != null) 3.25f else 2.8f
    val fs = minOf(m.unit * 1.15f, m.width * 0.92f / budget)
    val style = if (tint == null) {
        TextStyle(
            brush = Brush.linearGradient(listOf(Color(0xFFFF5FA2), Color(0xFFB07CFF), Color(0xFF3DDCFF))),
            fontSize = fs.sp,
            fontWeight = f.weight(FontWeight.ExtraBold),
            fontFamily = f.font,
            letterSpacing = (-fs * 0.02f).sp,
        )
    } else {
        TextStyle(
            color = tint,
            fontSize = fs.sp,
            fontWeight = f.weight(FontWeight.ExtraBold),
            fontFamily = f.font,
            letterSpacing = (-fs * 0.02f).sp,
        )
    }
    Row(verticalAlignment = Alignment.Bottom) {
        BasicText(f.time.digits, style = style, maxLines = 1, softWrap = false)
        AmPm(f, m, Color(0xFFB07CFF))
    }
    InfoLines(f, m, Color.White)
}

// ---- 6. Glitch Clock ---------------------------------------------------------------------

@Composable
private fun DigitalGlitchLayer(
    text: String,
    fs: Float,
    color: Color,
    weight: FontWeight,
    family: FontFamily,
    dx: Float,
    dy: Float,
) {
    Text(
        text,
        color = color,
        fontSize = fs.sp,
        fontWeight = weight,
        fontFamily = family,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.offset(x = dx.dp, y = dy.dp),
    )
}

@Composable
private fun DigitalGlitch(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF050507),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF08080C), Color(0xFF030304))))
        // Faint CRT scanlines.
        val step = size.height / 80f
        var y = 0f
        while (y < size.height) {
            drawRect(Color.White.copy(alpha = 0.025f), topLeft = Offset(0f, y), size = Size(size.width, step * 0.35f))
            y += step
        }
        // A few deterministic glitch slices.
        for ((x, yy, s) in scatter(31, 9)) {
            val bw = size.width * (0.15f + x * 0.5f)
            val bh = size.height * (0.004f + s * 0.012f)
            val tint = if (s > 0.5f) Color(0xFFFF2D55) else Color(0xFF00E5FF)
            drawRect(
                tint.copy(alpha = 0.10f + 0.20f * s),
                topLeft = Offset(x * (size.width - bw), yy * size.height),
                size = Size(bw, bh),
            )
        }
    },
) { m ->
    val red = Color(0xFFFF2D55)
    val cyan = Color(0xFF00E5FF)
    val main = f.ink(Color.White)
    val family = if (f.font == FontFamily.Default) FontFamily.Monospace else f.font
    val weight = f.weight(FontWeight.Bold)
    val fs = minOf(m.unit * 1.0f, m.width * 0.90f / 3.2f)
    val d = fs * 0.03f
    val text = f.time.digits

    Box(contentAlignment = Alignment.Center) {
        DigitalGlitchLayer(text, fs, red.copy(alpha = 0.9f), weight, family, -d, d * 0.3f)
        DigitalGlitchLayer(text, fs, cyan.copy(alpha = 0.9f), weight, family, d, -d * 0.3f)
        DigitalGlitchLayer(text, fs, main, weight, family, 0f, 0f)
        // Two displaced slice bars cutting across the digits.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-d * 3f).dp, y = (fs * 0.30f).dp)
                .size(width = (fs * 1.1f).dp, height = (fs * 0.05f).coerceAtLeast(2f).dp)
                .background(cyan.copy(alpha = 0.45f)),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (d * 3f).dp, y = (-fs * 0.28f).dp)
                .size(width = (fs * 0.9f).dp, height = (fs * 0.04f).coerceAtLeast(2f).dp)
                .background(red.copy(alpha = 0.5f)),
        )
    }
    Spacer(Modifier.height((fs * 0.10f).dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = (fs * 0.5f).dp, height = (fs * 0.035f).coerceAtLeast(2f).dp).background(red))
        Spacer(Modifier.width((fs * 0.04f).dp))
        Box(Modifier.size(width = (fs * 0.25f).dp, height = (fs * 0.035f).coerceAtLeast(2f).dp).background(cyan))
        f.time.amPm?.let {
            Text(
                it,
                color = cyan,
                fontSize = (fs * 0.16f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = (fs * 0.06f).dp),
            )
        }
    }
    Spacer(Modifier.height((fs * 0.05f).dp))
    InfoLines(f, m, Color(0xFFB8F6FF))
}

// ---- registry ----------------------------------------------------------------------------

internal val digitalThemes: Map<String, ThemeContent> = mapOf(
    "split_clock" to { f, m -> DigitalSplitClock(f, m) },
    "retro_digital" to { f, m -> DigitalRetro(f, m) },
    "dot_matrix" to { f, m -> DigitalDotMatrix(f, m) },
    "large_typography" to { f, m -> DigitalLargeTypography(f, m) },
    "gradient_digits" to { f, m -> DigitalGradient(f, m) },
    "glitch_clock" to { f, m -> DigitalGlitch(f, m) },
)
