package com.atvantiq.wfms.ui.screens.admin.ui.claimApproval

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.widget.doAfterTextChanged
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityClaimApprovalDetailBinding
import com.atvantiq.wfms.databinding.ItemClaimReviewExpenseBinding
import com.atvantiq.wfms.databinding.ItemClaimReviewSiteBinding
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.review.ExpenseDecisionInput
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Claim Approval (spec 8.2): confirm an approved amount for every expense and submit once. The
 * approval is handed back to the list, which updates the row.
 */
@AndroidEntryPoint
class ClaimApprovalDetailActivity : BaseActivity<ActivityClaimApprovalDetailBinding, ClaimApprovalDetailVM>() {

    private var claimId: Long = NO_ID

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_approval_detail, ClaimApprovalDetailVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        binding.toolbar.toolbarTitle.text = getString(R.string.claim_approval)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        claimId = intent.getLongExtra(SharingKeys.CLAIM_ID, NO_ID)
        if (claimId == NO_ID) {
            showToast(this, getString(R.string.claim_id_missing))
            finish()
            return
        }
        binding.btnSubmit.setOnClickListener { viewModel.submit() }
        viewModel.load(claimId)
    }

    override fun subscribeToEvents(vm: ClaimApprovalDetailVM) {
        vm.claimResponse.observe(this) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.LOADING -> showProgress()
                Status.SUCCESS -> {
                    dismissProgress()
                    val answer = response.response
                    if (answer?.code != ValConstants.SUCCESS_CODE) handleRejectedResponse(answer?.code, answer?.message)
                }
                Status.ERROR -> {
                    dismissProgress()
                    handleApiFailure(response.throwable)
                }
            }
        }
        vm.claim.observe(this) { claim -> claim?.let(::render) }
        vm.approvalTotal.observe(this) { total -> binding.approvalTotal = getString(R.string.rupee_format, total) }
        vm.isReviewed.observe(this) { reviewed ->
            binding.reviewed = reviewed
            binding.canAct = vm.canAct
        }
        vm.decisionError.observe(this) { error ->
            error ?: return@observe
            showToast(this, getString(error))
        }
        vm.approveResponse.observe(this) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.LOADING -> showProgress()
                Status.SUCCESS -> {
                    dismissProgress()
                    val answer = response.response
                    if (answer?.success == true) {
                        answer.message?.let { showToast(this, it) }
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra(SharingKeys.CLAIM_ID, claimId)
                            putExtra(SharingKeys.APPROVED_TOTAL, vm.approvalTotal.value ?: 0.0)
                        })
                    } else {
                        handleRejectedResponse(answer?.code, answer?.message)
                    }
                }
                Status.ERROR -> {
                    dismissProgress()
                    handleApiFailure(response.throwable)
                }
            }
        }
    }

    private fun render(claim: ClaimData) {
        binding.claim = claim
        binding.claimIdLabel = claim.id?.let { getString(R.string.claim_id_format, it) }
        binding.amount = getString(R.string.rupee_format, claim.totalAmount ?: 0.0)
        val unknown = getString(R.string.unknown_value)
        binding.dateLabel = DateUtils.formatYmdLabel(claim.date) ?: claim.date ?: unknown
        binding.category = claim.claimCategory.orUnknown(unknown)
        binding.purpose = claim.claimPurpose.orUnknown(unknown)
        binding.route = getString(
            R.string.travel_route_format,
            claim.travellingFrom.orUnknown(unknown),
            claim.travellingTo.orUnknown(unknown)
        )
        binding.canAct = viewModel.canAct
        renderExpenses(viewModel.inputs)
    }

    private fun String?.orUnknown(unknown: String): String = this?.takeIf { it.isNotBlank() } ?: unknown

    /** A numbered card per site, one tile per expense; typing goes straight into the ViewModel. */
    private fun renderExpenses(inputs: List<ExpenseDecisionInput>) {
        val container = binding.sitesContainer
        container.removeAllViews()
        inputs.groupBy { it.siteNumber }.forEach { (number, expenses) ->
            val first = expenses.first()
            val site = ItemClaimReviewSiteBinding.inflate(layoutInflater, container, false)
            site.number = number.toString()
            site.name = first.siteName ?: getString(R.string.unknown_value)
            site.expenseCount = resources.getQuantityString(R.plurals.expense_count, expenses.size, expenses.size)
            site.amount = getString(R.string.rupee_format, first.siteAmount ?: expenses.sumOf { it.claimed })
            expenses.forEach { input -> site.expensesContainer.addView(expenseView(site, input)) }
            site.executePendingBindings()
            container.addView(site.root)
        }
    }

    private fun expenseView(site: ItemClaimReviewSiteBinding, input: ExpenseDecisionInput) =
        ItemClaimReviewExpenseBinding.inflate(layoutInflater, site.expensesContainer, false).apply {
            type = input.expenseType ?: getString(R.string.unknown_value)
            claimed = getString(R.string.claimed_format, getString(R.string.rupee_format, input.claimed))
            val editable = viewModel.canAct && input.expenseId != null
            etAmount.setText(input.amountText)
            etRemarks.setText(input.remarks)
            etAmount.isEnabled = editable
            etRemarks.isEnabled = editable
            if (input.expenseId == null) amountLayout.error = getString(R.string.expense_id_missing)
            etAmount.doAfterTextChanged {
                input.amountText = it?.toString().orEmpty()
                viewModel.onInputsChanged()
            }
            etRemarks.doAfterTextChanged { input.remarks = it?.toString().orEmpty() }
            executePendingBindings()
        }.root

    private companion object {
        const val NO_ID = -1L
    }
}
