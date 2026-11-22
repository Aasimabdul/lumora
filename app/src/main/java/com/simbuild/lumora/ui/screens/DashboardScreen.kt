package com.simbuild.lumora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.service.NotificationAccess
import com.simbuild.lumora.ui.SectionCard
import com.simbuild.lumora.ui.clock.ThemeSurface
import com.simbuild.lumora.ui.clock.rememberClockFrame

@Composable
fun DashboardScreen(
    settings: AppSettings,
    customThemes: List<CustomTheme>,
    onStartStandby: () -> Unit,
    onOpenThemes: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeName = customThemes.firstOrNull { it.id == settings.themeId }?.name
        ?: ThemeCatalog.resolveOrDefault(settings.themeId).displayName
    val frame = rememberClockFrame(settings)
    val heroShape = RoundedCornerShape(20.dp)

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Lumora", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Your Night. Your Time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(heroShape)
                .background(Color.Black)
                .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))), heroShape),
        ) {
            ThemeSurface(settings.themeId, frame, customThemes, Modifier.fillMaxSize(), photoMaxEdge = 900)
            Text(
                themeName,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onStartStandby,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Start standby", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onOpenThemes, modifier = Modifier.weight(1f)) { Text("Change theme") }
            FilledTonalButton(onClick = onCustomize, modifier = Modifier.weight(1f)) { Text("Customize") }
        }
        Spacer(Modifier.height(8.dp))

        SectionCard("Right now") {
            StatusLine("Clock", if (settings.use24Hour) "24-hour" else "12-hour")
            StatusLine("Font", settings.font.label)
            val previewsInBuild = NotificationAccess.isAvailable(LocalContext.current)
            StatusLine("Notification previews", if (!previewsInBuild) "Not in this build" else if (settings.preview.enabled) "On" else "Off")
            StatusLine("Charging automation", if (settings.chargingAutomation) "On" else "Off")
        }

        Text(
            "Lumora is an app-level standby clock. It does not replace your lock screen or your phone's Always-On Display.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
