package com.atvantiq.wfms.data.repository.atten

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
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
    private lateinit var prefMain: SecurePrefMain
    private lateinit var repo: AttendanceRepo
    private val token = "test_token"

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        prefMain = mockk(relaxed = true)
        every { prefMain.get(PrefKeys.LOGIN_TOKEN, any<String>()) } returns token
        repo = AttendanceRepo(apiService, prefMain)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `attendanceCheckInRequest calls apiService with correct token and params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<CheckInOutResponse>()
        coEvery { apiService.attendanceCheckIn(any(), params) } returns expectedResponse

        val result = repo.attendanceCheckInRequest(params)

        coVerify { apiService.attendanceCheckIn("Bearer $token", params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceCheckOutRequest calls apiService with correct token and params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<CheckInOutResponse>()
        coEvery { apiService.attendanceCheckOut(any(), params) } returns expectedResponse

        val result = repo.attendanceCheckOutRequest(params)

        coVerify { apiService.attendanceCheckOut("Bearer $token", params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceCheckInStatus calls apiService with correct token`() = runTest {
        val expectedResponse = mockk<CheckInStatusResponse>()
        coEvery { apiService.attendanceCheckInStatus(any()) } returns expectedResponse

        val result = repo.attendanceCheckInStatus()

        coVerify { apiService.attendanceCheckInStatus("Bearer $token") }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `attendanceDetails calls apiService with correct token, month, and year`() = runTest {
        val month = 6
        val year = 2024
        val expectedResponse = mockk<AttendanceDetailListResponse>()
        coEvery { apiService.attendanceDetails(any(), month, year) } returns expectedResponse

        val result = repo.attendanceDetails(month, year)

        coVerify { apiService.attendanceDetails("Bearer $token", month, year) }
        assertEquals(expectedResponse, result)
    }
}
