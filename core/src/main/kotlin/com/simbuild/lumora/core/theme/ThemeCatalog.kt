package com.simbuild.lumora.core.theme

enum class ThemeCategory(val label: String) {
    DIGITAL("Digital & typography"),
    ANALOG("Analog & watch"),
    FUTURISTIC("Futuristic & graphical"),
    NATURE("Nature & atmospheric"),
    CREATIVE("Creative & customizable"),
}

/**
 * Catalog metadata for one built-in theme. This is identity and grouping only;
 * the Compose renderer for each theme lives in the Android UI layer.
 */
data class ThemeDefinition(
    val id: String,
    val displayName: String,
    val category: ThemeCategory,
)

private fun slug(name: String): String =
    name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

private fun theme(name: String, category: ThemeCategory) =
    ThemeDefinition(slug(name), name, category)

object ThemeCatalog {
    const val DEFAULT_ID = "minimal_digital"

    val all: List<ThemeDefinition> = listOf(
        // Digital and typography
        theme("Minimal Digital", ThemeCategory.DIGITAL),
        theme("Neon Cyan", ThemeCategory.DIGITAL),
        theme("Flip Clock", ThemeCategory.DIGITAL),
        theme("Split Clock", ThemeCategory.DIGITAL),
        theme("Vertical Clock", ThemeCategory.DIGITAL),
        theme("Retro Digital", ThemeCategory.DIGITAL),
        theme("Dot Matrix", ThemeCategory.DIGITAL),
        theme("Large Typography", ThemeCategory.DIGITAL),
        theme("Gradient Digits", ThemeCategory.DIGITAL),
        theme("Glitch Clock", ThemeCategory.DIGITAL),
        // Analog and watch
        theme("Analog Classic", ThemeCategory.ANALOG),
        theme("Minimal Analog", ThemeCategory.ANALOG),
        theme("Circle Glow", ThemeCategory.ANALOG),
        theme("Roman Numerals", ThemeCategory.ANALOG),
        theme("Analog + Calendar", ThemeCategory.ANALOG),
        theme("Split Analog Dashboard", ThemeCategory.ANALOG),
        theme("Modern Watch", ThemeCategory.ANALOG),
        theme("Retro Clock Radio", ThemeCategory.ANALOG),
        // Futuristic and graphical
        theme("Cyberpunk", ThemeCategory.FUTURISTIC),
        theme("Aurora Glow", ThemeCategory.FUTURISTIC),
        theme("Wave Flow", ThemeCategory.FUTURISTIC),
        theme("Geometric Clock", ThemeCategory.FUTURISTIC),
        theme("Particle Glow", ThemeCategory.FUTURISTIC),
        theme("Speed Lines", ThemeCategory.FUTURISTIC),
        theme("Matrix Clock", ThemeCategory.FUTURISTIC),
        theme("HUD Dashboard", ThemeCategory.FUTURISTIC),
        // Nature and atmospheric
        theme("Moon Phase", ThemeCategory.NATURE),
        theme("Starry Night", ThemeCategory.NATURE),
        theme("Mountain Night", ThemeCategory.NATURE),
        theme("Forest", ThemeCategory.NATURE),
        theme("Sunset", ThemeCategory.NATURE),
        theme("Space Explorer", ThemeCategory.NATURE),
        theme("Ocean Waves", ThemeCategory.NATURE),
        theme("Aurora Sky", ThemeCategory.NATURE),
        // Creative and customizable
        theme("Colorful iOS-inspired Clock", ThemeCategory.CREATIVE),
        theme("Anime-inspired Clock", ThemeCategory.CREATIVE),
        theme("Dragon", ThemeCategory.CREATIVE),
        theme("Wolf", ThemeCategory.CREATIVE),
        theme("Custom Photo Clock", ThemeCategory.CREATIVE),
        theme("My Custom Layout", ThemeCategory.CREATIVE),
    )

    private val index: Map<String, ThemeDefinition> = all.associateBy { it.id }

    fun byId(id: String): ThemeDefinition? = index[id]

    /** Resolves a stored id, falling back to the default theme for unknown or null ids. */
    fun resolveOrDefault(id: String?): ThemeDefinition =
        id?.let(index::get) ?: index.getValue(DEFAULT_ID)

    /** Case-insensitive name search, optionally restricted to one category. */
    fun search(query: String, category: ThemeCategory? = null): List<ThemeDefinition> {
        val q = query.trim()
        return all.filter { t ->
            (category == null || t.category == category) &&
                (q.isEmpty() || t.displayName.contains(q, ignoreCase = true))
        }
    }
}
