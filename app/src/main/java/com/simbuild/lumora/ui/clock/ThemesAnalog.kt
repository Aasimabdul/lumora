package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.battery.ChargeStatus
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- 1. Circle Glow ----------------------------------------------------------------------

private fun DrawScope.analogGlowHand(
    center: Offset,
    degrees: Double,
    length: Float,
    width: Float,
    glow: Color,
    core: Color,
) {
    drawHand(center, degrees, length, width * 3.4f, glow.copy(alpha = 0.10f))
    drawHand(center, degrees, length, width * 2.1f, glow.copy(alpha = 0.22f))
    drawHand(center, degrees, length, width, core)
}

@Composable
private fun ThemeCircleGlow(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF03060C),
    backdrop = {
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF0A1428), Color(0xFF02040A)),
                center = center,
                radius = size.maxDimension * 0.7f,
            ),
        )
    },
) { m ->
    val neon = f.accent(LumoraCyan)
    val ink = f.ink(Color.White)
    val d = m.dial
    Box(Modifier.size(d.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f
            // Thin neon outer ring with a soft halo.
            val ringR = r * 0.95f
            drawCircle(neon.copy(alpha = 0.06f), radius = ringR, center = c, style = Stroke(width = r * 0.07f))
            drawCircle(neon.copy(alpha = 0.12f), radius = ringR, center = c, style = Stroke(width = r * 0.035f))
            drawCircle(neon.copy(alpha = 0.6f), radius = ringR, center = c, style = Stroke(width = r * 0.008f))

            // Minute progress arc: faint track, then layered translucent strokes for the glow.
            val arcR = r * 0.84f
            val topLeft = Offset(c.x - arcR, c.y - arcR)
            val arcSize = Size(arcR * 2f, arcR * 2f)
            val sweep = (f.zoned.minute / 60f * 360f).coerceAtLeast(1f)
            drawCircle(neon.copy(alpha = 0.10f), radius = arcR, center = c, style = Stroke(width = r * 0.012f))
            val layers = listOf(0.10f to 0.07f, 0.07f to 0.12f, 0.04f to 0.25f, 0.018f to 1f)
            for ((w, a) in layers) {
                drawArc(
                    color = neon.copy(alpha = a),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = r * w, cap = StrokeCap.Round),
                )
            }
            val head = polar(c, sweep.toDouble(), arcR)
            drawCircle(neon.copy(alpha = 0.25f), radius = r * 0.06f, center = head)
            drawCircle(Color.White, radius = r * 0.022f, center = head)

            // Tiny dots at the 12 hour positions.
            for (i in 0 until 12) {
                val big = i % 3 == 0
                drawCircle(
                    ink.copy(alpha = if (big) 0.9f else 0.55f),
                    radius = r * if (big) 0.026f else 0.017f,
                    center = polar(c, i * 30.0, r * 0.70f),
                )
            }

            // Glowing hands.
            analogGlowHand(c, f.hourDegrees, r * 0.40f, r * 0.030f, neon, ink)
            analogGlowHand(c, f.minuteDegrees, r * 0.60f, r * 0.020f, neon, ink)
            drawCircle(neon.copy(alpha = 0.3f), radius = r * 0.07f, center = c)
            drawCircle(ink, radius = r * 0.03f, center = c)
        }
        val digital = f.time.digits + (f.time.amPm?.let { " $it" } ?: "")
        Text(
            digital,
            style = TextStyle(
                color = ink.copy(alpha = 0.9f),
                fontSize = (d * 0.07f).sp,
                fontWeight = f.weight(FontWeight.Medium),
                shadow = Shadow(color = neon.copy(alpha = 0.7f), offset = Offset.Zero, blurRadius = 14f),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (d * 0.20f).dp),
        )
    }
    InfoLines(f, m, ink)
}

// ---- 2. Roman Numerals -------------------------------------------------------------------

private val AnalogRomans = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

/** Ornate leaf-shaped hour hand pointing at 12 o'clock, pivoting on [c]. */
private fun analogLeafHourPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y + r * 0.14f)
    lineTo(c.x - r * 0.018f, c.y + r * 0.02f)
    lineTo(c.x - r * 0.018f, c.y - r * 0.16f)
    cubicTo(c.x - r * 0.14f, c.y - r * 0.22f, c.x - r * 0.09f, c.y - r * 0.40f, c.x, c.y - r * 0.52f)
    cubicTo(c.x + r * 0.09f, c.y - r * 0.40f, c.x + r * 0.14f, c.y - r * 0.22f, c.x + r * 0.018f, c.y - r * 0.16f)
    lineTo(c.x + r * 0.018f, c.y + r * 0.02f)
    close()
}

/** Slim tapered minute hand pointing at 12 o'clock, pivoting on [c]. */
private fun analogSlimMinutePath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r * 0.84f)
    lineTo(c.x + r * 0.020f, c.y - r * 0.02f)
    lineTo(c.x + r * 0.012f, c.y + r * 0.16f)
    lineTo(c.x - r * 0.012f, c.y + r * 0.16f)
    lineTo(c.x - r * 0.020f, c.y - r * 0.02f)
    close()
}

@Composable
private fun ThemeRomanNumerals(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF140B08),
    backdrop = {
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF3A1D14), Color(0xFF1A0D09), Color(0xFF0C0605)),
                center = center,
                radius = size.maxDimension * 0.75f,
            ),
        )
    },
) { m ->
    val gold = f.accent(Color(0xFFC9A24B))
    val ink = f.ink(Color(0xFF3B2A14))
    val ivory = Color(0xFFF6EEDA)
    val d = m.dial
    val r = d / 2f
    Box(Modifier.size(d.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val rr = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFFBEF), ivory, Color(0xFFE6D9B5)),
                    center = c,
                    radius = rr,
                ),
                radius = rr * 0.985f,
                center = c,
            )
            // Outer double ring.
            drawCircle(gold, radius = rr * 0.972f, center = c, style = Stroke(width = rr * 0.024f))
            drawCircle(gold, radius = rr * 0.925f, center = c, style = Stroke(width = rr * 0.008f))
            drawCircle(ink.copy(alpha = 0.5f), radius = rr * 0.885f, center = c, style = Stroke(width = rr * 0.006f))
            // Minute track.
            for (i in 0 until 60) {
                val hour = i % 5 == 0
                drawLine(
                    color = if (hour) gold else ink.copy(alpha = 0.7f),
                    start = polar(c, i * 6.0, if (hour) rr * 0.885f else rr * 0.900f),
                    end = polar(c, i * 6.0, rr * 0.920f),
                    strokeWidth = if (hour) rr * 0.020f else rr * 0.007f,
                    cap = StrokeCap.Round,
                )
            }
        }
        val cell = d * 0.16f
        for (i in 1..12) {
            val p = polar(Offset(r, r), i * 30.0, r * 0.72f)
            Box(
                Modifier
                    .offset(x = (p.x - cell / 2f).dp, y = (p.y - cell / 2f).dp)
                    .size(cell.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    AnalogRomans[i - 1],
                    color = ink,
                    fontFamily = FontFamily.Serif,
                    fontWeight = f.weight(FontWeight.SemiBold),
                    fontSize = (d * 0.075f).sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val rr = size.minDimension / 2f
            rotate(degrees = f.hourDegrees.toFloat(), pivot = c) {
                val leaf = analogLeafHourPath(c, rr)
                drawPath(leaf, ink)
                drawPath(leaf, gold, style = Stroke(width = rr * 0.008f))
                drawCircle(gold, radius = rr * 0.03f, center = Offset(c.x, c.y - rr * 0.16f))
            }
            rotate(degrees = f.minuteDegrees.toFloat(), pivot = c) {
                val blade = analogSlimMinutePath(c, rr)
                drawPath(blade, ink)
                drawPath(blade, gold, style = Stroke(width = rr * 0.006f))
            }
            drawCircle(gold, radius = rr * 0.045f, center = c)
            drawCircle(ink, radius = rr * 0.02f, center = c)
        }
    }
    InfoLines(f, m, Color.White)
}

// ---- 3. Analog Calendar ------------------------------------------------------------------

@Composable
private fun ThemeAnalogCalendar(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF0A1018),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF14202E), Color(0xFF070B11))))
    },
) { m ->
    val ink = f.ink(Color.White)
    val accent = f.accent(Color(0xFFFF7043))
    val d = m.dial * 0.8f
    val r = d / 2f
    val locale = Locale.getDefault()

    // Month + year above the dial.
    Text(
        f.zoned.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).uppercase(locale),
        color = ink.copy(alpha = 0.85f),
        fontSize = (d * 0.07f).sp,
        fontWeight = f.weight(FontWeight.SemiBold),
        letterSpacing = (d * 0.012f).sp,
        maxLines = 1,
    )
    Spacer(Modifier.height((d * 0.03f).dp))

    Box(Modifier.size(d.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val rr = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF1E2A3A), Color(0xFF0E141C)), center = c, radius = rr),
                radius = rr * 0.98f,
                center = c,
            )
            drawCircle(accent.copy(alpha = 0.7f), radius = rr * 0.98f, center = c, style = Stroke(width = rr * 0.014f))
            for (i in 0 until 60) {
                val major = i % 5 == 0
                drawLine(
                    color = if (major) ink else ink.copy(alpha = 0.45f),
                    start = polar(c, i * 6.0, if (major) rr * 0.84f else rr * 0.90f),
                    end = polar(c, i * 6.0, rr * 0.95f),
                    strokeWidth = if (major) rr * 0.026f else rr * 0.009f,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Date window at 3 o'clock.
        val winW = d * 0.17f
        val winH = d * 0.105f
        val winShape = RoundedCornerShape((winH * 0.2f).dp)
        Box(
            Modifier
                .offset(x = (r + r * 0.60f - winW / 2f).dp, y = (r - winH / 2f).dp)
                .size(width = winW.dp, height = winH.dp)
                .clip(winShape)
                .background(Color(0xFFF3F1EA))
                .border((d * 0.006f).coerceAtLeast(1f).dp, accent, winShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                f.zoned.dayOfMonth.toString(),
                color = Color(0xFF14181F),
                fontWeight = FontWeight.Bold,
                fontSize = (winH * 0.62f).sp,
                maxLines = 1,
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val rr = size.minDimension / 2f
            drawHand(c, f.hourDegrees, rr * 0.48f, rr * 0.045f, ink)
            drawHand(c, f.minuteDegrees, rr * 0.76f, rr * 0.028f, accent)
            drawCircle(accent, radius = rr * 0.05f, center = c)
            drawCircle(Color(0xFF0E141C), radius = rr * 0.02f, center = c)
        }
    }
    Spacer(Modifier.height((d * 0.05f).dp))

    // Calendar strip: the current week, Monday to Sunday.
    val today = f.zoned.toLocalDate()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val cw = d * 0.125f
    val ch = d * 0.19f
    val cellShape = RoundedCornerShape((cw * 0.25f).dp)
    Row(horizontalArrangement = Arrangement.spacedBy((d * 0.02f).dp)) {
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            val isToday = day == today
            val label = DayOfWeek.of(i + 1).getDisplayName(java.time.format.TextStyle.SHORT, locale).take(3)
            val fg = if (isToday) Color(0xFF10151C) else ink
            Column(
                Modifier
                    .size(width = cw.dp, height = ch.dp)
                    .clip(cellShape)
                    .background(if (isToday) accent else ink.copy(alpha = 0.08f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    color = fg.copy(alpha = 0.75f),
                    fontSize = (cw * 0.26f).sp,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    day.dayOfMonth.toString(),
                    color = fg,
                    fontSize = (cw * 0.42f).sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
    InfoLines(f, m, ink)
}

// ---- 4. Split Analog Dashboard -----------------------------------------------------------

private fun analogBatteryColor(percent: Int?): Color = when {
    percent == null -> Color(0xFF9E9E9E)
    percent > 50 -> Color(0xFF4CAF50)
    percent >= 20 -> Color(0xFFFFB300)
    else -> Color(0xFFE53935)
}

@Composable
private fun AnalogDashPanel(f: ClockFrame, m: Metrics, pw: Float, dial: Float, ink: Color, accent: Color) {
    val fs = minOf(pw * 0.24f, dial * 0.34f)
    val infoSize = minOf(infoFontSize(f, m), pw * 0.07f)
    val shape = RoundedCornerShape((pw * 0.06f).dp)
    Column(
        Modifier
            .width(pw.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, ink.copy(alpha = 0.14f), shape)
            .padding((pw * 0.06f).dp),
        verticalArrangement = Arrangement.spacedBy((pw * 0.025f).dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                f.time.digits,
                color = ink,
                fontSize = fs.sp,
                fontWeight = f.weight(FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
            f.time.amPm?.let {
                Text(
                    it,
                    color = ink.copy(alpha = 0.7f),
                    fontSize = (fs * 0.3f).sp,
                    modifier = Modifier.padding(start = (fs * 0.1f).dp, bottom = (fs * 0.12f).dp),
                )
            }
        }
        if (f.showDate) {
            Text(
                f.weekday,
                color = accent,
                fontSize = (fs * 0.40f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                f.date,
                color = ink.copy(alpha = 0.75f),
                fontSize = infoSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (f.showBattery) {
            val pct = f.battery.percent
            val barH = pw * 0.05f
            val fill = analogBatteryColor(pct)
            Text(
                batteryText(f.battery),
                color = ink.copy(alpha = 0.75f),
                fontSize = infoSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((barH * 0.6f).dp),
            ) {
                val barShape = RoundedCornerShape(barH.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(barH.dp)
                        .clip(barShape)
                        .background(ink.copy(alpha = 0.15f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(((pct ?: 0) / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(fill),
                    )
                }
                if (f.battery.status == ChargeStatus.CHARGING) {
                    Canvas(Modifier.size(width = (barH * 1.1f).dp, height = (barH * 1.8f).dp)) {
                        val w = size.width
                        val h = size.height
                        val bolt = Path().apply {
                            moveTo(w * 0.62f, 0f)
                            lineTo(w * 0.10f, h * 0.56f)
                            lineTo(w * 0.46f, h * 0.56f)
                            lineTo(w * 0.34f, h)
                            lineTo(w * 0.92f, h * 0.38f)
                            lineTo(w * 0.55f, h * 0.38f)
                            close()
                        }
                        drawPath(bolt, Color(0xFFFFD54F))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSplitDashboard(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF0B1118),
    backdrop = {
        drawRect(
            Brush.linearGradient(
                listOf(Color(0xFF111B27), Color(0xFF070A10)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
    },
) { m ->
    val ink = f.ink(Color.White)
    val accent = f.accent(Color(0xFF4FC3F7))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxHeight > maxWidth) {
            val dl = m.dial * 0.78f
            val pw = m.width * 0.88f
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((dl * 0.08f).dp),
            ) {
                AnalogDial(f, dl.dp, outline = true, allTicks = false, ink = ink, accent = accent)
                AnalogDashPanel(f, m, pw, dl, ink, accent)
            }
        } else {
            val dl = minOf(m.dial, m.width * 0.42f)
            val pw = m.width * 0.46f
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((m.width * 0.04f).dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnalogDial(f, dl.dp, outline = true, allTicks = false, ink = ink, accent = accent)
                AnalogDashPanel(f, m, pw, dl, ink, accent)
            }
        }
    }
}

// ---- 5. Modern Watch ---------------------------------------------------------------------

@Composable
private fun ThemeModernWatch(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF07090D),
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF10141B), Color(0xFF050608))))
    },
) { m ->
    val ink = f.ink(Color.White)
    val minuteColor = f.accent(Color(0xFFFF9F0A))
    val ringColor = f.accent(Color(0xFF30D158))
    val d = m.dial
    val body = d * 0.94f
    val cx = body / 2f
    val cy = d / 2f
    val pct = f.battery.percent
    Box(Modifier.size(d.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            val bs = s * 0.94f
            val top = (s - bs) / 2f
            val cen = Offset(bs / 2f, s / 2f)
            // Crown.
            drawRoundRect(
                Color(0xFF6B717B),
                topLeft = Offset(bs * 0.985f, s * 0.38f),
                size = Size(s * 0.06f, s * 0.16f),
                cornerRadius = CornerRadius(s * 0.015f),
            )
            // Gradient bezel.
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color(0xFF6B717B), Color(0xFF1B1E23), Color(0xFF4A4F57), Color(0xFF15171B)),
                    start = Offset(0f, 0f),
                    end = Offset(s, s),
                ),
                topLeft = Offset(0f, top),
                size = Size(bs, bs),
                cornerRadius = CornerRadius(bs * 0.26f),
            )
            drawRoundRect(
                Color.White.copy(alpha = 0.18f),
                topLeft = Offset(0f, top),
                size = Size(bs, bs),
                cornerRadius = CornerRadius(bs * 0.26f),
                style = Stroke(width = bs * 0.006f),
            )
            // Screen.
            val inset = bs * 0.035f
            drawRoundRect(
                Brush.radialGradient(listOf(Color(0xFF151A22), Color(0xFF020304)), center = cen, radius = bs * 0.62f),
                topLeft = Offset(inset, top + inset),
                size = Size(bs - inset * 2f, bs - inset * 2f),
                cornerRadius = CornerRadius(bs * 0.225f),
            )
            // Minimalist ticks.
            for (i in 0 until 12) {
                val major = i % 3 == 0
                drawLine(
                    color = if (major) ink else ink.copy(alpha = 0.45f),
                    start = polar(cen, i * 30.0, if (major) bs * 0.385f else bs * 0.415f),
                    end = polar(cen, i * 30.0, bs * 0.44f),
                    strokeWidth = if (major) bs * 0.016f else bs * 0.009f,
                    cap = StrokeCap.Round,
                )
            }
            // Battery ring gauge (top complication).
            if (f.showBattery) {
                val rd = bs * 0.085f
                val rc = Offset(cen.x, cen.y - bs * 0.22f)
                val ringTopLeft = Offset(rc.x - rd, rc.y - rd)
                val ringSize = Size(rd * 2f, rd * 2f)
                val stroke = rd * 0.22f
                drawArc(
                    color = ink.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = (pct ?: 0) * 3.6f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            // Date chip backing (bottom complication).
            if (f.showDate) {
                val chipW = bs * 0.5f
                val chipH = bs * 0.1f
                drawRoundRect(
                    ink.copy(alpha = 0.12f),
                    topLeft = Offset(cen.x - chipW / 2f, cen.y + bs * 0.27f - chipH / 2f),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(chipH / 2f),
                )
            }
        }
        if (f.showBattery) {
            val rd = body * 0.085f
            Box(
                Modifier
                    .offset(x = (cx - rd).dp, y = (cy - body * 0.22f - rd).dp)
                    .size((rd * 2f).dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    pct?.toString() ?: "--",
                    color = ink,
                    fontSize = (rd * 0.72f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (f.showDate) {
            val chipW = body * 0.5f
            val chipH = body * 0.1f
            Box(
                Modifier
                    .offset(x = (cx - chipW / 2f).dp, y = (cy + body * 0.27f - chipH / 2f).dp)
                    .size(width = chipW.dp, height = chipH.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    f.date,
                    color = ink.copy(alpha = 0.9f),
                    fontSize = (body * 0.045f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = (chipH * 0.4f).dp),
                )
            }
            val wdW = body * 0.17f
            val wdH = body * 0.1f
            Box(
                Modifier
                    .offset(x = (cx - body * 0.27f - wdW / 2f).dp, y = (cy - wdH / 2f).dp)
                    .size(width = wdW.dp, height = wdH.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    f.weekday.take(3).uppercase(),
                    color = minuteColor.copy(alpha = 0.95f),
                    fontSize = (body * 0.052f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            val bs = s * 0.94f
            val cen = Offset(bs / 2f, s / 2f)
            drawHand(cen, f.hourDegrees, bs * 0.24f, bs * 0.052f, ink)
            drawHand(cen, f.minuteDegrees, bs * 0.37f, bs * 0.036f, minuteColor)
            drawCircle(minuteColor, radius = bs * 0.03f, center = cen)
            drawCircle(Color(0xFF05070A), radius = bs * 0.012f, center = cen)
        }
    }
    // Date and battery are drawn inside the watch face; only add text when a complication is missing a place.
    if (!f.showDate && !f.showBattery) Spacer(Modifier.height(0.dp))
}

// ---- 6. Retro Clock Radio ----------------------------------------------------------------

/** Dark wooden panel: vertical gradient, horizontal grain stripes and a vignette. */
private fun DrawScope.analogRetroBackdrop() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5A3A22), Color(0xFF3B2314), Color(0xFF2A180D))))
    for ((_, y, s) in scatter(7, 70)) {
        val yy = y * size.height
        drawLine(
            color = Color(0xFF1E0F06).copy(alpha = 0.10f + 0.22f * s),
            start = Offset(0f, yy),
            end = Offset(size.width, yy + (s - 0.5f) * size.height * 0.012f),
            strokeWidth = 1f + s * 3f,
        )
    }
    for ((_, y, s) in scatter(19, 24)) {
        val yy = y * size.height
        drawLine(
            color = Color(0xFFB98A5A).copy(alpha = 0.05f + 0.08f * s),
            start = Offset(0f, yy),
            end = Offset(size.width, yy),
            strokeWidth = 1f + s * 1.5f,
        )
    }
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Black.copy(alpha = 0.40f)),
        ),
    )
}

@Composable
private fun AnalogSpeakerGrille(w: Float, h: Float) {
    Canvas(
        Modifier
            .size(width = w.dp, height = h.dp)
            .clip(RoundedCornerShape((w * 0.1f).dp))
            .background(Color(0xFF241509)),
    ) {
        val cols = 5
        val gap = size.width / cols
        val rows = (size.height / gap).toInt().coerceAtLeast(1)
        val y0 = (size.height - gap * rows) / 2f + gap / 2f
        for (rw in 0 until rows) {
            for (cl in 0 until cols) {
                val ctr = Offset(gap * cl + gap / 2f, y0 + gap * rw)
                drawCircle(Color(0xFF4A3322), radius = gap * 0.22f, center = Offset(ctr.x, ctr.y + gap * 0.04f))
                drawCircle(Color(0xFF050201), radius = gap * 0.20f, center = ctr)
            }
        }
    }
}

@Composable
private fun AnalogLabelPlate(text: String, color: Color, fontSize: Float, modifier: Modifier) {
    val shape = RoundedCornerShape((fontSize * 0.4f).dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF4A3520), Color(0xFF2E1E10))))
            .border(1.dp, Color(0xFF8A6A44), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = color,
            fontSize = fontSize.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = (fontSize * 0.6f).dp, vertical = (fontSize * 0.3f).dp),
        )
    }
}

@Composable
private fun ThemeRetroClockRadio(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = Color(0xFF3B2314),
    backdrop = { analogRetroBackdrop() },
) { m ->
    val led = f.accent(Color(0xFFFF2A1A))
    val cream = f.ink(Color(0xFFE9D8B4))
    val u = m.unit
    val pw = minOf(m.width * 0.94f, u * 4.6f)
    val pad = pw * 0.035f
    val gridW = pw * 0.2f
    val windowW = pw - pad * 3f - gridW - pad
    val fs = minOf(u * 0.58f, windowW / 3.5f)
    val wh = fs * 1.35f
    val plateSize = minOf(infoFontSize(f, m), pw * 0.045f)
    val faceShape = RoundedCornerShape((pw * 0.04f).dp)

    Column(
        Modifier
            .width(pw.dp)
            .clip(faceShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2E1D12), Color(0xFF1B0F08))))
            .border((pw * 0.004f).coerceAtLeast(1f).dp, Color(0xFF6A4A30), faceShape)
            .padding(pad.dp),
        verticalArrangement = Arrangement.spacedBy((pad * 0.8f).dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(pad.dp),
        ) {
            // Inset dark window with red LED digits.
            val windowShape = RoundedCornerShape((wh * 0.12f).dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(wh.dp)
                    .clip(windowShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF1A0505), Color(0xFF000000))))
                    .border((wh * 0.03f).coerceAtLeast(1f).dp, Color(0xFF55402E), windowShape),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        f.time.digits,
                        style = TextStyle(
                            color = led,
                            fontSize = fs.sp,
                            fontWeight = f.weight(FontWeight.Bold),
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(color = led.copy(alpha = 0.85f), offset = Offset.Zero, blurRadius = 24f),
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    f.time.amPm?.let {
                        Text(
                            it,
                            color = led.copy(alpha = 0.8f),
                            fontSize = (fs * 0.25f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = (fs * 0.08f).dp, bottom = (fs * 0.12f).dp),
                        )
                    }
                }
            }
            AnalogSpeakerGrille(gridW, wh)
        }

        // Tuner scale with a red needle whose position follows the minute.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height((u * 0.26f).dp),
        ) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF3A2B1A), Color(0xFF1E140A))),
                cornerRadius = CornerRadius(h * 0.18f),
            )
            val left = w * 0.06f
            val right = w * 0.94f
            val steps = 40
            drawLine(
                color = cream.copy(alpha = 0.5f),
                start = Offset(left, h * 0.58f),
                end = Offset(right, h * 0.58f),
                strokeWidth = (h * 0.02f).coerceAtLeast(1f),
            )
            for (i in 0..steps) {
                val x = left + (right - left) * i / steps
                val major = i % 5 == 0
                drawLine(
                    color = cream.copy(alpha = if (major) 0.85f else 0.5f),
                    start = Offset(x, h * 0.58f),
                    end = Offset(x, h * if (major) 0.30f else 0.44f),
                    strokeWidth = (h * if (major) 0.035f else 0.02f).coerceAtLeast(1f),
                )
            }
            val nx = left + (right - left) * (f.zoned.minute / 59f)
            drawLine(led.copy(alpha = 0.22f), Offset(nx, h * 0.08f), Offset(nx, h * 0.92f), strokeWidth = h * 0.13f)
            drawLine(led, Offset(nx, h * 0.08f), Offset(nx, h * 0.92f), strokeWidth = (h * 0.045f).coerceAtLeast(1.5f))
            drawCircle(led, radius = h * 0.06f, center = Offset(nx, h * 0.10f))
        }

        // Label plates for date and battery.
        if (f.showDate || f.showBattery) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(pad.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (f.showDate) {
                    AnalogLabelPlate("${f.weekday} · ${f.date}", cream, plateSize, Modifier.weight(1f))
                }
                if (f.showBattery) {
                    AnalogLabelPlate(batteryText(f.battery), cream, plateSize, Modifier.weight(1f))
                }
            }
        }
    }
}

// ---- registry ----------------------------------------------------------------------------

internal val analogThemes: Map<String, ThemeContent> = mapOf(
    "circle_glow" to { f, m -> ThemeCircleGlow(f, m) },
    "roman_numerals" to { f, m -> ThemeRomanNumerals(f, m) },
    "analog_calendar" to { f, m -> ThemeAnalogCalendar(f, m) },
    "split_analog_dashboard" to { f, m -> ThemeSplitDashboard(f, m) },
    "modern_watch" to { f, m -> ThemeModernWatch(f, m) },
    "retro_clock_radio" to { f, m -> ThemeRetroClockRadio(f, m) },
)
