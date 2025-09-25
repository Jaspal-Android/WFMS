package com.atvantiq.wfms.data.repository.auth

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.loginResponse.LoginResponse
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
class AuthRepoTest {

    private lateinit var apiService: ApiService
    private lateinit var prefMain: SecurePrefMain
    private lateinit var repo: AuthRepo
    private val token = "test_token"

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        prefMain = mockk(relaxed = true)
        every { prefMain.get(PrefKeys.LOGIN_TOKEN, any<String>()) } returns token
        repo = AuthRepo(apiService, prefMain)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loginRequest calls apiService with correct params`() = runTest {
        val params = JsonObject()
        val expectedResponse = mockk<LoginResponse>()
        coEvery { apiService.loginRequest(params) } returns expectedResponse

        val result = repo.loginRequest(params)

        coVerify { apiService.loginRequest(params) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `empDetails calls apiService with correct token`() = runTest {
        val expectedResponse = mockk<EmpDetailResponse>()
        coEvery { apiService.empDetails(any()) } returns expectedResponse

        val result = repo.empDetails()

        coVerify { apiService.empDetails("Bearer $token") }
        assertEquals(expectedResponse, result)
    }
}
