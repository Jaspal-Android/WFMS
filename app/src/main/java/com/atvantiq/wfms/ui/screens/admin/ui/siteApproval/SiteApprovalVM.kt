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
            apiCall = { attendanceRepo.attendanceDetails(month, year) },
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
    fun approveRejectWorkSite(
        workSiteId: Long,
        employeeId: Long,
        status: Int,
        remarks: String,
        selectedTypes: List<WorkType>?
    ) {
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
            liveData = approveWorkSiteResponse
        )
    }
}