package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

import android.app.Application
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.base.savedField
import com.atvantiq.wfms.base.savedValue
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
    private val creationRepo: ICreationRepo,
    state: SavedStateHandle
) : BaseViewModel(application) {

    // What the user typed and picked is kept in the SavedStateHandle, so the form survives Android
    // ending the process while the user is in another app.
    var selectedClientId: Long? by state.savedValue("clientId", null)
        private set
    var selectedProjectId: Long? by state.savedValue("projectId", null)
        private set
    var selectedCircleId: Long? by state.savedValue("circleId", null)
        private set

    /** Names shown in the pickers; null means "Not selected". */
    val clientName = state.savedField<String?>("clientName", null)
    val projectName = state.savedField<String?>("projectName", null)
    val circleName = state.savedField<String?>("circleName", null)

    var clients: List<Client> = ArrayList()
    var projects: List<ProjectData> = ArrayList()
    var circles: List<CircleData> = ArrayList()

    val isClientLoading = ObservableField<Boolean>().apply { set(false) }
    val isProjectLoading = ObservableField<Boolean>().apply { set(false) }
    val isCircleLoading = ObservableField<Boolean>().apply { set(false) }

    var siteId = state.savedField("siteId", "")
    var siteName = state.savedField("siteName", "")
    var siteAddress = state.savedField("siteAddress", "")
    var siteLatitude = state.savedField("siteLatitude", "")
    var siteLongitude = state.savedField("siteLongitude", "")

    /** The first failing check (spec order), shown under the form once the user has started. */
    val validationError = ObservableField<Int?>()

    /** Create Site is greyed out until the form is valid, and while it is being sent. */
    val canCreate = ObservableBoolean(false)

    private var isSubmitting = false
    private var hasStarted = false
    private val formFields = listOf(
        clientName, projectName, circleName, siteId, siteName, siteAddress, siteLatitude, siteLongitude
    )
    private val revalidate = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            hasStarted = true
            validate()
        }
    }

    init {
        formFields.forEach { it.addOnPropertyChangedCallback(revalidate) }
        validate()
    }

    /** After the process was recreated the picked values are back but the lists behind the pickers are not. */
    private fun reloadPickerLists() {
        selectedClientId?.let { getProjectListByClientId(it) }
        selectedProjectId?.let { getCircleListByProject(it) }
    }

    private fun firstError(): Int? = AddSiteValidation.firstError(
        hasClient = selectedClientId != null,
        hasProject = selectedProjectId != null,
        hasCircle = selectedCircleId != null,
        siteId = siteId.get(),
        name = siteName.get(),
        address = siteAddress.get(),
        latitude = siteLatitude.get(),
        longitude = siteLongitude.get()
    )

    private fun validate() {
        val error = firstError()
        validationError.set(if (hasStarted) error else null)
        canCreate.set(error == null && !isSubmitting)
    }

    override fun onCleared() {
        formFields.forEach { it.removeOnPropertyChangedCallback(revalidate) }
        super.onCleared()
    }

    fun onSaveClick() = createSite()

    // API LiveData
    var clientListResponse = MutableLiveData<ApiState<ClientListResponse>>()
    var projectListByClientResponse = MutableLiveData<ApiState<ProjectListByClientResponse>>()
    var circleListByProjectResponse = MutableLiveData<ApiState<CircleListByProjectResponse>>()
    var createSiteResponse = MutableLiveData<ApiState<CreateSiteResponse>>()

    // Last: the picker lists are fetched into the response fields above.
    init {
        reloadPickerLists()
    }

    /** A new client clears the project and circle chosen for the previous one. */
    fun selectClient(client: Client) {
        selectedClientId = client.id
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

    /** `POST /site/create`. Latitude and longitude are optional and left out when empty. */
    private fun createSite() {
        hasStarted = true
        if (isSubmitting || firstError() != null) {
            validate()
            return
        }
        val params = JsonObject().apply {
            addProperty("project_id", selectedProjectId)
            addProperty("circle_id", selectedCircleId)
            addProperty("site_id", siteId.get().orEmpty().trim())
            addProperty("name", siteName.get().orEmpty().trim())
            addProperty("address", siteAddress.get().orEmpty().trim())
            siteLatitude.get()?.trim()?.toDoubleOrNull()?.let { addProperty("latitude", it) }
            siteLongitude.get()?.trim()?.toDoubleOrNull()?.let { addProperty("longitude", it) }
        }
        isSubmitting = true
        validate()
        executeApiCall(
            apiCall = { creationRepo.createSite(params) },
            liveData = createSiteResponse,
            onSuccess = { finishSubmitting() },
            onError = { finishSubmitting() }
        )
    }

    private fun finishSubmitting() {
        isSubmitting = false
        validate()
    }
}
