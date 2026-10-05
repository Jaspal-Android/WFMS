package com.atvantiq.wfms.data.repository.tracking

import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.models.myDay.MyDayResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class TrackingRepoTest {

    private val api = mockk<ApiService>()
    private val repo = TrackingRepo(api)

    @Test
    fun `a location is uploaded as given`() = runTest {
        val params = JsonObject().apply { addProperty("latitude", 30.7) }
        val answer = mockk<SendLocationResponse>()
        coEvery { api.sendLocation(params) } returns answer

        assertSame(answer, repo.sendLocation(params))
        coVerify(exactly = 1) { api.sendLocation(params) }
    }

    @Test
    fun `My Day is requested for a date, or for today with none`() = runTest {
        val onDate = mockk<MyDayResponse>()
        val today = mockk<MyDayResponse>()
        coEvery { api.myDay("2026-10-05") } returns onDate
        coEvery { api.myDay(null) } returns today

        assertSame(onDate, repo.myDay("2026-10-05"))
        assertSame(today, repo.myDay(null))
    }

    @Test(expected = IOException::class)
    fun `an upload failure reaches the caller so the queue keeps the event`() = runTest {
        coEvery { api.sendLocation(any()) } throws IOException("offline")

        repo.sendLocation(JsonObject())
    }
}
