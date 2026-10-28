package com.atvantiq.wfms.ui.screens.admin.attendanceApproval

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.allProjects.Circle
import com.atvantiq.wfms.models.allProjects.Data
import com.atvantiq.wfms.models.allProjects.Project
import com.atvantiq.wfms.models.attendance.attendanceDetails.Employee
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerPurpose
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerUiState
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.Logs
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.AttendanceApprovalVM
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefMethods
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AttendanceApprovalVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val attendanceRepo = mockk<IAttendanceRepo>(relaxed = true)
    private val prefMain = mockk<SecurePrefMain>(relaxed = true)
    private val creationRepo = mockk<ICreationRepo>(relaxed = true)

    private fun record(id: Long? = 345633198668L, status: Int? = 0, approvalStatus: Int? = null) = AttendanceRecord(
        action = "Submitted by employee", approvalStatus = approvalStatus, canHrMarkAttendance = false, checkin = null,
        checkout = null, createdAt = "2026-09-23T04:05:17.540282Z", employee = Employee("E1", 7L, "Kamal Sharma"),
        employeeRemarks = null, id = id, logs = Logs(null, null, null), status = status, workHours = ""
    )

    private fun viewModel(role: AppRole): AttendanceApprovalVM {
        every { PrefMethods.getAppRole(prefMain) } returns role
        return AttendanceApprovalVM(application, attendanceRepo, prefMain, creationRepo)
    }

    private fun activeProjects(vararg ids: Long) = AllProjectsResponse(
        code = 200, message = "", success = true,
        data = Data(
            columns = null, defaultColumns = null, page = 1, pageSize = null, totalColumns = null,
            totalCount = ids.size, totalPages = 1, totalRecords = ids.size,
            projects = ids.map {
                Project(circle = listOf(Circle("CHD", 1L, "Chandigarh")), client = null, createdAt = null, createdBy = null,
                    id = it, isActive = 1, name = "Goa darshan", site = null)
            }
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils, PrefMethods)
        every { Utils.isInternet(application) } returns true
        every { application.getString(R.string.attendance_reviewed_by, "PM") } returns "Attendance reviewed by PM"
        every { application.getString(R.string.approved_by_format, "PM") } returns "Approved by PM"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `a decision starts on Present with the role's remarks`() {
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record())

        assertEquals(AttendanceApprovalStatus.PRESENT, vm.decisionStatus.get())
        assertEquals("Attendance reviewed by PM", vm.decisionRemarks.get())
    }

    @Test
    fun `submitting sends status and remarks, then marks the record approved by the role`() {
        val body = slot<JsonObject>()
        coEvery { attendanceRepo.attendanceApprove(345633198668L, capture(body)) } returns mockk(relaxed = true) {
            every { success } returns true
        }
        val vm = viewModel(AppRole.PM)
        val record = record()
        vm.startDecision(record)
        vm.selectStatus(AttendanceApprovalStatus.IDLE)

        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, body.captured.get("status").asInt)
        assertEquals("Attendance reviewed by PM", body.captured.get("remarks").asString)
        assertEquals(4, record.status)
        assertEquals("Approved by PM", record.action)
    }

    @Test
    fun `nothing is sent without a role that approves, or without an id`() {
        val employee = viewModel(AppRole.EMPLOYEE)
        employee.startDecision(record())
        employee.submitDecision()
        assertEquals(R.string.attendance_approval_not_permitted, employee.decisionError.value)

        val pm = viewModel(AppRole.PM)
        pm.startDecision(record(id = null))
        pm.submitDecision()
        assertEquals(R.string.attendance_id_missing, pm.decisionError.value)

        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { attendanceRepo.attendanceApprove(any(), any()) }
    }

    @Test
    fun `a double tap sends the decision once`() {
        coEvery { attendanceRepo.attendanceApprove(any(), any()) } returns mockk(relaxed = true)
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record())

        vm.submitDecision()
        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.attendanceApprove(any(), any()) }
    }

    // --- Idle → Present is booked to a project ---

    @Test
    fun `Idle to Present asks for the project first and sends nothing`() {
        coEvery { creationRepo.allProjects() } returns activeProjects(493287548012, 2L)
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record(status = 4))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)

        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        val picker = vm.projectPicker.state.value
        assertEquals(ProjectPickerUiState.Phase.CHOOSE, picker?.phase)
        assertEquals(ProjectPickerPurpose.MarkPresent("Kamal Sharma"), picker?.purpose)
        assertNull(picker?.selectedId)
        coVerify(exactly = 0) { attendanceRepo.attendanceApprove(any(), any()) }
    }

    @Test
    fun `Mark Present sends status 1 with the chosen project, then closes the picker`() {
        val body = slot<JsonObject>()
        coEvery { creationRepo.allProjects() } returns activeProjects(493287548012)
        coEvery { attendanceRepo.attendanceApprove(345633198668L, capture(body)) } returns mockk(relaxed = true) {
            every { success } returns true
        }
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record(status = 4))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)
        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        vm.submitDecisionOnProject(493287548012)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, body.captured.get("status").asInt)
        assertEquals(493287548012, body.captured.get("project_id").asLong)
        assertNull(vm.projectPicker.state.value)
    }

    @Test
    fun `a reviewer's Idle decision counts as Idle even when the employee's own status is Submitted`() {
        coEvery { creationRepo.allProjects() } returns activeProjects(1L)
        val vm = viewModel(AppRole.OPS)
        vm.startDecision(record(status = 0, approvalStatus = 4))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)

        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.allProjects() }
        coVerify(exactly = 0) { attendanceRepo.attendanceApprove(any(), any()) }
    }

    @Test
    fun `any other change sends no project and shows no picker`() {
        val body = slot<JsonObject>()
        coEvery { attendanceRepo.attendanceApprove(any(), capture(body)) } returns mockk(relaxed = true)
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record(status = 0))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)

        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(body.captured.has("project_id"))
        assertNull(vm.projectPicker.state.value)
        coVerify(exactly = 0) { creationRepo.allProjects() }
    }

    @Test
    fun `no active projects means no approval, and the next attempt loads again`() {
        coEvery { creationRepo.allProjects() } returns activeProjects()
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record(status = 4))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)

        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProjectPickerUiState.Phase.EMPTY, vm.projectPicker.state.value?.phase)

        coEvery { creationRepo.allProjects() } returns activeProjects(9L)
        vm.projectPicker.close()
        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(9L, vm.projectPicker.state.value?.selectedId)
        coVerify(exactly = 2) { creationRepo.allProjects() }
        coVerify(exactly = 0) { attendanceRepo.attendanceApprove(any(), any()) }
    }

    @Test
    fun `a failed approval keeps the picker open on the chosen project`() {
        coEvery { creationRepo.allProjects() } returns activeProjects(1L)
        coEvery { attendanceRepo.attendanceApprove(any(), any()) } returns mockk(relaxed = true) {
            every { success } returns false
        }
        val vm = viewModel(AppRole.PM)
        vm.startDecision(record(status = 4))
        vm.selectStatus(AttendanceApprovalStatus.PRESENT)
        vm.submitDecision()
        dispatcher.scheduler.advanceUntilIdle()

        vm.submitDecisionOnProject(1L)
        dispatcher.scheduler.advanceUntilIdle()

        val picker = vm.projectPicker.state.value
        assertEquals(1L, picker?.selectedId)
        assertEquals(false, picker?.isSubmitting)
    }
}
