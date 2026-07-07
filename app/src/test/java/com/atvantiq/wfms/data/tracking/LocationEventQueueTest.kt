package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocationEventQueueTest {

    private lateinit var prefMain: SecurePrefMain
    private lateinit var queue: LocationEventQueue
    private var storedQueue: String? = null

    @Before
    fun setUp() {
        prefMain = mockk(relaxed = true)
        every { prefMain.get(PrefKeys.LOCATION_EVENT_QUEUE, null) } answers { storedQueue }
        val valueSlot = slot<String?>()
        every { prefMain.put(PrefKeys.LOCATION_EVENT_QUEUE, captureNullable(valueSlot)) } answers {
            storedQueue = valueSlot.captured
        }
        every { prefMain.delete(PrefKeys.LOCATION_EVENT_QUEUE) } answers {
            storedQueue = null
        }
        queue = LocationEventQueue(prefMain)
    }

    @Test
    fun `enqueue persists event`() {
        queue.enqueue(
            QueuedLocationEvent(
                latitude = 12.34,
                longitude = 56.78,
                recordedAtMillis = 1000L,
                accuracyMeters = 10f
            )
        )

        val events = queue.peekAll()
        assertEquals(1, events.size)
        assertEquals(12.34, events.first().latitude, 0.0)
        assertEquals(56.78, events.first().longitude, 0.0)
    }

    @Test
    fun `removeSynced drops oldest synced events`() {
        repeat(3) { index ->
            queue.enqueue(
                QueuedLocationEvent(
                    latitude = index.toDouble(),
                    longitude = index.toDouble(),
                    recordedAtMillis = index.toLong(),
                    accuracyMeters = null
                )
            )
        }

        queue.removeSynced(2)

        val remaining = queue.peekAll()
        assertEquals(1, remaining.size)
        assertEquals(2.0, remaining.first().latitude, 0.0)
    }

    @Test
    fun `clear deletes persisted queue`() {
        queue.enqueue(
            QueuedLocationEvent(
                latitude = 1.0,
                longitude = 2.0,
                recordedAtMillis = 3L,
                accuracyMeters = null
            )
        )

        queue.clear()

        assertEquals(emptyList<QueuedLocationEvent>(), queue.peekAll())
        verify { prefMain.delete(PrefKeys.LOCATION_EVENT_QUEUE) }
    }
}
