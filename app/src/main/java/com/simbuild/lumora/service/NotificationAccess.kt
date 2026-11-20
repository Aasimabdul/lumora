package com.simbuild.lumora.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Notification Listener access is a special access the user grants in Android settings.
 * It is unrelated to POST_NOTIFICATIONS, which only concerns Lumora's own notifications.
 */
object NotificationAccess {
    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Opens the system Notification access screen. Returns false if no settings screen could be opened. */
    fun openSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                // try the next candidate
            }
        }
        return false
    }
}
