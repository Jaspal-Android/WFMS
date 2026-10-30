package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.Data
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
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

/** The employee's Claims list shows one month at a time (`from_date` / `to_date`). */
@ExperimentalCoroutinesApi
class ClaimsMonthListTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repo = mockk<IClaimRepo>()

    private fun claim(id: Long) = mockk<Record>(relaxed = true) { every { claimId } returns id }

    private fun response(vararg claims: Record, totalRecords: Int? = claims.size) = AllClaimsResponse(
        code = ValConstants.SUCCESS_CODE,
        data = Data(
            page = 1, pageSize = ValConstants.DEFAULT_PAGE_SIZE, records = claims.toList(),
            totalCount = 99, totalPages = 1, totalRecords = totalRecords
        ),
        message = "ok",
        success = true
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
    fun `the current month is requested and counted`() {
        val now = MonthYear.current()
        coEvery { repo.allClaims(1, ValConstants.DEFAULT_PAGE_SIZE, now.firstDay, now.lastDay) } returns
            response(claim(1), claim(2), totalRecords = 12)
        val vm = ReimbursementViewModel(application, repo)

        vm.claims.open()
        idle()

        assertEquals(listOf(1L, 2L), vm.claims.state.value!!.items.map { it.claimId })
        assertEquals(12, vm.claimCount.value)
    }

    @Test
    fun `stepping back a month asks for that month and clears the count until it arrives`() {
        coEvery { repo.allClaims(any(), any(), any(), any()) } returns response(claim(1), totalRecords = 3)
        val vm = ReimbursementViewModel(application, repo)
        vm.claims.open()
        idle()
        val previous = vm.month.value!!.previous()

        vm.showPreviousMonth()

        assertEquals(previous, vm.month.value)
        assertNull(vm.claimCount.value)
        idle()
        coVerify { repo.allClaims(1, ValConstants.DEFAULT_PAGE_SIZE, previous.firstDay, previous.lastDay) }
        assertEquals(3, vm.claimCount.value)
    }

    @Test
    fun `stepping forward moves to the next month`() {
        coEvery { repo.allClaims(any(), any(), any(), any()) } returns response()
        val vm = ReimbursementViewModel(application, repo)
        val next = vm.month.value!!.next()

        vm.showNextMonth()
        idle()

        assertEquals(next, vm.month.value)
        coVerify { repo.allClaims(1, ValConstants.DEFAULT_PAGE_SIZE, next.firstDay, next.lastDay) }
    }

    @Test
    fun `a rejected answer adds nothing and leaves no count`() {
        coEvery { repo.allClaims(any(), any(), any(), any()) } returns AllClaimsResponse(500, null, "boom", false)
        val vm = ReimbursementViewModel(application, repo)

        vm.claims.open()
        idle()

        assertEquals(emptyList<Record>(), vm.claims.state.value!!.items)
        assertNull(vm.claimCount.value)
    }
}
