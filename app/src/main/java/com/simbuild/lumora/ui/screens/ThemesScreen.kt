package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.core.theme.ThemeCategory
import com.simbuild.lumora.core.theme.ThemeDefinition
import com.simbuild.lumora.ui.clock.ClockFrame
import com.simbuild.lumora.ui.clock.ThemeSurface
import com.simbuild.lumora.ui.clock.rememberClockFrame

private const val FILTER_ALL = "all"
private const val FILTER_FAVORITES = "favorites"
private const val FILTER_MINE = "mine"

/**
 * Searchable gallery of all built-in themes plus the user's saved themes, with favourites and
 * category filters. Every theme is selectable; the two "template" themes start a personal theme.
 */
@Composable
fun ThemesScreen(
    settings: AppSettings,
    customThemes: List<CustomTheme>,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateFromTemplate: (String) -> Unit,
    onEditCustom: (String) -> Unit,
    onDuplicateCustom: (String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var deleting by remember { mutableStateOf<CustomTheme?>(null) }
    val frame = rememberClockFrame(settings)

    val matchesQuery: (String) -> Boolean = { name -> query.isBlank() || name.contains(query.trim(), ignoreCase = true) }
    val builtIn = ThemeCatalog.all.filter { matchesQuery(it.displayName) }
    val mine = customThemes.filter { matchesQuery(it.name) }
    val showMine = filter == FILTER_ALL || filter == FILTER_MINE || filter == FILTER_FAVORITES
    val mineShown = when (filter) {
        FILTER_FAVORITES -> mine.filter { it.favorite }
        FILTER_ALL, FILTER_MINE -> mine
        else -> emptyList()
    }
    val builtInShown = when (filter) {
        FILTER_MINE -> emptyList()
        FILTER_FAVORITES -> builtIn.filter { it.id in settings.favorites }
        FILTER_ALL -> builtIn
        else -> builtIn.filter { it.category.name == filter }
    }
    val total = ThemeCatalog.all.size

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
                    "$total built-in themes" + if (customThemes.isNotEmpty()) " · ${customThemes.size} of your own" else "",
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
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filter == FILTER_ALL, { filter = FILTER_ALL }, label = { Text("All") })
                    FilterChip(filter == FILTER_FAVORITES, { filter = FILTER_FAVORITES }, label = { Text("Favourites") })
                    FilterChip(filter == FILTER_MINE, { filter = FILTER_MINE }, label = { Text("My themes") })
                    ThemeCategory.entries.forEach { c ->
                        FilterChip(filter == c.name, { filter = c.name }, label = { Text(c.label) })
                    }
                }
            }
        }

        if (mineShown.isEmpty() && builtInShown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    when {
                        filter == FILTER_MINE -> "You haven't saved a theme yet. Open Customize and choose “Save as my theme”, or tap Custom Photo Clock to start with a photo."
                        filter == FILTER_FAVORITES -> "No favourites yet. Tap the star on a theme to add it here."
                        else -> "No themes match \"$query\"."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showMine && mineShown.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_mine") {
                Text("My themes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            items(mineShown, key = { it.id }) { t ->
                GalleryCard(
                    name = t.name,
                    selected = t.id == settings.themeId,
                    favorite = t.favorite,
                    onClick = { onSelect(t.id) },
                    onFavorite = { onToggleFavorite(t.id) },
                    thumbnail = { ThemeSurface(t.id, frame, customThemes, Modifier.fillMaxSize(), photoMaxEdge = 400) },
                    actions = {
                        TextButton(onClick = { onEditCustom(t.id) }) { Text("Edit") }
                        TextButton(onClick = { onDuplicateCustom(t.id) }) { Text("Copy") }
                        TextButton(onClick = { deleting = t }) { Text("Delete") }
                    },
                )
            }
        }

        val groups = ThemeCategory.entries.associateWith { c -> builtInShown.filter { it.category == c } }
        groups.forEach { (category, themes) ->
            if (themes.isEmpty()) return@forEach
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_${category.name}") {
                Text(category.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            items(themes, key = { it.id }) { theme -> BuiltInCard(theme, settings, frame, customThemes, onSelect, onToggleFavorite, onCreateFromTemplate) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.padding(bottom = 16.dp)) }
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${t.name}”?") },
            text = { Text("This removes the theme and its photo from this device. It can't be undone.") },
            confirmButton = { TextButton(onClick = { deleting = null; onDeleteCustom(t.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BuiltInCard(
    theme: ThemeDefinition,
    settings: AppSettings,
    frame: ClockFrame,
    customThemes: List<CustomTheme>,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateFromTemplate: (String) -> Unit,
) {
    val template = theme.id in TEMPLATE_CARDS
    GalleryCard(
        name = theme.displayName,
        selected = theme.id == settings.themeId,
        favorite = theme.id in settings.favorites,
        onClick = { if (template) onCreateFromTemplate(theme.id) else onSelect(theme.id) },
        onFavorite = { onToggleFavorite(theme.id) },
        thumbnail = { ThemeSurface(theme.id, frame, customThemes, Modifier.fillMaxSize()) },
        caption = if (template) TEMPLATE_CARDS.getValue(theme.id) else null,
    )
}

private val TEMPLATE_CARDS = mapOf(
    "custom_photo_clock" to "Tap to make your own with a photo",
    "my_custom_layout" to "Tap to make your own layout",
)

@Composable
private fun GalleryCard(
    name: String,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    thumbnail: @Composable () -> Unit,
    caption: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF24384D), shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(Color.Black)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = "$name, " + if (selected) "selected" else "tap to use"
                    this.selected = selected
                },
            contentAlignment = Alignment.Center,
        ) { thumbnail() }
        Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (selected) "$name ✓" else name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                )
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.semantics { contentDescription = if (favorite) "Remove $name from favourites" else "Add $name to favourites" },
            ) {
                Text(if (favorite) "★" else "☆", color = if (favorite) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actions != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { actions() }
    }
}
