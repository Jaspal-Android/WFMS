package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface IWorkRepo {

    suspend fun workAssignedAll(page:Int,pageSize:Int): WorkAssignedResponse

    suspend fun workAccept(workSiteId: Long): WorkDetailResponse

    suspend fun workStart(workSiteId: RequestBody, latitude: RequestBody, longitude: RequestBody, photo: MultipartBody.Part): WorkDetailResponse

    suspend fun workEnd(params: JsonObject): WorkDetailResponse

    suspend fun workSelfAssign(params: JsonObject): SelfAssignResponse

    suspend fun workById(workSiteId:Long): WorkDetailResponse

    suspend fun workDetailByDate(date: String): WorkDetailsByDateResponse
}