package com.simbuild.lumora.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.simbuild.lumora.R

/** Display face used for the Lumora wordmark and screen titles. */
val DisplayFont: FontFamily = FontFamily(Font(R.font.audiowide))

private val LumoraColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF7C9CFF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0D1B2A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF13263A),
    onSurfaceVariant = Color(0xFFB8C7D6),
    outline = Color(0xFF24384D),
)

private val Base = Typography()

private val LumoraTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontFamily = DisplayFont),
    headlineLarge = Base.headlineLarge.copy(fontFamily = DisplayFont),
    headlineMedium = Base.headlineMedium.copy(fontFamily = DisplayFont),
    headlineSmall = Base.headlineSmall.copy(fontFamily = DisplayFont),
    titleLarge = Base.titleLarge.copy(fontFamily = DisplayFont),
)

@Composable
fun LumoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LumoraColors, typography = LumoraTypography, content = content)
}
