package com.atvantiq.wfms.data.repository.claims

import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimApproveResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.google.gson.JsonObject
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface IClaimRepo {
    suspend fun workSiteByDate(date: String): WorkSiteByDateResponse
    suspend fun employeeByCircle(circleId: String): EmployeeByCircleResponse
    suspend fun createClaim(data: RequestBody, files: List<MultipartBody.Part>): CreateClaimResponse
    suspend fun allClaims(page:Int,pageSize:Int ): AllClaimsResponse
    suspend fun claimById(claimId: Long): ClaimDetailResponse

    /** The admin claims list; [search] null or blank lists everything. */
    suspend fun claimsForReview(page: Int, pageSize: Int, search: String?): ClaimReviewListResponse

    suspend fun approveClaim(params: JsonObject): ClaimApproveResponse
    suspend fun updateClaim(claimId: Long, data: RequestBody, files: List<MultipartBody.Part>): CreateClaimResponse
    suspend fun deleteClaim(claimId: Long): DeleteClaimResponse
}