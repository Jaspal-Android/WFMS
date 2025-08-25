package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.Posts
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Header

interface IAuthRepo {
    suspend fun loginRequest(params: JsonObject): LoginResponse
    suspend fun empDetails(): EmpDetailResponse
}