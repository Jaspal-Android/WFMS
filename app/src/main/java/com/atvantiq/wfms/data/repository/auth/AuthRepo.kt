package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) : IAuthRepo {

    override suspend fun loginRequest(params: JsonObject): LoginResponse = apiService.loginRequest(params)
    override suspend fun empDetails(): EmpDetailResponse = apiService.empDetails(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,"")
    )
}

