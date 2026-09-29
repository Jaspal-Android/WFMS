package com.atvantiq.wfms.utils

import org.json.JSONObject
import retrofit2.HttpException

/**
 * The backend answers business-rule failures (e.g. a locked claim) with a 4xx status and a
 * `{"code", "message", "data", "success"}` body. Returns that `message`, or null when the body
 * is absent or not in that shape, so callers can fall back to a generic text.
 */
fun HttpException.serverMessage(): String? = try {
    response()?.errorBody()?.string()
        ?.let { JSONObject(it).optString("message") }
        ?.takeIf { it.isNotBlank() }
} catch (e: Exception) {
    null
}
