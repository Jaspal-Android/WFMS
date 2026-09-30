package com.atvantiq.wfms.models.empDetail

import com.atvantiq.wfms.models.circle.CircleData
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * `employee/me` returns `circle` as an array of circle objects, but older builds (and the
 * EMP_DATA payload already cached on upgrading devices) stored it as a plain code string.
 *
 * Accepting both shapes keeps the dashboard from throwing
 * "Expected a string but was BEGIN_ARRAY at path $.data.circle" and, just as importantly,
 * stops a stale string-shaped cache from crashing the first launch after an update.
 *
 * Only deserialization is customised; Gson keeps using reflection to write the list back out.
 */
class CircleListDeserializer : JsonDeserializer<List<CircleData>> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<CircleData> {
        if (json == null || json.isJsonNull) return emptyList()

        // Current API shape: [{ "id": .., "code": "CHD", "name": "chandigarh" }]
        if (json.isJsonArray) {
            return json.asJsonArray.mapNotNull { element ->
                runCatching { context?.deserialize<CircleData>(element, CircleData::class.java) }
                    .getOrNull()
            }
        }

        // Legacy shape: "CHD" — keep the code so the UI still has something to show.
        if (json.isJsonPrimitive) {
            val code = json.asString.orEmpty()
            return if (code.isBlank()) emptyList()
            else listOf(CircleData(code = code, id = 0L, name = code))
        }

        // Single object shape, tolerated defensively.
        if (json.isJsonObject) {
            return runCatching { context?.deserialize<CircleData>(json, CircleData::class.java) }
                .getOrNull()
                ?.let { listOf(it) }
                ?: emptyList()
        }

        return emptyList()
    }
}
