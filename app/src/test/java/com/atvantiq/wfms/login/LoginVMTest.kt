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
import com.atvantiq.wfms.models.loginResponse.Data
import com.atvantiq.wfms.models.loginResponse.LoginResponse
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
        val result = viewModel.run { 
            val method = this.javaClass.getDeclaredMethod("isValidLoginDetails")
            method.isAccessible = true
            method.invoke(this) as Boolean
        }
        assertFalse(result)
        assertEquals(LoginErrorHandler.EMPTY_USERNAME, viewModel.errorHandler.value)
    }

    @Test
    fun `isValidLoginDetails returns false and sets EMPTY_PASSWORD when password is blank`() {
        viewModel.userName.value = "user"
        viewModel.password.value = ""
        val result = viewModel.run { 
            val method = this.javaClass.getDeclaredMethod("isValidLoginDetails")
            method.isAccessible = true
            method.invoke(this) as Boolean
        }
        assertFalse(result)
        assertEquals(LoginErrorHandler.EMPTY_PASSWORD, viewModel.errorHandler.value)
    }

    @Test
    fun `isValidLoginDetails returns true when username and password are not blank`() {
        viewModel.userName.value = "user"
        viewModel.password.value = "password"
        val result = viewModel.run { 
            val method = this.javaClass.getDeclaredMethod("isValidLoginDetails")
            method.isAccessible = true
            method.invoke(this) as Boolean
        }
        assertTrue(result)
    }

    @Test
    fun `onForgetPasswordClick posts ON_FORGET_PASSWORD_CLICK event`() {
        val observer = mock(Observer::class.java) as Observer<LoginClickEvents>
        viewModel.clickEvents.observeForever(observer)
        viewModel.onForgetPasswordClick()
        assertEquals(LoginClickEvents.ON_FORGET_PASSWORD_CLICK, viewModel.clickEvents.value)
    }

    @Test
    fun `onPasswordToggleClick posts ON_PASSWORD_TOGGLE event`() {
        val observer = mock(Observer::class.java) as Observer<LoginClickEvents>
        viewModel.clickEvents.observeForever(observer)
        viewModel.onPasswordToggleClick()
        assertEquals(LoginClickEvents.ON_PASSWORD_TOGGLE, viewModel.clickEvents.value)
    }

    val loginResponse = LoginResponse(
        code = 200,
        message = "Login successful",
        data = Data(
            accessToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjVtUUlQanJ6QmFnSDFzOVciLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2hmemdjd25ja25iYnFxY211Y2h1LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiIyMzdhMDk4Ny02MGFjLTQ5NDItOTM2Ni0zYmU2YzE4ODk4YWYiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzU2ODk1MTQyLCJpYXQiOjE3NTY4MDg3NDIsImVtYWlsIjoiZW1wbG95ZWVAYXR2YW50aXEuY29tIiwicGhvbmUiOiIiLCJhcHBfbWV0YWRhdGEiOnsicHJvdmlkZXIiOiJlbWFpbCIsInByb3ZpZGVycyI6WyJlbWFpbCJdfSwidXNlcl9tZXRhZGF0YSI6eyJlbWFpbF92ZXJpZmllZCI6dHJ1ZX0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoicGFzc3dvcmQiLCJ0aW1lc3RhbXAiOjE3NTY4MDg3NDJ9XSwic2Vzc2lvbl9pZCI6IjNhYjU3OThlLTRjNmMtNGM2ZC1hNjUwLTNiZjlkNTNkMWRlMiIsImlzX2Fub255bW91cyI6ZmFsc2V9.1enyrwo9gfM9fBh-5oAe9WuAhVezIPY2DFYJMQ1WX4s",
            refreshToken = "zbp5sbof5ohl",
            user = User(
                userId = 39580123,
                email = "employee@atvantiq.com",
                firstName = "Happy",
                lastName = "Singh",
                shortName = "Happy",
                role = "Employee"
            )
        ),
        success = true
    )
}
