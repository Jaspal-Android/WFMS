package com.atvantiq.wfms.network
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
	/***
	 * Network calls
	 */
	@POST(NetworkEndPoints.loginRequest)
	suspend fun loginRequest(@Body params: JsonObject): LoginResponse
}