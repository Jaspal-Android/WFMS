package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
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

    /*Delete own claim*/
    var deleteClaimResponse = MutableLiveData<ApiState<DeleteClaimResponse>>()
    private var isDeleting = false
    fun deleteClaim(claimId: Long) {
        if (isDeleting) return
        // executeApiCall does not invoke onError when offline, so check first; otherwise
        // isDeleting would stay set and block every later delete on this screen.
        if (!Utils.isInternet(getApplication())) {
            deleteClaimResponse.value = ApiState.error(NoInternetException("No Internet Connection"))
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
