package com.simbuild.lumora.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.simbuild.lumora.LumoraApp
import com.simbuild.lumora.core.notifications.IncomingNotification

/**
 * Observes notifications only to feed the in-memory preview controller.
 * It never replies to, dismisses, or modifies notifications, and never logs their content.
 * Whether anything is shown is decided by the controller (enabled + access + standby visible).
 */
class LumoraNotificationListener : NotificationListenerService() {
    private val app get() = application as LumoraApp

    override fun onListenerConnected() {
        app.previewController.setNotificationAccessGranted(true)
    }

    override fun onListenerDisconnected() {
        app.previewController.setNotificationAccessGranted(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
        app.previewController.onNotificationPosted(
            IncomingNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appLabel(sbn.packageName),
                sender = title,
                text = text,
                postedAtMillis = sbn.postTime,
                isOngoing = sbn.isOngoing,
                isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            ),
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        app.previewController.onNotificationRemoved(sbn.key)
    }

    // Falls back to the package name when Android's package visibility hides the app.
    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }
}
