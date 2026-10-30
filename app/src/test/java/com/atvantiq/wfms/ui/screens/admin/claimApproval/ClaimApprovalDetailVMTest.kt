package com.atvantiq.wfms.ui.screens.admin.claimApproval

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimApproveResponse
import com.atvantiq.wfms.ui.screens.admin.ui.claimApproval.ClaimApprovalDetailVM
import com.atvantiq.wfms.utils.Utils
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ClaimApprovalDetailVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val claimRepo = mockk<IClaimRepo>()
    private val prefMain = mockk<SecurePrefMain>(relaxed = true)
    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private fun detail(status: String = "Submitted") = gson.fromJson(
        """{ "code": 200, "message": "ok", "success": true, "data": { "id": 42, "status": "$status",
            "sites": [ { "site_name": "Site A", "site_code": "A1", "amount_site": 50.0, "expenses": [
              { "expense_id": 10, "expense_type": "Travel", "claimed_amount": 20.0 },
              { "expense_id": 11, "expense_type": "Food", "claimed_amount": 30.5 } ] } ] } }""",
        ClaimDetailResponse::class.java
    )

    private fun viewModel(role: AppRole = AppRole.PM): ClaimApprovalDetailVM {
        every { PrefMethods.getAppRole(prefMain) } returns role
        return ClaimApprovalDetailVM(application, claimRepo, prefMain)
    }

    private fun loaded(role: AppRole = AppRole.PM, status: String = "Submitted"): ClaimApprovalDetailVM {
        coEvery { claimRepo.claimById(42L) } returns detail(status)
        return viewModel(role).also {
            it.load(42L)
            idle()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils, PrefMethods)
        every { Utils.isInternet(application) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `loading prefills every expense with its claimed amount and totals them`() {
        val vm = loaded()

        assertEquals(listOf("20", "30.5"), vm.inputs.map { it.amountText })
        assertEquals(50.5, vm.approvalTotal.value!!, 0.0)
        assertTrue(vm.canAct)
    }

    @Test
    fun `a second load of the same claim does not call the server again`() {
        val vm = loaded()

        vm.load(42L)
        idle()

        coVerify(exactly = 1) { claimRepo.claimById(42L) }
    }

    @Test
    fun `typing an amount updates the total`() {
        val vm = loaded()

        vm.inputs[0].amountText = "5"
        vm.onInputsChanged()

        assertEquals(35.5, vm.approvalTotal.value!!, 0.0)
    }

    @Test
    fun `an employee, or a claim already past this role, cannot act`() {
        assertFalse(loaded(AppRole.EMPLOYEE).canAct)
        assertFalse(loaded(AppRole.PM, status = "Approved by PM").canAct)
    }

    @Test
    fun `submitting a role that may not act reports it and sends nothing`() {
        val vm = loaded(AppRole.EMPLOYEE)

        vm.submit()
        idle()

        assertEquals(R.string.claim_decision_not_permitted, vm.decisionError.value)
        coVerify(exactly = 0) { claimRepo.approveClaim(any()) }
    }

    @Test
    fun `an invalid amount is reported and nothing is sent`() {
        val vm = loaded()
        vm.inputs[0].amountText = ""

        vm.submit()
        idle()

        assertEquals(R.string.enter_valid_approved_amounts, vm.decisionError.value)
        coVerify(exactly = 0) { claimRepo.approveClaim(any()) }
    }

    @Test
    fun `a valid decision is sent once and marks the claim reviewed`() {
        val body = slot<JsonObject>()
        coEvery { claimRepo.approveClaim(capture(body)) } returns ClaimApproveResponse(200, "ok", true)
        val vm = loaded()

        vm.submit()
        vm.submit()
        idle()

        coVerify(exactly = 1) { claimRepo.approveClaim(any()) }
        assertEquals(42L, body.captured.getAsJsonArray("claims")[0].asJsonObject.get("claim_id").asLong)
        assertEquals(true, vm.isReviewed.value)
        assertFalse(vm.canAct)
    }

    @Test
    fun `after a successful review a second submit says it was already reviewed`() {
        coEvery { claimRepo.approveClaim(any()) } returns ClaimApproveResponse(200, "ok", true)
        val vm = loaded()
        vm.submit()
        idle()

        vm.submit()

        assertEquals(R.string.claim_already_reviewed, vm.decisionError.value)
    }

    @Test
    fun `a rejected approval leaves the claim open for another attempt`() {
        coEvery { claimRepo.approveClaim(any()) } returns ClaimApproveResponse(400, "no", false)
        val vm = loaded()

        vm.submit()
        idle()

        assertNull(vm.decisionError.value)
        assertTrue(vm.canAct)
        vm.submit()
        idle()
        coVerify(exactly = 2) { claimRepo.approveClaim(any()) }
    }
}
