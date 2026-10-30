package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveVM
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** A leave request keeps what was typed and picked when Android ends the process (PD-1). */
class ApplyLeaveRestoreTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = mockk<Application>(relaxed = true)
    private val repo = mockk<IAttendanceRepo>(relaxed = true)

    private fun viewModel(handle: SavedStateHandle) = ApplyLeaveVM(application, repo, handle)

    @Test
    fun `a recreated form starts from what was saved`() {
        val handle = SavedStateHandle()
        with(viewModel(handle)) {
            leaveStartDate.set("2026-10-22")
            leaveEndDate.set("2026-10-23")
            leaveType.set("Casual Leave")
            leaveReason.set("Family function")
            leaveAttachmentPath.set("/cache/certificate.jpg")
        }

        val restored = viewModel(handle)

        assertEquals("2026-10-22", restored.leaveStartDate.get())
        assertEquals("2026-10-23", restored.leaveEndDate.get())
        assertEquals("Casual Leave", restored.leaveType.get())
        assertEquals("Family function", restored.leaveReason.get())
        assertEquals("/cache/certificate.jpg", restored.leaveAttachmentPath.get())
    }

    @Test
    fun `a new form starts empty`() {
        val vm = viewModel(SavedStateHandle())

        assertEquals("", vm.leaveStartDate.get())
        assertEquals("", vm.leaveType.get())
        assertEquals("", vm.leaveAttachmentPath.get())
    }

    @Test
    fun `a submitted and cleared form is not restored`() {
        val handle = SavedStateHandle()
        with(viewModel(handle)) {
            leaveStartDate.set("2026-10-22")
            leaveReason.set("Family function")
            clearData()
        }

        val restored = viewModel(handle)

        assertEquals("", restored.leaveStartDate.get())
        assertEquals("", restored.leaveReason.get())
    }
}
