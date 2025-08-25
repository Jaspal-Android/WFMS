package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.auth.AuthRepo
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
) : AndroidViewModel(application) {

    // variables initializations
    var clickEvents = MutableLiveData<DashboardClickEvents>()


    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking

    // Methods
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


        //*----------------------------API's------------------------------*//*

    /*
     * Attendance Check In API
     */
    var attendanceCheckInResponse = MutableLiveData<ApiState<CheckInOutResponse>>()
    fun checkInAttendance(latitude: Double, longitude: Double) {
        if (Utils.isInternet(getApplication())) {
            var params = JsonObject()
            params.addProperty("latitude", latitude)
            params.addProperty("longitude",longitude)

            viewModelScope.launch {
                attendanceCheckInResponse.postValue(ApiState.loading())
                try {
                    var response = attendanceRepo.attendanceCheckInRequest(params)
                    attendanceCheckInResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    attendanceCheckInResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            attendanceCheckInResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }


    /*
     * Attendance Check Out API
     */
    var attendanceCheckOutResponse = MutableLiveData<ApiState<CheckInOutResponse>>()
    fun checkOutAttendance(lat: Double, long: Double) {
        if (Utils.isInternet(getApplication())) {
            var params = JsonObject()
            params.addProperty("latitude", lat)
            params.addProperty("longitude",long)

            viewModelScope.launch {
                attendanceCheckOutResponse.postValue(ApiState.loading())
                try {
                    var response = attendanceRepo.attendanceCheckOutRequest(params)
                    attendanceCheckOutResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    attendanceCheckOutResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            attendanceCheckOutResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
     * Attendance Check In Status API
     */
    var attendanceCheckInStatusResponse = MutableLiveData<ApiState<CheckInStatusResponse>>()
    fun checkInStatusAttendance() {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                attendanceCheckInStatusResponse.postValue(ApiState.loading())
                try {
                    var response = attendanceRepo.attendanceCheckInStatus()
                    attendanceCheckInStatusResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    attendanceCheckInStatusResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            attendanceCheckInStatusResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * get Emp details API
    * */
    var empDetailsResponse = MutableLiveData<ApiState<EmpDetailResponse>>()
    fun getEmpDetails() {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                empDetailsResponse.postValue(ApiState.loading())
                try {
                    var response = authRepo.empDetails()
                    empDetailsResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    empDetailsResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            empDetailsResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

}