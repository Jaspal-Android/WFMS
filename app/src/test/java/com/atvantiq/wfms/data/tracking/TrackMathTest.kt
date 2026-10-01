package com.atvantiq.wfms.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMathTest {

    private val minute = 60_000L

    // ~1.11 km per 0.01° of latitude
    private fun point(lat: Double, minutes: Long, accuracy: Double? = 10.0, lon: Double = 76.7) =
        TrackPoint(lat, lon, minutes * minute, accuracy)

    @Test
    fun `distance is the haversine sum, rounded to one decimal`() {
        val km = TrackMath.distanceKm(listOf(point(30.70, 0), point(30.71, 10), point(30.72, 20)))
        assertEquals(2.2, km, 0.0)
    }

    @Test
    fun `points are counted in time order whatever order they arrive in`() {
        val inOrder = listOf(point(30.70, 0), point(30.71, 10), point(30.70, 20))
        assertEquals(TrackMath.distanceKm(inOrder), TrackMath.distanceKm(inOrder.reversed()), 0.0)
    }

    @Test
    fun `inaccurate fixes and positions at 0,0 are not counted`() {
        val points = listOf(
            point(30.70, 0),
            point(30.80, 5, accuracy = 250.0),      // cell-tower fix
            TrackPoint(0.0, 0.0, 6 * minute, 5.0),  // not recorded
            point(30.71, 10)
        )
        assertEquals(1.1, TrackMath.distanceKm(points), 0.0)
    }

    @Test
    fun `a jump faster than 150 km per hour is dropped`() {
        // 0.5° (~55 km) in one minute
        val points = listOf(point(30.70, 0), point(31.20, 1), point(30.71, 10))
        assertEquals(1.1, TrackMath.distanceKm(points), 0.0)
    }

    @Test
    fun `moving minutes count only legs faster than 3 km per hour`() {
        val points = listOf(
            point(30.70, 0),
            point(30.71, 10),   // ~6.7 km/h: moving, 10 min
            point(30.7101, 40)  // ~0.02 km/h: standing
        )
        assertEquals(10, TrackMath.movingMinutes(points))
    }

    @Test
    fun `no points means no distance`() {
        assertEquals(0.0, TrackMath.distanceKm(emptyList()), 0.0)
        assertEquals(0, TrackMath.movingMinutes(emptyList()))
    }
}
