package com.atvantiq.wfms.services

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftNotificationViewsTest {

    @Test
    fun `the timer base makes the chronometer show the time since check-in`() {
        val checkIn = 1_000_000L
        val now = checkIn + 2 * 3_600_000L + 11 * 60_000L + 42_000L // 2:11:42 later
        val elapsedRealtime = 50_000_000L

        val base = ShiftNotificationViews.elapsedBase(checkIn, now, elapsedRealtime)

        assertEquals(now - checkIn, elapsedRealtime - base)
    }

    @Test
    fun `a check-in in the future, from a clock change, starts the timer at zero`() {
        val base = ShiftNotificationViews.elapsedBase(checkInMillis = 2_000L, nowMillis = 1_000L, elapsedRealtimeMillis = 9_000L)

        assertEquals(9_000L, base)
    }
}
