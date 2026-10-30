package com.atvantiq.wfms.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <token>` to every request, reading the token when the request is made
 * so a login/logout is picked up immediately and callers never handle the token themselves.
 *
 * - Endpoints that must not carry a token (login, OTP) are marked with [NO_AUTH_HEADER_LINE] in
 *   [ApiService]; the marker is stripped before the request leaves the device.
 * - A request that already sets its own `Authorization` header keeps it.
 * - With no stored token the header is omitted rather than sent empty.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isPublic = request.header(NO_AUTH_HEADER) != null
        val builder = request.newBuilder().removeHeader(NO_AUTH_HEADER)

        if (!isPublic && request.header(AUTHORIZATION) == null) {
            tokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                builder.header(AUTHORIZATION, "$BEARER_PREFIX$token")
            }
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val NO_AUTH_HEADER = "No-Auth"
        const val NO_AUTH_HEADER_LINE = "$NO_AUTH_HEADER: true"
    }
}
