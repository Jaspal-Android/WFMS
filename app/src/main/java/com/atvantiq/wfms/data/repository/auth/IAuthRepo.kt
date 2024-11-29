package com.atvantiq.wfms.data.repository.auth
import com.atvantiq.wfms.models.Posts

interface IAuthRepo {
    //suspend fun loginRequest(): Flow<Posts>
    suspend fun loginRequest(): Posts

}