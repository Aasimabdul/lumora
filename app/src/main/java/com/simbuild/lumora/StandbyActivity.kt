package com.simbuild.lumora

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.ui.NotificationPreviewCard
import com.simbuild.lumora.ui.clock.ThemeRenderers
import com.simbuild.lumora.ui.clock.rememberClockFrame
import kotlinx.coroutines.delay

/**
 * Full-screen standby clock. An app-level screen: it does not replace the system lock screen
 * or a manufacturer Always-On Display.
 *
 * The screen is kept on with FLAG_KEEP_SCREEN_ON (a window flag, not a wake lock) only while
 * this activity is visible. Tap anywhere to reveal an Exit button; Back also exits.
 */
class StandbyActivity : ComponentActivity() {
    private val app get() = application as LumoraApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent { StandbyScreen(onExit = { finish() }) }
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        app.refreshNotificationAccess()
        app.previewController.setStandbyVisible(true)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onStop() {
        // Leaving standby discards any visible preview and stops showing new ones.
        app.previewController.setStandbyVisible(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showSystemBars()
        super.onStop()
    }

    private fun hideSystemBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    @Composable
    private fun StandbyScreen(onExit: () -> Unit) {
        val settings by app.settingsRepository.settings
            .collectAsStateWithLifecycle(initialValue = AppSettings())
        val preview by app.previewController.state.collectAsStateWithLifecycle()
        val frame = rememberClockFrame(settings)
        var showExit by remember { mutableStateOf(false) }

        // The Exit button hides itself again after a few seconds.
        LaunchedEffect(showExit) {
            if (showExit) {
                delay(5_000)
                showExit = false
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showExit = !showExit },
        ) {
            ThemeRenderers.Render(settings.themeId, frame, Modifier.fillMaxSize())
            NotificationPreviewCard(
                state = preview,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
            if (showExit) {
                Button(
                    onClick = onExit,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                ) { Text("Exit standby") }
            }
        }
    }
}
