package com.atvantiq.wfms.ui.screens.admin.siteApproval

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.workSites.workAssignments.AssignmentEmployee
import com.atvantiq.wfms.models.workSites.workAssignments.AssignmentSite
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignment
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignmentsPage
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignmentsResponse
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.WorkApprovalVM
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
class WorkApprovalVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repo = mockk<IAttendanceRepo>()

    private fun assignment(id: Long, employeeId: Long? = 10L, workSiteId: Long? = 20L) = WorkAssignment(
        id = id,
        employee = AssignmentEmployee(employeeId, "E$id", "Asha", null),
        project = null,
        site = AssignmentSite(1L, "S1", "Site", workSiteId),
        circle = null,
        status = null,
        assignedBy = null,
        visitNo = 1,
        progress = null,
        createdAt = null
    )

    private fun response(vararg records: WorkAssignment, totalRecords: Int? = records.size) = WorkAssignmentsResponse(
        ValConstants.SUCCESS_CODE, WorkAssignmentsPage(1, 25, totalRecords, 1, records.toList()), "ok", true
    )

    private fun stubAnyRequest(answer: WorkAssignmentsResponse) {
        coEvery { repo.workAssignments(any(), any(), any(), any(), any()) } returns answer
    }

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
    fun `the first page of the current month is requested without a search`() {
        val now = MonthYear.current()
        coEvery { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, now.firstDay, now.lastDay, null) } returns
            response(assignment(1), totalRecords = 40)
        val vm = WorkApprovalVM(application, repo)

        vm.assignments.open()
        idle()

        assertEquals(listOf(1L), vm.assignments.state.value!!.items.map { it.id })
        assertEquals(40, vm.assignmentCount.value)
    }

    @Test
    fun `assignments with no employee or no work site cannot be reviewed and are dropped`() {
        stubAnyRequest(response(assignment(1), assignment(2, employeeId = null), assignment(3, workSiteId = null), assignment(4)))
        val vm = WorkApprovalVM(application, repo)

        vm.assignments.open()
        idle()

        assertEquals(listOf(1L, 4L), vm.assignments.state.value!!.items.map { it.id })
    }

    @Test
    fun `stepping back a month asks for that month and clears the count until it arrives`() {
        stubAnyRequest(response(assignment(1), totalRecords = 7))
        val vm = WorkApprovalVM(application, repo)
        vm.assignments.open()
        idle()
        val previous = vm.month.value!!.previous()

        vm.showPreviousMonth()

        assertEquals(previous, vm.month.value)
        assertNull(vm.assignmentCount.value)
        idle()
        coVerify { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, previous.firstDay, previous.lastDay, null) }
        assertEquals(7, vm.assignmentCount.value)
    }

    @Test
    fun `stepping forward moves to the next month`() {
        stubAnyRequest(response())
        val vm = WorkApprovalVM(application, repo)
        val next = vm.month.value!!.next()

        vm.showNextMonth()
        idle()

        assertEquals(next, vm.month.value)
        coVerify { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, next.firstDay, next.lastDay, null) }
    }

    @Test
    fun `a search is trimmed, sent from page 1 and kept when the month changes`() {
        stubAnyRequest(response(assignment(1)))
        val vm = WorkApprovalVM(application, repo)
        vm.assignments.open()
        idle()

        vm.search("  kamal ")
        idle()
        vm.showNextMonth()
        idle()

        val next = vm.month.value!!
        assertEquals("kamal", vm.appliedSearch)
        coVerify { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, any(), any(), "kamal") }
        coVerify { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, next.firstDay, next.lastDay, "kamal") }
    }

    @Test
    fun `clearing the search goes back to the unfiltered list`() {
        stubAnyRequest(response(assignment(1)))
        val vm = WorkApprovalVM(application, repo)
        vm.search("kamal")
        idle()

        vm.search("")
        idle()

        assertEquals("", vm.appliedSearch)
        coVerify { repo.workAssignments(1, ValConstants.APPROVAL_PAGE_SIZE, any(), any(), null) }
    }

    @Test
    fun `the same search again only refreshes`() {
        stubAnyRequest(response(assignment(1)))
        val vm = WorkApprovalVM(application, repo)
        vm.search("kamal")
        idle()

        vm.search(" kamal")
        idle()

        assertEquals("kamal", vm.appliedSearch)
        assertEquals(listOf(1L), vm.assignments.state.value!!.items.map { it.id })
    }

    @Test
    fun `a rejected answer adds nothing and leaves no count`() {
        stubAnyRequest(WorkAssignmentsResponse(500, null, "boom", false))
        val vm = WorkApprovalVM(application, repo)

        vm.assignments.open()
        idle()

        assertEquals(emptyList<WorkAssignment>(), vm.assignments.state.value!!.items)
        assertNull(vm.assignmentCount.value)
    }
}
