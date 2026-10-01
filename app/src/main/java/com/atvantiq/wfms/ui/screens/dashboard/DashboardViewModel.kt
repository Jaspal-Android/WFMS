package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.data.tracking.ShiftState
import com.atvantiq.wfms.data.tracking.ShiftTracker
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceRemarks.AttendanceRemarksResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.services.LocationTrackingService
import com.atvantiq.wfms.ui.screens.admin.SharedDashClickEvents
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo,
    private val authRepo: IAuthRepo,
    private val prefMain: SecurePrefMain,
    private val shiftTracker: ShiftTracker
) : BaseViewModel(application) {

    /** Pause state and totals of the current shift, shared with the service and its notification. */
    val shiftState: LiveData<ShiftState> = shiftTracker.state.asLiveData()

    /** When today's day started, from the check-in status or a successful Start Day. */
    private var checkInMillis: Long? = null

    /** Whether tracking was started for the active day. */
    val isTrackingStarted: Boolean get() = prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false)

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

    fun onFetchCurrentLatitudeLongitudeClicks() {
        clickEvents.value = DashboardClickEvents.onFetchCurrentLatitudeLongitudeClicks
    }

    fun startTracking() {
        _isTracking.value = true
        shiftTracker.startShift(checkInMillis ?: System.currentTimeMillis())
        prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, true)
        val serviceIntent = Intent(getApplication(), LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        shiftTracker.endShift()
        prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false)
        val serviceIntent = Intent(getApplication(), LocationTrackingService::class.java)
        getApplication<Application>().stopService(serviceIntent)
    }

    /** Pause 15 min; the same path as the notification's action. */
    fun pauseTracking() = LocationTrackingService.sendAction(getApplication(), LocationTrackingService.ACTION_PAUSE)

    /** Resume early; the same path as the notification's action. */
    fun resumeTracking() = LocationTrackingService.sendAction(getApplication(), LocationTrackingService.ACTION_RESUME)

    // Tracking must end with the attendance day even if no screen is visible to react to the
    // response, so the stop is driven from the API callback rather than from the UI observers.
    private fun stopTrackingIfActive() {
        if (prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false)) stopTracking()
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
                liveData = attendanceCheckInResponse,
                // The day starts now; tracking (started next) counts the shift from here.
                onSuccess = { if (it.code == ValConstants.SUCCESS_CODE) checkInMillis = System.currentTimeMillis() }
            )
        }
    }

    var attendanceCheckOutResponse = MutableLiveData<ApiState<CheckInOutResponse>>()
    fun checkOutAttendance(lat: Double, long: Double,requiredDayProgress:Boolean) {
        val params = JsonObject().apply {
            addProperty("latitude", lat)
            addProperty("longitude", long)
            addProperty("day_progress", requiredDayProgress)
        }
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceCheckOutRequest(params) },
                liveData = attendanceCheckOutResponse,
                onSuccess = { if (it.code == ValConstants.SUCCESS_CODE) stopTracking() }
            )
        }
    }

    var attendanceCheckInStatusResponse = MutableLiveData<ApiState<CheckInStatusResponse>>()
    fun checkInStatusAttendance() {
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceCheckInStatus() },
                liveData = attendanceCheckInStatusResponse,
                onSuccess = {
                    if (it.hasNoActiveDay()) stopTrackingIfActive()
                    else checkInMillis = DateUtils.parseUtcIso(it.data?.checkinTime)
                }
            )
        }
    }

    // Not checked in, or the server reports the day as already completed (400): either way there
    // is no open attendance day left to track, e.g. after a server-side auto checkout.
    private fun CheckInStatusResponse.hasNoActiveDay(): Boolean =
        (code == ValConstants.SUCCESS_CODE && data?.checkedIn != true) ||
            code == ValConstants.BAD_REQUEST_CODE

    var attendanceRemarksResponse = MutableLiveData<ApiState<AttendanceRemarksResponse>>()
    fun setAttendanceEmpRemarks(attendanceId: Long,remarks:String) {
        val params = JsonObject().apply {
            addProperty("remarks", remarks)
        }
        viewModelScope.launch {
            executeApiCall(
                apiCall = { attendanceRepo.attendanceEmpRemarks(attendanceId,params) },
                liveData = attendanceRemarksResponse
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

    fun onLogoutClick(){
        clickEvents.value = DashboardClickEvents.LOGOUT_CLICK
    }

    fun onSitesClick(){
        clickEvents.value = DashboardClickEvents.OPEN_SITES_CLICK
    }

    fun onSitesApprovalsClick(){
        clickEvents.value = DashboardClickEvents.OPEN_SITES_APPROVALS_CLICK
    }

    fun onClaimApprovalsClick(){
        clickEvents.value = DashboardClickEvents.OPEN_CLAIM_APPROVALS_CLICK
    }

    fun onProfileClick(){
        clickEvents.value = DashboardClickEvents.OPEN_PROFILE_CLICK
    }

    fun onApplyLeaveClick(){
        clickEvents.value = DashboardClickEvents.APPLY_LEAVE_CLICK
    }

    fun onChangeThemeClick() {
        clickEvents.value = DashboardClickEvents.CHANGE_THEME_CLICK
    }
}
