package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReimbursementViewModel @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo
) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<ReimbursementClickEvents>()

    private fun postClickEvent(event: ReimbursementClickEvents) {
        clickEvents.value = event
    }

    fun onCreateClaimClick() = postClickEvent(ReimbursementClickEvents.ON_CLICK_CREATE_CLAIM)


    /*Get all claims*/
    var allClaimsResponse = MutableLiveData<ApiState<AllClaimsResponse>>()
    fun getAllClaims(page: Int, pageSize: Int) {
        executeApiCall(
            apiCall = { claimRepo.allClaims(page, pageSize) },
            liveData = allClaimsResponse
        )
    }

    /*Get Claim by ID */
    var claimByIdResponse = MutableLiveData<ApiState<ClaimDetailResponse>>()
    fun getClaimById(claimId: Long) {
        executeApiCall(
            apiCall = { claimRepo.claimById(claimId) },
            liveData = claimByIdResponse
        )
    }
}