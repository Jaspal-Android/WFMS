package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.workSites.WorkSitesResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteResponse
import com.atvantiq.wfms.network.ApiState
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SiteApprovalVM @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo
) : BaseViewModel(application) {

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

    var approveWorkSiteResponse  = MutableLiveData<ApiState<ApproveWorkSiteResponse>>()
    fun approveRejectWorkSite(
        siteWorkId: Long,
        employeeId: Long,
        status: Int,
        remarks: String
    ) {
        val params = JsonObject().apply {
            addProperty("work_site_id", siteWorkId)
            addProperty("employee_id", employeeId)
            addProperty("status", status)
            if (remarks.isNotEmpty()) addProperty("remarks", remarks)
        }
        executeApiCall(
            apiCall = {
                attendanceRepo.approveWorkSite(params)
            },
            liveData = approveWorkSiteResponse
        )
    }
}