package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.settings.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings.() -> AppSettings)) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        SwitchRow("24-hour time", settings.use24Hour) { onChange { copy(use24Hour = it) } }
        SwitchRow("Show date", settings.showDate) { onChange { copy(showDate = it) } }
        SwitchRow("Show battery", settings.showBattery) { onChange { copy(showBattery = it) } }

        Text("Date format", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateStyle.entries.forEach { style ->
                FilterChip(
                    selected = settings.dateStyle == style,
                    onClick = { onChange { copy(dateStyle = style) } },
                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Text("About", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text(
            "Lumora is open source under the MIT License. Third-party components are listed in THIRD_PARTY_NOTICES.md in the repository.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
