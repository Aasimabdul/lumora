package com.simbuild.lumora.core.theme

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Versioned JSON storage format for saved custom themes. Decoding never throws: corrupted JSON
 * yields an empty list, and individual invalid entries are dropped while valid ones are kept.
 */
object CustomThemeCodec {
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun encode(themes: List<CustomTheme>): String {
        val array = JsonArray(themes.map { json.encodeToJsonElement(it.sanitized()) })
        val root = buildJsonObject {
            put("version", VERSION)
            put("themes", array)
        }
        return root.toString()
    }

    fun decode(raw: String?): List<CustomTheme> {
        if (raw.isNullOrBlank()) return emptyList()
        val root: JsonObject = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }
        val version = root["version"]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() } ?: VERSION
        if (version > VERSION) return emptyList() // written by a newer app version; don't guess
        val items = runCatching { root["themes"]?.jsonArray }.getOrNull() ?: return emptyList()
        val seen = HashSet<String>()
        val result = ArrayList<CustomTheme>()
        for (element in items) {
            val theme = try {
                json.decodeFromJsonElement<CustomTheme>(element).sanitized()
            } catch (_: Exception) {
                continue
            }
            if (seen.add(theme.id)) result += theme
            if (result.size >= CustomThemes.MAX_THEMES) break
        }
        return result
    }
}
