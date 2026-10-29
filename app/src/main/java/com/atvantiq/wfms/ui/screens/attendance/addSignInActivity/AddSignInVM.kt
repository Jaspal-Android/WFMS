package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.base.savedArrayList
import com.atvantiq.wfms.base.savedObject
import com.atvantiq.wfms.base.savedValue
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
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
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.Utils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.atvantiq.wfms.constants.ValConstants


@HiltViewModel
class AddSignInVM @Inject constructor(
    application: Application,
    private val creationRepo: ICreationRepo,
    private val workRepo: IWorkRepo,
    private val state: SavedStateHandle
) : BaseViewModel(application) {

    var clickEvents = LiveEvent<AddSignInClickEvents>()
    var errorHandler = LiveEvent<AssignTaskError>()

    // What the user picked is kept in the SavedStateHandle, so the form survives Android ending the
    // process while the user is in another app. The lists offered by the pickers are fetched again.
    var selectedClient: Client? by state.savedObject("client")
    var selectedProjectId: Long? by state.savedValue("projectId", null)
    var selectedPoNumberId: Long? by state.savedValue("poNumberId", null)
    var selectedCircleId: Long? by state.savedValue("circleId", null)
    var selectedSiteId: Long? by state.savedValue("siteId", null)

    var clients: List<Client> = ArrayList()
    var projects: List<ProjectData> = ArrayList()
    var poNumbers: List<PoData> = ArrayList()
    var circles: List<CircleData> = ArrayList()
    var sites: List<SiteData> = ArrayList()
    var types: List<TypeData> = ArrayList()

    /** Assign a new list to change it; a list edited in place is not saved. */
    var selectedTypeIdList: ArrayList<TypeData>? by state.savedArrayList("types")
    val activitySelection = TypeActivitySelection(
        restoredPicks = state.get<String>(PICKS_KEY)?.let { Gson().fromJson(it, Array<TypePicks>::class.java).toList() }.orEmpty(),
        onPicksChanged = { state[PICKS_KEY] = Gson().toJson(it) }
    )

    val isClientLoading = ObservableField<Boolean>().apply { set(false) }
    val isProjectLoading = ObservableField<Boolean>().apply { set(false) }
    val isPoLoading = ObservableField<Boolean>().apply { set(false) }
    val isCircleLoading = ObservableField<Boolean>().apply { set(false) }
    val isSiteLoading = ObservableField<Boolean>().apply { set(false) }
    val isTypeLoading = ObservableField<Boolean>().apply { set(false) }
    val isActivityLoading = ObservableField<Boolean>().apply { set(false) }
    val isSubmitting = ObservableField<Boolean>().apply { set(false) }

    // Click event handlers
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
    var typeListByProjectResponse = MutableLiveData<ApiState<TypeListByProjectResponse>>()
    var typeActivitiesResponse = MutableLiveData<ApiState<List<TypeActivities>>>()
    var workAssignedResponse = MutableLiveData<ApiState<SelfAssignResponse>>()

    // Last: the picker lists are fetched into the response fields above.
    init {
        reloadPickerLists()
    }

    /** After the process was recreated the picks are back but the lists behind the pickers are not. */
    private fun reloadPickerLists() {
        selectedClient?.let { getProjectListByClientId(it.id) }
        selectedProjectId?.let { projectId ->
            getPoNumberListByProject(projectId)
            getCircleListByProject(projectId)
            getSiteListByProject(projectId)
        }
        selectedPoNumberId?.let { poId ->
            getTypeListByPo(poId)
            loadMissingActivities()
        }
    }

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

    /** The (type, type-name) pairs the activity lists are keyed by, in the order the user picked them. */
    fun selectedTypeKeys(): List<Pair<Long, String>> =
        selectedTypeIdList.orEmpty().map { it.id to it.name.orEmpty() }

    fun clearTypes() {
        selectedTypeIdList = ArrayList()
        activitySelection.clear()
    }

    /**
     * Applies a new type selection: activities picked for types that stay selected are kept, and
     * activities are fetched for every selected type that has none yet.
     */
    fun onTypesSelected(types: Collection<TypeData>) {
        selectedTypeIdList = ArrayList(types)
        activitySelection.retainTypes(types.map { it.id }.toSet())
        loadMissingActivities()
    }

    /** Fetches the activities of every selected type that has none loaded (also serves as retry). */
    fun loadMissingActivities() {
        val poId = selectedPoNumberId ?: return
        val missing = selectedTypeIdList.orEmpty().filterNot { activitySelection.hasActivities(it.id) }
        if (missing.isEmpty()) return

        executeApiCall(
            apiCall = {
                coroutineScope {
                    missing.map { type ->
                        async { TypeActivities(type.id, creationRepo.activityListByPoType(poId, type.id)) }
                    }.awaitAll()
                }
            },
            liveData = typeActivitiesResponse,
            onSuccess = { results ->
                // The user may have changed the PO or the types while this was in flight.
                if (selectedPoNumberId == poId) {
                    val stillSelected = selectedTypeIdList.orEmpty().map { it.id }.toSet()
                    results
                        .filter { it.typeId in stillSelected && it.response.code == ValConstants.SUCCESS_CODE }
                        // Activities are not shown on Add Work; every activity of the type is sent.
                        .forEach { activitySelection.setAvailable(it.typeId, it.response.data, selectAll = true) }
                }
                isActivityLoading.set(false)
            },
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
            isActivityLoading.get() == true -> {
                errorHandler.value = AssignTaskError.ON_ACTIVITY_LOADING
                false
            }
            !activitySelection.coversAll(selectedTypeIdList.orEmpty().map { it.id }) -> {
                errorHandler.value = AssignTaskError.ON_ACTIVITY_ERROR
                false
            }
            else -> true
        }
    }

    // Work assigned API using executeApiCall
    private fun getWorkAssigned() {
        if (isSubmitting.get() == true) return
        if (!validateAssignTaskFields()) return
        if (!Utils.isInternet(getApplication())) {
            workAssignedResponse.value = ApiState.error(noInternetError())
            return
        }
        isSubmitting.set(true)

        val params = JsonObject().apply {
            addProperty("po_id", selectedPoNumberId)
            addProperty("client_id", selectedClient?.id)
            addProperty("project_id", selectedProjectId)
            addProperty("circle_id", selectedCircleId)

            // Site array
            val siteArray = JsonArray()
            val siteObj = JsonObject()
            siteObj.addProperty("id", selectedSiteId)
            siteArray.add(siteObj)
            add("site", siteArray)

            // Type array
            val typeArray = JsonArray()
            for (type in selectedTypeIdList ?: emptyList()) {
                val typeObj = JsonObject()
                typeObj.addProperty("id", type.id)
                // Activity array inside type
                val activityArray = JsonArray()
                activitySelection.selectedIds(type.id).forEach { activityId ->
                    val activityObj = JsonObject()
                    activityObj.addProperty("id", activityId)
                    activityArray.add(activityObj)
                }
                typeObj.add("activity", activityArray)
                typeArray.add(typeObj)
            }
            add("type", typeArray)
        }
        executeApiCall(
            apiCall = { workRepo.workSelfAssign(params) },
            liveData = workAssignedResponse,
            onSuccess = { isSubmitting.set(false) },
            onError = { isSubmitting.set(false) }
        )
    }

    fun onSubmitCompleted() {
        isSubmitting.set(false)
    }

    private companion object {
        const val PICKS_KEY = "activityPicks"
    }
}


/** The activity list the server returned for one type. */
data class TypeActivities(val typeId: Long, val response: ActivityListByProjectTypeResponse)
