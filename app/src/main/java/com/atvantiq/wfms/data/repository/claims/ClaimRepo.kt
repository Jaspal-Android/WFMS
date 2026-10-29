package com.atvantiq.wfms.data.repository.claims

import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton
import com.atvantiq.wfms.models.reimbursement.review.ClaimApproveResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.google.gson.JsonObject

@Singleton
class ClaimRepo @Inject constructor(private val apiService: ApiService) : IClaimRepo {

    override suspend fun workSiteByDate(date: String): WorkSiteByDateResponse =apiService.workSiteByDate(
        date = date
    )

    override suspend fun employeeByCircle(circleId: String): EmployeeByCircleResponse = apiService.employeeByCircle(
        circleId = circleId
    )

    override suspend fun createClaim(
        data: RequestBody,
        files: List<MultipartBody.Part>
    ) : CreateClaimResponse = apiService.createClaim(
        data = data,
        files = files
    )

    override suspend fun allClaims(page: Int, pageSize: Int, fromDate: String, toDate: String): AllClaimsResponse =
        apiService.allClaims(page = page, pageSize = pageSize, fromDate = fromDate, toDate = toDate)

    override suspend fun claimById(claimId: Long): ClaimDetailResponse = apiService.claimById(
        claimId = claimId
    )

    override suspend fun claimsForReview(page: Int, pageSize: Int, search: String?): ClaimReviewListResponse =
        apiService.claimsForReview(page, pageSize, search?.trim()?.takeIf { it.isNotEmpty() })

    override suspend fun approveClaim(params: JsonObject): ClaimApproveResponse = apiService.approveClaim(params)

    override suspend fun updateClaim(
        claimId: Long,
        data: RequestBody,
        files: List<MultipartBody.Part>
    ): CreateClaimResponse = apiService.updateClaim(
        claimId = claimId,
        data = data,
        files = files
    )

    override suspend fun deleteClaim(claimId: Long): DeleteClaimResponse = apiService.deleteClaim(
        claimId = claimId
    )
}
