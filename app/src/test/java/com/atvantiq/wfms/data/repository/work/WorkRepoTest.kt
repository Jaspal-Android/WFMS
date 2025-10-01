package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
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
    private lateinit var prefMain: SecurePrefMain
    private lateinit var repo: WorkRepo
    private val token = "test_token"

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        prefMain = mockk(relaxed = true)
        every { prefMain.get(PrefKeys.LOGIN_TOKEN, any<String>()) } returns token
        repo = WorkRepo(apiService, prefMain)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `workAssignedAll calls apiService with correct token, page, and pageSize`() = runTest {
        val page = 1
        val pageSize = 10
        val expectedResponse = mockk<WorkAssignedAllResponse>()
        coEvery { apiService.workAssignedAll(any(), page, pageSize) } returns expectedResponse

        val result = repo.workAssignedAll(page, pageSize)

        coVerify { apiService.workAssignedAll("Bearer $token", page, pageSize) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workAccept calls apiService with correct token and workId`() = runTest {
        val workId = 123L
        val expectedResponse = mockk<AcceptWorkResponse>()
        coEvery { apiService.workAccept(any(), workId) } returns expectedResponse

        val result = repo.workAccept(workId)

        coVerify { apiService.workAccept("Bearer $token", workId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workStart calls apiService with correct token and params`() = runTest {
        val workId = mockk<RequestBody>()
        val latitude = mockk<RequestBody>()
        val longitude = mockk<RequestBody>()
        val photo = mockk<MultipartBody.Part>()
        val expectedResponse = mockk<StartWorkResponse>()
        coEvery { apiService.workStart(any(), workId, latitude, longitude, photo) } returns expectedResponse

        val result = repo.workStart(workId, latitude, longitude, photo)

        coVerify { apiService.workStart("Bearer $token", workId, latitude, longitude, photo) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workEnd calls apiService with correct token and params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<EndWorkResponse>()
        coEvery { apiService.workEnd(any(), params) } returns expectedResponse

        val result = repo.workEnd(params)

        coVerify { apiService.workEnd("Bearer $token", params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workSelfAssign calls apiService with correct token and params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<SelfAssignResponse>()
        coEvery { apiService.workSelfAssign(any(), params) } returns expectedResponse

        val result = repo.workSelfAssign(params)

        coVerify { apiService.workSelfAssign("Bearer $token", params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workById calls apiService with correct token and workId`() = runTest {
        val workId = 456L
        val expectedResponse = mockk<WorkDetailResponse>()
        coEvery { apiService.workById(any(), workId) } returns expectedResponse

        val result = repo.workById(workId)

        coVerify { apiService.workById("Bearer $token", workId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `workDetailByDate calls apiService with correct token and date`() = runTest {
        val date = "2024-06-01"
        val expectedResponse = mockk<WorkDetailsByDateResponse>()
        coEvery { apiService.workDetailByDate(any(), date) } returns expectedResponse

        val result = repo.workDetailByDate(date)

        coVerify { apiService.workDetailByDate("Bearer $token", date) }
        assertEquals(expectedResponse, result)
    }
}
