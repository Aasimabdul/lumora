package com.simbuild.lumora.core.theme

import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.settings.ClockFont
import com.simbuild.lumora.core.settings.ClockWeight
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class BackgroundKind { COLOR, PHOTO }

/**
 * A user-made theme: one built-in clock face ([baseThemeId]) over a colour or personal photo,
 * plus every manual customisation. The photo itself lives in app-private storage; only its
 * file name is stored here.
 */
@Serializable
data class CustomTheme(
    val id: String,
    val name: String = "My theme",
    val baseThemeId: String = ThemeCatalog.DEFAULT_ID,
    val background: BackgroundKind = BackgroundKind.COLOR,
    val backgroundColor: String = "#000000",
    val photoFile: String? = null,
    /** Black overlay strength over the photo, 0..1. */
    val dim: Float = 0.35f,
    /** Blur radius in dp, 0..25 (applied only on Android 12+). */
    val blur: Float = 0f,
    /** Photo zoom, 1..4. */
    val zoom: Float = 1f,
    /** Photo pan, -1..1 (limited by the zoom so edges never show). */
    val panX: Float = 0f,
    val panY: Float = 0f,
    val textColor: String? = null,
    val font: ClockFont = ClockFont.DEFAULT,
    val weight: ClockWeight = ClockWeight.THEME,
    val clockScale: Float = 1f,
    val infoScale: Float = 1f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val favorite: Boolean = false,
) {
    /** Returns a copy whose every field is valid; never throws. */
    fun sanitized(): CustomTheme {
        fun f(v: Float, default: Float, min: Float, max: Float) = if (v.isFinite()) v.coerceIn(min, max) else default
        val base = if (ThemeCatalog.byId(baseThemeId) != null && baseThemeId !in ThemeCatalog.TEMPLATE_IDS) {
            baseThemeId
        } else {
            ThemeCatalog.DEFAULT_ID
        }
        val file = photoFile?.takeIf { SAFE_FILE.matches(it) && ".." !in it }
        val kind = if (background == BackgroundKind.PHOTO && file != null) BackgroundKind.PHOTO else BackgroundKind.COLOR
        val zoomValue = f(zoom, 1f, 1f, MAX_ZOOM)
        val panLimit = 1f
        return copy(
            id = if (id.startsWith(AppSettings.CUSTOM_PREFIX) && id.length <= 80) id else newId(),
            name = name.trim().take(MAX_NAME).ifEmpty { "My theme" },
            baseThemeId = base,
            background = kind,
            backgroundColor = HexColor.normalize(backgroundColor) ?: "#000000",
            photoFile = file,
            dim = f(dim, 0.35f, 0f, 1f),
            blur = f(blur, 0f, 0f, 25f),
            zoom = zoomValue,
            panX = f(panX, 0f, -panLimit, panLimit),
            panY = f(panY, 0f, -panLimit, panLimit),
            textColor = HexColor.normalize(textColor),
            clockScale = f(clockScale, 1f, AppSettings.SCALE_MIN, AppSettings.SCALE_MAX),
            infoScale = f(infoScale, 1f, AppSettings.INFO_SCALE_MIN, AppSettings.INFO_SCALE_MAX),
            positionX = f(positionX, 0f, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT),
            positionY = f(positionY, 0f, -AppSettings.POSITION_LIMIT, AppSettings.POSITION_LIMIT),
        )
    }

    companion object {
        const val MAX_NAME = 40
        const val MAX_ZOOM = 4f
        private val SAFE_FILE = Regex("^[A-Za-z0-9._-]{1,80}$")

        fun newId(): String = AppSettings.CUSTOM_PREFIX + UUID.randomUUID().toString()

        fun create(name: String, baseThemeId: String = ThemeCatalog.DEFAULT_ID): CustomTheme =
            CustomTheme(id = newId(), name = name, baseThemeId = baseThemeId).sanitized()
    }
}

/** Pure list operations for the saved-theme collection (all return a new list). */
object CustomThemes {
    const val MAX_THEMES = 100

    /** Adds [theme], replacing any theme with the same id. Fails (returns the input) beyond [MAX_THEMES]. */
    fun upsert(list: List<CustomTheme>, theme: CustomTheme): List<CustomTheme> {
        val clean = theme.sanitized()
        val exists = list.any { it.id == clean.id }
        return when {
            exists -> list.map { if (it.id == clean.id) clean else it }
            list.size >= MAX_THEMES -> list
            else -> list + clean
        }
    }

    fun delete(list: List<CustomTheme>, id: String): List<CustomTheme> = list.filterNot { it.id == id }

    fun rename(list: List<CustomTheme>, id: String, name: String): List<CustomTheme> =
        list.map { if (it.id == id) it.copy(name = name).sanitized() else it }

    fun toggleFavorite(list: List<CustomTheme>, id: String): List<CustomTheme> =
        list.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }

    /** Copies [id] under a new id and " copy" name, inserted right after the original. */
    fun duplicate(list: List<CustomTheme>, id: String): List<CustomTheme> {
        val src = list.firstOrNull { it.id == id } ?: return list
        if (list.size >= MAX_THEMES) return list
        val copy = src.copy(id = CustomTheme.newId(), name = "${src.name.take(CustomTheme.MAX_NAME - 5)} copy", favorite = false).sanitized()
        val index = list.indexOfFirst { it.id == id }
        return list.take(index + 1) + copy + list.drop(index + 1)
    }

    /** Resets every customisation of [id] to defaults but keeps its identity, name, base theme and background. */
    fun resetCustomisation(list: List<CustomTheme>, id: String): List<CustomTheme> =
        list.map {
            if (it.id != id) it else {
                val d = CustomTheme(id = it.id, name = it.name, baseThemeId = it.baseThemeId)
                d.copy(background = it.background, backgroundColor = it.backgroundColor, photoFile = it.photoFile, favorite = it.favorite).sanitized()
            }
        }
}
