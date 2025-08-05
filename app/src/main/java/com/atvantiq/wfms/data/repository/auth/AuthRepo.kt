package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepo @Inject constructor(private val apiService: ApiService) : IAuthRepo {

    override suspend fun loginRequest(params: JsonObject): LoginResponse = apiService.loginRequest(params)
}

