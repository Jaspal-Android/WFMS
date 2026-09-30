package com.atvantiq.wfms.data.repository.atten

import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class AttendanceRepoTest {

    private lateinit var apiService: ApiService
    private lateinit var repo: AttendanceRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        repo = AttendanceRepo(apiService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `attendanceCheckInRequest calls apiService with params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<CheckInOutResponse>()
        coEvery { apiService.attendanceCheckIn(params) } returns expectedResponse

        val result = repo.attendanceCheckInRequest(params)

        coVerify { apiService.attendanceCheckIn(params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceCheckOutRequest calls apiService with params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<CheckInOutResponse>()
        coEvery { apiService.attendanceCheckOut(params) } returns expectedResponse

        val result = repo.attendanceCheckOutRequest(params)

        coVerify { apiService.attendanceCheckOut(params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceCheckInStatus calls apiService `() = runTest {
        val expectedResponse = mockk<CheckInStatusResponse>()
        coEvery { apiService.attendanceCheckInStatus() } returns expectedResponse

        val result = repo.attendanceCheckInStatus()

        coVerify { apiService.attendanceCheckInStatus() }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceDetails calls apiService with month, and year`() = runTest {
        val month = 6
        val year = 2024
        val flag = false
        val expectedResponse = mockk<AttendanceDetailListResponse>()
        coEvery { apiService.attendanceDetails(month, year, flag) } returns expectedResponse

        val result = repo.attendanceDetails(month, year, flag)

        coVerify { apiService.attendanceDetails(month, year, flag) }
        assertEquals(expectedResponse, result)
    }
}
