package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.data.remote.ApiService
import com.atvantiq.wfms.models.Posts
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepo @Inject constructor(private val apiService: ApiService) : IAuthRepo {

    override suspend fun loginRequest(): Posts = apiService.loginRequest()

    /* override suspend fun loginRequest(): Flow<Posts> {
         return flow {
             var response  = apiService.loginRequest()
             emit(response)
         }.flowOn(Dispatchers.IO)
     }*/

}