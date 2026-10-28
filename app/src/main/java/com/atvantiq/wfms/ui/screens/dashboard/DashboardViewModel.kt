package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import android.content.Intent
import android.os.Build
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.tracking.ShiftState
import com.atvantiq.wfms.data.tracking.ShiftTracker
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceRemarks.AttendanceRemarksResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.project.ManagedProjectsResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.services.LocationTrackingService
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPicker
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerPurpose
import com.atvantiq.wfms.ui.screens.projectPicker.toProjectOption
import com.atvantiq.wfms.utils.DateUtils
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo,
    private val prefMain: SecurePrefMain,
    private val shiftTracker: ShiftTracker,
    private val creationRepo: ICreationRepo
) : BaseViewModel(application) {

    /** Pause state and totals of the current shift, shared with the service and its notification. */
    val shiftState: LiveData<ShiftState> = shiftTracker.state.asLiveData()

    /** When today's day started, from the check-in status or a successful Start Day. */
    private var checkInMillis: Long? = null

    /** Whether tracking was started for the active day. */
    val isTrackingStarted: Boolean get() = prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false)

    var clickEvents = LiveEvent<DashboardClickEvents>()


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

    /** The project a PM's Start Day is booked to. */
    val projectPicker = ProjectPicker()
    val managedProjectsResponse = MutableLiveData<ApiState<ManagedProjectsResponse>>()

    /** Today's attendance, from the status or a check-in; it stays after a check-out. */
    private var todayAttendanceId: Long? = null

    /**
     * A PM's first check-in of the day is booked to a project they choose. Later check-ins the
     * same day keep that project (the server ignores a new one), so they don't ask again.
     */
    val needsProjectToStartDay: Boolean
        get() = todayAttendanceId == null && PrefMethods.getAppRole(prefMain) == AppRole.PM

    /** `GET /project/my-projects`: opens the picker on the PM's projects for today. */
    fun loadStartDayProjects() {
        projectPicker.startLoading(ProjectPickerPurpose.StartDay)
        executeApiCall(
            apiCall = { creationRepo.managedProjects() },
            liveData = managedProjectsResponse,
            onSuccess = { response ->
                projectPicker.onResponse(
                    response.success, response.code, response.message,
                    response.data?.mapNotNull { it.toProjectOption() }
                )
            },
            onError = projectPicker::onError,
            cancelPrevious = true
        )
    }

    var attendanceCheckInResponse = MutableLiveData<ApiState<CheckInOutResponse>>()

    /** `POST /attendance/checkin`, with the project chosen in [projectPicker] when one is needed. */
    fun checkInAttendance(latitude: Double, longitude: Double) {
        val projectId = projectPicker.selectedProjectId
        // The server doesn't require a PM's project yet, so the app never starts that day without one.
        if (projectId == null && needsProjectToStartDay) {
            attendanceCheckInResponse.value = ApiState.error(
                IllegalStateException(getApplication<Application>().getString(R.string.select_project_to_start_day))
            )
            return
        }
        val params = JsonObject().apply {
            addProperty("latitude", latitude)
            addProperty("longitude", longitude)
            projectId?.let { addProperty(PROJECT_ID_KEY, it) }
        }
        executeApiCall(
            apiCall = { attendanceRepo.attendanceCheckInRequest(params) },
            liveData = attendanceCheckInResponse,
            onSuccess = {
                if (it.isAccepted) {
                    // The day starts now; tracking (started next) counts the shift from here.
                    checkInMillis = System.currentTimeMillis()
                    todayAttendanceId = it.data?.attendanceId?.takeIf { id -> id > 0 } ?: todayAttendanceId
                }
            }
        )
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
                    if (it.code == ValConstants.SUCCESS_CODE) todayAttendanceId = it.data?.attendanceId?.takeIf { id -> id > 0 }
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

    fun onApplyLeaveClick(){
        clickEvents.value = DashboardClickEvents.APPLY_LEAVE_CLICK
    }

    private companion object {
        const val PROJECT_ID_KEY = "project_id"
    }
}

/** The server marked the attendance: `success: false` is a refusal, even with code 200. */
val CheckInOutResponse.isAccepted: Boolean
    get() = success && code == ValConstants.SUCCESS_CODE
