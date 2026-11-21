package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** First-run introduction: what Lumora is, what it is not, and what it will and won't ask for. */
@Composable
fun OnboardingScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Lumora", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("Your Night. Your Time.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Point("A clock for your night stand", "Pick from 40 themes, then tap Start standby. The screen stays on and shows a large, calm clock on black.")
        Point("Make it yours", "Change the font, size, colour and position, or save your own theme with a colour or photo background.")
        Point("Not a lock screen", "Lumora is an app-level standby clock. It does not replace your lock screen or your phone's Always-On Display.")
        Point("Nothing required", "Lumora asks for no permissions to work. Notification previews are optional and off by default; if you turn them on, Android will ask you for notification access, and message text is only ever kept in memory.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun Point(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
