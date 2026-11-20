package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.ui.clock.ThemeRenderers
import com.simbuild.lumora.ui.clock.rememberClockFrame

@Composable
fun DashboardScreen(
    settings: AppSettings,
    onStartStandby: () -> Unit,
    onOpenThemes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = ThemeCatalog.resolveOrDefault(settings.themeId)
    val frame = rememberClockFrame(settings)
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Lumora", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Your Night. Your Time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Selected theme: ${theme.displayName}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
        ) {
            ThemeRenderers.Render(settings.themeId, frame)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStartStandby, modifier = Modifier.fillMaxWidth()) { Text("Start standby") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenThemes, modifier = Modifier.fillMaxWidth()) { Text("Change theme") }
        Spacer(Modifier.height(16.dp))
        Text(
            "Lumora is an app-level standby clock. It does not replace your lock screen or your phone's Always-On Display.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
