package com.atvantiq.wfms.utils

import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.network.ApiHttpException
import com.google.gson.JsonParser
import retrofit2.HttpException

/**
 * The backend answers business-rule failures (e.g. a locked claim) with a 4xx status and a
 * `{"code", "message", "data", "success"}` body. Returns that `message`, or null when the body
 * is absent or not in that shape, so callers can fall back to a generic text.
 *
 * The body can only be read once; an [ApiHttpException] has already captured it.
 */
fun HttpException.serverMessage(): String? {
    if (this is ApiHttpException) return serverMessage
    return try {
        val body = response()?.errorBody()?.string() ?: return null
        JsonParser.parseString(body).asJsonObject.get("message")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

/** True for an HTTP 401, i.e. the session is no longer valid. */
fun Throwable?.isUnauthorized(): Boolean =
    this is HttpException && code() == ValConstants.UNAUTHORIZED_CODE
