package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.services.LocationTrackingService
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo,
    private val authRepo: IAuthRepo
) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<DashboardClickEvents>()

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking

    var GEOFENCE_LAT = ObservableField<Double>().apply {
        set(0.0)
    }
    var GEOFENCE_LON = ObservableField<Double>().apply {
        set(0.0)
    }

    fun onAnnouncementsClicks() {
        clickEvents.value = DashboardClickEvents.onAnnouncementsClicks
    }

    fun startTracking() {
        _isTracking.value = true
        val serviceIntent = Intent(getApplication(), LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        val serviceIntent = Intent(getApplication(), LocationTrackingService::class.java)
        getApplication<Application>().stopService(serviceIntent)
    }

    var attendanceCheckInResponse = MutableLiveData<ApiState<CheckInOutResponse>>()
    fun checkInAttendance(latitude: Double, longitude: Double) {
        val params = JsonObject().apply {
            addProperty("latitude", latitude)
            addProperty("longitude", longitude)
        }
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceCheckInRequest(params) },
                liveData = attendanceCheckInResponse
            )
        }
    }

    var attendanceCheckOutResponse = MutableLiveData<ApiState<CheckInOutResponse>>()
    fun checkOutAttendance(lat: Double, long: Double) {
        val params = JsonObject().apply {
            addProperty("latitude", lat)
            addProperty("longitude", long)
        }
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceCheckOutRequest(params) },
                liveData = attendanceCheckOutResponse
            )
        }
    }

    var attendanceCheckInStatusResponse = MutableLiveData<ApiState<CheckInStatusResponse>>()
    fun checkInStatusAttendance() {
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceCheckInStatus() },
                liveData = attendanceCheckInStatusResponse
            )
        }
    }

    var empDetailsResponse = MutableLiveData<ApiState<EmpDetailResponse>>()
    fun getEmpDetails() {
        viewModelScope.launch {
            executeApiCall(
                apiCall = { authRepo.empDetails() },
                liveData = empDetailsResponse
            )
        }
    }
}
