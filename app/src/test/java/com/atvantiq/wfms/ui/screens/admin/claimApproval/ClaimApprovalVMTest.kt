package com.atvantiq.wfms.ui.screens.admin.claimApproval

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewPage
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewRecord
import com.atvantiq.wfms.ui.screens.admin.ui.claimApproval.ClaimApprovalVM
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
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
class ClaimApprovalVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val claimRepo = mockk<IClaimRepo>()
    private val prefMain = mockk<SecurePrefMain>(relaxed = true)

    private fun claim(id: Long, status: String? = "Submitted", approved: Double? = null) = ClaimReviewRecord(
        claimId = id, claimNumber = "C$id", date = null, createdAt = null, employeeId = 1, employeeName = "Asha",
        employeeCode = "E1", expenseCategory = null, totalAmount = 100.0, latestApprovedAmount = approved, status = status
    )

    private fun page(vararg records: ClaimReviewRecord, total: Int? = records.size) =
        ClaimReviewListResponse(ValConstants.SUCCESS_CODE, ClaimReviewPage(records.toList(), total), "ok", true)

    private fun viewModel(role: AppRole = AppRole.PM): ClaimApprovalVM {
        every { PrefMethods.getAppRole(prefMain) } returns role
        return ClaimApprovalVM(application, claimRepo, prefMain)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils, PrefMethods)
        every { Utils.isInternet(application) } returns true
        every { application.getString(R.string.approved_by_format, "PM") } returns "Approved by PM"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `opening loads the first page and shows the total as the claim count`() {
        coEvery { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "") } returns page(claim(1), claim(2), total = 658)
        val vm = viewModel()

        vm.claims.open()
        idle()

        assertEquals(listOf(1L, 2L), vm.claims.state.value!!.items.map { it.claimId })
        assertEquals(658, vm.claimCount.value)
    }

    @Test
    fun `the count falls back to the page size when the server sends no total`() {
        coEvery { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "") } returns page(claim(1), total = null)
        val vm = viewModel()

        vm.claims.open()
        idle()

        assertEquals(1, vm.claimCount.value)
    }

    @Test
    fun `a new search is trimmed, clears the count and reloads from page 1`() {
        coEvery { claimRepo.claimsForReview(any(), any(), any()) } returns page(claim(1))
        val vm = viewModel()
        vm.claims.open()
        idle()

        vm.search("  Asha  ")

        assertEquals("Asha", vm.appliedSearch)
        assertNull(vm.claimCount.value)
        idle()
        coVerify { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "Asha") }
    }

    @Test
    fun `searching the same text again only refreshes and keeps the count`() {
        coEvery { claimRepo.claimsForReview(any(), any(), any()) } returns page(claim(1), total = 5)
        val vm = viewModel()
        vm.search("Asha")
        idle()

        vm.search("Asha ")
        idle()

        assertEquals(5, vm.claimCount.value)
        coVerify(exactly = 2) { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "Asha") }
    }

    @Test
    fun `a rejected page adds no items`() {
        coEvery { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "") } returns
            ClaimReviewListResponse(500, null, "boom", false)
        val vm = viewModel()

        vm.claims.open()
        idle()

        assertEquals(emptyList<ClaimReviewRecord>(), vm.claims.state.value!!.items)
        assertNull(vm.claimCount.value)
    }

    @Test
    fun `an approval updates that claim in place with the role and the approved total`() {
        coEvery { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "") } returns page(claim(1), claim(2))
        val vm = viewModel(AppRole.PM)
        vm.claims.open()
        idle()

        vm.applyApproval(2L, 75.5)

        val items = vm.claims.state.value!!.items
        assertEquals("Submitted", items[0].status)
        assertEquals("Approved by PM", items[1].status)
        assertEquals(75.5, items[1].latestApprovedAmount!!, 0.0)
    }

    @Test
    fun `an approval for a claim that is not listed changes nothing`() {
        coEvery { claimRepo.claimsForReview(1, ValConstants.APPROVAL_PAGE_SIZE, "") } returns page(claim(1))
        val vm = viewModel()
        vm.claims.open()
        idle()

        vm.applyApproval(99L, 10.0)

        assertEquals("Submitted", vm.claims.state.value!!.items.single().status)
    }
}
