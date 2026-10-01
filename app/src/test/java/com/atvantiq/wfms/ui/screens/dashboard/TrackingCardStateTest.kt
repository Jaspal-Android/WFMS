package com.atvantiq.wfms.ui.screens.dashboard

import com.atvantiq.wfms.data.tracking.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingCardStateTest {

    private val now = 1_000_000L

    private fun resolve(
        dayActive: Boolean = true,
        started: Boolean = true,
        foreground: Boolean = true,
        background: Boolean = true,
        gps: Boolean = true,
        pausedUntil: Long? = null
    ) = TrackingCardState.resolve(dayActive, started, foreground, background, gps, ShiftState(pausedUntilMillis = pausedUntil), now)

    @Test
    fun `hidden when no day is active`() = assertNull(resolve(dayActive = false))

    @Test
    fun `active when everything is in place`() = assertEquals(TrackingCardState.ACTIVE, resolve())

    @Test
    fun `paused until the pause runs out`() {
        assertEquals(TrackingCardState.PAUSED, resolve(pausedUntil = now + 1))
        assertEquals(TrackingCardState.ACTIVE, resolve(pausedUntil = now))
    }

    @Test
    fun `problems outrank everything, GPS first`() {
        assertEquals(TrackingCardState.GPS_OFF, resolve(gps = false, foreground = false, pausedUntil = now + 1))
        assertEquals(TrackingCardState.PERMISSION_DENIED, resolve(foreground = false, pausedUntil = now + 1))
    }

    @Test
    fun `starting before tracking began, then asking for all-the-time access`() {
        assertEquals(TrackingCardState.STARTING, resolve(started = false))
        assertEquals(TrackingCardState.NEEDS_ALWAYS, resolve(background = false))
        assertEquals(TrackingCardState.PAUSED, resolve(background = false, pausedUntil = now + 1))
    }
}
