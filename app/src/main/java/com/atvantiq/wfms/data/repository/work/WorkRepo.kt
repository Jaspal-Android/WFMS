package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.atvantiq.wfms.models.inventory.InventoryByProjectResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepo @Inject constructor(private val apiService: ApiService) : IWorkRepo {

    override suspend fun workAssignedAll(page: Int, pageSize: Int, search: String?, status: String?): WorkAssignedResponse =
        apiService.workAssignedAll(
            page = page,
            page_size = pageSize,
            search = search?.takeIf { it.isNotBlank() },
            status = status
        )

    override suspend fun workAccept(workSiteId:Long): WorkDetailResponse = apiService.workAccept(
        workSiteId)

    override suspend fun workStart(workSiteId: RequestBody,latitude: RequestBody,longitude: RequestBody,photo: MultipartBody.Part): WorkDetailResponse = apiService.workStart(
        workSiteId = workSiteId,
        latitude = latitude,
        longitude = longitude,
        photo = photo
    )

    override suspend fun workEnd(params: JsonObject): WorkDetailResponse = apiService.workEnd(
        params = params
    )

    override suspend fun workSelfAssign(params: JsonObject): SelfAssignResponse = apiService.workSelfAssign(
        params = params
    )

    override suspend fun workById(workSiteId: Long): WorkDetailResponse = apiService.workById(
        workSiteId = workSiteId
    )

    override suspend fun workDetailByDate(date: String): WorkDetailsByDateResponse  = apiService.workDetailByDate(
        date = date
    )

    override suspend fun inventoryByProject(projectId: Long): InventoryByProjectResponse = apiService.inventoryByProject(
            projectId = projectId
    )
}