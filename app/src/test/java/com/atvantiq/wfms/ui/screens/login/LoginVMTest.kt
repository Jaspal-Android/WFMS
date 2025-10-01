package com.atvantiq.wfms.ui.screens.login

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.loginResponse.Data
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginResponse.OfficialLocation
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class LoginVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var authRepo: IAuthRepo
    private lateinit var viewModel: LoginVM
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        authRepo = mockk(relaxed = true)

        // Use NetworkConnectivityHelper for mocking
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true

        viewModel = LoginVM(application, authRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onForgetPasswordClick sets clickEvents value`() {
        viewModel.onForgetPasswordClick()
        assertEquals(LoginClickEvents.ON_FORGET_PASSWORD_CLICK, viewModel.clickEvents.value)
    }

    @Test
    fun `onPasswordToggleClick sets clickEvents value`() {
        viewModel.onPasswordToggleClick()
        assertEquals(LoginClickEvents.ON_PASSWORD_TOGGLE, viewModel.clickEvents.value)
    }

    @Test
    fun `onSubmitLoginClick with empty username sets errorHandler`() {
        viewModel.userName.value = ""
        viewModel.password.value = "password"
        viewModel.onSubmitLoginClick()
        assertEquals(LoginErrorHandler.EMPTY_USERNAME, viewModel.errorHandler.value)
    }

    @Test
    fun `onSubmitLoginClick with empty password sets errorHandler`() {
        viewModel.userName.value = "user"
        viewModel.password.value = ""
        viewModel.onSubmitLoginClick()
        assertEquals(LoginErrorHandler.EMPTY_PASSWORD, viewModel.errorHandler.value)
    }

    @Test
    fun `onSubmitLoginClick with valid details calls loginRequest and updates LiveData`() = runTest {
        val response = LoginResponse(
            code = 200,
            message = "Login successful",
            success = true,
            data = Data(
                accessToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjVtUUlQanJ6QmFnSDFzOVciLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2hmemdjd25ja25iYnFxY211Y2h1LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiIyMzdhMDk4Ny02MGFjLTQ5NDItOTM2Ni0zYmU2YzE4ODk4YWYiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzU2OTE3MDk0LCJpYXQiOjE3NTY4MzA2OTQsImVtYWlsIjoiZW1wbG95ZWVAYXR2YW50aXEuY29tIiwicGhvbmUiOiIiLCJhcHBfbWV0YWRhdGEiOnsicHJvdmlkZXIiOiJlbWFpbCIsInByb3ZpZGVycyI6WyJlbWFpbCJdfSwidXNlcl9tZXRhZGF0YSI6eyJlbWFpbF92ZXJpZmllZCI6dHJ1ZX0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoicGFzc3dvcmQiLCJ0aW1lc3RhbXAiOjE3NTY4MzA2OTR9XSwic2Vzc2lvbl9pZCI6ImY2ZjhkYjQ2LWZmOTktNGYyOS05MDM5LWZmZTA0ZmQ1YjYwMCIsImlzX2Fub255bW91cyI6ZmFsc2V9.c5SRBzYE4E1gm_LbYp5VzEd7L0nJS5bV2nNkQMo3ToU",
                refreshToken = "zpykmth3agug",
                user = User(
                    userId = 39580123,
                    email = "employee@atvantiq.com",
                    firstName = "Happy",
                    lastName = "Singh",
                    shortName = "Happy",
                    role = "Employee",
                    officialLocation = OfficialLocation(
                        latitude = 28.6139,
                        longitude = 77.209,
                    )
                )
            )
        )

        coEvery { authRepo.loginRequest(any()) } returns response

        viewModel.userName.value = "user@domain.com"
        viewModel.password.value = "password"
        viewModel.onSubmitLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { authRepo.loginRequest(any()) }
        assertNotNull(viewModel.loginResponse.value)
        assertEquals(Status.SUCCESS, viewModel.loginResponse.value?.status)
        assertEquals(response, viewModel.loginResponse.value?.response)
        assertTrue(viewModel.isButtonEnabled.value == true)
    }
}
