package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) : IWorkRepo {

    override suspend fun workAssignedAll(page: Int, pageSize: Int): WorkAssignedAllResponse = apiService.workAssignedAll(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        page = page,
        page_size = pageSize
    )

    override suspend fun workAccept(workId:Int): AcceptWorkResponse = apiService.workAccept(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),workId)

    override suspend fun workStart(workId: RequestBody,latitude: RequestBody,longitude: RequestBody,photo: MultipartBody.Part): StartWorkResponse = apiService.workStart(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        workId = workId,
        latitude = latitude,
        longitude = longitude,
        photo = photo
    )

    override suspend fun workEnd(params: JsonObject): EndWorkResponse = apiService.workEnd(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )

    override suspend fun workSelfAssign(params: JsonObject): SelfAssignResponse = apiService.workSelfAssign(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        params = params
    )

    override suspend fun workById(workId: Int): WorkDetailResponse  = apiService.workById(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        workId = workId
    )
}