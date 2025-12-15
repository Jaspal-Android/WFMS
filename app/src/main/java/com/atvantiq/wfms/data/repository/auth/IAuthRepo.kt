package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.Posts
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.forgotPassword.ForgotPasswordResponse
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginWithOTP.RequestOtpResponse
import com.atvantiq.wfms.models.notification.UpdateNotificationTokenResponse
import com.atvantiq.wfms.network.NetworkEndPoints
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface IAuthRepo {
    suspend fun loginRequest(params: JsonObject): LoginResponse
    suspend fun empDetails(): EmpDetailResponse
    suspend fun sendNotificationToken(params: JsonObject) : UpdateNotificationTokenResponse
    suspend fun forgotPassword(params: JsonObject): ForgotPasswordResponse
    suspend fun requestOTP(params: JsonObject): RequestOtpResponse
    suspend fun verifyOTP(params: JsonObject): LoginResponse
}