package com.simbuild.lumora.core.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NotificationDisplayDuration(val seconds: Int) {
    S10(10),
    S15(15),
    S20(20),
    S25(25),
    S30(30),
    ;

    val millis: Long get() = seconds * 1000L

    companion object {
        val DEFAULT = S15

        /** Unknown or corrupted stored values fall back to the 15 s default. */
        fun fromSeconds(seconds: Int?): NotificationDisplayDuration =
            entries.firstOrNull { it.seconds == seconds } ?: DEFAULT
    }
}

fun interface Cancellable {
    fun cancel()
}

/** Abstraction over delayed work so expiry logic is deterministic under test. */
interface Scheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): Cancellable
}

fun interface TimeSource {
    fun nowMillis(): Long
}

/** Monotonic production time source (not affected by wall-clock changes). */
object MonotonicTimeSource : TimeSource {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000L
}

/** Production scheduler: one cancellable coroutine per timer, owned by [scope]. */
class CoroutineScheduler(private val scope: CoroutineScope) : Scheduler {
    override fun schedule(delayMillis: Long, action: () -> Unit): Cancellable {
        val job = scope.launch {
            delay(delayMillis)
            action()
        }
        return Cancellable { job.cancel() }
    }
}

/**
 * A notification as observed by the listener service. Text fields are transient and
 * never persisted; [toString] is redacted so they cannot leak through logs.
 */
class IncomingNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String?,
    val text: String?,
    val postedAtMillis: Long,
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false,
) {
    internal fun fingerprint(): Int = listOf(appName, sender, text, postedAtMillis).hashCode()

    override fun toString() = "IncomingNotification(key=<redacted>, package=$packageName)"
}

/** What the UI may display, already filtered by the privacy settings. */
class PreviewCard(
    val key: String,
    val appName: String,
    val sender: String?,
    val text: String?,
    val postedAtMillis: Long,
) {
    override fun toString() = "PreviewCard(app=$appName, <content redacted>)"
}

data class PreviewSettings(
    val enabled: Boolean = false,
    val duration: NotificationDisplayDuration = NotificationDisplayDuration.DEFAULT,
    val showText: Boolean = true,
    val showSender: Boolean = true,
    val privacyMode: Boolean = false,
    /** Empty means every app is allowed; otherwise only these package names. */
    val allowedPackages: Set<String> = emptySet(),
) {
    internal fun privacyRelevantEquals(other: PreviewSettings) =
        showText == other.showText && showSender == other.showSender && privacyMode == other.privacyMode

    internal fun allows(packageName: String) =
        allowedPackages.isEmpty() || packageName in allowedPackages
}

sealed interface PreviewState {
    data object Hidden : PreviewState

    class Showing(val card: PreviewCard, val expiresAtMillis: Long) : PreviewState {
        override fun toString() = "Showing(card=$card, expiresAt=$expiresAtMillis)"
    }
}

enum class PostOutcome { SHOWN, UPDATED, DUPLICATE_IGNORED, IGNORED }

/**
 * Policy (documented in docs/ARCHITECTURE.md):
 *  - At most one card; the newest eligible notification replaces the current one.
 *  - A genuine update (same key, different content) replaces the card and restarts the timer.
 *    An identical repeat event is ignored and does NOT restart the timer.
 *  - Removal of the source notification hides its card. The system notification itself is never touched.
 *  - Nothing is shown unless enabled && access granted && standby visible. Losing any of those
 *    clears the card at once; events arriving while not eligible are discarded, not queued.
 *  - Changing privacy-relevant settings clears the card (text is not retained in a revealable form).
 *  - Changing the duration re-times the active card from when it was shown (hides now if already past).
 *  - Every timer carries a generation number, so a callback from a superseded timer is a no-op.
 */
class NotificationPreviewController(
    private val scheduler: Scheduler,
    private val time: TimeSource,
    initialSettings: PreviewSettings = PreviewSettings(),
) {
    private val lock = Any()
    private val _state = MutableStateFlow<PreviewState>(PreviewState.Hidden)
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    private var settings = initialSettings
    private var accessGranted = false
    private var standbyVisible = false
    private var released = false

    private var generation = 0L
    private var timer: Cancellable? = null
    private var shownAt = 0L
    private var currentKey: String? = null
    private var currentPackage: String? = null
    private var currentFingerprint = 0

    fun setNotificationAccessGranted(granted: Boolean) = synchronized(lock) {
        accessGranted = granted
        if (!granted) hideLocked()
    }

    fun setStandbyVisible(visible: Boolean) = synchronized(lock) {
        standbyVisible = visible
        if (!visible) hideLocked()
    }

    fun updateSettings(new: PreviewSettings) = synchronized(lock) {
        val old = settings
        settings = new
        when {
            !new.enabled -> hideLocked()
            !old.privacyRelevantEquals(new) -> hideLocked()
            currentPackage?.let { !new.allows(it) } == true -> hideLocked()
            old.duration != new.duration && _state.value is PreviewState.Showing -> retimeLocked()
        }
    }

    fun onNotificationPosted(n: IncomingNotification): PostOutcome = synchronized(lock) {
        if (!eligible(n)) return PostOutcome.IGNORED
        val fp = n.fingerprint()
        val showing = _state.value is PreviewState.Showing
        if (showing && n.key == currentKey) {
            if (fp == currentFingerprint) return PostOutcome.DUPLICATE_IGNORED
            showLocked(n, fp)
            return PostOutcome.UPDATED
        }
        showLocked(n, fp)
        PostOutcome.SHOWN
    }

    fun onNotificationRemoved(key: String) = synchronized(lock) {
        if (key == currentKey) hideLocked()
    }

    /** Clears any visible card and cancels its timer. */
    fun clear() = synchronized(lock) { hideLocked() }

    /** Permanently stops the controller (activity/service destroyed). */
    fun release() = synchronized(lock) {
        released = true
        hideLocked()
    }

    private fun eligible(n: IncomingNotification) =
        !released && settings.enabled && accessGranted && standbyVisible &&
            !n.isOngoing && !n.isGroupSummary && settings.allows(n.packageName)

    private fun showLocked(n: IncomingNotification, fingerprint: Int) {
        val hideText = settings.privacyMode || !settings.showText
        val card = PreviewCard(
            key = n.key,
            appName = n.appName,
            sender = if (settings.showSender) n.sender else null,
            text = if (hideText) null else n.text,
            postedAtMillis = n.postedAtMillis,
        )
        shownAt = time.nowMillis()
        currentKey = n.key
        currentPackage = n.packageName
        currentFingerprint = fingerprint
        val duration = settings.duration.millis
        armLocked(duration)
        _state.value = PreviewState.Showing(card, shownAt + duration)
    }

    private fun retimeLocked() {
        val showing = _state.value as? PreviewState.Showing ?: return
        val total = settings.duration.millis
        val remaining = total - (time.nowMillis() - shownAt)
        if (remaining <= 0) {
            hideLocked()
        } else {
            armLocked(remaining)
            _state.value = PreviewState.Showing(showing.card, shownAt + total)
        }
    }

    private fun armLocked(delayMillis: Long) {
        generation++
        timer?.cancel()
        val mine = generation
        timer = scheduler.schedule(delayMillis) {
            synchronized(lock) { if (mine == generation) hideLocked() }
        }
    }

    private fun hideLocked() {
        generation++
        timer?.cancel()
        timer = null
        currentKey = null
        currentPackage = null
        currentFingerprint = 0
        _state.value = PreviewState.Hidden
    }
}
