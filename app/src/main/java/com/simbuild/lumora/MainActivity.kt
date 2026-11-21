package com.simbuild.lumora

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simbuild.lumora.core.battery.ChargingEventDeduplicator
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.core.theme.CustomTheme
import com.simbuild.lumora.core.theme.CustomThemes
import com.simbuild.lumora.core.theme.ThemeCatalog
import com.simbuild.lumora.ui.clock.rememberBattery
import com.simbuild.lumora.ui.screens.CustomizeScreen
import com.simbuild.lumora.ui.screens.DashboardScreen
import com.simbuild.lumora.ui.screens.OnboardingScreen
import com.simbuild.lumora.ui.screens.PrivacyScreen
import com.simbuild.lumora.ui.screens.SettingsScreen
import com.simbuild.lumora.ui.screens.ThemesScreen
import kotlinx.coroutines.flow.Flow
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
    CUSTOMIZE("Customize"),
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

    private fun startStandby() = startActivity(Intent(this, StandbyActivity::class.java))

    @Composable
    private fun LumoraRoot() {
        val settingsFlow: Flow<AppSettings?> = app.settingsRepository.settings
        val loaded by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
        val customThemes by app.settingsRepository.customThemes
            .collectAsStateWithLifecycle(initialValue = emptyList<CustomTheme>())
        val settings = loaded
        if (settings == null) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        } else {
            LumoraContent(settings, customThemes)
        }
    }

    @Composable
    private fun LumoraContent(settings: AppSettings, customThemes: List<CustomTheme>) {
        val scope = rememberCoroutineScope()
        val repo = app.settingsRepository
        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
        val update: (AppSettings.() -> AppSettings) -> Unit = { change ->
            scope.launch { repo.update { it.change() } }
        }

        // Optional charging automation. Android only lets an app open its own screen from the
        // foreground, so this acts only while Lumora is on screen (RESUMED).
        val battery by rememberBattery()
        val latestSettings by rememberUpdatedState(settings)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val deduplicator = remember { ChargingEventDeduplicator() }
        LaunchedEffect(battery.isPluggedIn) {
            val fire = deduplicator.shouldLaunch(battery.isPluggedIn, SystemClock.elapsedRealtime())
            if (fire && latestSettings.chargingAutomation && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                startStandby()
            }
        }

        if (!settings.onboardingDone) {
            OnboardingScreen(onDone = { update { copy(onboardingDone = true) } })
            return
        }

        fun createTheme(theme: CustomTheme) {
            if (customThemes.size >= CustomThemes.MAX_THEMES) {
                Toast.makeText(this, "You can keep up to ${CustomThemes.MAX_THEMES} themes. Delete one first.", Toast.LENGTH_LONG).show()
                return
            }
            scope.launch {
                repo.updateBoth({ CustomThemes.upsert(it, theme) }, { it.copy(themeId = theme.id) })
            }
            tab = Tab.CUSTOMIZE
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
                            label = { Text(it.label, maxLines = 1) },
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
                    customThemes = customThemes,
                    onStartStandby = ::startStandby,
                    onOpenThemes = { tab = Tab.THEMES },
                    onCustomize = { tab = Tab.CUSTOMIZE },
                    modifier = m,
                )
                Tab.THEMES -> ThemesScreen(
                    settings = settings,
                    customThemes = customThemes,
                    onSelect = { id -> update { copy(themeId = id) } },
                    onToggleFavorite = { id ->
                        scope.launch {
                            if (AppSettings.isCustomId(id)) {
                                repo.updateCustomThemes { CustomThemes.toggleFavorite(it, id) }
                            } else {
                                repo.update {
                                    val next = if (id in it.favorites) it.favorites - id else it.favorites + id
                                    it.copy(favorites = next.take(AppSettings.MAX_FAVORITES).toSet())
                                }
                            }
                        }
                    },
                    onCreateFromTemplate = { templateId ->
                        val theme = if (templateId == "custom_photo_clock") {
                            CustomTheme.create("My photo clock", ThemeCatalog.DEFAULT_ID)
                        } else {
                            CustomTheme.create("My layout", templateId)
                        }
                        createTheme(theme)
                    },
                    onEditCustom = { id ->
                        update { copy(themeId = id) }
                        tab = Tab.CUSTOMIZE
                    },
                    onDuplicateCustom = { id -> scope.launch { repo.updateCustomThemes { CustomThemes.duplicate(it, id) } } },
                    onDeleteCustom = { id ->
                        scope.launch {
                            repo.updateBoth(
                                { CustomThemes.delete(it, id) },
                                { if (it.themeId == id) it.copy(themeId = ThemeCatalog.DEFAULT_ID) else it },
                            )
                            // Remove the deleted theme's photo (and any other photo nothing refers to any more).
                            app.photoStore.deleteUnused(customThemes.filter { it.id != id }.mapNotNull { it.photoFile }.toSet())
                        }
                    },
                    modifier = m,
                )
                Tab.CUSTOMIZE -> CustomizeScreen(
                    settings = settings,
                    customThemes = customThemes,
                    onSettings = update,
                    onSaveTheme = { theme -> scope.launch { repo.updateCustomThemes { CustomThemes.upsert(it, theme) } } },
                    onCreateTheme = ::createTheme,
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
