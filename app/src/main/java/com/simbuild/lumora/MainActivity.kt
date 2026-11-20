package com.simbuild.lumora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.ui.screens.DashboardScreen
import com.simbuild.lumora.ui.screens.PrivacyScreen
import com.simbuild.lumora.ui.screens.SettingsScreen
import com.simbuild.lumora.ui.screens.ThemesScreen
import kotlinx.coroutines.launch

private val LumoraColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0D1B2A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF13263A),
    onSurfaceVariant = Color(0xFFB8C7D6),
)

private enum class Tab(val label: String) {
    HOME("Home"),
    THEMES("Themes"),
    PRIVACY("Privacy"),
    SETTINGS("Settings"),
}

class MainActivity : ComponentActivity() {
    private val app get() = application as LumoraApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = LumoraColors) { LumoraRoot() } }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed notification access in system settings while we were away.
        app.refreshNotificationAccess()
    }

    @Composable
    private fun LumoraRoot() {
        val scope = rememberCoroutineScope()
        val settings by app.settingsRepository.settings
            .collectAsStateWithLifecycle(initialValue = AppSettings())
        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
        val update: (AppSettings.() -> AppSettings) -> Unit = { change ->
            scope.launch { app.settingsRepository.update { it.change() } }
        }

        Scaffold(
            containerColor = Color.Black,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0D1B2A)) {
                    Tab.entries.forEach {
                        NavigationBarItem(
                            selected = tab == it,
                            onClick = { tab = it },
                            icon = {},
                            label = { Text(it.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            },
        ) { padding ->
            val m = Modifier.padding(padding)
            when (tab) {
                Tab.HOME -> DashboardScreen(
                    settings = settings,
                    onStartStandby = { startActivity(Intent(this, StandbyActivity::class.java)) },
                    onOpenThemes = { tab = Tab.THEMES },
                    modifier = m,
                )
                Tab.THEMES -> ThemesScreen(
                    selectedId = settings.themeId,
                    settings = settings,
                    onSelect = { id -> update { copy(themeId = id) } },
                    modifier = m,
                )
                Tab.PRIVACY -> PrivacyScreen(
                    settings = settings,
                    onChange = update,
                    modifier = m,
                )
                Tab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onChange = update,
                    modifier = m,
                )
            }
        }
    }
}
