package com.simbuild.lumora.ui.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.simbuild.lumora.core.battery.BatteryState
import com.simbuild.lumora.core.clock.ClockFormatter
import com.simbuild.lumora.core.clock.FormattedTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.simbuild.lumora.core.settings.AppSettings
import com.simbuild.lumora.R
import com.simbuild.lumora.core.settings.ClockFont
import com.simbuild.lumora.core.settings.ClockWeight
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Everything a theme needs to draw one frame. Built from live time and battery data. */
data class ClockFrame(
    val zoned: ZonedDateTime,
    val time: FormattedTime,
    val weekday: String,
    val date: String,
    val battery: BatteryState,
    val showDate: Boolean,
    val showBattery: Boolean,
    /** User clock-size multiplier (applied by [Stage]). */
    val scale: Float = 1f,
    /** User multiplier for date / battery text. */
    val infoScale: Float = 1f,
    /** -1 left .. 1 right. */
    val positionX: Float = 0f,
    /** -1 top .. 1 bottom. */
    val positionY: Float = 0f,
    /** User text-colour override; null = each theme uses its own colours. */
    val tint: Color? = null,
    val font: FontFamily = FontFamily.Default,
    /** User weight override; null = each theme uses its own weight. */
    val weightOverride: FontWeight? = null,
    /** True when drawn over a user photo: themes must not paint their own background. */
    val overlay: Boolean = false,
)

/** Main text/ink colour: the user's override if set, otherwise the theme's [default]. */
fun ClockFrame.ink(default: Color): Color = tint ?: default

/** Accent colour: the user's override if set, otherwise the theme's [default]. */
fun ClockFrame.accent(default: Color): Color = tint ?: default

/** Font weight: the user's override if set, otherwise the theme's [default]. */
fun ClockFrame.weight(default: FontWeight): FontWeight = weightOverride ?: default

internal fun parseHex(hex: String?): Color? =
    hex?.takeIf { it.length == 7 && it[0] == '#' }?.drop(1)?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

internal fun ClockFont.toFamily(): FontFamily = when (this) {
    ClockFont.DEFAULT -> FontFamily.Default
    ClockFont.SANS -> FontFamily.SansSerif
    ClockFont.SERIF -> FontFamily.Serif
    ClockFont.MONO -> FontFamily.Monospace
    ClockFont.CURSIVE -> FontFamily.Cursive
    ClockFont.AUDIOWIDE -> bundled(R.font.audiowide)
    ClockFont.MICHROMA -> bundled(R.font.michroma)
    ClockFont.BEBAS -> bundled(R.font.bebas_neue)
    ClockFont.RIGHTEOUS -> bundled(R.font.righteous)
    ClockFont.ANTON -> bundled(R.font.anton)
    ClockFont.POIRET -> bundled(R.font.poiret_one)
    ClockFont.PACIFICO -> bundled(R.font.pacifico)
    ClockFont.LOBSTER -> bundled(R.font.lobster)
    ClockFont.TECH_MONO -> bundled(R.font.share_tech_mono)
    ClockFont.TERMINAL -> bundled(R.font.vt323)
    ClockFont.PIXEL -> bundled(R.font.press_start_2p)
}

private fun bundled(@androidx.annotation.FontRes id: Int): FontFamily = FontFamily(Font(id))

internal fun ClockWeight.toWeight(): FontWeight? = when (this) {
    ClockWeight.THEME -> null
    ClockWeight.LIGHT -> FontWeight.Light
    ClockWeight.NORMAL -> FontWeight.Normal
    ClockWeight.BOLD -> FontWeight.Bold
    ClockWeight.BLACK -> FontWeight.Black
}

/** Current instant, refreshed on every minute boundary and on time / zone / locale changes. */
@Composable
fun rememberNow(): State<Instant> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val now = remember { mutableStateOf(Instant.now()) }
    var systemChanges by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                now.value = Instant.now()
                systemChanges++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Runs only while the screen is at least STARTED; restarts after a system time change.
    LaunchedEffect(lifecycleOwner, systemChanges) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = Instant.now()
                now.value = current
                delay(ClockFormatter.millisUntilNextMinute(current, ZoneId.systemDefault()))
            }
        }
    }
    return now
}

/** Battery level and charging status from the sticky ACTION_BATTERY_CHANGED broadcast. */
@Composable
fun rememberBattery(): State<BatteryState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(BatteryState.UNKNOWN) }

    DisposableEffect(context) {
        fun apply(intent: Intent?) {
            if (intent == null) return
            state.value = BatteryState.from(
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
                androidStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = apply(intent)
        }
        apply(
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            ),
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
fun rememberClockFrame(settings: AppSettings): ClockFrame {
    val now by rememberNow()
    val battery by rememberBattery()
    val locale = LocalConfiguration.current.locales[0]
    return remember(now, battery, locale, settings) {
        val zone = ZoneId.systemDefault()
        ClockFrame(
            zoned = now.atZone(zone),
            time = ClockFormatter.time(now, zone, locale, settings.use24Hour),
            weekday = ClockFormatter.weekday(now, zone, locale),
            date = ClockFormatter.date(now, zone, locale, settings.dateStyle),
            battery = battery,
            showDate = settings.showDate,
            showBattery = settings.showBattery,
            scale = settings.clockScale,
            infoScale = settings.infoScale,
            positionX = settings.positionX,
            positionY = settings.positionY,
            tint = parseHex(settings.textColor),
            font = settings.font.toFamily(),
            weightOverride = settings.weight.toWeight(),
        )
    }
}
