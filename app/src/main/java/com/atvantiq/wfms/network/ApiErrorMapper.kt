package com.atvantiq.wfms.network

import androidx.annotation.StringRes
import com.atvantiq.wfms.R
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.serverMessage
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException

/** What went wrong with a request, in terms a user (not a developer) can act on. */
enum class ApiErrorKind(@StringRes val messageRes: Int) {
    NO_INTERNET(R.string.error_no_internet),
    TIMEOUT(R.string.error_timeout),
    UNAUTHORIZED(R.string.error_unauthorized),
    FORBIDDEN(R.string.error_forbidden),
    CLIENT(R.string.error_client),
    SERVER(R.string.error_server),
    MALFORMED(R.string.error_malformed),
    UNKNOWN(R.string.something_went_wrong);

    /** Kinds that point at a bug rather than the user's connection or input, so they are reported. */
    val isUnexpected: Boolean get() = this == MALFORMED || this == UNKNOWN
}

/** A failed request that did not get an HTTP response. [message] is safe to show to the user. */
class ApiException(
    val kind: ApiErrorKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * A failed HTTP response with a user-facing [message]. It stays an [HttpException], so existing
 * `is HttpException` / `code() == 401` checks keep working. The error body can only be read once,
 * so the server's own [serverMessage] is captured here.
 */
class ApiHttpException(
    original: HttpException,
    val kind: ApiErrorKind,
    override val message: String,
    val serverMessage: String?
) : HttpException(requireNotNull(original.response())) {
    init {
        initCause(original)
    }
}

/**
 * Turns any failure into an exception whose `message` is user-readable. Screens throughout the app
 * already show `throwable.message`, so mapping once where errors are caught fixes all of them.
 */
object ApiErrorMapper {

    fun kindOf(throwable: Throwable): ApiErrorKind = when (throwable) {
        is NoInternetException -> ApiErrorKind.NO_INTERNET
        is ApiException -> throwable.kind
        is ApiHttpException -> throwable.kind
        is HttpException -> kindOfStatus(throwable.code())
        is InterruptedIOException -> ApiErrorKind.TIMEOUT
        is FileNotFoundException -> ApiErrorKind.UNKNOWN
        // Gson's parse failures are checked before IOException: MalformedJsonException is one.
        is JsonParseException, is MalformedJsonException -> ApiErrorKind.MALFORMED
        is IOException -> ApiErrorKind.NO_INTERNET
        else -> ApiErrorKind.UNKNOWN
    }

    private fun kindOfStatus(code: Int): ApiErrorKind = when (code) {
        401 -> ApiErrorKind.UNAUTHORIZED
        403 -> ApiErrorKind.FORBIDDEN
        408 -> ApiErrorKind.TIMEOUT
        in 400..499 -> ApiErrorKind.CLIENT
        in 500..599 -> ApiErrorKind.SERVER
        else -> ApiErrorKind.UNKNOWN
    }

    /**
     * Returns [throwable] as an exception with a user-facing message from [messageFor]. A 4xx
     * response keeps the backend's own message when it sent one (business-rule failures such as
     * "claim is locked"); everything else gets the generic text for its [ApiErrorKind].
     */
    fun userFacing(throwable: Exception, messageFor: (ApiErrorKind) -> String): Exception {
        if (throwable is ApiException || throwable is ApiHttpException || throwable is NoInternetException) {
            return throwable
        }
        val kind = kindOf(throwable)
        if (throwable is HttpException) {
            val serverMessage = throwable.serverMessage()
            val message = if (throwable.code() in 400..499 && serverMessage != null) {
                serverMessage
            } else {
                messageFor(kind)
            }
            return ApiHttpException(throwable, kind, message, serverMessage)
        }
        return ApiException(kind, messageFor(kind), throwable)
    }
}
