package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ReimbursementData
import com.atvantiq.wfms.constants.ValConstants
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
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.site.detail.SiteDetail
import com.atvantiq.wfms.models.site.detail.SiteDetailResponse
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.workSiteByDate.Site
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
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
    val isSubmitting = ObservableField<Boolean>().apply { set(false) }

    /* Edit mode: header (date, sites, project, circle, category) is locked; only expenses change. */
    val isEditMode = ObservableField<Boolean>().apply { set(false) }
    val lockedSitesSummary = ObservableField<String>().apply { set("") }
    private var lockedHeader: LockedClaimHeader? = null

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
        selectedProjectId = project.id
        selectedCircleId = null
        selectedCircleCode = null
        circles = project.circle.orEmpty().filterNotNull()
    }

    /*On Project circle selected*/
    fun onCircleSelected(circle: Circle) {
        selectedCircleId = circle.id
        selectedCircleCode = circle.code
    }

    /*Handling multiple site*/
    fun addMultipleSite(sites: List<SiteData>) {
        val currentList = selectedSitesIdList.value?.toMutableList() ?: mutableListOf()
        currentList.clear()
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

    /** The claim being edited plus one of its sites, which supplies the claim's project and circle. */
    data class ClaimEditSource(val claim: ClaimDetailResponse, val site: SiteDetailResponse?)

    /*Claim to edit*/
    var claimForEditResponse = MutableLiveData<ApiState<ClaimEditSource>>()
    fun loadClaimForEdit(claimId: Long) {
        isEditMode.set(true)
        if (lockedHeader?.claimId == claimId) return // already loaded; keep the user's edits
        executeApiCall(
            apiCall = {
                val claim = claimRepo.claimById(claimId)
                val siteId = claim.data?.sites?.firstNotNullOfOrNull { it.siteId }
                val site = if (claim.code == ValConstants.SUCCESS_CODE && siteId != null) {
                    creationRepo.siteById(siteId)
                } else null
                ClaimEditSource(claim, site)
            },
            liveData = claimForEditResponse,
        )
    }

    /** Fills the form from an existing claim. Returns false when the claim cannot be edited. */
    fun applyClaimForEdit(claim: ClaimData?, site: SiteDetail?): Boolean {
        // LiveData replays the last response after a configuration change; don't overwrite edits.
        if (lockedHeader != null && lockedHeader?.claimId == claim?.id) return true
        val editable = claim?.let { ClaimEditMapper.toEditableClaim(it, site) } ?: return false
        val header = editable.header
        lockedHeader = header
        selectedCircleCode = header.circleCode // "travelling with" lists employees of this circle
        date.set(header.date)
        purpose.set(header.purpose)
        isMultiSite.set(!header.claimType.equals(ReimbursementData.CLAIM_SINGLE_SITE, ignoreCase = true))
        isOutstationExpense.set(header.expenseCategory.equals(ReimbursementData.CLAIM_TYPE_OUTSTATION, ignoreCase = true))
        lockedSitesSummary.set(header.siteNames.joinToString(", "))
        remarks.value = editable.remarks
        travelingEntriesList.value = editable.travel
        daEntriesList.value = editable.da
        hotelEntriesList.value = editable.hotel
        othersEntriesList.value = editable.other
        return true
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
        if (isEditMode.get() == true) {
            // Header comes from the saved claim and is not editable; only expenses need checking.
            return hasAnyExpenseEntry(includeHotel = isOutstationExpense.get() == true) ||
                postValidationError(CreateClaimErrorHandler.EMPTY_EXPENSES)
        }
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

            if (!hasAnyExpenseEntry(includeHotel = isOutstationExpense.get() == true)) {
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

        if (!hasAnyExpenseEntry(includeHotel = isOutstationExpense.get() == true)) {
            return postValidationError(CreateClaimErrorHandler.EMPTY_EXPENSES)
        }

        return true
    }

    /*On submit claim*/
    var createClaimResponse = MutableLiveData<ApiState<CreateClaimResponse>>()
    fun onSubmitClaim() {
        if (isSubmitting.get() == true) return
        // Never fall through to "create" while editing a claim that failed to load.
        if (isEditMode.get() == true && lockedHeader == null) return
        if (!validateCreateClaim()) return
        if (!Utils.isInternet(getApplication())) {
            createClaimResponse.value = ApiState.error(NoInternetException("No Internet Connection"))
            return
        }
        isSubmitting.set(true)

        val (dataPart, fileParts) = buildClaimRequest()
        val editingClaimId = lockedHeader?.claimId
        executeApiCall(
            apiCall = {
                if (editingClaimId != null) claimRepo.updateClaim(editingClaimId, dataPart, fileParts)
                else claimRepo.createClaim(dataPart, files = fileParts)
            },
            liveData = createClaimResponse,
            onSuccess = { onSubmitResponse(it.code) },
            onError = { isSubmitting.set(false) }
        )
    }

    /** True when the server accepted the claim: created (201), or updated (200) in edit mode. */
    fun isClaimSaved(code: Int?): Boolean =
        code == ValConstants.SUCCESS_CREATION_CODE ||
            (isEditMode.get() == true && code == ValConstants.SUCCESS_CODE)

    // Once the claim is saved Submit stays locked. Releasing it on every response would let a
    // dismissed success dialog re-send the same claim and create a duplicate.
    fun onSubmitResponse(code: Int?) {
        if (!isClaimSaved(code)) isSubmitting.set(false)
    }

    /**
     * Serializes the whole claim into the JSON `data` part plus its multipart file parts.
     * Kept out of onSubmitClaim so that method stays validate -> guard -> delegate -> call.
     */
    private fun buildClaimRequest(): Pair<RequestBody, List<MultipartBody.Part>> {
        val collector = AttachmentCollector()
        val expenseTypeJson = buildExpenseTypeJson(collector)
        val selectedRemarks = remarks.value?.trim().orEmpty()
        val header = lockedHeader
        val dataJson = if (header != null) buildLockedHeaderJson(header) else buildNewClaimHeaderJson()
        dataJson
            .put("expense_type", expenseTypeJson)
            .put("remarks", selectedRemarks)

        val dataPart: RequestBody =
            dataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        return dataPart to collector.fileParts
    }

    private fun buildExpenseTypeJson(collector: AttachmentCollector): JSONObject = JSONObject()
        .put("travel", buildTravelJson(collector))
        .put("da", buildAmountOnlyJson("d", daEntriesList.value.orEmpty(), collector,
            amountOf = { it.amount }, pathsOf = { it.receiptAttachments }))
        .put("hotel", buildAmountOnlyJson("h", hotelEntriesList.value.orEmpty(), collector,
            amountOf = { it.amount }, pathsOf = { it.receiptAttachments }))
        .put("other", buildOtherJson(collector))

    /**
     * Edit: echoes the saved claim's header unchanged, in the same per-site shape create sends
     * (multi-site: purchase_order_ids, single-site: work_site_id).
     */
    private fun buildLockedHeaderJson(header: LockedClaimHeader): JSONObject {
        val isMultipleSite = !header.claimType.equals(ReimbursementData.CLAIM_SINGLE_SITE, ignoreCase = true)
        val siteIdsJson = JSONArray().apply {
            header.sites.forEach { site ->
                val siteJson = JSONObject().put("id", site.id)
                if (isMultipleSite) {
                    siteJson.put("purchase_order_ids", JSONArray(site.purchaseOrderIds))
                } else {
                    site.workSiteId?.let { siteJson.put("work_site_id", it) }
                }
                put(siteJson)
            }
        }
        return JSONObject()
            .put("date", header.date)
            .put("type", header.claimType)
            .put("purpose", header.purpose)
            .put("site_id", siteIdsJson)
            .put("project_id", header.projectId)
            .put("expense_category", header.expenseCategory)
            .put("circle_id", header.circleId)
    }

    private fun buildNewClaimHeaderJson(): JSONObject {
        val selectedDate = date.get()?.trim().orEmpty()
        val selectedPurpose = purpose.get()?.trim().orEmpty()
        val isMultipleSite = isMultiSite.get() == true
        val type = if (isMultipleSite) "multiple_site" else "single_site"

        val siteIdsJson = JSONArray().apply {
            if (isMultipleSite) {
                selectedSitesIdList.value.orEmpty().forEach { s ->
                    val poIds = JSONArray().apply { s.selectedPo?.id?.let { put(it) } }
                    put(JSONObject().put("id", s.id).put("purchase_order_ids", poIds))
                }
            } else {
                selectedSingleSite?.siteId?.let { id ->
                    put(JSONObject().put("id", id).put("work_site_id", selectedSingleSite?.workSiteId))
                }
            }
        }

        return JSONObject()
            .put("date", selectedDate)
            .put("type", type)
            .put("purpose", selectedPurpose)
            .put("site_id", siteIdsJson)
            .put("project_id", selectedProjectId ?: JSONObject.NULL)
            .put("expense_category",
                isOutstationExpense.get()?.let { if (it) "outstation" else "local" } ?: "local")
            .put("circle_id", selectedCircleId ?: JSONObject.NULL)
    }

    private fun buildTravelJson(collector: AttachmentCollector): JSONArray = JSONArray().apply {
        travelingEntriesList.value.orEmpty().forEachIndexed { i, t ->
            val attachments = collector.collect("t", i + 1, t.receiptAttachments)
            val travellingWithJson = JSONArray().apply {
                t.travelingWith.orEmpty().forEach { tw ->
                    put(JSONObject().put("id", tw.id).put("name", tw.name))
                }
            }
            put(
                JSONObject()
                    .put("travel_mode", t.mode?.label ?: "")
                    .put("amount", t.amount)
                    .put("travelling_with", travellingWithJson)
                    .put("start_location", t.from)
                    .put("end_location", t.to)
                    .put("attachments", attachments)
                    .apply { putAutoKmFields(t) }
            )
        }
    }

    /** Auto-fetched KM data only exists on saved trips; new trips leave these out as before. */
    private fun JSONObject.putAutoKmFields(t: TravelExpense) {
        t.distanceKm?.let { put("distance_km", it) }
        t.distanceSource?.let { put("distance_source", it) }
        t.tripRefs?.takeIf { it.isNotEmpty() }?.let { refs ->
            put("trip_refs", JSONArray().apply {
                refs.forEach { put(JSONObject().put("start_at", it.startAt).put("end_at", it.endAt)) }
            })
        }
    }

    private fun buildOtherJson(collector: AttachmentCollector): JSONArray = JSONArray().apply {
        othersEntriesList.value.orEmpty().forEachIndexed { i, o ->
            put(
                JSONObject()
                    .put("category", o.category)
                    .put("amount", o.amount)
                    .put("attachments", collector.collect("other", i + 1, o.receiptAttachments))
            )
        }
    }

    /** DA and Hotel share the same amount-only shape; only the file-key letter and source list differ. */
    private fun <T> buildAmountOnlyJson(
        letter: String,
        entries: List<T>,
        collector: AttachmentCollector,
        amountOf: (T) -> Any?,
        pathsOf: (T) -> List<String>?
    ): JSONArray = JSONArray().apply {
        entries.forEachIndexed { i, entry ->
            put(
                JSONObject()
                    .put("amount", amountOf(entry))
                    .put("attachments", collector.collect(letter, i + 1, pathsOf(entry)))
            )
        }
    }

    /**
     * Accumulates multipart file parts while producing the per-entry `attachments` JSON array.
     * File key format preserved exactly: file_{letter}{entry}_{index}, e.g. file_t1_1, file_other2_3.
     */
    private class AttachmentCollector {
        val fileParts = mutableListOf<MultipartBody.Part>()

        fun collect(letter: String, entryIndex1Based: Int, paths: List<String>?): JSONArray {
            val keys = mutableListOf<String>()
            paths.orEmpty().forEachIndexed { j, path ->
                val key = "file_${letter}${entryIndex1Based}_${j + 1}"
                val file = File(path)
                if (file.exists() && file.isFile) {
                    val body = file.asRequestBody("application/octet-stream".toMediaType())
                    fileParts += MultipartBody.Part.createFormData(key, file.name, body)
                    keys += key
                }
            }
            return JSONArray().apply { keys.forEach { put(JSONObject().put("fileKey", it)) } }
        }
    }

    fun onSubmitCompleted() {
        isSubmitting.set(false)
    }

    fun clearProjectSelection() {
        selectedProjectId = null
        projects = emptyList()
    }

    fun clearPurposeSelection() {
        purpose.set("")
    }

}
