package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.models.myDay.MyDayResponse
import com.google.gson.JsonObject
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

/** The upload rules of the tracking queue, with a scripted fake in place of the network. */
class QueueUploaderTest {

    /** Answers each call from [script] in order; a Throwable in the script is thrown. */
    private class FakeTrackingRepo(private val script: List<Any>) : ITrackingRepo {
        var calls = 0
            private set

        override suspend fun sendLocation(params: JsonObject): SendLocationResponse {
            val step = script.getOrElse(calls++) { accepted() }
            if (step is Throwable) throw step
            return step as SendLocationResponse
        }

        override suspend fun myDay(date: String?): MyDayResponse = error("not used by the uploader")
    }

    private var stored: String? = null
    private lateinit var queue: LocationEventQueue

    @Before
    fun setUp() {
        val prefs = mockk<SecurePrefMain>(relaxed = true)
        every { prefs.get(PrefKeys.LOCATION_EVENT_QUEUE, null) } answers { stored }
        val value = slot<String?>()
        every { prefs.put(PrefKeys.LOCATION_EVENT_QUEUE, captureNullable(value)) } answers { stored = value.captured }
        every { prefs.delete(PrefKeys.LOCATION_EVENT_QUEUE) } answers { stored = null }
        queue = LocationEventQueue(prefs)
    }

    private fun enqueue(count: Int) = repeat(count) {
        queue.enqueue(QueuedLocationEvent(30.0 + it, 76.0, 1_000L + it, null))
    }

    private fun uploader(vararg script: Any) = QueueUploader(FakeTrackingRepo(script.toList()), queue)

    @Test
    fun `everything queued is sent and removed`() = runTest {
        enqueue(3)
        assertEquals(UploadOutcome.Complete, uploader().flush())
        assertTrue(queue.peekAll().isEmpty())
    }

    @Test
    fun `an empty queue sends nothing`() = runTest {
        val repo = FakeTrackingRepo(emptyList())
        assertEquals(UploadOutcome.Complete, QueueUploader(repo, queue).flush())
        assertEquals(0, repo.calls)
    }

    @Test
    fun `a failure pauses at that point, keeping it and everything after for the next attempt`() = runTest {
        enqueue(4)
        val failure = java.io.IOException("offline")

        val outcome = uploader(accepted(), accepted(), failure).flush()

        assertSame(failure, (outcome as UploadOutcome.Paused).cause)
        assertEquals(listOf(32.0, 33.0), queue.peekAll().map { it.latitude })
    }

    @Test
    fun `an HTTP 401 means the session expired, and points already sent are not kept`() = runTest {
        enqueue(3)
        val unauthorized = HttpException(Response.error<Any>(401, "".toResponseBody()))

        assertEquals(UploadOutcome.SessionExpired, uploader(accepted(), unauthorized).flush())
        assertEquals(listOf(31.0, 32.0), queue.peekAll().map { it.latitude })
    }

    @Test
    fun `an HTTP 200 carrying code 401 also means the session expired`() = runTest {
        enqueue(2)
        assertEquals(UploadOutcome.SessionExpired, uploader(SendLocationResponse(401, null, "Not authenticated", false)).flush())
        assertEquals(2, queue.peekAll().size)
    }

    @Test
    fun `a cancelled upload rethrows, but points already sent are still removed`() = runTest {
        enqueue(3)
        try {
            uploader(accepted(), CancellationException("cancelled")).flush()
            fail("cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals(listOf(31.0, 32.0), queue.peekAll().map { it.latitude })
        }
    }
}

private fun accepted() = SendLocationResponse(200, null, "ok", true)
