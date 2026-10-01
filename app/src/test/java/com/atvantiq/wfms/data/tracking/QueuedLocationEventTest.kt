package com.atvantiq.wfms.data.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QueuedLocationEventTest {

    // 2026-09-18T04:32:10Z
    private val fixTime = 1_789_705_930_000L

    @Test
    fun `the upload body carries the fix time as ISO UTC and the accuracy`() {
        val body = QueuedLocationEvent(30.7046, 76.7179, fixTime, 12.5f).toUploadParams()

        assertEquals(30.7046, body.get("latitude").asDouble, 0.0)
        assertEquals(76.7179, body.get("longitude").asDouble, 0.0)
        assertEquals("2026-09-18T04:32:10Z", body.get("recordedAt").asString)
        assertEquals(12.5, body.get("accuracy").asDouble, 0.0)
    }

    @Test
    fun `accuracy is left out when the fix had none`() {
        val body = QueuedLocationEvent(30.7, 76.7, fixTime, null).toUploadParams()

        assertFalse(body.has("accuracy"))
    }
}
