package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

import android.app.Application
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.create.CreateSiteResponse
import com.atvantiq.wfms.network.ApiState
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Add Site: Client → Project → Circle, each choice unlocking (and resetting) the next. */
@HiltViewModel
class AddSiteVM @Inject constructor(
    application: Application,
    private val creationRepo: ICreationRepo) : BaseViewModel(application) {

    var errorHandler = MutableLiveData<AddSiteErrorHandler>()

    var selectedClient: Client? = null
        private set
    var selectedProjectId: Long? = null
        private set
    var selectedCircleId: Long? = null
        private set

    /** Names shown in the pickers; null means "Not selected". */
    val clientName = ObservableField<String?>()
    val projectName = ObservableField<String?>()
    val circleName = ObservableField<String?>()

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

    /** Create Site is enabled once the circle and every required field are filled. */
    val canCreate = object : ObservableBoolean(circleName, siteId, siteName, siteAddress) {
        override fun get(): Boolean = circleName.get() != null &&
            !siteId.get().isNullOrBlank() &&
            !siteName.get().isNullOrBlank() &&
            !siteAddress.get().isNullOrBlank()
    }

    fun onSaveClick() = createSite()

    // API LiveData
    var clientListResponse = MutableLiveData<ApiState<ClientListResponse>>()
    var projectListByClientResponse = MutableLiveData<ApiState<ProjectListByClientResponse>>()
    var circleListByProjectResponse = MutableLiveData<ApiState<CircleListByProjectResponse>>()
    var createSiteResponse = MutableLiveData<ApiState<CreateSiteResponse>>()

    /** A new client clears the project and circle chosen for the previous one. */
    fun selectClient(client: Client) {
        selectedClient = client
        clientName.set(client.companyName)
        clearProject()
        getProjectListByClientId(client.id)
    }

    /** A new project clears the circle chosen for the previous one. */
    fun selectProject(project: ProjectData) {
        selectedProjectId = project.id
        projectName.set(project.name)
        clearCircle()
        getCircleListByProject(project.id)
    }

    fun selectCircle(circle: CircleData) {
        selectedCircleId = circle.id
        circleName.set(circle.name)
    }

    private fun clearProject() {
        selectedProjectId = null
        projectName.set(null)
        projects = emptyList()
        clearCircle()
    }

    private fun clearCircle() {
        selectedCircleId = null
        circleName.set(null)
        circles = emptyList()
    }

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
            onError = { isProjectLoading.set(false) },
            cancelPrevious = true // switching clients quickly: the last client picked must win
        )
        isProjectLoading.set(true)
    }

    fun getCircleListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.circleByProject(projectId) },
            liveData = circleListByProjectResponse,
            onSuccess = { isCircleLoading.set(false) },
            onError = { isCircleLoading.set(false) },
            cancelPrevious = true
        )
        isCircleLoading.set(true)
    }

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
            siteId.get().isNullOrBlank() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_ID_ERROR
                false
            }
            siteName.get().isNullOrBlank() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_NAME_ERROR
                false
            }
            siteAddress.get().isNullOrBlank() -> {
                errorHandler.value = AddSiteErrorHandler.ON_SITE_ADDRESS_ERROR
                false
            }
            else -> true
        }
    }

    /** `POST /site/create`. Latitude and longitude are optional and left out when empty. */
    private fun createSite() {
        if (!validateAssignTaskFields()) return
        val params = JsonObject().apply {
            addProperty("project_id", selectedProjectId)
            addProperty("circle_id", selectedCircleId)
            addProperty("site_id", siteId.get().orEmpty().trim())
            addProperty("name", siteName.get().orEmpty().trim())
            addProperty("address", siteAddress.get().orEmpty().trim())
            siteLatitude.get()?.trim()?.toDoubleOrNull()?.let { addProperty("latitude", it) }
            siteLongitude.get()?.trim()?.toDoubleOrNull()?.let { addProperty("longitude", it) }
        }
        executeApiCall(
            apiCall = { creationRepo.createSite(params) },
            liveData = createSiteResponse
        )
    }
}
