package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftTrackerTest {

    private val minute = 60_000L
    private var now = 1_000_000_000L

    /** Secure prefs backed by a map, so a "restart" can read what was written. */
    private val store = mutableMapOf<String, String?>()
    private val prefs = mockk<SecurePrefMain>(relaxed = true).apply {
        val key = slot<String>()
        val value = slot<String>()
        every { put(capture(key), capture(value)) } answers { store[key.captured] = value.captured }
        every { get(capture(key), any<String>()) } answers { store[key.captured] }
        every { delete(capture(key)) } answers { store.remove(key.captured); Unit }
    }

    private fun tracker() = ShiftTracker(prefs).also { it.now = { now } }

    private fun fix(lat: Double, minutes: Long, accuracy: Double? = 10.0) =
        TrackPoint(lat, 76.7, minutes * minute, accuracy)

    @Test
    fun `pause lasts exactly 15 minutes and fixes during it are discarded`() {
        val tracker = tracker()
        tracker.startShift(checkInMillis = 0L)

        tracker.pause()
        assertEquals(now + 15 * minute, tracker.state.value.pausedUntilMillis)
        assertFalse(tracker.recordFix(fix(30.70, 1)))
        assertEquals(0, tracker.state.value.tally.locations)

        now += 15 * minute
        assertTrue("auto-resumes when the pause is up", tracker.recordFix(fix(30.70, 16)))
        assertNull(tracker.state.value.pausedUntilMillis)
        assertEquals(1, tracker.state.value.tally.locations)
    }

    @Test
    fun `resume ends the pause early`() {
        val tracker = tracker()
        tracker.pause()

        tracker.resume()

        assertFalse(tracker.state.value.isPaused(now))
        assertFalse(tracker.refreshPause())
    }

    @Test
    fun `a pause survives a restart`() {
        tracker().pause()

        val restarted = tracker()

        assertTrue(restarted.state.value.isPaused(now))
    }

    @Test
    fun `end day clears the pause and the totals`() {
        val tracker = tracker()
        tracker.startShift(0L)
        tracker.recordFix(fix(30.70, 0))
        tracker.pause()

        tracker.endShift()

        assertEquals(ShiftState(), tracker.state.value)
        assertNull(store[PrefKeys.SHIFT_STATE])
    }

    @Test
    fun `restarting tracking for the same check-in keeps the totals, a new check-in resets them`() {
        val tracker = tracker()
        tracker.startShift(100L)
        tracker.recordFix(fix(30.70, 0))

        tracker.startShift(100L)
        assertEquals(1, tracker.state.value.tally.locations)

        tracker.startShift(200L)
        assertEquals(0, tracker.state.value.tally.locations)
        assertEquals(200L, tracker.state.value.checkInMillis)
    }

    @Test
    fun `the running distance applies the same filters as the server`() {
        var tally = TrackTally()
        listOf(
            fix(30.70, 0),
            fix(30.80, 5, accuracy = 250.0), // inaccurate: counted as a location, not as distance
            fix(31.20, 6),                   // ~55 km in a minute: a GPS jump
            fix(30.71, 10)
        ).forEach { tally = tally.add(it) }

        assertEquals(4, tally.locations)
        assertEquals(1.1, Math.round(tally.distanceKm * 10) / 10.0, 0.0)
    }
}
