package com.atvantiq.wfms.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Header
import retrofit2.http.Headers

class AuthInterceptorTest {

    private fun proceed(interceptor: AuthInterceptor, request: Request): Request {
        val sent = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(capture(sent)) } returns mockk<Response>()
        interceptor.intercept(chain)
        return sent.captured
    }

    private fun request(vararg headers: Pair<String, String>) =
        Request.Builder().url("https://example.com/api").apply { headers.forEach { (k, v) -> header(k, v) } }.build()

    @Test
    fun `adds the bearer token`() {
        val sent = proceed(AuthInterceptor { "abc" }, request())
        assertEquals("Bearer abc", sent.header("Authorization"))
    }

    @Test
    fun `reads the token on every request`() {
        var token: String? = "first"
        val interceptor = AuthInterceptor { token }

        assertEquals("Bearer first", proceed(interceptor, request()).header("Authorization"))
        token = "second"
        assertEquals("Bearer second", proceed(interceptor, request()).header("Authorization"))
    }

    @Test
    fun `omits the header when there is no token`() {
        assertNull(proceed(AuthInterceptor { null }, request()).header("Authorization"))
        assertNull(proceed(AuthInterceptor { "  " }, request()).header("Authorization"))
    }

    @Test
    fun `public requests get no token and the marker is stripped`() {
        val sent = proceed(
            AuthInterceptor { "abc" },
            request(AuthInterceptor.NO_AUTH_HEADER to "true")
        )
        assertNull(sent.header("Authorization"))
        assertNull(sent.header(AuthInterceptor.NO_AUTH_HEADER))
    }

    @Test
    fun `an explicit Authorization header is kept`() {
        val sent = proceed(AuthInterceptor { "abc" }, request("Authorization" to "Bearer explicit"))
        assertEquals("Bearer explicit", sent.header("Authorization"))
    }

    /** Guards the migration: tokens are only ever added by the interceptor. */
    @Test
    fun `only login and OTP endpoints are public and no endpoint takes a token parameter`() {
        val publicEndpoints = ApiService::class.java.methods
            .filter { method ->
                method.getAnnotation(Headers::class.java)?.value?.contains(AuthInterceptor.NO_AUTH_HEADER_LINE) == true
            }
            .map { it.name }
            .toSet()
        assertEquals(setOf("loginRequest", "requestOTP", "verifyOTP"), publicEndpoints)

        val withTokenParam = ApiService::class.java.methods.filter { method ->
            method.parameterAnnotations.flatten().any { it is Header && it.value.equals("Authorization", true) }
        }
        assertTrue("Endpoints still take a token: ${withTokenParam.map { it.name }}", withTokenParam.isEmpty())
    }
}
