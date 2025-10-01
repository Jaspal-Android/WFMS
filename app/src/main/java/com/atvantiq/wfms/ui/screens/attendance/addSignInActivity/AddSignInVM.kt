package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.activity.ActivityData
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoData
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeData
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddSignInVM @Inject constructor(
    application: Application,
    private val creationRepo: ICreationRepo,
    private val workRepo: IWorkRepo
) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<AddSignInClickEvents>()
    var errorHandler = MutableLiveData<AssignTaskError>()

    var selectedClient: Client? = null
    var selectedProjectId: Long? = null
    var selectedPoNumberId: Long? = null
    var selectedCircleId: Long? = null
    var selectedSiteId: Long? = null

    var clients: List<Client> = ArrayList()
    var projects: List<ProjectData> = ArrayList()
    var poNumbers: List<PoData> = ArrayList()
    var circles: List<CircleData> = ArrayList()
    var sites: List<SiteData> = ArrayList()
    var types: List<TypeData> = ArrayList()
    var activities: List<ActivityData> = ArrayList()

    var selectedTypeIdList: ArrayList<Long>? = ArrayList()
    var selectedActivityIdList: ArrayList<Long>? = ArrayList()

    val isClientLoading = ObservableField<Boolean>().apply { set(false) }
    val isProjectLoading = ObservableField<Boolean>().apply { set(false) }
    val isPoLoading = ObservableField<Boolean>().apply { set(false) }
    val isCircleLoading = ObservableField<Boolean>().apply { set(false) }
    val isSiteLoading = ObservableField<Boolean>().apply { set(false) }
    val isTypeLoading = ObservableField<Boolean>().apply { set(false) }
    val isActivityLoading = ObservableField<Boolean>().apply { set(false) }

    // Click event handlers
    fun onCameraClick() = postClickEvent(AddSignInClickEvents.ON_CAMERA_CLICK)
    fun onSaveClick() = getWorkAssigned()
    fun onCancelClick() = postClickEvent(AddSignInClickEvents.ON_CANCEL_CLICK)

    private fun postClickEvent(event: AddSignInClickEvents) {
        clickEvents.value = event
    }

    // API LiveData
    var clientListResponse = MutableLiveData<ApiState<ClientListResponse>>()
    var projectListByClientResponse = MutableLiveData<ApiState<ProjectListByClientResponse>>()
    var poNumberListByProjectResponse = MutableLiveData<ApiState<PoListByProjectResponse>>()
    var circleListByProjectResponse = MutableLiveData<ApiState<CircleListByProjectResponse>>()
    var siteListByProjectResponse = MutableLiveData<ApiState<SiteListByProjectResponse>>()
    var typeListByProjectResponse = MutableLiveData<ApiState<com.atvantiq.wfms.models.type.TypeListByProjectResponse>>()
    var activityListByProjectTypeResponse = MutableLiveData<ApiState<com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse>>()
    var workAssignedResponse = MutableLiveData<ApiState<SelfAssignResponse>>()

    // API methods using executeApiCall from BaseViewModel
    fun getClientList() {
        executeApiCall(
            apiCall = { creationRepo.clientList() },
            liveData = clientListResponse,
            onSuccess = { isClientLoading.set(false) },
            onError = { isClientLoading.set(false) }
        )
        isClientLoading.set(true)
    }

    fun getProjectListByClientId(clientId: Long) {
        executeApiCall(
            apiCall = { creationRepo.projectListByClientId(clientId) },
            liveData = projectListByClientResponse,
            onSuccess = { isProjectLoading.set(false) },
            onError = { isProjectLoading.set(false) }
        )
        isProjectLoading.set(true)
    }

    fun getPoNumberListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.poNumberListByProject(projectId) },
            liveData = poNumberListByProjectResponse,
            onSuccess = { isPoLoading.set(false) },
            onError = { isPoLoading.set(false) }
        )
        isPoLoading.set(true)
    }

    fun getCircleListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.circleByProject(projectId) },
            liveData = circleListByProjectResponse,
            onSuccess = { isCircleLoading.set(false) },
            onError = { isCircleLoading.set(false) }
        )
        isCircleLoading.set(true)
    }

    fun getSiteListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.siteListByProject(projectId) },
            liveData = siteListByProjectResponse,
            onSuccess = { isSiteLoading.set(false) },
            onError = { isSiteLoading.set(false) }
        )
        isSiteLoading.set(true)
    }

    fun getTypeListByPo(poId: Long) {
        executeApiCall(
            apiCall = { creationRepo.typeListByPo(poId) },
            liveData = typeListByProjectResponse,
            onSuccess = { isTypeLoading.set(false) },
            onError = { isTypeLoading.set(false) }
        )
        isTypeLoading.set(true)
    }

    fun getActivityListByPoType(poId: Long, typeId: Long) {
        executeApiCall(
            apiCall = { creationRepo.activityListByPoType(poId, typeId) },
            liveData = activityListByProjectTypeResponse,
            onSuccess = { isActivityLoading.set(false) },
            onError = { isActivityLoading.set(false) }
        )
        isActivityLoading.set(true)
    }

    // Validation logic
    fun validateAssignTaskFields(): Boolean {
        return when {
            selectedClient == null -> {
                errorHandler.value = AssignTaskError.ON_CLIENT_ERROR
                false
            }
            selectedProjectId == null -> {
                errorHandler.value = AssignTaskError.ON_PROJECT_ERROR
                false
            }
            selectedPoNumberId == null -> {
                errorHandler.value = AssignTaskError.ON_PO_NUMBER_ERROR
                false
            }
            selectedCircleId == null -> {
                errorHandler.value = AssignTaskError.ON_CIRCLE_ERROR
                false
            }
            selectedSiteId == null -> {
                errorHandler.value = AssignTaskError.ON_SITE_ERROR
                false
            }
            selectedTypeIdList.isNullOrEmpty() -> {
                errorHandler.value = AssignTaskError.ON_TYPE_ERROR
                false
            }
            selectedActivityIdList.isNullOrEmpty() -> {
                errorHandler.value = AssignTaskError.ON_ACTIVITY_ERROR
                false
            }
            else -> true
        }
    }

    // Work assigned API using executeApiCall
    private fun getWorkAssigned() {
        if (!validateAssignTaskFields()) return

        val params = JsonObject().apply {
            addProperty("po_id", selectedPoNumberId)
            addProperty("client_id", selectedClient?.id)
            addProperty("project_id", selectedProjectId)
            addProperty("circle_id", selectedCircleId)
            // Site array
            val siteArray = com.google.gson.JsonArray()
            val siteObj = JsonObject()
            siteObj.addProperty("id", selectedSiteId)
            siteArray.add(siteObj)
            add("site", siteArray)

            // Type array
            val typeArray = com.google.gson.JsonArray()
            for (type in selectedTypeIdList ?: emptyList()) {
                val typeObj = JsonObject()
                typeObj.addProperty("id", type)

                // Dynamic activity array inside type
                val activityArray = com.google.gson.JsonArray()
                for (activity in selectedActivityIdList ?: emptyList()) {
                    val activityObj = JsonObject()
                    activityObj.addProperty("id", activity)
                    activityArray.add(activityObj)
                }
                typeObj.add("activity", activityArray)
                typeArray.add(typeObj)
            }
            add("type", typeArray)
        }
        executeApiCall(
            apiCall = { workRepo.workSelfAssign(params) },
            liveData = workAssignedResponse
        )
    }
}
