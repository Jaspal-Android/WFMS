package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class WorkRepoTest {

    private lateinit var apiService: ApiService
    private lateinit var repo: WorkRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        repo = WorkRepo(apiService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `workAssignedAll calls apiService with page, and pageSize`() = runTest {
        val page = 1
        val pageSize = 10
        val expectedResponse = mockk<WorkAssignedResponse>()
        coEvery { apiService.workAssignedAll(page, pageSize) } returns expectedResponse

        val result = repo.workAssignedAll(page, pageSize)

        coVerify { apiService.workAssignedAll(page, pageSize) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workAccept calls apiService with workId`() = runTest {
        val workId = 123L
        val expectedResponse = mockk<WorkDetailResponse>()
        coEvery { apiService.workAccept(workId) } returns expectedResponse

        val result = repo.workAccept(workId)

        coVerify { apiService.workAccept(workId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workStart calls apiService with params`() = runTest {
        val workId = mockk<RequestBody>()
        val latitude = mockk<RequestBody>()
        val longitude = mockk<RequestBody>()
        val photo = mockk<MultipartBody.Part>()
        val expectedResponse = mockk<WorkDetailResponse>()
        coEvery { apiService.workStart(workId, latitude, longitude, photo) } returns expectedResponse

        val result = repo.workStart(workId, latitude, longitude, photo)

        coVerify { apiService.workStart(workId, latitude, longitude, photo) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workEnd calls apiService with params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<WorkDetailResponse>()
        coEvery { apiService.workEnd(params) } returns expectedResponse

        val result = repo.workEnd(params)

        coVerify { apiService.workEnd(params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workSelfAssign calls apiService with params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<SelfAssignResponse>()
        coEvery { apiService.workSelfAssign(params) } returns expectedResponse

        val result = repo.workSelfAssign(params)

        coVerify { apiService.workSelfAssign(params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workById calls apiService with workId`() = runTest {
        val workId = 456L
        val expectedResponse = mockk<WorkDetailResponse>()
        coEvery { apiService.workById(workId) } returns expectedResponse

        val result = repo.workById(workId)

        coVerify { apiService.workById(workId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workDetailByDate calls apiService with date`() = runTest {
        val date = "2024-06-01"
        val expectedResponse = mockk<WorkDetailsByDateResponse>()
        coEvery { apiService.workDetailByDate(date) } returns expectedResponse

        val result = repo.workDetailByDate(date)

        coVerify { apiService.workDetailByDate(date) }
        assertEquals(expectedResponse, result)
    }
}
