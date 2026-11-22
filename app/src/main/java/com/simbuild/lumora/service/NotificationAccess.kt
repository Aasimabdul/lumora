package com.simbuild.lumora.service

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Notification Listener access is a special access the user grants in Android settings.
 * It is unrelated to POST_NOTIFICATIONS, which only concerns Lumora's own notifications.
 */
object NotificationAccess {
    /**
     * True only in builds that declare the notification listener service (the "notifications" flavor).
     * The "standard" build does not include notification previews at all.
     */
    fun isAvailable(context: Context): Boolean = try {
        context.packageManager.getServiceInfo(ComponentName(context, LumoraNotificationListener::class.java), 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

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
