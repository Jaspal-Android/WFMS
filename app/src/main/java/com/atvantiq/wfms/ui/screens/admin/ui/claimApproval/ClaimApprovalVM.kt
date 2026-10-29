package com.atvantiq.wfms.ui.screens.admin.ui.claimApproval

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewRecord
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Claims Approval list (spec 8.1): the claims to review, paged, searched on the keyboard's Search key. */
@HiltViewModel
class ClaimApprovalVM @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo,
    prefMain: SecurePrefMain
) : BaseViewModel(application) {

    val role: AppRole = PrefMethods.getAppRole(prefMain)

    /** The search last run; the field's own text only applies once Search is pressed. */
    var appliedSearch: String = ""
        private set

    /** "N claims" beside the header. */
    val claimCount = MutableLiveData<Int?>()

    val claims = PagedList<ClaimReviewListResponse, ClaimReviewRecord>(
        pageSize = ValConstants.APPROVAL_PAGE_SIZE,
        fetch = { page, pageSize -> claimRepo.claimsForReview(page, pageSize, appliedSearch) },
        pageItems = { response ->
            if (response.code == ValConstants.SUCCESS_CODE) {
                val page = response.data
                claimCount.value = page?.totalRecords ?: page?.records?.size
                page?.records.orEmpty()
            } else {
                null
            }
        }
    )

    /** Runs [query] from page 1; the same query again only refreshes. */
    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed == appliedSearch) {
            claims.refresh()
            return
        }
        appliedSearch = trimmed
        claimCount.value = null
        claims.reload()
    }

    /** A claim approved on the detail screen: "Approved by {role}" and its approval total, in place. */
    fun applyApproval(claimId: Long, approvedTotal: Double) {
        val approvedBy = role.approverTag?.let { getApplication<Application>().getString(R.string.approved_by_format, it) }
        val position = claims.state.value?.items?.indexOfFirst { it.claimId == claimId } ?: -1
        claims.updateItem(position) { claim ->
            if (approvedBy != null) claim.status = approvedBy
            claim.latestApprovedAmount = approvedTotal
        }
    }
}
