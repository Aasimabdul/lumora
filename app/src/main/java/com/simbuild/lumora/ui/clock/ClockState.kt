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
import com.simbuild.lumora.core.settings.AppSettings
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Everything a theme needs to draw one frame. Built from live time and battery data. */
class ClockFrame(
    val zoned: ZonedDateTime,
    val time: FormattedTime,
    val weekday: String,
    val date: String,
    val battery: BatteryState,
    val showDate: Boolean,
    val showBattery: Boolean,
)

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
    return remember(now, battery, locale, settings.use24Hour, settings.dateStyle, settings.showDate, settings.showBattery) {
        val zone = ZoneId.systemDefault()
        ClockFrame(
            zoned = now.atZone(zone),
            time = ClockFormatter.time(now, zone, locale, settings.use24Hour),
            weekday = ClockFormatter.weekday(now, zone, locale),
            date = ClockFormatter.date(now, zone, locale, settings.dateStyle),
            battery = battery,
            showDate = settings.showDate,
            showBattery = settings.showBattery,
        )
    }
}
