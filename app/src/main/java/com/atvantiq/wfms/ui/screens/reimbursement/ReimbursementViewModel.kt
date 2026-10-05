package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.MonthYear
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReimbursementViewModel @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo
) : BaseViewModel(application) {

    var clickEvents = LiveEvent<ReimbursementClickEvents>()

    private fun postClickEvent(event: ReimbursementClickEvents) {
        clickEvents.value = event
    }

    fun onCreateClaimClick() = postClickEvent(ReimbursementClickEvents.ON_CLICK_CREATE_CLAIM)


    /** The month on screen; opens on the current one. */
    val month = MutableLiveData(MonthYear.current())

    /** Claims in [month] (`total_records`). */
    val claimCount = MutableLiveData<Int?>()

    /** The employee's claims in [month], paged. ◀ ▶ step the month and reload from page 1. */
    val claims = PagedList<AllClaimsResponse, Record>(
        fetch = { page, pageSize ->
            val shown = month.value ?: MonthYear.current()
            claimRepo.allClaims(page, pageSize, shown.firstDay, shown.lastDay)
        },
        pageItems = { response ->
            if (response.code == ValConstants.SUCCESS_CODE) {
                claimCount.value = response.data?.totalRecords
                response.data?.records.orEmpty()
            } else {
                null
            }
        }
    )

    fun showPreviousMonth() = showMonth(month.value?.previous())

    fun showNextMonth() = showMonth(month.value?.next())

    private fun showMonth(target: MonthYear?) {
        month.value = target ?: return
        claimCount.value = null
        claims.reload()
    }

    /*Get Claim by ID */
    var claimByIdResponse = MutableLiveData<ApiState<ClaimDetailResponse>>()
    fun getClaimById(claimId: Long) {
        executeApiCall(
            apiCall = { claimRepo.claimById(claimId) },
            liveData = claimByIdResponse
        )
    }

    /*Delete own claim*/
    var deleteClaimResponse = MutableLiveData<ApiState<DeleteClaimResponse>>()
    private var isDeleting = false
    fun deleteClaim(claimId: Long) {
        if (isDeleting) return
        // Fail before taking the lock, so an offline attempt cannot leave isDeleting set.
        if (!Utils.isInternet(getApplication())) {
            deleteClaimResponse.value = ApiState.error(noInternetError())
            return
        }
        isDeleting = true
        executeApiCall(
            apiCall = { claimRepo.deleteClaim(claimId) },
            liveData = deleteClaimResponse,
            onSuccess = { isDeleting = false },
            onError = { isDeleting = false }
        )
    }
}
