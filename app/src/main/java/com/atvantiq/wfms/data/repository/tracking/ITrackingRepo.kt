package com.atvantiq.wfms.data.repository.tracking

import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.models.myDay.MyDayResponse
import com.google.gson.JsonObject

interface ITrackingRepo {
    suspend fun sendLocation(params: JsonObject):SendLocationResponse

    /** [date] is yyyy-MM-dd, or null for today. */
    suspend fun myDay(date: String?): MyDayResponse
}
