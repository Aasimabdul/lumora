package com.simbuild.lumora

import android.app.Application
import com.simbuild.lumora.core.notifications.CoroutineScheduler
import com.simbuild.lumora.core.notifications.MonotonicTimeSource
import com.simbuild.lumora.core.notifications.NotificationPreviewController
import com.simbuild.lumora.data.SettingsRepository
import com.simbuild.lumora.service.NotificationAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application-wide singletons (manual DI; the app is small enough not to need a framework). */
class LumoraApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var previewController: NotificationPreviewController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        previewController = NotificationPreviewController(CoroutineScheduler(appScope), MonotonicTimeSource)
        appScope.launch {
            settingsRepository.settings.collect { previewController.updateSettings(it.preview) }
        }
        refreshNotificationAccess()
    }

    /** Re-reads whether the user has granted notification access in Android settings. */
    fun refreshNotificationAccess() {
        previewController.setNotificationAccessGranted(NotificationAccess.isEnabled(this))
    }
}
