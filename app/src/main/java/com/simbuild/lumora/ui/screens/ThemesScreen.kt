package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.core.theme.ThemeCategory
import com.simbuild.lumora.core.theme.ThemeDefinition
import com.simbuild.lumora.ui.clock.ThemeRenderers
import com.simbuild.lumora.ui.clock.implementedThemeIds
import com.simbuild.lumora.ui.clock.rememberClockFrame

/**
 * Searchable, categorised gallery. Themes without a renderer are listed but clearly marked
 * "Coming soon" and cannot be selected.
 */
@Composable
fun ThemesScreen(
    selectedId: String,
    settings: AppSettings,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { ThemeCatalog.search(query) }
    val grouped = remember(results) { ThemeCategory.entries.associateWith { c -> results.filter { it.category == c } } }
    val frame = rememberClockFrame(settings)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Themes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
                Text(
                    "${implementedThemeIds.size} of ${ThemeCatalog.all.size} themes are available so far.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search themes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No themes match \"$query\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        grouped.forEach { (category, themes) ->
            if (themes.isEmpty()) return@forEach
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_${category.name}") {
                Text(category.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            items(themes, key = { it.id }) { theme ->
                ThemeCard(theme, theme.id == selectedId, settings, frame, onSelect)
            }
        }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun ThemeCard(
    theme: ThemeDefinition,
    selected: Boolean,
    settings: AppSettings,
    frame: com.simbuild.lumora.ui.clock.ClockFrame,
    onSelect: (String) -> Unit,
) {
    val available = theme.id in implementedThemeIds
    val shape = RoundedCornerShape(12.dp)
    val state = when {
        selected -> "selected"
        available -> "available"
        else -> "coming soon"
    }
    Column(
        Modifier
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF24384D), shape)
            .then(if (available) Modifier.clickable { onSelect(theme.id) } else Modifier)
            .semantics {
                contentDescription = "${theme.displayName}, $state"
                this.selected = selected
            },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(Color.Black), contentAlignment = Alignment.Center) {
            if (available) {
                ThemeRenderers.Render(theme.id, frame)
            } else {
                Text("Coming soon", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            if (selected) "${theme.displayName} ✓" else theme.displayName,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
