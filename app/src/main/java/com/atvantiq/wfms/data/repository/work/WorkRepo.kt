package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.models.inventory.InventoryByProjectResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) : IWorkRepo {

    override suspend fun workAssignedAll(page: Int, pageSize: Int, search: String?, status: String?): WorkAssignedResponse =
        apiService.workAssignedAll(
            token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""),
            page = page,
            page_size = pageSize,
            search = search?.takeIf { it.isNotBlank() },
            status = status
        )

    override suspend fun workAccept(workSiteId:Long): WorkDetailResponse = apiService.workAccept(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),workSiteId)

    override suspend fun workStart(workSiteId: RequestBody,latitude: RequestBody,longitude: RequestBody,photo: MultipartBody.Part): WorkDetailResponse = apiService.workStart(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        workSiteId = workSiteId,
        latitude = latitude,
        longitude = longitude,
        photo = photo
    )

    override suspend fun workEnd(params: JsonObject): WorkDetailResponse = apiService.workEnd(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )

    override suspend fun workSelfAssign(params: JsonObject): SelfAssignResponse = apiService.workSelfAssign(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )

    override suspend fun workById(workSiteId: Long): WorkDetailResponse = apiService.workById(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        workSiteId = workSiteId
    )

    override suspend fun workDetailByDate(date: String): WorkDetailsByDateResponse  = apiService.workDetailByDate(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        date = date
    )

    override suspend fun inventoryByProject(projectId: Long): InventoryByProjectResponse = apiService.inventoryByProject(
            token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
            projectId = projectId
    )
}