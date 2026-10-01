package com.atvantiq.wfms.ui.screens

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.loginResponse.Permission
import com.atvantiq.wfms.models.loginResponse.User
import com.ssas.jibli.data.prefs.PrefMethods
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SplashVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val prefMain = mockk<SecurePrefMain>(relaxed = true)
    private lateinit var viewModel: SplashVM

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(PrefMethods)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        viewModel = SplashVM(mockk<Application>(relaxed = true), prefMain).apply {
            ioDispatcher = dispatcher
            now = { dispatcher.scheduler.currentTime }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun session(token: String?, user: User?) {
        every { PrefMethods.getUserToken(prefMain) } returns token
        every { PrefMethods.getUserData(prefMain) } returns user
    }

    private fun user(role: String, permissions: List<Permission>): User = mockk(relaxed = true) {
        every { this@mockk.role } returns role
        every { this@mockk.permissions } returns permissions
    }

    @Test
    fun `no saved session opens login`() {
        session(token = null, user = null)

        viewModel.resolve(minVisibleMs = 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashTarget.LOGIN, viewModel.destination.value?.target)
    }

    @Test
    fun `an employee session opens the employee dashboard with its permissions`() {
        val permission = mockk<Permission>(relaxed = true)
        session(token = "t", user = user(ValConstants.ROLE_EMPLOYEE, listOf(permission)))

        viewModel.resolve(minVisibleMs = 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashDestination(SplashTarget.EMPLOYEE_DASHBOARD, listOf(permission)), viewModel.destination.value)
    }

    @Test
    fun `unreadable secure prefs open login instead of crashing`() {
        every { PrefMethods.getUserToken(prefMain) } throws SecurityException("keystore")

        viewModel.resolve(minVisibleMs = 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashTarget.LOGIN, viewModel.destination.value?.target)
    }

    @Test
    fun `a normal launch keeps the splash up for the minimum time`() {
        session(token = null, user = null)

        viewModel.resolve(minVisibleMs = 600)
        dispatcher.scheduler.advanceTimeBy(599)
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.destination.value)

        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()
        assertEquals(SplashTarget.LOGIN, viewModel.destination.value?.target)
    }

    @Test
    fun `a notification tap during the wait routes straight away`() {
        session(token = null, user = null)

        viewModel.resolve(minVisibleMs = 600)
        dispatcher.scheduler.advanceTimeBy(100)
        viewModel.resolve(minVisibleMs = 0)
        dispatcher.scheduler.runCurrent()

        assertEquals(SplashTarget.LOGIN, viewModel.destination.value?.target)
    }
}
