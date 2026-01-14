package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.Type
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.services.LocationTrackingService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject


@HiltViewModel
class AttendanceViewModel @Inject constructor(
    application: Application,
    private val workRepo: IWorkRepo,
    private val attendanceRepo: IAttendanceRepo
) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<AttendanceClickEvents>()
    var itemPosition = MutableLiveData<Int>().apply { value = -1 }
    var currentWorkId: Long? = null

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking

    // Common LiveData for API responses
    val workAssignedAllResponse = MutableLiveData<ApiState<WorkAssignedResponse>>()
    val workByIdResponse = MutableLiveData<ApiState<WorkDetailResponse>>()
    val workDetailsByDateResponse = MutableLiveData<ApiState<WorkDetailsByDateResponse>>()
    val workAcceptResponse = MutableLiveData<ApiState<WorkDetailResponse>>()
    val workStartResponse = MutableLiveData<ApiState<WorkDetailResponse>>()
    val workEndResponse = MutableLiveData<ApiState<WorkDetailResponse>>()
    val attendanceCheckInStatusResponse = MutableLiveData<ApiState<CheckInStatusResponse>>()

    // Click event handlers
    fun onSignInClick() = postClickEvent(AttendanceClickEvents.ON_SIGN_IN_CLICK)
    fun onMyProgressClick() = postClickEvent(AttendanceClickEvents.ON_MY_PROGRESS_CLICK)
    fun onSignInDetailsClick() = postClickEvent(AttendanceClickEvents.ON_SIGN_IN_DETAILS_CLICK)

    private fun postClickEvent(event: AttendanceClickEvents) {
        clickEvents.value = event
    }

    // Location tracking methods
    fun startTracking() {
        _isTracking.value = true
        startService(LocationTrackingService::class.java)
    }

    fun stopTracking() {
        _isTracking.value = false
        stopService(LocationTrackingService::class.java)
    }

    private fun startService(serviceClass: Class<*>) {
        val serviceIntent = Intent(getApplication(), serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }
    }

    private fun stopService(serviceClass: Class<*>) {
        val serviceIntent = Intent(getApplication(), serviceClass)
        getApplication<Application>().stopService(serviceIntent)
    }

    // API call methods
    fun getWorkAssignedAll(page: Int, pageSize: Int) {
        executeApiCall(
            apiCall = { workRepo.workAssignedAll(page, pageSize) },
            liveData = workAssignedAllResponse
        )
    }

    fun workById(workId: Long) {
        executeApiCall(
            apiCall = { workRepo.workById(workId) },
            liveData = workByIdResponse
        )
    }

    fun workDetailsByDate(date: String) {
        executeApiCall(
            apiCall = { workRepo.workDetailByDate(date)},
            liveData = workDetailsByDateResponse
        )
    }

    fun workAccept(workSiteId: Long, position: Int) {
        itemPosition.postValue(position)
        executeApiCall(
            apiCall = { workRepo.workAccept(workSiteId) },
            liveData = workAcceptResponse,
            onSuccess = { response ->
                if (response.code == 200) itemPosition.postValue(position) else itemPosition.postValue(-1)
            },
            onError = { itemPosition.postValue(-1) }
        )
    }

    fun workStart(workId: String, latitude: String, longitude: String, photoPath: String, position: Int) {
        val file = File(photoPath)
        val photoPart = MultipartBody.Part.createFormData(
            "photo", file.name, file.asRequestBody("image/*".toMediaType())
        )
        val workIdBody = workId.toRequestBody("text/plain".toMediaType())
        val latitudeBody = latitude.toRequestBody("text/plain".toMediaType())
        val longitudeBody = longitude.toRequestBody("text/plain".toMediaType())

        itemPosition.postValue(position)
        executeApiCall(
            apiCall = { workRepo.workStart(workIdBody, latitudeBody, longitudeBody, photoPart) },
            liveData = workStartResponse,
            onSuccess = { response ->
                if (response.code == 200) itemPosition.postValue(position) else itemPosition.postValue(-1)
            },
            onError = { itemPosition.postValue(-1) }
        )
    }

    fun workEnd(workSiteId: Long, latitude: Double, longitude: Double, types: List<Type>,statusId:Int,remarks:String,position: Int) {
        val params = JsonObject().apply {
            addProperty("work_site_id", workSiteId)
            addProperty("latitude", latitude)
            addProperty("longitude", longitude)
            val typesArray = JsonArray()
            types.forEach { type ->
                val typeObject = JsonObject().apply {
                    addProperty("type_id", type.id)
                    addProperty("status", statusId)
                    addProperty("remarks",remarks)
                }
                typesArray.add(typeObject)
            }
            add("types", typesArray)
        }
        itemPosition.postValue(position)
        executeApiCall(
            apiCall = { workRepo.workEnd(params) },
            liveData = workEndResponse,
            onSuccess = { response ->
                if (response.code == 200) itemPosition.postValue(position) else itemPosition.postValue(-1)
            },
            onError = { itemPosition.postValue(-1) }
        )
    }

    fun checkInStatusAttendance() {
        executeApiCall(
            apiCall = { attendanceRepo.attendanceCheckInStatus() },
            liveData = attendanceCheckInStatusResponse
        )
    }
}
