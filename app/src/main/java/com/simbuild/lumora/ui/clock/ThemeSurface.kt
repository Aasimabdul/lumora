package com.simbuild.lumora.ui.clock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.LumoraApp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.BackgroundKind
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.ThemeCatalog

/** Frame for a saved custom theme: the theme's own look replaces the global one and the background is the user's. */
internal fun ClockFrame.withCustom(t: CustomTheme): ClockFrame = copy(
    scale = t.clockScale,
    infoScale = t.infoScale,
    positionX = t.positionX,
    positionY = t.positionY,
    tint = parseHex(t.textColor),
    font = t.font.toFamily(),
    weightOverride = t.weight.toWeight(),
    showDate = t.showDate,
    showBattery = t.showBattery,
    overlay = true,
)

/**
 * Draws whatever [themeId] refers to: a built-in theme, or a saved custom theme from [customThemes].
 * An unknown id (for example a deleted custom theme) falls back to the default theme.
 */
@Composable
fun ThemeSurface(
    themeId: String,
    frame: ClockFrame,
    customThemes: List<CustomTheme>,
    modifier: Modifier = Modifier,
    photoMaxEdge: Int = 1600,
) {
    val custom = if (AppSettings.isCustomId(themeId)) customThemes.firstOrNull { it.id == themeId } else null
    if (custom != null) {
        CustomThemeSurface(custom, frame, modifier, photoMaxEdge)
    } else {
        val id = if (ThemeCatalog.byId(themeId) != null) themeId else ThemeCatalog.DEFAULT_ID
        ThemeRenderers.Render(id, frame, modifier)
    }
}

/** A custom theme: its background (colour or photo) with the chosen clock face drawn on top. */
@Composable
fun CustomThemeSurface(
    theme: CustomTheme,
    frame: ClockFrame,
    modifier: Modifier = Modifier,
    photoMaxEdge: Int = 1600,
) {
    Box(modifier) {
        when (theme.background) {
            BackgroundKind.PHOTO -> PhotoBackground(theme, photoMaxEdge, Modifier.fillMaxSize())
            BackgroundKind.COLOR -> Box(Modifier.fillMaxSize().background(parseHex(theme.backgroundColor) ?: Color.Black))
        }
        ThemeRenderers.Render(theme.baseThemeId, frame.withCustom(theme), Modifier.fillMaxSize())
    }
}

@Composable
private fun PhotoBackground(theme: CustomTheme, maxEdge: Int, modifier: Modifier) {
    val store = (LocalContext.current.applicationContext as LumoraApp).photoStore
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(theme.photoFile, maxEdge) {
        val file = theme.photoFile
        bitmap = if (file == null) null else store.load(file, maxEdge)?.asImageBitmap()
    }
    Box(modifier.clipToBounds().background(Color.Black)) {
        bitmap?.let { image ->
            val blurModifier = if (theme.blur > 0.5f) Modifier.blur(theme.blur.dp, BlurredEdgeTreatment.Unbounded) else Modifier
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
                    .graphicsLayer {
                        scaleX = theme.zoom
                        scaleY = theme.zoom
                        // The zoomed image overhangs by (zoom - 1) / 2 of the view on each side; pan within that.
                        translationX = theme.panX * (theme.zoom - 1f) * size.width / 2f
                        translationY = theme.panY * (theme.zoom - 1f) * size.height / 2f
                    },
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.dim)))
    }
}
