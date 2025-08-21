package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.services.LocationTrackingService
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody // Import for toRequestBody
import java.io.File

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    application: Application,
    private val workRepo:IWorkRepo,
    private val attendanceRepo: IAttendanceRepo
) : AndroidViewModel(application) {

    var clickEvents = MutableLiveData<AttendanceClickEvents>()
    var itemPosition = MutableLiveData<Int>().apply { value = -1 }
    var currentWorkId: Long? = null

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking


    fun onSignInClick() {
        clickEvents.value = AttendanceClickEvents.ON_SIGN_IN_CLICK
    }

    fun onMyProgressClick() {
        clickEvents.value = AttendanceClickEvents.ON_MY_PROGRESS_CLICK
    }

    fun onSignInDetailsClick() {
        clickEvents.value = AttendanceClickEvents.ON_SIGN_IN_DETAILS_CLICK
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

    /*
    * Get as work assigned all API
    * */

    var workAssignedAllResponse = MutableLiveData<ApiState<WorkAssignedAllResponse>>()
    fun getWorkAssignedAll( page: Int, pageSize: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                workAssignedAllResponse.postValue(ApiState.loading())
                try {
                    var response = workRepo.workAssignedAll(page,pageSize)
                    workAssignedAllResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    workAssignedAllResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workAssignedAllResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
   * Work by ID API
   * */
    var workByIdResponse = MutableLiveData<ApiState<WorkDetailResponse>>()
    fun workById(workId: Long) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                workByIdResponse.postValue(ApiState.loading())
                try {
                    var response = workRepo.workById(workId)
                    workByIdResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    workByIdResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workByIdResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Work details by date API
    * */
    var workDetailsByDateResponse = MutableLiveData<ApiState<WorkDetailsByDateResponse>>()
    fun workDetailsByDate(date: String) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                workDetailsByDateResponse.postValue(ApiState.loading())
                try {
                    var response = workRepo.workDetailByDate(date)
                    workDetailsByDateResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    workDetailsByDateResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workDetailsByDateResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Accept work API
    * */
    var workAcceptResponse = MutableLiveData<ApiState<AcceptWorkResponse>>()
    fun workAccept(workId: Long, position: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                workAcceptResponse.postValue(ApiState.loading())
                try {
                    itemPosition .postValue(position)
                    var response = workRepo.workAccept(workId)
                    if( response.code == 200) {
                        itemPosition .postValue(position)
                    } else {
                        itemPosition .postValue(-1)
                    }
                    workAcceptResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    itemPosition .postValue(-1)
                    workAcceptResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workAcceptResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Start work API
    * */
    var workStartResponse = MutableLiveData<ApiState<StartWorkResponse>>()
    fun workStart(workId: String,latitude:String, longitude: String, photoPath: String,position: Int) {
        if (Utils.isInternet(getApplication())) {

            val file = File(photoPath)
            val requestFile = file.asRequestBody("image/*".toMediaType())
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

            // Prepare other form fields
            val workIdBody = workId.toRequestBody("text/plain".toMediaType())
            val latitudeBody = latitude.toRequestBody("text/plain".toMediaType())
            val longitudeBody = longitude.toRequestBody("text/plain".toMediaType())

            viewModelScope.launch {
                workStartResponse.postValue(ApiState.loading())
                try {
                    itemPosition .postValue(position)
                    var response = workRepo.workStart(workIdBody, latitudeBody, longitudeBody, photoPart)
                    if( response.code == 200) {
                        itemPosition .postValue(position)
                    } else {
                        itemPosition .postValue(-1)
                    }
                    workStartResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    itemPosition .postValue(-1)
                    workStartResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workStartResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * End work API
    * */
    var workEndResponse = MutableLiveData<ApiState<EndWorkResponse>>()
    fun workEnd(workId: Long,latitude:Double,longitude:Double,status:Int,remarks:String, position: Int) {
        if (Utils.isInternet(getApplication())) {
            var params = JsonObject()
            params.addProperty("work_id",workId )
            params.addProperty("latitude",latitude )
            params.addProperty("longitude",longitude )
            params.addProperty("status",status )
            if(remarks.isNotEmpty()){
                params.addProperty("remarks",remarks )
            }
            viewModelScope.launch {
                workEndResponse.postValue(ApiState.loading())
                try {
                    itemPosition .postValue(position)
                    var response = workRepo.workEnd(params)
                    if( response.code == 200) {
                        itemPosition .postValue(position)
                    } else {
                        itemPosition .postValue(-1)
                    }
                    workEndResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    itemPosition .postValue(-1)
                    workEndResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workEndResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }


    /*
    * Attendance Check In Status API
    */
    var attendanceCheckInStatusResponse = MutableLiveData<ApiState<CheckInStatusResponse>>()
    fun checkInStatusAttendance() {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                attendanceCheckInStatusResponse.value = ApiState.loading()
                try {
                    var response = attendanceRepo.attendanceCheckInStatus()
                    attendanceCheckInStatusResponse.value = (ApiState.success(response))
                } catch (e: Exception) {
                    attendanceCheckInStatusResponse.value = (ApiState.error(e))
                }
            }
        } else {
            attendanceCheckInStatusResponse.value = (ApiState.error(NoInternetException("No Internet Connection")))
        }
    }
}