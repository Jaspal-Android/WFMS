package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.applyLeave.ApplyLeaveResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveClickEvents
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveErrorHandler
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveVM
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
class ApplyLeaveVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repo = mockk<IAttendanceRepo>()
    private lateinit var vm: ApplyLeaveVM

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        vm = ApplyLeaveVM(application, repo, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun fillForm(
        start: String = "2026-10-05",
        end: String = "2026-10-07",
        type: String = "Casual",
        reason: String = "Family function"
    ) {
        vm.leaveStartDate.set(start)
        vm.leaveEndDate.set(end)
        vm.leaveType.set(type)
        vm.leaveReason.set(reason)
    }

    @Test
    fun `the first missing field is the one reported`() {
        vm.onClickSubmitLeave()
        assertEquals(ApplyLeaveErrorHandler.START_DATE_EMPTY, vm.errorHandler.value)

        vm.leaveStartDate.set("2026-10-05")
        vm.onClickSubmitLeave()
        assertEquals(ApplyLeaveErrorHandler.END_DATE_EMPTY, vm.errorHandler.value)

        vm.leaveEndDate.set("2026-10-06")
        vm.onClickSubmitLeave()
        assertEquals(ApplyLeaveErrorHandler.LEAVE_TYPE_EMPTY, vm.errorHandler.value)

        vm.leaveType.set("Casual")
        vm.onClickSubmitLeave()
        assertEquals(ApplyLeaveErrorHandler.LEAVE_REASON_EMPTY, vm.errorHandler.value)
    }

    @Test
    fun `blank text counts as missing`() {
        fillForm(reason = "   ")

        vm.onClickSubmitLeave()

        assertEquals(ApplyLeaveErrorHandler.LEAVE_REASON_EMPTY, vm.errorHandler.value)
    }

    @Test
    fun `an end date before the start date is rejected and nothing is sent`() {
        fillForm(start = "2026-10-07", end = "2026-10-05")

        vm.onClickSubmitLeave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ApplyLeaveErrorHandler.START_DATE_AFTER_END_DATE, vm.errorHandler.value)
        coVerify(exactly = 0) { repo.applyLeave(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a one-day leave is allowed`() {
        coEvery { repo.applyLeave(any(), any(), any(), any(), any()) } returns mockk<ApplyLeaveResponse>(relaxed = true)
        fillForm(start = "2026-10-05", end = "2026-10-05")

        vm.onClickSubmitLeave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.errorHandler.value)
        coVerify(exactly = 1) { repo.applyLeave(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a valid form is sent and the answer is published`() {
        val answer = mockk<ApplyLeaveResponse>(relaxed = true)
        coEvery { repo.applyLeave(any(), any(), any(), any(), null) } returns answer
        fillForm()

        vm.onClickSubmitLeave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Status.SUCCESS, vm.applyLeaveResponse.value!!.status)
        assertEquals(answer, vm.applyLeaveResponse.value!!.response)
    }

    @Test
    fun `an attachment path that does not exist is not sent`() {
        coEvery { repo.applyLeave(any(), any(), any(), any(), null) } returns mockk<ApplyLeaveResponse>(relaxed = true)
        fillForm()
        vm.leaveAttachmentPath.set("/no/such/file.jpg")

        vm.onClickSubmitLeave()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.applyLeave(any(), any(), any(), any(), null) }
    }

    @Test
    fun `clearing the form empties every field`() {
        fillForm()
        vm.leaveAttachmentPath.set("/tmp/a.jpg")

        vm.clearData()

        listOf(vm.leaveStartDate, vm.leaveEndDate, vm.leaveType, vm.leaveReason, vm.leaveAttachmentPath)
            .forEach { assertEquals("", it.get()) }
    }

    @Test
    fun `taps are posted as click events`() {
        vm.onClickStartDate()
        assertEquals(ApplyLeaveClickEvents.START_DATE_CLICK, vm.clickEvents.value)
        vm.onClickLeaveType()
        assertEquals(ApplyLeaveClickEvents.LEAVE_TYPE_CLICK, vm.clickEvents.value)
        vm.onClickAttachment()
        assertEquals(ApplyLeaveClickEvents.ATTACHMENT_CLICK, vm.clickEvents.value)
    }
}
