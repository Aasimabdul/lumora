package com.simbuild.lumora.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPreviewControllerTest {
    private lateinit var clock: FakeClock

    @Before
    fun setUp() {
        clock = FakeClock()
    }

    private fun controller(
        settings: PreviewSettings = PreviewSettings(enabled = true),
        access: Boolean = true,
        standby: Boolean = true,
    ) = NotificationPreviewController(clock, clock, settings).also {
        it.setNotificationAccessGranted(access)
        it.setStandbyVisible(standby)
    }

    private fun note(
        key: String = "k1",
        pkg: String = "com.chat",
        sender: String? = "Asha",
        text: String? = "secret meeting at 5",
        postedAt: Long = 1000,
        ongoing: Boolean = false,
        summary: Boolean = false,
    ) = IncomingNotification(key, pkg, "Chat", sender, text, postedAt, ongoing, summary)

    private fun NotificationPreviewController.card(): PreviewCard? =
        (state.value as? PreviewState.Showing)?.card

    @Test
    fun disabledByDefaultShowsNothing() {
        val c = NotificationPreviewController(clock, clock)
        c.setNotificationAccessGranted(true)
        c.setStandbyVisible(true)
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note()))
        assertEquals(PreviewState.Hidden, c.state.value)
    }

    @Test
    fun permissionDeniedShowsNothing() {
        val c = controller(access = false)
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note()))
        assertEquals(PreviewState.Hidden, c.state.value)
    }

    @Test
    fun standbyNotVisibleDiscardsAndDoesNotQueue() {
        val c = controller(standby = false)
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note()))
        c.setStandbyVisible(true)
        assertEquals(PreviewState.Hidden, c.state.value)
    }

    @Test
    fun defaultDurationIsFifteenSeconds() {
        assertEquals(15_000L, PreviewSettings().duration.millis)
        assertEquals(NotificationDisplayDuration.S15, NotificationDisplayDuration.fromSeconds(null))
        assertEquals(NotificationDisplayDuration.S15, NotificationDisplayDuration.fromSeconds(12))
    }

    @Test
    fun hidesExactlyAtEachSupportedDuration() {
        for (d in NotificationDisplayDuration.entries) {
            val local = FakeClock()
            val c = NotificationPreviewController(local, local, PreviewSettings(enabled = true, duration = d))
            c.setNotificationAccessGranted(true)
            c.setStandbyVisible(true)
            assertEquals(PostOutcome.SHOWN, c.onNotificationPosted(note()))
            local.advanceBy(d.millis - 1)
            assertTrue("${d.seconds}s: still visible 1ms early", c.state.value is PreviewState.Showing)
            local.advanceBy(1)
            assertEquals("${d.seconds}s: hidden on expiry", PreviewState.Hidden, c.state.value)
            assertEquals(0, local.pendingCount)
        }
    }

    @Test
    fun expiresAtReportedInState() {
        val c = controller()
        clock.advanceBy(500)
        c.onNotificationPosted(note())
        assertEquals(500 + 15_000L, (c.state.value as PreviewState.Showing).expiresAtMillis)
    }

    @Test
    fun newerNotificationReplacesCardAndRestartsTimer() {
        val c = controller()
        c.onNotificationPosted(note(key = "a", text = "first"))
        clock.advanceBy(10_000)
        assertEquals(PostOutcome.SHOWN, c.onNotificationPosted(note(key = "b", text = "second")))
        assertEquals("second", c.card()!!.text)
        clock.advanceBy(14_999)
        assertNotNull("old timer must not hide the new card", c.card())
        clock.advanceBy(1)
        assertNull(c.card())
    }

    @Test
    fun rapidArrivalsProduceOneCardAndOneLiveTimer() {
        val c = controller()
        repeat(20) { i -> c.onNotificationPosted(note(key = "k$i", text = "m$i")) }
        assertEquals("m19", c.card()!!.text)
        assertEquals(1, clock.pendingCount)
    }

    @Test
    fun identicalRepeatDoesNotRestartTimer() {
        val c = controller()
        c.onNotificationPosted(note())
        clock.advanceBy(10_000)
        assertEquals(PostOutcome.DUPLICATE_IGNORED, c.onNotificationPosted(note()))
        clock.advanceBy(5_000)
        assertNull("timer must still expire at the original time", c.card())
    }

    @Test
    fun genuineUpdateReplacesContentAndRestartsTimer() {
        val c = controller()
        c.onNotificationPosted(note(text = "one"))
        clock.advanceBy(10_000)
        assertEquals(PostOutcome.UPDATED, c.onNotificationPosted(note(text = "two")))
        assertEquals("two", c.card()!!.text)
        clock.advanceBy(14_999)
        assertNotNull(c.card())
        clock.advanceBy(1)
        assertNull(c.card())
    }

    @Test
    fun removalHidesMatchingCardOnly() {
        val c = controller()
        c.onNotificationPosted(note(key = "a"))
        c.onNotificationRemoved("other")
        assertNotNull(c.card())
        c.onNotificationRemoved("a")
        assertNull(c.card())
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun staleCallbackFromCancelledTimerIsHarmless() {
        val c = controller()
        c.onNotificationPosted(note(key = "a"))
        c.onNotificationPosted(note(key = "b", text = "second"))
        clock.fireCancelledTasksAnyway()
        assertEquals("second", c.card()!!.text)
    }

    @Test
    fun privacyModeNeverPlacesTextInState() {
        val c = controller(PreviewSettings(enabled = true, privacyMode = true))
        c.onNotificationPosted(note())
        val card = c.card()!!
        assertNull(card.text)
        assertEquals("Chat", card.appName)
        assertFalse(c.state.value.toString().contains("secret"))
    }

    @Test
    fun showTextAndShowSenderTogglesAreHonoured() {
        val noText = controller(PreviewSettings(enabled = true, showText = false))
        noText.onNotificationPosted(note())
        assertNull(noText.card()!!.text)
        assertEquals("Asha", noText.card()!!.sender)

        val noSender = controller(PreviewSettings(enabled = true, showSender = false))
        noSender.onNotificationPosted(note())
        assertNull(noSender.card()!!.sender)
        assertEquals("secret meeting at 5", noSender.card()!!.text)
    }

    @Test
    fun genericNotificationWithoutContentStillShowsAppName() {
        val c = controller()
        c.onNotificationPosted(note(sender = null, text = null))
        assertEquals("Chat", c.card()!!.appName)
        assertNull(c.card()!!.text)
    }

    @Test
    fun ongoingAndGroupSummaryNotificationsAreIgnored() {
        val c = controller()
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note(ongoing = true)))
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note(summary = true)))
        assertEquals(PreviewState.Hidden, c.state.value)
    }

    @Test
    fun allowlistRestrictsPackages() {
        val c = controller(PreviewSettings(enabled = true, allowedPackages = setOf("com.chat")))
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note(pkg = "com.other")))
        assertEquals(PostOutcome.SHOWN, c.onNotificationPosted(note(pkg = "com.chat")))
    }

    @Test
    fun tighteningAllowlistClearsDisallowedVisibleCard() {
        val c = controller()
        c.onNotificationPosted(note(pkg = "com.chat"))
        c.updateSettings(PreviewSettings(enabled = true, allowedPackages = setOf("com.other")))
        assertNull(c.card())
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun disablingFeatureClearsImmediatelyAndCancelsTimer() {
        val c = controller()
        c.onNotificationPosted(note())
        c.updateSettings(PreviewSettings(enabled = false))
        assertEquals(PreviewState.Hidden, c.state.value)
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun revokingAccessClearsImmediately() {
        val c = controller()
        c.onNotificationPosted(note())
        c.setNotificationAccessGranted(false)
        assertEquals(PreviewState.Hidden, c.state.value)
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note(key = "z")))
    }

    @Test
    fun leavingStandbyClearsAndCancels() {
        val c = controller()
        c.onNotificationPosted(note())
        c.setStandbyVisible(false)
        assertEquals(PreviewState.Hidden, c.state.value)
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun changingPrivacySettingsClearsVisibleCard() {
        val c = controller()
        c.onNotificationPosted(note())
        c.updateSettings(PreviewSettings(enabled = true, privacyMode = true))
        assertEquals(PreviewState.Hidden, c.state.value)
    }

    @Test
    fun lengtheningDurationExtendsActiveCardFromItsStart() {
        val c = controller(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S10))
        c.onNotificationPosted(note())
        clock.advanceBy(6_000)
        c.updateSettings(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S30))
        assertEquals(30_000L, (c.state.value as PreviewState.Showing).expiresAtMillis)
        assertEquals("obsolete timer cancelled", 1, clock.pendingCount)
        clock.advanceBy(23_999)
        assertNotNull(c.card())
        clock.advanceBy(1)
        assertNull(c.card())
    }

    @Test
    fun shorteningDurationBelowElapsedHidesNow() {
        val c = controller(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S30))
        c.onNotificationPosted(note())
        clock.advanceBy(12_000)
        c.updateSettings(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S10))
        assertEquals(PreviewState.Hidden, c.state.value)
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun shorteningDurationAboveElapsedRetimesRemaining() {
        val c = controller(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S30))
        c.onNotificationPosted(note())
        clock.advanceBy(5_000)
        c.updateSettings(PreviewSettings(enabled = true, duration = NotificationDisplayDuration.S10))
        clock.advanceBy(4_999)
        assertNotNull(c.card())
        clock.advanceBy(1)
        assertNull(c.card())
    }

    @Test
    fun releaseCancelsTimerAndRejectsFurtherEvents() {
        val c = controller()
        c.onNotificationPosted(note())
        c.release()
        assertEquals(0, clock.pendingCount)
        assertEquals(PostOutcome.IGNORED, c.onNotificationPosted(note(key = "late")))
    }

    @Test
    fun clearHidesCard() {
        val c = controller()
        c.onNotificationPosted(note())
        c.clear()
        assertEquals(PreviewState.Hidden, c.state.value)
        assertEquals(0, clock.pendingCount)
    }

    @Test
    fun toStringNeverLeaksContent() {
        val n = note()
        c@ run {
            val c = controller()
            c.onNotificationPosted(n)
            val dump = n.toString() + c.card().toString() + c.state.value.toString()
            assertFalse(dump.contains("secret"))
            assertFalse(dump.contains("Asha"))
        }
    }
}
