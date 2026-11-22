package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.settings.ClockFont
import com.simbuild.lumora.ui.clock.toFamily

/** Grid of font cards; every card previews a time in its own font so the choice is visible before tapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontPicker(selected: ClockFont, modifier: Modifier = Modifier, onSelect: (ClockFont) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    FlowRow(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClockFont.entries.forEach { font ->
            val isSelected = font == selected
            Column(
                Modifier
                    .width(104.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
                    .clickable(role = Role.RadioButton) { onSelect(font) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = "${font.label} font" + if (isSelected) ", selected" else ""
                    }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("12:45", fontFamily = font.toFamily(), fontSize = 18.sp, maxLines = 1, softWrap = false)
                Text(
                    font.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
