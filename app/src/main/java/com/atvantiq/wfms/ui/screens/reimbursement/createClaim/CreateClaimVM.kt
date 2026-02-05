package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.allProjects.Circle
import com.atvantiq.wfms.models.allProjects.Project
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.workSiteByDate.Site
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CreateClaimVM @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo,
    private val creationRepo: CreationRepo
) : BaseViewModel(application) {

    var isOutstationExpense = ObservableField<Boolean>().apply { set(false) }
    var isMultiSite = ObservableField<Boolean>().apply { set(false) }

    var remarks = MutableLiveData<String>().apply { value = "" }
    var date = ObservableField<String>().apply { set("") }
    var purpose = ObservableField<String>().apply { set("") }
    var selectedSingleSite: Site? = null
    var selectedProjectId: Long? = null
    var selectedCircleId: Long? = null
    var selectedCircleCode: String? = null
    var travelingEntriesList = MutableLiveData<List<TravelExpense>>().apply { value = emptyList() }
    var daEntriesList = MutableLiveData<List<DAExpense>>().apply { value = emptyList() }
    var hotelEntriesList = MutableLiveData<List<HotelExpense>>().apply { value = emptyList() }
    var othersEntriesList = MutableLiveData<List<OtherExpense>>().apply { value = emptyList() }
    var selectedSitesIdList = MutableLiveData<List<SiteData>>().apply { value = emptyList() }

    /*Listing data*/
    var singleSites: List<Site> = ArrayList()
    var multiSites: List<SiteData> = ArrayList()
    var projects: List<Project> = ArrayList()
    var circles: List<Circle> = ArrayList()

    var clickEvents = MutableLiveData<CreateClaimClickEvents>()
    var errorEvents = MutableLiveData<CreateClaimErrorHandler>()

    private fun postClickEvent(event: CreateClaimClickEvents) {
        clickEvents.value = event
    }

    fun onDateClick() = postClickEvent(CreateClaimClickEvents.ON_DATE_PICKER_CLICK)

    fun onSingleSiteClick() = postClickEvent(CreateClaimClickEvents.ON_SINGLE_SITE_CLICK)

    fun onMultiSiteClick() = postClickEvent(CreateClaimClickEvents.ON_MULTI_SITE_CLICK)

    fun onLocalClaimClick() = postClickEvent(CreateClaimClickEvents.ON_LOCAL_CLAIM_CLICK)

    fun onOutstationClaimClick() = postClickEvent(CreateClaimClickEvents.ON_OUTSTATION_CLAIM_CLICK)

    fun onAddMultipleSiteClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_MULTIPLE_SITE_CLICK)

    fun onSiteDropdownClick() = postClickEvent(CreateClaimClickEvents.ON_SINGLE_SITE_DROPDOWN_CLICK)

    fun onAddTravelingExpenseClick() =
        postClickEvent(CreateClaimClickEvents.ON_ADD_TRAVELING_EXPENSE_CLICK)

    fun onAddDailyAllowanceClick() =
        postClickEvent(CreateClaimClickEvents.ON_ADD_DAILY_ALLOWANCE_CLICK)

    fun onAddHotelExpenseClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_HOTEL_EXPENSE_CLICK)

    fun onAddOthersExpenseClick() =
        postClickEvent(CreateClaimClickEvents.ON_ADD_OTHERS_EXPENSE_CLICK)

    fun onCancelClick() = postClickEvent(CreateClaimClickEvents.ON_CANCEL_CLICK)

    fun onPurposeClick() = postClickEvent(CreateClaimClickEvents.ON_PURPOSE_CLICK)

    fun onProjectDropdownClick() = postClickEvent(CreateClaimClickEvents.ON_PROJECT_DROPDOWN_CLICK)

    fun onCircleDropdownClick() = postClickEvent(CreateClaimClickEvents.ON_CIRCLE_DROPDOWN_CLICK)

    /*Handling single site*/
    fun onSingleSiteSelected(siteId: Site) {
        selectedSingleSite = siteId
        selectedProjectId = siteId.projectId
        selectedCircleId = siteId.circleId
        selectedCircleCode = siteId.circleCode
    }

    /*On project selected*/
    fun onProjectSelected(project: Project) {
        selectedProjectId = project?.id
        circles = project?.circle as List<Circle>
    }

    /*On Project circle selected*/
    fun onCircleSelected(circle: Circle) {
        selectedCircleId = circle.id
        selectedCircleCode = circle.code
    }

    /*Handling multiple site*/
    fun addMultipleSite(sites: List<SiteData>) {
        val currentList = selectedSitesIdList.value?.toMutableList() ?: mutableListOf()
        currentList?.clear()
        currentList.addAll(sites)
        selectedSitesIdList.value = currentList
    }

    /*Removing item from multiple site*/
    fun removeMultipleSite(site: SiteData) {
        val currentList = selectedSitesIdList.value?.toMutableList() ?: mutableListOf()
        val iterator = currentList.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == site.id) {
                iterator.remove()
                break
            }
        }
        selectedSitesIdList.value = currentList
    }

    fun clearSelectedMultiSites() {
        selectedSitesIdList.value = emptyList()
        selectedProjectId = null
        circles = emptyList()
        selectedCircleId = null
        selectedCircleCode = null
        clearPurposeSelection()
    }

    fun clearSelectedSingleSite() {
        selectedSingleSite = null
        selectedProjectId = null
        selectedCircleId = null
        selectedCircleCode = null
    }

    /* Add Travel Expense*/
    fun addTravelExpense(expense: TravelExpense) {
        val currentList = travelingEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        travelingEntriesList.value = currentList
    }

    /*Remove item from travel expense*/
    fun removeTravelExpense(index: Int) {
        val currentList = travelingEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            travelingEntriesList.value = currentList
        }
    }

    /* Add DA Expense */
    fun addDAExpense(expense: DAExpense) {
        val currentList = daEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        daEntriesList.value = currentList
    }

    /* remove item from DA Expense*/
    fun removeDAExpense(index: Int) {
        val currentList = daEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            daEntriesList.value = currentList
        }
    }

    /* Add Hotel Expense*/
    fun addHotelExpense(expense: HotelExpense) {
        val currentList = hotelEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        hotelEntriesList.value = currentList
    }

    /*Remove item from hotel list*/
    fun removeHotelExpense(index: Int) {
        val currentList = hotelEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            hotelEntriesList.value = currentList
        }
    }

    /*Add other expense*/
    fun addOtherExpense(expense: OtherExpense) {
        val currentList = othersEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        othersEntriesList.value = currentList
    }

    /*Remove item from the Other expense*/
    fun removeOtherExpense(index: Int) {
        val currentList = othersEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            othersEntriesList.value = currentList
        }
    }

    fun clearTravelingEntries() {
        travelingEntriesList.value = emptyList()
    }

    fun clearHotelEntries() {
        hotelEntriesList.value = emptyList()
    }

    /*Work Site By Date API Call*/
    var workSiteByDateResponse = MutableLiveData<ApiState<WorkSiteByDateResponse>>()
    fun getWorkSitesByDate(date: String) {
        executeApiCall(
            apiCall = { claimRepo.workSiteByDate(date) },
            liveData = workSiteByDateResponse,
        )
    }

    /* All Projects API Call*/
    var allProjectsResponse = MutableLiveData<ApiState<AllProjectsResponse>>()
    fun getAllProjects() {
        executeApiCall(
            apiCall = { creationRepo.allProjects() },
            liveData = allProjectsResponse,
        )
    }

    /*Site list by*/
    var siteListByProjectResponse = MutableLiveData<ApiState<SiteListByProjectResponse>>()
    fun getSiteListByProject(projectId: Long) {
        executeApiCall(
            apiCall = { creationRepo.siteListByProject(projectId) },
            liveData = siteListByProjectResponse,
        )
    }

    private fun postValidationError(error: CreateClaimErrorHandler): Boolean {
        errorEvents.value = error
        return false
    }

    private fun hasAnyExpenseEntry(includeHotel: Boolean): Boolean {
        val hasTravel = !travelingEntriesList.value.isNullOrEmpty()
        val hasDa = !daEntriesList.value.isNullOrEmpty()
        val hasOther = !othersEntriesList.value.isNullOrEmpty()
        val hasHotel = !hotelEntriesList.value.isNullOrEmpty()
        return if (includeHotel) (hasTravel || hasDa || hasHotel || hasOther) else (hasTravel || hasDa || hasOther)
    }

    private fun validateCreateClaim(): Boolean {
        val selectedDate = date.get()?.trim().orEmpty()
        val selectedPurpose = purpose.get()?.trim().orEmpty()

        // Use the actual toggle/flag, not the current list contents.
        val isMultipleSite = isMultiSite.get() == true

        if (!isMultipleSite) {
            // single-site validations
            if (selectedDate.isEmpty()) return postValidationError(CreateClaimErrorHandler.EMPTY_DATE)
            if (selectedSingleSite?.siteId == null) {
                return postValidationError(CreateClaimErrorHandler.EMPTY_SELECTED_SITE)
            }
            if (selectedProjectId == null) return postValidationError(CreateClaimErrorHandler.EMPTY_PROJECT)
            if (selectedCircleId == null) return postValidationError(CreateClaimErrorHandler.EMPTY_CIRCLE)

            if (!hasAnyExpenseEntry(includeHotel = false)) {
                return postValidationError(CreateClaimErrorHandler.EMPTY_EXPENSES)
            }
            return true
        }

        // multi-site validations (date is NOT mandatory here)
        if (selectedPurpose.isEmpty()) return postValidationError(CreateClaimErrorHandler.EMPTY_PURPOSE)
        if (selectedProjectId == null) return postValidationError(CreateClaimErrorHandler.EMPTY_PROJECT)

        if (selectedSitesIdList.value.isNullOrEmpty()) {
            return postValidationError(CreateClaimErrorHandler.EMPTY_MUTLI_SITES)
        }
        if (selectedCircleId == null) return postValidationError(CreateClaimErrorHandler.EMPTY_CIRCLE)

        if (!hasAnyExpenseEntry(includeHotel = true)) {
            return postValidationError(CreateClaimErrorHandler.EMPTY_EXPENSES)
        }

        return true
    }

    /*On submit claim*/
    var createClaimResponse = MutableLiveData<ApiState<CreateClaimResponse>>()
    fun onSubmitClaim() {
        if (!validateCreateClaim()) return
        val selectedDate = date.get()?.trim().orEmpty()
        val selectedPurpose = purpose.get()?.trim().orEmpty()
        val selectedRemarks = remarks.value?.trim().orEmpty()

        val projectId = selectedProjectId
        val circleId = selectedCircleId

        // Determine type + site_ids
        val isMultipleSite = !selectedSitesIdList.value.isNullOrEmpty()

        val type = if (isMultipleSite) "multiple_site" else "single_site"

        val siteIdsJson = JSONArray().apply {
            if (isMultipleSite) {
                selectedSitesIdList.value.orEmpty().forEach { s ->
                    put(JSONObject().put("id", s.id))
                }
            } else {
                selectedSingleSite?.siteId?.let { put(JSONObject().put("id", it)) }
            }
        }

        // Build expense_type JSON + collect file parts
        val fileParts = mutableListOf<MultipartBody.Part>()

        fun addFilePart(fileKey: String, file: File) {
            val mediaType = "application/octet-stream".toMediaType()
            val body = file.asRequestBody(mediaType)
            fileParts += MultipartBody.Part.createFormData(fileKey, file.name, body)
        }

        fun nextKey(prefix: String, index1Based: Int) = "file_${prefix}${index1Based}"

        fun buildAttachmentsArray(fileKeyList: List<String>): JSONArray =
            JSONArray().apply { fileKeyList.forEach { put(JSONObject().put("fileKey", it)) } }

        // NOTE:
        // If your models differ, map accordingly (e.g., Uri -> File, or already-uploaded keys).
        val travelJson = JSONArray().apply {
            travelingEntriesList.value.orEmpty().forEachIndexed { i, t ->
                val attachmentKeys = mutableListOf<String>()
                t.receiptAttachments.orEmpty().forEachIndexed { j, path ->
                    val key = "${nextKey("t${i + 1}_", j + 1)}" // file_t1_1, file_t1_2, ...
                    attachmentKeys += key
                    addFilePart(key, File(path))
                }

                val travellingWithJson = JSONArray().apply {
                    t.travelingWith.orEmpty().forEach { tw ->
                        put(JSONObject().put("id", tw.id).put("name", tw.name ?: ""))
                    }
                }

                put(
                    JSONObject()
                        .put("travel_mode", t.mode?.label ?: "")
                        .put("amount", t.amount ?: 0)
                        .put("travelling_with", travellingWithJson)
                        .put("start_location", t.from ?: "")
                        .put("end_location", t.to ?: "")
                        .put("attachments", buildAttachmentsArray(attachmentKeys))
                )
            }
        }

        val daJson = JSONArray().apply {
            daEntriesList.value.orEmpty().forEachIndexed { i, d ->
                val attachmentKeys = mutableListOf<String>()
                // TODO: adjust `d.attachments` mapping to your actual model
                d.receiptAttachments.orEmpty().forEachIndexed { j, path ->
                    val key = "${nextKey("d${i + 1}_", j + 1)}" // file_d1_1, ...
                    attachmentKeys += key
                    addFilePart(key, File(path))
                }
                put(
                    JSONObject()
                        .put("amount", d.amount ?: 0)
                        .put("attachments", buildAttachmentsArray(attachmentKeys))
                )
            }
        }


        val hotelJson = JSONArray().apply {
            hotelEntriesList.value.orEmpty().forEachIndexed { i, h ->
                val attachmentKeys = mutableListOf<String>()
                h.receiptAttachments.orEmpty().forEachIndexed { j, path ->
                    val key = "${nextKey("h${i + 1}_", j + 1)}" // file_h1_1, file_h1_2, ...
                    attachmentKeys += key
                    addFilePart(key, File(path))
                }

                put(
                    JSONObject()
                        .put("amount", h.amount ?: 0)
                        .put("attachments", buildAttachmentsArray(attachmentKeys))
                )
            }
        }
        val otherJson = JSONArray().apply {
            othersEntriesList.value.orEmpty().forEachIndexed { i, o ->
                val attachmentKeys = mutableListOf<String>()
                // TODO: adjust `o.attachments` mapping to your actual model
                o.receiptAttachments.orEmpty().forEachIndexed { j, path ->
                    val key = "${
                        nextKey(
                            "other${i + 1}_",
                            j + 1
                        )
                    }" // file_other1_1 (adjust if backend requires file_other_1 style)
                    attachmentKeys += key
                    addFilePart(key, File(path))
                }

                put(
                    JSONObject()
                        .put("category", o.category ?: "")
                        .put("amount", o.amount ?: 0)
                        .put("attachments", buildAttachmentsArray(attachmentKeys))
                )
            }
        }

        val expenseTypeJson = JSONObject()
            .put("travel", travelJson)
            .put("da", daJson)
            .put("hotel", hotelJson)
            .put("other", otherJson)

        val dataJson = JSONObject()
            .put("date", selectedDate)
            .put("type", type)
            .put("purpose", selectedPurpose)
            .put("site_id", siteIdsJson)
            .put("project_id", projectId ?: JSONObject.NULL)
            .put(
                "expense_category",
                isOutstationExpense.get()?.let { if (it) "outstation" else "local" } ?: "local")
            .put("circle_id", circleId ?: JSONObject.NULL)
            .put("expense_type", expenseTypeJson)
            .put("remarks", selectedRemarks)

        val dataPart: RequestBody =
            dataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        Log.e("CreateClaimVM", "onSubmitClaim: Data JSON: $dataJson")

        Log.e("CreateClaimVM", "onSubmitClaim: Total File Parts: ${fileParts}")

        executeApiCall(
            apiCall = { claimRepo.createClaim(dataPart, files = fileParts) },
            liveData = createClaimResponse,
        )
    }

    fun clearProjectSelection() {
        selectedProjectId = null
        projects = emptyList()
    }

    fun clearPurposeSelection() {
        purpose.set("")
    }

}
