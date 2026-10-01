package com.atvantiq.wfms.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripModePolicyTest {

    private val minute = 60_000L

    // 0.001° of latitude ≈ 111 m
    private fun at(lat: Double, minutes: Long, accuracy: Double? = 10.0) = TrackPoint(lat, 76.7, minutes * minute, accuracy)

    @Test
    fun `a shift starts at rest and stays there for small movements`() {
        val policy = TripModePolicy()
        assertEquals(SamplingMode.REST, policy.onFix(at(30.7000, 0)))
        assertEquals(SamplingMode.REST, policy.onFix(at(30.7015, 15))) // ~167 m
    }

    @Test
    fun `200 metres from the rest point starts a trip`() {
        val policy = TripModePolicy()
        policy.onFix(at(30.7000, 0))
        assertEquals(SamplingMode.TRIP, policy.onFix(at(30.7020, 15))) // ~222 m
    }

    @Test
    fun `inaccurate fixes never start a trip`() {
        val policy = TripModePolicy()
        policy.onFix(at(30.7000, 0))
        assertEquals(SamplingMode.REST, policy.onFix(at(30.7100, 1, accuracy = 500.0)))
    }

    @Test
    fun `ten minutes without moving 30 metres ends the trip`() {
        val policy = tripping()
        policy.onFix(at(30.7022, 16))                                    // ~22 m: not a move
        assertEquals(SamplingMode.TRIP, policy.onFix(at(30.7021, 20)))
        assertEquals(SamplingMode.REST, policy.onFix(at(30.7021, 25)))   // 10 min since the last move
    }

    @Test
    fun `moving keeps the trip going`() {
        val policy = tripping()
        assertEquals(SamplingMode.TRIP, policy.onFix(at(30.7050, 24)))   // a move
        assertEquals(SamplingMode.TRIP, policy.onFix(at(30.7051, 33)))   // 9 min after it
    }

    @Test
    fun `a still phone in trip mode sends no fixes, so the timer ends the trip`() {
        val policy = tripping()
        assertEquals(15 * minute, policy.lastMoveMillis)
        assertEquals(SamplingMode.TRIP, policy.onStillTimeout(24 * minute))
        assertEquals(SamplingMode.REST, policy.onStillTimeout(25 * minute))
        assertNull(policy.lastMoveMillis)
    }

    @Test
    fun `after a trip ends, the next trip needs 200 metres from the new rest point`() {
        val policy = tripping()
        policy.onStillTimeout(25 * minute)                              // rests at 30.7020
        assertEquals(SamplingMode.REST, policy.onFix(at(30.7035, 30)))  // ~167 m from there
        assertEquals(SamplingMode.TRIP, policy.onFix(at(30.7045, 31)))  // ~278 m
    }

    @Test
    fun `uploads go straight up at rest and in batches while travelling`() {
        assertTrue(TripModePolicy.shouldUpload(SamplingMode.REST, queued = 1, lastUploadMillis = 0, nowMillis = 1))
        assertFalse(TripModePolicy.shouldUpload(SamplingMode.TRIP, queued = 3, lastUploadMillis = 0, nowMillis = 30_000))
        assertTrue(TripModePolicy.shouldUpload(SamplingMode.TRIP, queued = 10, lastUploadMillis = 0, nowMillis = 30_000))
        assertTrue(TripModePolicy.shouldUpload(SamplingMode.TRIP, queued = 1, lastUploadMillis = 0, nowMillis = 60_000))
    }

    /** At rest at 30.7000 from minute 0, travelling since minute 15 at 30.7020. */
    private fun tripping() = TripModePolicy().apply {
        onFix(at(30.7000, 0))
        onFix(at(30.7020, 15))
    }
}
