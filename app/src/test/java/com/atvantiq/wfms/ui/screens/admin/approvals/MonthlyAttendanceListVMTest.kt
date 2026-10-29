package com.atvantiq.wfms.ui.screens.admin.approvals

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailData
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.Employee
import com.atvantiq.wfms.models.attendance.attendanceDetails.Logs
import com.atvantiq.wfms.ui.screens.admin.ui.approvals.MonthlyAttendanceListVM
import com.atvantiq.wfms.utils.MonthYear
import com.atvantiq.wfms.utils.Utils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
class MonthlyAttendanceListVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class TestVM(application: Application, repo: IAttendanceRepo) : MonthlyAttendanceListVM(application, repo)

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repo = mockk<IAttendanceRepo>()

    private fun record(id: Long, employeeId: Long?) = AttendanceRecord(
        action = null, approvalStatus = null, canHrMarkAttendance = false, checkin = null, checkout = null,
        createdAt = null, employee = Employee("E$id", employeeId, "Asha"), employeeRemarks = null,
        id = id, logs = Logs(null, null, null), status = 0, workHours = ""
    )

    private fun response(vararg records: AttendanceRecord, totalRecords: Int? = records.size) = AttendanceDetailListResponse(
        ValConstants.SUCCESS_CODE, AttendanceDetailData(1, 25, records.toList(), totalRecords, 1), "ok", true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `the first page of the current month is requested`() {
        val now = MonthYear.current()
        coEvery { repo.attendanceForApproval(1, ValConstants.APPROVAL_PAGE_SIZE, now.month, now.year) } returns
            response(record(1, 10), totalRecords = 40)
        val vm = TestVM(application, repo)

        vm.records.open()
        idle()

        assertEquals(1, vm.records.state.value!!.items.size)
        assertEquals(40, vm.monthCount.value)
    }

    @Test
    fun `records with no employee cannot be reviewed and are dropped`() {
        coEvery { repo.attendanceForApproval(any(), any(), any(), any()) } returns
            response(record(1, 10), record(2, null), record(3, 30))
        val vm = TestVM(application, repo)

        vm.records.open()
        idle()

        assertEquals(listOf(1L, 3L), vm.records.state.value!!.items.map { it.id })
    }

    @Test
    fun `stepping back a month reloads that month and clears the count until it arrives`() {
        coEvery { repo.attendanceForApproval(any(), any(), any(), any()) } returns response(record(1, 10), totalRecords = 7)
        val vm = TestVM(application, repo)
        vm.records.open()
        idle()
        val shown = vm.month.value!!

        vm.showPreviousMonth()

        assertEquals(shown.previous(), vm.month.value)
        assertNull(vm.monthCount.value)
        idle()
        coVerify { repo.attendanceForApproval(1, ValConstants.APPROVAL_PAGE_SIZE, shown.previous().month, shown.previous().year) }
    }

    @Test
    fun `stepping forward moves to the next month`() {
        coEvery { repo.attendanceForApproval(any(), any(), any(), any()) } returns response()
        val vm = TestVM(application, repo)
        val shown = vm.month.value!!

        vm.showNextMonth()
        idle()

        assertEquals(shown.next(), vm.month.value)
        coVerify { repo.attendanceForApproval(1, ValConstants.APPROVAL_PAGE_SIZE, shown.next().month, shown.next().year) }
    }

    @Test
    fun `a rejected answer adds nothing and leaves no count`() {
        coEvery { repo.attendanceForApproval(any(), any(), any(), any()) } returns
            AttendanceDetailListResponse(500, null, "boom", false)
        val vm = TestVM(application, repo)

        vm.records.open()
        idle()

        assertEquals(emptyList<AttendanceRecord>(), vm.records.state.value!!.items)
        assertNull(vm.monthCount.value)
    }
}
