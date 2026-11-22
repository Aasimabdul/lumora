package com.simbuild.lumora.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.simbuild.lumora.core.notifications.NotificationDisplayDuration
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.LumoraApp
import com.simbuild.lumora.service.NotificationAccess
import com.simbuild.lumora.ui.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Permissions & privacy for the one optional feature that needs special access.
 * Lumora's other features need no permissions at all.
 */
@Composable
fun PrivacyScreen(
    settings: AppSettings,
    onChange: ((AppSettings.() -> AppSettings)) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var accessEnabled by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }

    // Re-check when returning from Android's Notification access screen.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessEnabled = NotificationAccess.isEnabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (!NotificationAccess.isAvailable(context)) {
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Permissions & Privacy", style = MaterialTheme.typography.headlineMedium)
            SectionCard("Notification previews") {
                Text(
                    "This build of Lumora does not include notification previews, so it never asks for Notification access. " +
                        "That is why Google Play Protect lets it install. " +
                        "To use previews, install the \"notifications\" build instead (see docs/INSTALL.md in the repository).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SectionCard("Permissions") {
                Text(
                    "Lumora requests no permissions: no notifications, photos, contacts, SMS, location, microphone, storage, overlay or accessibility access. " +
                        "Background photos are chosen with the system photo picker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        return
    }

    val p = settings.preview
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Permissions & Privacy", style = MaterialTheme.typography.headlineMedium)

        Text("Notification previews (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            "When on, Lumora can show a temporary card on the standby screen for notifications from other apps. " +
                "That can include sender names and message text, depending on what each app shares. " +
                "Some apps hide their content, so previews may be generic. " +
                "Lumora never replies to, dismisses or changes your notifications, and never saves message text.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SwitchRow("Show notification previews", p.enabled) { on -> onChange { copy(preview = preview.copy(enabled = on)) } }

        Text(
            "Notification access: " + if (accessEnabled) "granted" else "not granted",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            if (accessEnabled) {
                "You can revoke it any time in Android Settings → Notifications → Notification access (or Device & app notifications → Notification access)."
            } else {
                "Previews stay off until you grant access in Android settings. If you decline, everything else in Lumora keeps working."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                if (!NotificationAccess.openSettings(context)) {
                    Toast.makeText(context, "Couldn't open Android settings on this device.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (accessEnabled) "Open Notification access settings" else "Grant notification access") }

        Text("What to show", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        SwitchRow("Show sender name", p.showSender, enabled = p.enabled) { on -> onChange { copy(preview = preview.copy(showSender = on)) } }
        SwitchRow("Show message text", p.showText && !p.privacyMode, enabled = p.enabled && !p.privacyMode) { on ->
            onChange { copy(preview = preview.copy(showText = on)) }
        }
        SwitchRow("Privacy mode (hide message text)", p.privacyMode, enabled = p.enabled) { on ->
            onChange { copy(preview = preview.copy(privacyMode = on)) }
        }

        Text("Notification display duration", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationDisplayDuration.entries.forEach { d ->
                FilterChip(
                    selected = p.duration == d,
                    enabled = p.enabled,
                    onClick = { onChange { copy(preview = preview.copy(duration = d)) } },
                    label = { Text("${d.seconds}s") },
                )
            }
        }

        Text("Which apps can show previews", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text(
            if (p.allowedPackages.isEmpty()) "All apps (default)." else "Only ${p.allowedPackages.size} chosen app(s).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        var choosing by remember { mutableStateOf(false) }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { choosing = !choosing }, enabled = p.enabled) { Text(if (choosing) "Hide app list" else "Choose apps") }
            if (p.allowedPackages.isNotEmpty()) {
                OutlinedButton(onClick = { onChange { copy(preview = preview.copy(allowedPackages = emptySet())) } }) { Text("Allow all apps") }
            }
        }
        if (choosing) {
            val apps by produceState(initialValue = emptyList<LaunchableApp>()) {
                value = withContext(Dispatchers.IO) { launchableApps(context) }
            }
            Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                apps.forEach { app ->
                    SwitchRow(app.label, app.packageName in p.allowedPackages) { on ->
                        onChange {
                            val next = if (on) preview.allowedPackages + app.packageName else preview.allowedPackages - app.packageName
                            copy(preview = preview.copy(allowedPackages = next))
                        }
                    }
                }
            }
        }

        Text("Current preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text(
            "Previews are held in memory only and disappear on their own. You can also clear the one on screen right now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { (context.applicationContext as LumoraApp).previewController.clear() },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Clear current preview") }

        Text("Other access", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text(
            "Lumora requests no other permissions: no photos, contacts, SMS, location, microphone, storage, overlay or accessibility access.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private class LaunchableApp(val label: String, val packageName: String)

/** Apps with a launcher icon, which is all the package visibility Lumora declares needs. */
private fun launchableApps(context: android.content.Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { LaunchableApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
