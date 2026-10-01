package com.atvantiq.wfms.ui.screens.admin.siteApproval

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteTypeResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.SiteApprovalVM
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonArray
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SiteApprovalVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var attendanceRepo: IAttendanceRepo
    private lateinit var viewModel: SiteApprovalVM

    private fun workType(id: Long): WorkType = mockk(relaxed = true) {
        every { this@mockk.id } returns id
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        attendanceRepo = mockk(relaxed = true)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        coEvery { attendanceRepo.approveWorkSite(any()) } returns mockk<ApproveWorkSiteTypeResponse>(relaxed = true)
        viewModel = SiteApprovalVM(application, attendanceRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `one approval entry is sent per selected type`() {
        val body = slot<JsonArray>()
        coEvery { attendanceRepo.approveWorkSite(capture(body)) } returns mockk(relaxed = true)

        viewModel.approveRejectWorkSite(11L, 22L, 1, "Approved by pm", listOf(workType(100L), workType(200L)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, body.captured.size())
        val first = body.captured[0].asJsonObject
        assertEquals(11L, first.get("work_site_id").asLong)
        assertEquals(100L, first.get("type_id").asLong)
        assertEquals(22L, first.get("employee_id").asLong)
        assertEquals(1, first.get("status").asInt)
        assertEquals(200L, body.captured[1].asJsonObject.get("type_id").asLong)
    }

    @Test
    fun `nothing is sent when no type is selected`() {
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", emptyList())
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `sentinel ids are never sent to the server`() {
        viewModel.approveRejectWorkSite(-1L, 22L, 1, "x", listOf(workType(1L)))
        viewModel.approveRejectWorkSite(11L, -1L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `a double tap sends the approval once`() {
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `a later approval is allowed once the first has finished`() {
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.approveRejectWorkSite(11L, 22L, 0, "x", listOf(workType(2L)))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `going offline does not lock later approvals`() {
        every { Utils.isInternet(application) } returns false
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        assertEquals(Status.ERROR, viewModel.approveWorkSiteResponse.value?.status)
        coVerify(exactly = 0) { attendanceRepo.approveWorkSite(any()) }

        every { Utils.isInternet(application) } returns true
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `a failed request releases the guard so the admin can retry`() {
        coEvery { attendanceRepo.approveWorkSite(any()) } throws RuntimeException("boom")
        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Status.ERROR, viewModel.approveWorkSiteResponse.value?.status)

        viewModel.approveRejectWorkSite(11L, 22L, 1, "x", listOf(workType(1L)))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { attendanceRepo.approveWorkSite(any()) }
    }

    @Test
    fun `Work Approval pages the shown month, counts it and drops records with no employee`() {
        val withEmployee = mockk<AttendanceRecord>(relaxed = true) { every { employee?.id } returns 7L }
        val withoutEmployee = mockk<AttendanceRecord>(relaxed = true) { every { employee } returns null }
        coEvery { attendanceRepo.attendanceForApproval(any(), any(), any(), any()) } returns mockk(relaxed = true) {
            every { code } returns 200
            every { data?.totalRecords } returns 3
            every { data?.records } returns listOf(withEmployee, withoutEmployee)
        }
        val start = viewModel.month.value!!

        viewModel.records.open()
        viewModel.showPreviousMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        val shown = start.previous()
        coVerify { attendanceRepo.attendanceForApproval(1, 25, shown.month, shown.year) }
        assertEquals(listOf(withEmployee), viewModel.records.state.value?.items)
        assertEquals(3, viewModel.monthCount.value)
    }
}
