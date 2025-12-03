package com.atvantiq.wfms.ui.screens.login

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.loginResponse.AccessLevel
import com.atvantiq.wfms.models.loginResponse.Data
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginResponse.OfficialLocation
import com.atvantiq.wfms.models.loginResponse.Permission
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
            data = Data(
                accessToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6InZ2SWRHZHkxanpUQVZEUm8iLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2VtY2tmZmJncnh3aWZ3eW1oaWt2LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiJhNWY3M2MwNC01YTQwLTQ3N2YtOGY0My1mYWFiMzRiNjNlN2QiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzY0MDY4ODIwLCJpYXQiOjE3NjM0NjQwMjAsImVtYWlsIjoiamFzcGFsMDA2QHlvcG1haWwuY29tIiwicGhvbmUiOiIiLCJhcHBfbWV0YWRhdGEiOnsicHJvdmlkZXIiOiJlbWFpbCIsInByb3ZpZGVycyI6WyJlbWFpbCJdfSwidXNlcl9tZXRhZGF0YSI6eyJlbWFpbF92ZXJpZmllZCI6dHJ1ZX0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoicGFzc3dvcmQiLCJ0aW1lc3RhbXAiOjE3NjM0NjQwMjB9XSwic2Vzc2lvbl9pZCI6IjBlOTljMDNiLTYxMDItNGM4My1iYTc3LTczZmUwZjU5YWM4ZiIsImlzX2Fub255bW91cyI6ZmFsc2V9.G2658-GyjlBNiRGkVsejNp2QQlX3N-BpvelPY-RjeAg",
                refreshToken = "iptpjd5rgnpv",
                user = User(
                    userId = 324475492436,
                    email = "jaspal006@yopmail.com",
                    firstName = "Jaspal",
                    lastName = "Kumar",
                    shortName = "Jaspal Kumar",
                    role = "Employee",
                    roleId = 199427269040,
                    officialLocation = OfficialLocation(
                        latitude = 30.7149239,
                        longitude = 76.7033976
                    ),
                    permissions = listOf(
                        Permission(
                            featureId = 976896675679,
                            featureName = "Employee Deck",
                            accessLevels = listOf(
                                AccessLevel( "Full Access",851659960058),
                            )
                        ),
                        Permission(
                            featureId = 751345906091,
                            featureName = "Type Activity",
                            accessLevels = listOf(
                                AccessLevel( "Full Access",851659960058),
                            )
                        )
                    )
                )
            ),
            success = true
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
