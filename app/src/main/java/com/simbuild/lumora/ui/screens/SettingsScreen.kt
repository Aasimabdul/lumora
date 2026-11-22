package com.simbuild.lumora.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.clock.DateStyle
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.settings.ChargingEndBehavior
import com.simbuild.lumora.ui.SectionCard

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings.() -> AppSettings)) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionCard("Clock") {
            SwitchRow("24-hour time", settings.use24Hour) { onChange { copy(use24Hour = it) } }
            SwitchRow("Show date", settings.showDate) { onChange { copy(showDate = it) } }
            SwitchRow("Show battery", settings.showBattery) { onChange { copy(showBattery = it) } }
            Text("Date format", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateStyle.entries.forEach { style ->
                    FilterChip(
                        selected = settings.dateStyle == style,
                        onClick = { onChange { copy(dateStyle = style) } },
                        label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }

        SectionCard("Clock font") {
            Text(
                "Applies to every built-in theme. Size, weight and colour are in the Customize tab, and your own saved themes keep their own font.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FontPicker(settings.font) { f -> onChange { copy(font = f) } }
        }

        SectionCard("Charging automation") {
            SwitchRow("Start standby when charging begins", settings.chargingAutomation) { onChange { copy(chargingAutomation = it) } }
            Text(
                "Off by default. This works while Lumora is open on screen: plug in your charger and the standby clock opens. " +
                    "Android does not let apps open themselves from the background, so Lumora cannot start on its own when it is closed, " +
                    "and a phone's own settings or battery saver may still interrupt it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (settings.chargingAutomation) {
                Text("When the charger is unplugged", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChargingEndBehavior.entries.forEach { b ->
                        FilterChip(
                            selected = settings.chargingEnd == b,
                            onClick = { onChange { copy(chargingEnd = b) } },
                            label = { Text(b.label) },
                        )
                    }
                }
            }
        }

        AboutSection(onShowIntro = { onChange { copy(onboardingDone = false) } })
    }
}

private const val REPO_URL = "https://github.com/Aasimabdul/lumora"

@Composable
private fun AboutSection(onShowIntro: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
    SectionCard("About Lumora") {
        Text("Version $version", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        Text(
            "Lumora is an app-level standby clock. It does not replace your lock screen or your phone's Always-On Display. " +
                "It is open source under the MIT License. It uses Kotlin, Jetpack Compose and AndroidX (Apache License 2.0) and " +
                "bundled fonts under the SIL Open Font License; the full list is in THIRD_PARTY_NOTICES.md in the repository.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No app can open links on this device.", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Source code") }
            OutlinedButton(onClick = onShowIntro) { Text("Show introduction") }
        }
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
