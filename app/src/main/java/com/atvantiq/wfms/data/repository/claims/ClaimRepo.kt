package com.atvantiq.wfms.data.repository.claims

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiService
import com.ssas.jibli.data.prefs.PrefKeys
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaimRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) : IClaimRepo {

    override suspend fun workSiteByDate(date: String): WorkSiteByDateResponse =apiService.workSiteByDate(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        date = date
    )

    override suspend fun employeeByCircle(circleId: String): EmployeeByCircleResponse = apiService.employeeByCircle(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        circleId = circleId
    )

    override suspend fun createClaim(
        data: RequestBody,
        files: List<MultipartBody.Part>
    ) : CreateClaimResponse = apiService.createClaim(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        data = data,
        files = files
    )

    override suspend fun allClaims(page: Int, pageSize: Int): AllClaimsResponse = apiService.allClaims(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        page = page,
        pageSize = pageSize
    )
}