package com.atvantiq.wfms.ui.screens.dashboard.tabs.budget

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.budget.IBudgetRepo
import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.models.targets.MyTargetsResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets.MyTargetsVM
import com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard.ProjectDashboardVM
import com.atvantiq.wfms.utils.Utils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The month sent to `/targets` and `/projects` is `yyyy-MM` in ASCII digits, whatever the phone's language. */
@ExperimentalCoroutinesApi
class MonthParamTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val budgetRepo = mockk<IBudgetRepo>()
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun currentMonth(): String {
        val now = Calendar.getInstance()
        return "%04d-%02d".format(Locale.US, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    @Test
    fun `targets load on creation for the current month`() {
        coEvery { budgetRepo.myTargets(any()) } returns mockk<MyTargetsResponse>()

        val vm = MyTargetsVM(application, budgetRepo)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { budgetRepo.myTargets(currentMonth()) }
        assertEquals(Status.SUCCESS, vm.myTargetsResponse.value!!.status)
    }

    @Test
    fun `a single-digit month is zero-padded`() {
        coEvery { budgetRepo.myTargets(any()) } returns mockk<MyTargetsResponse>()
        val vm = MyTargetsVM(application, budgetRepo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectedYear = 2026
        vm.selectedMonth = 3
        vm.fetchMyTargets()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { budgetRepo.myTargets("2026-03") }
    }

    @Test
    fun `the month stays in ASCII digits on an Arabic phone`() {
        Locale.setDefault(Locale("ar", "EG"))
        coEvery { budgetRepo.myProjects(any()) } returns mockk<MyProjectsResponse>()
        val vm = ProjectDashboardVM(application, budgetRepo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectedYear = 2026
        vm.selectedMonth = 11
        vm.fetchMyProjects()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { budgetRepo.myProjects("2026-11") }
    }
}
