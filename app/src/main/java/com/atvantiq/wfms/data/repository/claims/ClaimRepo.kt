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

    override suspend fun allClaims(page: Int, pageSize: Int): AllClaimsResponse = apiService.allClaims(
        page = page,
        pageSize = pageSize
    )

    override suspend fun claimById(claimId: Long): ClaimDetailResponse = apiService.claimById(
        claimId = claimId
    )

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
