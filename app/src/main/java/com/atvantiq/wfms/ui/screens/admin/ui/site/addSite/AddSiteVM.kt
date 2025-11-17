package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

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
import com.atvantiq.wfms.models.site.create.CreateSiteResponse
import com.atvantiq.wfms.models.type.TypeData
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AssignTaskError
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddSiteVM @Inject constructor(
    application: Application,
    private val creationRepo: ICreationRepo) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<AddSiteClickEvents>()
    var errorHandler = MutableLiveData<AddSiteErrorHandler>()

    var selectedClient: Client? = null
    var selectedProjectId: Long? = null
    var selectedCircleId: Long? = null

    var clients: List<Client> = ArrayList()
    var projects: List<ProjectData> = ArrayList()
    var circles: List<CircleData> = ArrayList()

    val isClientLoading = ObservableField<Boolean>().apply { set(false) }
    val isProjectLoading = ObservableField<Boolean>().apply { set(false) }
    val isCircleLoading = ObservableField<Boolean>().apply { set(false) }

    var siteId = ObservableField<String>().apply { set("") }
    var siteName = ObservableField<String>().apply { set("") }
    var siteAddress = ObservableField<String>().apply { set("") }
    var siteLatitude = ObservableField<String>().apply { set("") }
    var siteLongitude = ObservableField<String>().apply { set("") }


    // Click event handlers
    fun onSaveClick() = createSite()

    fun onCancelClick() = postClickEvent(AddSiteClickEvents.ON_CANCEL_CLICK)

    private fun postClickEvent(event: AddSiteClickEvents) {
        clickEvents.value = event
    }

    // API LiveData
    var clientListResponse = MutableLiveData<ApiState<ClientListResponse>>()
    var projectListByClientResponse = MutableLiveData<ApiState<ProjectListByClientResponse>>()
    var circleListByProjectResponse = MutableLiveData<ApiState<CircleListByProjectResponse>>()
    var createSiteResponse = MutableLiveData<ApiState<CreateSiteResponse>>()

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

    fun getCircleListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.circleByProject(projectId) },
            liveData = circleListByProjectResponse,
            onSuccess = { isCircleLoading.set(false) },
            onError = { isCircleLoading.set(false) }
        )
        isCircleLoading.set(true)
    }

    // Validation logic
    fun validateAssignTaskFields(): Boolean {
        return when {
            selectedClient == null -> {
                errorHandler.value = AddSiteErrorHandler.ON_CLIENT_ERROR
                false
            }
            selectedProjectId == null -> {
                errorHandler.value = AddSiteErrorHandler.ON_PROJECT_ERROR
                false
            }
            selectedCircleId == null -> {
                errorHandler.value = AddSiteErrorHandler.ON_CIRCLE_ERROR
                false
            }
            siteId.get().isNullOrEmpty() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_ID_ERROR
                false
            }
            siteName.get().isNullOrEmpty() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_NAME_ERROR
                false
            }
            siteAddress.get().isNullOrEmpty() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_ADDRESS_ERROR
                false
            }
            else -> true
        }
    }

    // Create site API using executeApiCall
    private fun createSite() {
        if (!validateAssignTaskFields()) return
        val params = JsonObject().apply {
            addProperty("project_id", selectedProjectId)
            addProperty("circle_id", selectedCircleId)
            addProperty("site_id", siteId.get().toString().trim())
            addProperty("name", siteName.get().toString().trim())
            addProperty("address", siteAddress.get().toString().trim())
            addProperty("latitude", siteLatitude.get().toString().trim().ifEmpty { "0" })
            addProperty("longitude", siteLongitude.get().toString().trim().ifEmpty { "0" })
        }
        executeApiCall(
            apiCall = { creationRepo.createSite(params) },
            liveData = createSiteResponse
        )
    }
}
