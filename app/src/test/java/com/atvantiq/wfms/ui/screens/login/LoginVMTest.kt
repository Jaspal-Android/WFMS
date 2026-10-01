package com.atvantiq.wfms.ui.screens.login

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
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
    private lateinit var prefMain: SecurePrefMain
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
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { Utils.isInternet(application) } returns true

        prefMain = mockk(relaxed = true)
        viewModel = LoginVM(application, authRepo, prefMain)
        viewModel.fetchPushToken = { "push-token" }
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
    fun `login request keeps the password exactly as typed but trims the email`() = runTest {
        val params = slot<JsonObject>()
        coEvery { authRepo.loginRequest(capture(params)) } returns mockk(relaxed = true)

        viewModel.userName.value = "  user@domain.com  "
        viewModel.password.value = "  pass word  "
        viewModel.onSubmitLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("user@domain.com", params.captured.get("email").asString)
        assertEquals("  pass word  ", params.captured.get("password").asString)
    }

    @Test
    fun `a whitespace-only password is still rejected`() {
        viewModel.userName.value = "user@domain.com"
        viewModel.password.value = "   "

        viewModel.onSubmitLoginClick()

        assertEquals(LoginErrorHandler.EMPTY_PASSWORD, viewModel.errorHandler.value)
        coVerify(exactly = 0) { authRepo.loginRequest(any()) }
    }

    @Test
    fun `onSubmitLoginClick with valid details calls loginRequest and updates LiveData`() = runTest {
        val response = LoginResponse(
            code = 200,
            message = "Login successful",
            data = Data(
                accessToken = "test-access-token",
                refreshToken = "test-refresh-token",
                user = User(
                    userId = 1L,
                    email = "employee@example.com",
                    firstName = "Test",
                    lastName = "Employee",
                    shortName = "Test Employee",
                    role = "Employee",
                    roleId = 2L,
                    officialLocation = OfficialLocation(
                        latitude = 10.0,
                        longitude = 20.0
                    ),
                    permissions = listOf(
                        Permission(
                            featureId = 3L,
                            featureName = "Employee Deck",
                            accessLevels = listOf(
                                AccessLevel("Full Access", 5L),
                            )
                        ),
                        Permission(
                            featureId = 4L,
                            featureName = "Type Activity",
                            accessLevels = listOf(
                                AccessLevel("Full Access", 5L),
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

    private fun loginAnswer(accepted: Boolean): LoginResponse = mockk(relaxed = true) {
        every { code } returns if (accepted) 200 else 401
        every { success } returns accepted
        every { message } returns "msg"
        every { data?.accessToken } returns "access-token"
        every { data?.user } returns mockk(relaxed = true) {
            every { userId } returns 42L
        }
    }

    private fun submitLogin(answer: LoginResponse) {
        coEvery { authRepo.loginRequest(any()) } returns answer
        viewModel.userName.value = "user@example.com"
        viewModel.password.value = "secret"
        viewModel.onSubmitLoginClick()
    }

    @Test
    fun `an accepted login saves the session, registers the push token, then completes`() = runTest {
        val params = slot<JsonObject>()
        coEvery { authRepo.sendNotificationToken(capture(params)) } returns mockk(relaxed = true)

        submitLogin(loginAnswer(accepted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        verify { prefMain.put(PrefKeys.LOGIN_TOKEN, "access-token") }
        assertEquals("42", params.captured.get("employee_id").asString)
        assertEquals("push-token", params.captured.get("token").asString)
        assertEquals(true, viewModel.loginCompleted.value)
        assertEquals(42L, viewModel.user?.userId)
    }

    @Test
    fun `login still completes when the push token cannot be fetched`() = runTest {
        viewModel.fetchPushToken = { throw IllegalStateException("no token") }

        submitLogin(loginAnswer(accepted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { authRepo.sendNotificationToken(any()) }
        assertEquals(true, viewModel.loginCompleted.value)
    }

    @Test
    fun `login still completes when uploading the push token fails`() = runTest {
        coEvery { authRepo.sendNotificationToken(any()) } throws java.io.IOException("offline")

        submitLogin(loginAnswer(accepted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.loginCompleted.value)
    }

    @Test
    fun `login completes after the timeout when the push token never arrives`() = runTest {
        viewModel.fetchPushToken = { kotlinx.coroutines.awaitCancellation() }

        submitLogin(loginAnswer(accepted = true))
        testDispatcher.scheduler.advanceTimeBy(9_999)
        testDispatcher.scheduler.runCurrent()
        assertNull("still waiting before the timeout", viewModel.loginCompleted.value)

        testDispatcher.scheduler.advanceTimeBy(2)
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.loginCompleted.value)
    }

    @Test
    fun `a rejected login saves nothing and does not complete`() = runTest {
        submitLogin(loginAnswer(accepted = false))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { prefMain.put(PrefKeys.LOGIN_TOKEN, any<String>()) }
        assertNull(viewModel.loginCompleted.value)
        assertFalse(viewModel.isAccepted(viewModel.loginResponse.value?.response))
    }
}
