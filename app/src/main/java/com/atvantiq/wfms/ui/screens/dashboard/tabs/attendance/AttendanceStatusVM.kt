package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceStatusVM @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo
) : AndroidViewModel(application) {



    /*
    * Attendance details API
    * */
    var attendanceDetailsResponse = MutableLiveData<ApiState<AttendanceDetailListResponse>>()
    fun getAttendanceDetails(month:Int, year:Int) {
        if (Utils.isInternet(getApplication())) {
             viewModelScope.launch {
                attendanceDetailsResponse.postValue(ApiState.loading())
                try {
                    var response = attendanceRepo.attendanceDetails(month,year)
                    attendanceDetailsResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    attendanceDetailsResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            attendanceDetailsResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

}