package com.atvantiq.wfms.ui.screens.more

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.empDetail.EmpData
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.ui.screens.DashboardTab
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.isSessionLost
import com.ssas.jibli.data.prefs.PrefMethods
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ProfileVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val authRepo = mockk<IAuthRepo>()
    private val prefMain = mockk<SecurePrefMain>(relaxed = true)
    private val cached = mockk<EmpData>(relaxed = true)
    private val fresh = mockk<EmpData>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils, PrefMethods)
        every { Utils.isInternet(application) } returns true
        every { PrefMethods.getEmpDetailResponse(prefMain) } returns cached
        every { PrefMethods.saveEmpDetailResponse(prefMain, any()) } returns Unit
        every { PrefMethods.getUserData(prefMain) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun answer(code: Int): EmpDetailResponse = mockk(relaxed = true) {
        every { this@mockk.code } returns code
        every { data } returns fresh
    }

    @Test
    fun `shows the cached profile at once, then the server's, and caches it`() {
        coEvery { authRepo.empDetails() } returns answer(200)
        val viewModel = ProfileVM(application, authRepo, prefMain)
        assertSame(cached, viewModel.profile.value)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertSame(fresh, viewModel.profile.value)
        verify { PrefMethods.saveEmpDetailResponse(prefMain, fresh) }
    }

    @Test
    fun `a rejected refresh keeps the cached profile and reports a lost session`() {
        coEvery { authRepo.empDetails() } returns answer(401)
        val viewModel = ProfileVM(application, authRepo, prefMain)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertSame(cached, viewModel.profile.value)
        assertTrue(viewModel.profileResponse.value!!.isSessionLost { it.code })
        verify(exactly = 0) { PrefMethods.saveEmpDetailResponse(any(), any()) }
    }

    @Test
    fun `a network failure is not a lost session`() {
        coEvery { authRepo.empDetails() } throws java.io.IOException("offline")
        val viewModel = ProfileVM(application, authRepo, prefMain)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.profileResponse.value!!.isSessionLost { it.code })
        assertSame(cached, viewModel.profile.value)
    }

    @Test
    fun `the More rows post their click events`() {
        val viewModel = ProfileVM(application, authRepo, prefMain)
        viewModel.onAppearanceClick()
        assertEquals(MoreClickEvents.APPEARANCE, viewModel.clickEvents.value)
        viewModel.onLogoutClick()
        assertEquals(MoreClickEvents.LOGOUT, viewModel.clickEvents.value)
        viewModel.onViewProfileClick()
        assertEquals(MoreClickEvents.VIEW_PROFILE, viewModel.clickEvents.value)
    }

    @Test
    fun `tabs follow the profile's role and permissions, and update when it refreshes`() {
        every { cached.role } returns "employee"
        every { fresh.role } returns "ops"
        every { fresh.permissions } returns emptyList()
        coEvery { authRepo.empDetails() } returns answer(200)
        val viewModel = ProfileVM(application, authRepo, prefMain)
        val tabs = mutableListOf<List<DashboardTab>>()
        viewModel.tabs.observeForever { tabs += it }

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                listOf(DashboardTab.DASHBOARD, DashboardTab.WORK, DashboardTab.CLAIMS, DashboardTab.MORE),
                listOf(DashboardTab.DASHBOARD, DashboardTab.APPROVALS, DashboardTab.MORE)
            ),
            tabs
        )
    }

    @Test
    fun `with no cached profile the tabs follow the login role, without permissions`() {
        every { PrefMethods.getEmpDetailResponse(prefMain) } returns null
        every { PrefMethods.getUserData(prefMain) } returns mockk<User>(relaxed = true) { every { role } returns "pm" }
        val viewModel = ProfileVM(application, authRepo, prefMain)
        viewModel.tabs.observeForever { }

        assertEquals(listOf(DashboardTab.DASHBOARD, DashboardTab.APPROVALS, DashboardTab.MORE), viewModel.tabs.value)
    }
}
