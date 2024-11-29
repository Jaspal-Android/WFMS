package com.atvantiq.wfms.data.remote
import com.atvantiq.wfms.models.Posts
import retrofit2.http.GET

interface ApiService {
	/***
	 * Network calls
	 */
	@GET(NetworkEndPoints.loginRequest)
	suspend fun loginRequest(): Posts
}