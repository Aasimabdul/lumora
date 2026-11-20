package com.simbuild.lumora.core.battery

enum class ChargeStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN,
    ;

    companion object {
        // Values of android.os.BatteryManager.BATTERY_STATUS_*; duplicated here so this
        // module stays free of Android dependencies. The Android layer must keep them in sync.
        fun fromAndroid(status: Int): ChargeStatus = when (status) {
            2 -> CHARGING
            3 -> DISCHARGING
            4 -> NOT_CHARGING
            5 -> FULL
            else -> UNKNOWN
        }
    }
}

data class BatteryState(val percent: Int?, val status: ChargeStatus) {
    /** True while external power is connected (actively charging, or full on the charger). */
    val isPluggedIn: Boolean get() = status == ChargeStatus.CHARGING || status == ChargeStatus.FULL

    companion object {
        val UNKNOWN = BatteryState(null, ChargeStatus.UNKNOWN)

        /** Maps the raw ACTION_BATTERY_CHANGED extras. Invalid readings yield a null percent. */
        fun from(level: Int, scale: Int, androidStatus: Int): BatteryState {
            val percent = if (level < 0 || scale <= 0) {
                null
            } else {
                ((level * 100.0) / scale).let { Math.round(it).toInt() }.coerceIn(0, 100)
            }
            return BatteryState(percent, ChargeStatus.fromAndroid(androidStatus))
        }
    }
}

/**
 * Decides when charging should trigger an automatic standby launch.
 *
 * - The first observation only seeds the baseline (the sticky broadcast delivered on
 *   registration must not launch anything).
 * - Only a not-charging -> charging transition fires; repeated "charging" events do not.
 * - A cooldown suppresses launches caused by flapping connections.
 */
class ChargingEventDeduplicator(private val cooldownMillis: Long = 5_000L) {
    private var lastPluggedIn: Boolean? = null
    private var lastLaunchAt: Long? = null

    @Synchronized
    fun shouldLaunch(pluggedIn: Boolean, nowMillis: Long): Boolean {
        val previous = lastPluggedIn
        lastPluggedIn = pluggedIn
        if (previous == null || previous || !pluggedIn) return false
        val last = lastLaunchAt
        if (last != null && nowMillis - last < cooldownMillis) return false
        lastLaunchAt = nowMillis
        return true
    }
}
