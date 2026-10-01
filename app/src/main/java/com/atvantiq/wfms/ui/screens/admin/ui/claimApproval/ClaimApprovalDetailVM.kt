package com.atvantiq.wfms.ui.screens.admin.ui.claimApproval

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimApproveResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimDecision
import com.atvantiq.wfms.models.reimbursement.review.ExpenseDecisionInput
import com.atvantiq.wfms.models.reimbursement.review.canReviewClaim
import com.atvantiq.wfms.network.ApiState
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Claim Approval detail (spec 8.2): one claim, an approved amount per expense, one submit. */
@HiltViewModel
class ClaimApprovalDetailVM @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo,
    prefMain: SecurePrefMain
) : BaseViewModel(application) {

    private val role: AppRole = PrefMethods.getAppRole(prefMain)

    val claimResponse = MutableLiveData<ApiState<ClaimDetailResponse>>()
    val approveResponse = MutableLiveData<ApiState<ClaimApproveResponse>>()

    val claim = MutableLiveData<ClaimData?>()

    /** The expenses with what has been typed; kept here so typing survives rotation. */
    var inputs: List<ExpenseDecisionInput> = emptyList()
        private set

    /** The live sum of the approved amounts. */
    val approvalTotal = MutableLiveData(0.0)

    /** True once this claim was approved in this session: the bar reads "Claim Reviewed". */
    val isReviewed = MutableLiveData(false)

    /** A decision that can't be sent, as a message to show. One-shot. */
    val decisionError = MutableLiveData<Int?>()

    private var isSubmitting = false

    /** Whether this role may act on the loaded claim. */
    val canAct: Boolean
        get() = canReviewClaim(claim.value?.status, role) && isReviewed.value != true

    fun load(claimId: Long) {
        if (claim.value != null) return
        executeApiCall(
            apiCall = { claimRepo.claimById(claimId) },
            liveData = claimResponse,
            onSuccess = { response ->
                if (response.code == ValConstants.SUCCESS_CODE) {
                    inputs = ClaimDecision.inputsFor(response.data)
                    approvalTotal.value = ClaimDecision.total(inputs)
                    claim.value = response.data
                }
            }
        )
    }

    fun onInputsChanged() {
        approvalTotal.value = ClaimDecision.total(inputs)
    }

    fun submit() {
        if (isSubmitting) return
        val claimId = claim.value?.id ?: return
        if (isReviewed.value == true) {
            decisionError.value = R.string.claim_already_reviewed
            return
        }
        if (!canAct) {
            decisionError.value = R.string.claim_decision_not_permitted
            return
        }
        when (val decision = ClaimDecision.build(claimId, inputs)) {
            is ClaimDecision.Invalid -> decisionError.value = decision.messageRes
            is ClaimDecision.Valid -> {
                isSubmitting = true
                executeApiCall(
                    apiCall = { claimRepo.approveClaim(decision.body) },
                    liveData = approveResponse,
                    onSuccess = { response ->
                        isSubmitting = false
                        if (response.success == true) isReviewed.value = true
                    },
                    onError = { isSubmitting = false }
                )
            }
        }
    }
}
