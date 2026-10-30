package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import com.atvantiq.wfms.data.tracking.QueuedLocationEvent
import com.atvantiq.wfms.models.myDay.MyDayAttendance
import com.atvantiq.wfms.models.myDay.MyDayData
import com.atvantiq.wfms.models.myDay.MyDayEvent
import com.atvantiq.wfms.models.myDay.MyDayPoint
import com.atvantiq.wfms.models.myDay.MyDaySummary
import com.atvantiq.wfms.models.myDay.MyDayTrip
import com.atvantiq.wfms.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyDayContentTest {

    private fun at(time: String) = "2026-09-18T${time}Z"
    private fun millis(time: String) = DateUtils.parseUtcIso(at(time))!!

    private fun day(
        points: List<MyDayPoint>? = null,
        events: List<MyDayEvent>? = null,
        trips: List<MyDayTrip>? = null,
        summary: MyDaySummary? = null,
        attendance: MyDayAttendance? = MyDayAttendance(at("03:42:00"), null, 30.7046, 76.7179, null, null)
    ) = MyDayData("2026-09-18", attendance, points, events, trips, summary)

    private fun event(type: String?, time: String?, lat: Double? = null, lon: Double? = null) =
        MyDayEvent(type, time?.let { at(it) }, "S-1", "Site 1", lat, lon)

    @Test
    fun `a day with every list missing builds without crashing`() {
        val ui = MyDayContent.build(MyDayData(null, null, null, null, null, null), emptyList(), isToday = true)
        assertTrue(ui.isEmpty)
        assertEquals(0.0, ui.distanceKm, 0.0)
        assertNull(ui.mapBounds)
    }

    @Test
    fun `timeline is oldest first, events before trips at the same instant, undated events skipped`() {
        val data = day(
            events = listOf(event("WORK_START", "04:36:00"), event("CHECK_IN", "03:42:00"), event("CLAIM", null)),
            trips = listOf(MyDayTrip("Check-in", "Site 1", at("03:42:00"), at("04:36:00"), 14.2))
        )

        val kinds = MyDayContent.timeline(data).map { it.kind }

        assertEquals(listOf(TimelineKind.CHECK_IN, TimelineKind.TRIP, TimelineKind.WORK_START), kinds)
    }

    @Test
    fun `an unknown event type is shown as an activity`() {
        assertEquals(TimelineKind.ACTIVITY, MyDayContent.kindOf("SOMETHING_NEW"))
        assertEquals(TimelineKind.ACTIVITY, MyDayContent.kindOf(null))
        assertEquals(TimelineKind.CHECK_OUT, MyDayContent.kindOf("check_out"))
    }

    @Test
    fun `with nothing waiting to upload the server distance is shown as is`() {
        val ui = MyDayContent.build(day(summary = MyDaySummary(27.9, 84, 2, 63)), emptyList(), isToday = true)

        assertEquals(27.9, ui.distanceKm, 0.0)
        assertFalse(ui.isDistanceEstimate)
        assertEquals(63, ui.pointsRecorded)
        assertTrue(ui.isShiftActive)
    }

    @Test
    fun `today, queued points are merged without duplicates and the distance becomes an estimate`() {
        val server = listOf(MyDayPoint(30.70, 76.7, at("04:00:00"), 10.0))
        val queued = listOf(
            QueuedLocationEvent(30.70, 76.7, millis("04:00:00"), 10f), // already on the server
            QueuedLocationEvent(30.71, 76.7, millis("04:10:00"), 10f)
        )

        val ui = MyDayContent.build(day(points = server, summary = MyDaySummary(0.0, 0, 0, 1)), queued, isToday = true)

        assertTrue(ui.isDistanceEstimate)
        assertEquals(1.1, ui.distanceKm, 0.0)
        assertEquals(2, ui.pointsRecorded)
        assertEquals(2, ui.route.size)
    }

    @Test
    fun `queued points are ignored on a past day`() {
        val queued = listOf(QueuedLocationEvent(30.71, 76.7, millis("04:10:00"), 10f))

        val ui = MyDayContent.build(day(summary = MyDaySummary(5.5, 10, 1, 4)), queued, isToday = false)

        assertFalse(ui.isDistanceEstimate)
        assertEquals(5.5, ui.distanceKm, 0.0)
    }

    @Test
    fun `inaccurate points stay off the route but are still counted`() {
        val points = listOf(
            MyDayPoint(30.70, 76.7, at("04:00:00"), 10.0),
            MyDayPoint(30.75, 76.7, at("04:05:00"), 500.0)
        )

        val ui = MyDayContent.build(day(points = points), emptyList(), isToday = false)

        assertEquals(1, ui.route.size)
        assertEquals(2, ui.pointsRecorded)
    }

    @Test
    fun `a check-in at 0,0 is not placed on the map`() {
        val ui = MyDayContent.build(
            day(attendance = MyDayAttendance(at("03:42:00"), at("12:00:00"), 0.0, 0.0, null, null)),
            emptyList(), isToday = false
        )

        assertNull(ui.checkInPosition)
        assertFalse(ui.isShiftActive)
    }

    @Test
    fun `event pins need coordinates`() {
        val ui = MyDayContent.build(
            day(events = listOf(event("WORK_START", "04:36:00", 30.74, 76.78), event("CLAIM", "05:00:00"))),
            emptyList(), isToday = false
        )

        assertEquals(listOf(TimelineKind.WORK_START), ui.pins.map { it.kind })
    }

    @Test
    fun `map bounds pad the content by 40 percent and never span less than 0_01 degrees`() {
        val single = MyDayContent.paddedBounds(listOf(GeoPoint(30.7, 76.7)))!!
        assertEquals(0.01, single.north - single.south, 1e-9)
        assertEquals(0.01, single.east - single.west, 1e-9)

        val wide = MyDayContent.paddedBounds(listOf(GeoPoint(30.0, 76.0), GeoPoint(31.0, 77.0)))!!
        assertEquals(29.6, wide.south, 1e-9)
        assertEquals(31.4, wide.north, 1e-9)

        assertNull(MyDayContent.paddedBounds(emptyList()))
    }
}
