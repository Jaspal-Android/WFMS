package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteTypeResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkSiteDetailResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType
import com.atvantiq.wfms.models.workSites.workSites.WorkSitesResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SiteApprovalVM @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo
) : BaseViewModel(application) {

    var itemPosition = MutableLiveData<Int>().apply { value = -1 }

    var attendanceDetailsResponse = MutableLiveData<ApiState<AttendanceDetailListResponse>>()
    fun getAttendanceDetails(month: Int, year: Int) {

        executeApiCall(
            apiCall = { attendanceRepo.attendanceDetails(month, year,true) },
            liveData = attendanceDetailsResponse
        )
    }

    var workSites  = MutableLiveData<ApiState<WorkSitesResponse>>()
    fun getWorkSites(employeeId: String,date: String) {
        executeApiCall(
            apiCall = { attendanceRepo.workSites(employeeId,date) },
            liveData = workSites
        )
    }

    var workSiteDetails = MutableLiveData<ApiState<WorkSiteDetailResponse>>()
    fun getWorkSiteDetails(workSiteId: Long,employeeId: String,date: String) {
        executeApiCall(
            apiCall = { attendanceRepo.workSiteDetailsAdmin(workSiteId,employeeId,date) },
            liveData = workSiteDetails
        )
    }

    var approveWorkSiteResponse  = MutableLiveData<ApiState<ApproveWorkSiteTypeResponse>>()
    private var isApproving = false
    fun approveRejectWorkSite(
        workSiteId: Long,
        employeeId: Long,
        status: Int,
        remarks: String,
        selectedTypes: List<WorkType>?
    ) {
        if (isApproving) return
        // Never send an approval with a sentinel id or nothing selected.
        if (workSiteId <= 0 || employeeId <= 0 || selectedTypes.isNullOrEmpty()) return
        // executeApiCall does not invoke onError when offline, so check first; otherwise
        // isApproving would stay set and block every later approval on this screen.
        if (!Utils.isInternet(getApplication())) {
            approveWorkSiteResponse.value = ApiState.error(NoInternetException("No Internet Connection"))
            return
        }
        isApproving = true
        val paramsArray = JsonArray()
        selectedTypes?.forEach { workType ->
            val params = JsonObject().apply {
                addProperty("work_site_id", workSiteId)
                addProperty("type_id", workType.id)
                addProperty("employee_id", employeeId)
                addProperty("status", status)
                addProperty("remarks",remarks)
            }
            paramsArray.add(params)
        }
        executeApiCall(
            apiCall = {
                attendanceRepo.approveWorkSite(paramsArray)
            },
            liveData = approveWorkSiteResponse,
            onSuccess = { isApproving = false },
            onError = { isApproving = false }
        )
    }
}