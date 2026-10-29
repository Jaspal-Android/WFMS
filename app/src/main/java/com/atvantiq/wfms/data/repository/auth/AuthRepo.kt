package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.forgotPassword.ForgotPasswordResponse
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginWithOTP.RequestOtpResponse
import com.atvantiq.wfms.models.notification.UpdateNotificationTokenResponse
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepo @Inject constructor(private val apiService: ApiService) : IAuthRepo {

    override suspend fun loginRequest(params: JsonObject): LoginResponse = apiService.loginRequest(params)

    override suspend fun empDetails(): EmpDetailResponse = apiService.empDetails()

    override suspend fun sendNotificationToken(params: JsonObject): UpdateNotificationTokenResponse = apiService.sendNotificationToken(
            params = params
        )

    override suspend fun forgotPassword(params: JsonObject): ForgotPasswordResponse = apiService.forgotPassword(
        params = params)

    override suspend fun requestOTP(params: JsonObject): RequestOtpResponse = apiService.requestOTP(params)

    override suspend fun verifyOTP(params: JsonObject): LoginResponse = apiService.verifyOTP(params)
}

