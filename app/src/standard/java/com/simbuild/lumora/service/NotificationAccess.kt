package com.simbuild.lumora.service

import android.content.Context

/**
 * "standard" flavor: notification previews are not part of this build, so there is no notification
 * listener class, no manifest declaration and no code that touches Notification access. Google Play
 * Protect blocks sideloaded apps that can request it. The "notifications" flavor (src/notifications)
 * has the real implementation with the same API.
 */
object NotificationAccess {
    @Suppress("UNUSED_PARAMETER")
    fun isAvailable(context: Context): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun isEnabled(context: Context): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun openSettings(context: Context): Boolean = false
}
