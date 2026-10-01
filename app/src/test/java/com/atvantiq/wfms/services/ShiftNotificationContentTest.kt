package com.atvantiq.wfms.services

import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.tracking.ShiftState
import com.atvantiq.wfms.data.tracking.TrackTally
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftNotificationContentTest {

    private val now = 10_000_000L

    @Test
    fun `an active shift shows its totals, elapsed time and a Pause action`() {
        val content = ShiftNotificationContent.of(
            ShiftState(checkInMillis = 5_000L, tally = TrackTally(locations = 12, distanceKm = 3.4)), now
        )

        assertFalse(content.isPaused)
        assertEquals(R.string.shift_notification_active_title, content.title)
        assertEquals(R.string.pause_15_min, content.actionTitle)
        assertEquals(5_000L, content.chronometerBaseMillis)
        assertEquals(12, content.locations)
        assertEquals(3.4, content.distanceKm, 0.0)
        assertNull(content.pausedUntilMillis)
    }

    @Test
    fun `a paused shift shows when it resumes, keeps the timer and offers Resume`() {
        val content = ShiftNotificationContent.of(ShiftState(checkInMillis = 5_000L, pausedUntilMillis = now + 60_000L), now)

        assertTrue(content.isPaused)
        assertEquals(R.string.shift_notification_paused_title, content.title)
        assertEquals(R.string.resume, content.actionTitle)
        assertEquals(R.drawable.ic_tracking_pause, content.smallIcon)
        assertEquals(now + 60_000L, content.pausedUntilMillis)
        assertEquals(5_000L, content.chronometerBaseMillis)
    }
}
