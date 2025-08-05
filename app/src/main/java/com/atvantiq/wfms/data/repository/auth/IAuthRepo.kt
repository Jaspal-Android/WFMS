package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.Posts
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow

interface IAuthRepo {
    suspend fun loginRequest(params: JsonObject): LoginResponse
}