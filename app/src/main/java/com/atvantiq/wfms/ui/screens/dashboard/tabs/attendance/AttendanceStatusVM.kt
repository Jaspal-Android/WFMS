package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AttendanceStatusVM @Inject constructor(
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
}
