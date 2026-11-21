package com.simbuild.lumora.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.LumoraApp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.settings.ClockFont
import com.simbuild.lumora.core.settings.ClockLook
import com.simbuild.lumora.core.settings.ClockWeight
import com.simbuild.lumora.core.settings.look
import com.simbuild.lumora.core.settings.withLook
import com.simbuild.lumora.core.theme.BackgroundKind
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.HexColor
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.ui.clock.CustomThemeSurface
import com.simbuild.lumora.ui.clock.ThemeSurface
import com.simbuild.lumora.ui.clock.parseHex
import com.simbuild.lumora.ui.clock.rememberClockFrame
import com.simbuild.lumora.ui.clock.toFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Swatches = listOf(
    "#FFFFFF", "#FFF3D6", "#00E5FF", "#4D8DFF", "#B388FF", "#FF6BA8",
    "#FF5252", "#FF9100", "#FFD54F", "#69F0AE", "#C6FF00", "#9E9E9E",
)

/**
 * Manual clock customisation with a live preview.
 *
 * - With a built-in theme selected, the controls change the clock look for every built-in theme
 *   ("global" look) and can be saved as a named personal theme.
 * - With one of the user's saved themes selected, the controls edit that theme, including its
 *   background colour or photo.
 *
 * Changes appear in the preview immediately and are saved a moment after the last edit.
 */
@Composable
fun CustomizeScreen(
    settings: AppSettings,
    customThemes: List<CustomTheme>,
    onSettings: (AppSettings.() -> AppSettings) -> Unit,
    onSaveTheme: (CustomTheme) -> Unit,
    onCreateTheme: (CustomTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = if (AppSettings.isCustomId(settings.themeId)) customThemes.firstOrNull { it.id == settings.themeId } else null
    if (editing != null) {
        EditCustomTheme(editing, settings, customThemes, onSaveTheme, modifier)
    } else {
        EditGlobalLook(settings, customThemes, onSettings, onCreateTheme, modifier)
    }
}

// ---- built-in theme: global look ----------------------------------------------------------

@Composable
private fun EditGlobalLook(
    settings: AppSettings,
    customThemes: List<CustomTheme>,
    onSettings: (AppSettings.() -> AppSettings) -> Unit,
    onCreateTheme: (CustomTheme) -> Unit,
    modifier: Modifier,
) {
    var draft by remember(settings.themeId) { mutableStateOf(settings.look()) }
    val latestDraft by rememberUpdatedState(draft)
    val latestStored by rememberUpdatedState(settings.look())
    val latestOnSettings by rememberUpdatedState(onSettings)
    var saveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (draft != latestStored) {
            delay(SAVE_DELAY_MS)
            val look = draft
            latestOnSettings { withLook(look) }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val look = latestDraft
            if (look != latestStored) latestOnSettings { withLook(look) }
        }
    }

    val frame = rememberClockFrame(settings.withLook(draft))
    val theme = ThemeCatalog.resolveOrDefault(settings.themeId)

    Column(modifier.fillMaxSize()) {
        PreviewBox { ThemeSurface(settings.themeId, frame, customThemes, Modifier.fillMaxSize(), photoMaxEdge = 700) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Customize clock", style = MaterialTheme.typography.titleLarge)
            Text(
                "Theme: ${theme.displayName}. These settings apply to every built-in theme.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LookControls(draft) { draft = it }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { draft = draft.reset() }, enabled = !draft.isDefault) { Text("Reset") }
                Button(onClick = { saveDialog = true }) { Text("Save as my theme") }
            }
            Text(
                "A saved theme keeps this look and lets you add a colour or photo background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }
    }

    if (saveDialog) {
        NameDialog(
            title = "Save as my theme",
            initial = "${theme.displayName} (mine)",
            onDismiss = { saveDialog = false },
            onConfirm = { name ->
                saveDialog = false
                onCreateTheme(CustomTheme.create(name, settings.themeId).withLook(draft))
            },
        )
    }
}

// ---- saved custom theme -------------------------------------------------------------------

@Composable
private fun EditCustomTheme(
    stored: CustomTheme,
    settings: AppSettings,
    customThemes: List<CustomTheme>,
    onSave: (CustomTheme) -> Unit,
    modifier: Modifier,
) {
    var draft by remember(stored.id) { mutableStateOf(stored) }
    val latestDraft by rememberUpdatedState(draft)
    val latestStored by rememberUpdatedState(stored)
    val latestOnSave by rememberUpdatedState(onSave)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoStore = (context.applicationContext as LumoraApp).photoStore

    LaunchedEffect(draft) {
        if (draft != latestStored) {
            delay(SAVE_DELAY_MS)
            latestOnSave(draft)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val last = latestDraft
            if (last != latestStored) latestOnSave(last)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val file = photoStore.import(uri)
                if (file != null) {
                    draft = draft.copy(background = BackgroundKind.PHOTO, photoFile = file, zoom = 1f, panX = 0f, panY = 0f)
                } else {
                    Toast.makeText(context, "Couldn't use that image. Try another photo.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val frame = rememberClockFrame(settings)

    Column(modifier.fillMaxSize()) {
        PreviewBox { CustomThemeSurface(draft, frame, Modifier.fillMaxSize(), photoMaxEdge = 700) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Edit my theme", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it.take(CustomTheme.MAX_NAME)) },
                label = { Text("Theme name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            SectionTitle("Clock face")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeCatalog.all.filter { it.id !in ThemeCatalog.TEMPLATE_IDS }.forEach { t ->
                    FilterChip(
                        selected = draft.baseThemeId == t.id,
                        onClick = { draft = draft.copy(baseThemeId = t.id) },
                        label = { Text(t.displayName) },
                    )
                }
            }
            Text(
                "On a colour or photo background the face's own artwork is hidden so your background shows through.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Background")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.background == BackgroundKind.COLOR,
                    onClick = { draft = draft.copy(background = BackgroundKind.COLOR) },
                    label = { Text("Colour") },
                )
                FilterChip(
                    selected = draft.background == BackgroundKind.PHOTO,
                    onClick = {
                        if (draft.photoFile != null) draft = draft.copy(background = BackgroundKind.PHOTO)
                        else picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    label = { Text("Photo") },
                )
            }
            if (draft.background == BackgroundKind.COLOR) {
                ColorPicker(
                    label = "Background colour",
                    value = draft.backgroundColor,
                    noneLabel = null,
                    onPick = { draft = draft.copy(backgroundColor = it ?: "#000000") },
                )
            } else {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Change photo")
                    }
                    TextButton(onClick = { draft = draft.copy(background = BackgroundKind.COLOR, photoFile = null) }) { Text("Remove photo") }
                }
                Text(
                    "The photo is copied into Lumora's private storage. It never leaves your device and its location data is removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SliderRow("Darken photo", "${(draft.dim * 100).toInt()}%", draft.dim, 0f, 1f) { draft = draft.copy(dim = it) }
                SliderRow("Blur (Android 12 and newer)", "${draft.blur.toInt()}", draft.blur, 0f, 25f) { draft = draft.copy(blur = it) }
                SliderRow("Zoom", "%.1fx".format(draft.zoom), draft.zoom, 1f, CustomTheme.MAX_ZOOM) { draft = draft.copy(zoom = it) }
                if (draft.zoom > 1.01f) {
                    SliderRow("Move photo left / right", "%+.2f".format(draft.panX), draft.panX, -1f, 1f) { draft = draft.copy(panX = it) }
                    SliderRow("Move photo up / down", "%+.2f".format(draft.panY), draft.panY, -1f, 1f) { draft = draft.copy(panY = it) }
                }
            }

            SectionTitle("Clock text")
            LookControls(draft.look()) { draft = draft.withLook(it) }
            OutlinedButton(
                onClick = { draft = draft.withLook(draft.look().reset()) },
                enabled = !draft.look().isDefault,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
            ) { Text("Reset text settings") }
        }
    }
}

// ---- shared controls ----------------------------------------------------------------------

@Composable
private fun PreviewBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF24384D), RoundedCornerShape(16.dp))
                .background(Color.Black)
                .semantics { contentDescription = "Live preview of your clock" },
        ) { content() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

/** Every manual text option: size, position, font, weight, colour and the date / battery lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookControls(look: ClockLook, onLook: (ClockLook) -> Unit) {
    SliderRow("Clock size", "${(look.clockScale * 100).toInt()}%", look.clockScale, AppSettings.SCALE_MIN, AppSettings.SCALE_MAX) {
        onLook(look.copy(clockScale = it))
    }
    SliderRow("Date & battery text size", "${(look.infoScale * 100).toInt()}%", look.infoScale, AppSettings.INFO_SCALE_MIN, AppSettings.INFO_SCALE_MAX) {
        onLook(look.copy(infoScale = it))
    }
    SliderRow("Position left / right", "%+.2f".format(look.positionX), look.positionX, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT) {
        onLook(look.copy(positionX = it))
    }
    SliderRow("Position up / down", "%+.2f".format(look.positionY), look.positionY, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT) {
        onLook(look.copy(positionY = it))
    }
    TextButton(onClick = { onLook(look.copy(positionX = 0f, positionY = 0f)) }, enabled = look.positionX != 0f || look.positionY != 0f) {
        Text("Centre the clock")
    }

    SectionTitle("Font")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClockFont.entries.forEach { f ->
            FilterChip(
                selected = look.font == f,
                onClick = { onLook(look.copy(font = f)) },
                label = { Text(f.label, fontFamily = f.toFamily()) },
            )
        }
    }

    SectionTitle("Weight")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClockWeight.entries.forEach { w ->
            FilterChip(
                selected = look.weight == w,
                onClick = { onLook(look.copy(weight = w)) },
                label = { Text(w.label) },
            )
        }
    }

    ColorPicker("Text colour", look.textColor, noneLabel = "Theme colours") { onLook(look.copy(textColor = it)) }

    SectionTitle("Show")
    SwitchRow("Date", look.showDate) { onLook(look.copy(showDate = it)) }
    SwitchRow("Battery", look.showBattery) { onLook(look.copy(showBattery = it)) }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.semantics { contentDescription = "$label $valueText" },
        )
    }
}

/** Swatches plus a hex field. [noneLabel] adds a "use the theme's own colours" choice (null value). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(label: String, value: String?, noneLabel: String?, onPick: (String?) -> Unit) {
    SectionTitle(label)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (noneLabel != null) {
            FilterChip(selected = value == null, onClick = { onPick(null) }, label = { Text(noneLabel) })
        }
        Swatches.forEach { hex ->
            val selected = value.equals(hex, ignoreCase = true)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(parseHex(hex) ?: Color.Gray)
                    .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF55657A), CircleShape)
                    .clickable(role = Role.Button) { onPick(hex) }
                    .semantics { contentDescription = "$label $hex" + if (selected) ", selected" else "" },
            )
        }
    }
    var hex by remember(value) { mutableStateOf(value.orEmpty()) }
    OutlinedTextField(
        value = hex,
        onValueChange = {
            hex = it.take(7)
            HexColor.normalize(hex)?.let(onPick)
        },
        label = { Text("Hex colour, e.g. #FFAA00") },
        isError = hex.isNotEmpty() && !HexColor.isValid(hex),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
internal fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial.take(CustomTheme.MAX_NAME)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(CustomTheme.MAX_NAME) },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim().ifEmpty { "My theme" }) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val SAVE_DELAY_MS = 300L
