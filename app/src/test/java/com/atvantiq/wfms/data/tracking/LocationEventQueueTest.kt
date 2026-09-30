package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.google.gson.annotations.SerializedName
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    // R8 renames fields in release builds. The queue is persisted, so a field without an explicit
    // name would be written under a name that changes between releases.
    @Test
    fun `every persisted field pins its JSON name`() {
        QueuedLocationEvent::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .forEach { field ->
                val annotation = field.getAnnotation(SerializedName::class.java)
                assertNotNull("${field.name} needs @SerializedName", annotation)
                assertEquals(field.name, annotation!!.value)
            }
    }

    @Test
    fun `stored queue uses the stable field names`() {
        queue.enqueue(QueuedLocationEvent(12.5, 34.5, 1000L, 8f))

        val json = storedQueue.orEmpty()
        listOf("latitude", "longitude", "recordedAtMillis", "accuracyMeters").forEach {
            assertTrue("missing $it in $json", json.contains("\"$it\""))
        }
    }

    @Test
    fun `entries written under obfuscated keys are dropped instead of replaying as 0,0`() {
        storedQueue = """[{"a":12.5,"b":34.5,"c":1000}]"""

        assertTrue(queue.peekAll().isEmpty())
    }

    @Test
    fun `valid entries survive next to legacy ones`() {
        storedQueue = """[{"a":1.0,"b":2.0,"c":3},
            {"latitude":12.5,"longitude":34.5,"recordedAtMillis":1000}]"""

        val events = queue.peekAll()

        assertEquals(1, events.size)
        assertEquals(12.5, events.first().latitude, 0.0)
        assertEquals(1000L, events.first().recordedAtMillis)
    }

    @Test
    fun `an unreadable queue is treated as empty`() {
        storedQueue = "not json"

        assertTrue(queue.peekAll().isEmpty())
    }
}
