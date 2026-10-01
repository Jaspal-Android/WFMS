package com.atvantiq.wfms.models.reimbursement.review

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/** A claim `status` sent as a string ("Submitted") or an object with `code`, `name` or `status`. */
class ClaimStatusDeserializer : JsonDeserializer<String?> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): String? =
        statusText(json)

    companion object {
        private val OBJECT_KEYS = listOf("code", "name", "status")

        fun statusText(element: JsonElement?): String? = when {
            element == null || element.isJsonNull -> null
            element.isJsonObject -> OBJECT_KEYS.firstNotNullOfOrNull { key ->
                element.asJsonObject.get(key)?.takeIf { it.isJsonPrimitive }?.asString
            }
            element.isJsonPrimitive -> element.asString
            else -> null
        }
    }
}
