package com.atvantiq.wfms.login

import android.app.Application
import androidx.lifecycle.Observer
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.ui.screens.login.LoginClickEvents
import com.atvantiq.wfms.ui.screens.login.LoginErrorHandler
import com.atvantiq.wfms.ui.screens.login.LoginVM
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.models.loginResponse.AccessLevel
import com.atvantiq.wfms.models.loginResponse.Data
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginResponse.OfficialLocation
import com.atvantiq.wfms.models.loginResponse.Permission
import com.atvantiq.wfms.models.loginResponse.User

class LoginVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: LoginVM
    private lateinit var mockRepo: IAuthRepo
    private lateinit var mockApp: Application

    @Before
    fun setup() {
        mockRepo = mock(IAuthRepo::class.java)
        mockApp = mock(Application::class.java)
        viewModel = LoginVM(mockApp, mockRepo)
    }

    @Test
    fun `isValidLoginDetails returns false and sets EMPTY_USERNAME when username is blank`() {
        viewModel.userName.value = ""
        viewModel.password.value = "password"
        // Use onSubmitLoginClick which calls isValidLoginDetails internally
        viewModel.onSubmitLoginClick()
        assertEquals(LoginErrorHandler.EMPTY_USERNAME, viewModel.errorHandler.value)
        assertNull(viewModel.loginResponse.value)
    }

    @Test
    fun `isValidLoginDetails returns false and sets EMPTY_PASSWORD when password is blank`() {
        viewModel.userName.value = "user"
        viewModel.password.value = ""
        viewModel.onSubmitLoginClick()
        assertEquals(LoginErrorHandler.EMPTY_PASSWORD, viewModel.errorHandler.value)
        assertNull(viewModel.loginResponse.value)
    }

    @Test
    fun `isValidLoginDetails returns true when username and password are not blank`() {
        viewModel.userName.value = "user"
        viewModel.password.value = "password"
        // Should trigger loginRequest, but since repo is mocked, just check button state
        viewModel.onSubmitLoginClick()
        assertNull(viewModel.errorHandler.value)
        // Button should be disabled during request
        assertFalse(viewModel.isButtonEnabled.value!!)
    }

    @Test
    fun `onForgetPasswordClick posts ON_FORGET_PASSWORD_CLICK event`() {
        val observer = mock(Observer::class.java) as Observer<LoginClickEvents>
        viewModel.clickEvents.observeForever(observer)
        viewModel.onForgetPasswordClick()
        assertEquals(LoginClickEvents.ON_FORGET_PASSWORD_CLICK, viewModel.clickEvents.value)
        viewModel.clickEvents.removeObserver(observer)
    }

    @Test
    fun `onPasswordToggleClick posts ON_PASSWORD_TOGGLE event`() {
        val observer = mock(Observer::class.java) as Observer<LoginClickEvents>
        viewModel.clickEvents.observeForever(observer)
        viewModel.onPasswordToggleClick()
        assertEquals(LoginClickEvents.ON_PASSWORD_TOGGLE, viewModel.clickEvents.value)
        viewModel.clickEvents.removeObserver(observer)
    }

    val loginResponse = LoginResponse(
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
}
