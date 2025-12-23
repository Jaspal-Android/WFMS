package com.atvantiq.wfms.data.repository.work

import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface IWorkRepo {

    suspend fun workAssignedAll(page:Int,pageSize:Int): WorkAssignedResponse

    suspend fun workAccept(workId: Long): AcceptWorkResponse

    suspend fun workStart(workId: RequestBody, latitude: RequestBody, longitude: RequestBody, photo: MultipartBody.Part): StartWorkResponse

    suspend fun workEnd(params: JsonObject): EndWorkResponse

    suspend fun workSelfAssign(params: JsonObject): SelfAssignResponse

    suspend fun workById(workId:Long): WorkDetailResponse

    suspend fun workDetailByDate(date: String): WorkDetailsByDateResponse
}