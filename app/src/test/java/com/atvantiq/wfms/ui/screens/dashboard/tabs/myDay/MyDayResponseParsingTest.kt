package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import com.atvantiq.wfms.di.modules.NetModule
import com.atvantiq.wfms.models.myDay.MyDayResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The response exactly as the dev server sent it, through the app's own Gson configuration. */
class MyDayResponseParsingTest {

    private val devResponse = """
        {"code":200,"message":"Day summary fetched successfully.","data":{"date":"2026-10-01",
        "attendance":{"checkInAt":"2026-10-01T11:14:38.896602+00:00","checkOutAt":null,
        "checkInLatitude":30.71502,"checkInLongitude":76.7033083,"checkOutLatitude":null,"checkOutLongitude":null},
        "points":[{"latitude":30.7049983,"longitude":76.7185,"recordedAt":"2026-10-01T14:27:52+00:00","accuracy":5.0}],
        "events":[{"type":"CHECK_IN","at":"2026-10-01T11:14:38.896602+00:00","siteId":null,"siteName":null,"latitude":30.71502,"longitude":76.7033083},
        {"type":"WORK_START","at":"2026-10-01T11:30:05.001579+00:00","siteId":"CHD-100","siteName":"South Chandigahr","latitude":30.71502,"longitude":76.7033083},
        {"type":"CHECK_OUT","at":null,"siteId":null,"siteName":null,"latitude":null,"longitude":null}],
        "trips":[],"pauses":[],"summary":{"distanceKm":0.0,"movingMinutes":0,"sitesVisited":1,"pointsRecorded":1}},"success":true}
    """.trimIndent()

    @Test
    fun `the real dev response parses into an active day with its timeline and map`() {
        val response = NetModule().provideGson().fromJson(devResponse, MyDayResponse::class.java)

        val ui = MyDayContent.build(response.data, emptyList(), isToday = true)

        assertFalse(ui.isEmpty)
        assertTrue(ui.isShiftActive)
        assertNotNull(ui.checkInMillis)
        assertEquals(1, ui.pointsRecorded)
        assertEquals(listOf(TimelineKind.CHECK_IN, TimelineKind.WORK_START), ui.timeline.map { it.kind })
        assertEquals("South Chandigahr", ui.timeline[1].siteName)
        assertEquals(2, ui.pins.size)
        assertNotNull(ui.mapBounds)
    }
}
