package com.atvantiq.wfms.models.attendance.attendanceDetails

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * An attendance `status` sent either as a number (`4`) or as an object (`{"code": 4, "label": "IDLE"}`),
 * depending on the endpoint. Anything else reads as null, never a crash.
 */
class StatusCodeDeserializer : JsonDeserializer<Int?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Int? {
        val value = when {
            json == null || json.isJsonNull -> return null
            json.isJsonObject -> json.asJsonObject.get(CODE_KEY)
            else -> json
        }
        return value?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let { primitive ->
            if (primitive.isNumber) primitive.asInt else primitive.asString.toIntOrNull()
        }
    }

    private companion object {
        const val CODE_KEY = "code"
    }
}
