package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PhotoSample = Color(0xFF05070D)

/**
 * Starting point for "Custom Photo Clock". On its own it shows a soft gradient stand-in; once the
 * user creates their own version (Themes → tap this card → Customize) their photo replaces it.
 */
@Composable
internal fun TemplatePhotoClock(f: ClockFrame, modifier: Modifier) = Stage(
    f,
    modifier,
    background = PhotoSample,
    backdrop = {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2A41), PhotoSample)))
        glowCircle(Offset(size.width * 0.8f, size.height * 0.2f), size.minDimension * 0.7f, Color(0x332E5BFF))
        glowCircle(Offset(size.width * 0.15f, size.height * 0.85f), size.minDimension * 0.6f, Color(0x22FF6BA8))
    },
) { m ->
    Row(verticalAlignment = Alignment.Bottom) {
        Text(f.time.digits, color = f.ink(Color.White), fontSize = m.unit.sp, fontWeight = f.weight(FontWeight.Light))
        AmPm(f, m, Color.White.copy(alpha = 0.7f))
    }
    InfoLines(f, m)
}

/**
 * "My Custom Layout": a layout built to be rearranged. Date above, time, accent rule, battery
 * below; the user's size, position, font and colour settings reshape it.
 */
@Composable
internal fun TemplateCustomLayout(f: ClockFrame, modifier: Modifier) = Stage(f, modifier) { m ->
    val accent = f.accent(LumoraCyan)
    val small = infoFontSize(f, m, 0.15f)
    if (f.showDate) {
        Text(
            "${f.weekday.uppercase()} · ${f.date}",
            color = f.ink(Color.White).copy(alpha = 0.8f),
            fontSize = small.sp,
            letterSpacing = 2.sp,
        )
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(f.time.digits, color = f.ink(Color.White), fontSize = (m.unit * 0.95f).sp, fontWeight = f.weight(FontWeight.Medium))
        AmPm(f, m, accent)
    }
    Box(Modifier.padding(vertical = 6.dp).width((m.unit * 1.4f).dp).height(3.dp).background(accent))
    if (f.showBattery) {
        Text(batteryText(f.battery), color = accent, fontSize = small.sp)
    }
}

internal val templateThemes: Map<String, ThemeContent> = mapOf(
    "custom_photo_clock" to { f, m -> TemplatePhotoClock(f, m) },
    "my_custom_layout" to { f, m -> TemplateCustomLayout(f, m) },
)
