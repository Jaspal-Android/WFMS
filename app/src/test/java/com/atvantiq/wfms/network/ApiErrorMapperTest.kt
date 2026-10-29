package com.atvantiq.wfms.network

import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.isUnauthorized
import com.atvantiq.wfms.utils.serverMessage
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ApiErrorMapperTest {

    private fun http(code: Int, body: String = ""): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    private fun messageFor(kind: ApiErrorKind) = "text-for-${kind.name}"

    private fun userFacing(t: Exception) = ApiErrorMapper.userFacing(t, ::messageFor)

    // ---- classification ----

    @Test
    fun `connectivity failures map to no internet`() {
        listOf(
            UnknownHostException("api"), ConnectException("refused"),
            SSLHandshakeException("handshake"), IOException("unexpected end of stream")
        ).forEach { assertEquals(it.toString(), ApiErrorKind.NO_INTERNET, ApiErrorMapper.kindOf(it)) }
    }

    @Test
    fun `read connect and call timeouts map to timeout`() {
        assertEquals(ApiErrorKind.TIMEOUT, ApiErrorMapper.kindOf(SocketTimeoutException("read")))
        assertEquals(ApiErrorKind.TIMEOUT, ApiErrorMapper.kindOf(InterruptedIOException("timeout")))
        assertEquals(ApiErrorKind.TIMEOUT, ApiErrorMapper.kindOf(http(408)))
    }

    @Test
    fun `a missing receipt file is not reported as a connection problem`() {
        assertEquals(ApiErrorKind.UNKNOWN, ApiErrorMapper.kindOf(FileNotFoundException("/receipt.jpg")))
    }

    @Test
    fun `http statuses map by class`() {
        assertEquals(ApiErrorKind.UNAUTHORIZED, ApiErrorMapper.kindOf(http(401)))
        assertEquals(ApiErrorKind.FORBIDDEN, ApiErrorMapper.kindOf(http(403)))
        assertEquals(ApiErrorKind.CLIENT, ApiErrorMapper.kindOf(http(400)))
        assertEquals(ApiErrorKind.CLIENT, ApiErrorMapper.kindOf(http(404)))
        assertEquals(ApiErrorKind.SERVER, ApiErrorMapper.kindOf(http(500)))
        assertEquals(ApiErrorKind.SERVER, ApiErrorMapper.kindOf(http(503)))
    }

    @Test
    fun `unreadable json maps to malformed even though MalformedJsonException is an IOException`() {
        assertEquals(ApiErrorKind.MALFORMED, ApiErrorMapper.kindOf(JsonSyntaxException("Expected BEGIN_OBJECT")))
        assertEquals(ApiErrorKind.MALFORMED, ApiErrorMapper.kindOf(MalformedJsonException("bad")))
    }

    @Test
    fun `anything else is unknown`() {
        assertEquals(ApiErrorKind.UNKNOWN, ApiErrorMapper.kindOf(IllegalStateException("boom")))
        assertEquals(ApiErrorKind.UNKNOWN, ApiErrorMapper.kindOf(NullPointerException()))
    }

    @Test
    fun `only bug-like kinds are reported`() {
        assertTrue(ApiErrorKind.MALFORMED.isUnexpected)
        assertTrue(ApiErrorKind.UNKNOWN.isUnexpected)
        listOf(
            ApiErrorKind.NO_INTERNET, ApiErrorKind.TIMEOUT, ApiErrorKind.UNAUTHORIZED,
            ApiErrorKind.FORBIDDEN, ApiErrorKind.CLIENT, ApiErrorKind.SERVER
        ).forEach { assertFalse(it.name, it.isUnexpected) }
    }

    // ---- user-facing messages ----

    @Test
    fun `a non-http failure gets the generic text for its kind and keeps its cause`() {
        val cause = SocketTimeoutException("read timed out")

        val mapped = userFacing(cause) as ApiException

        assertEquals("text-for-TIMEOUT", mapped.message)
        assertEquals(ApiErrorKind.TIMEOUT, mapped.kind)
        assertSame(cause, mapped.cause)
    }

    @Test
    fun `a 4xx keeps the message the backend sent`() {
        val mapped = userFacing(http(400, """{"message":"Claim is locked","success":false}"""))

        assertEquals("Claim is locked", mapped.message)
    }

    @Test
    fun `a 4xx without a usable body falls back to the generic text`() {
        assertEquals("text-for-CLIENT", userFacing(http(404, "")).message)
        assertEquals("text-for-CLIENT", userFacing(http(404, "<html>nope</html>")).message)
        assertEquals("text-for-CLIENT", userFacing(http(404, """{"message":""}""")).message)
        assertEquals("text-for-FORBIDDEN", userFacing(http(403, "[]")).message)
    }

    @Test
    fun `a 5xx never shows the server's own text`() {
        val mapped = userFacing(http(500, """{"message":"NullPointerException at Foo.java:12"}"""))

        assertEquals("text-for-SERVER", mapped.message)
    }

    @Test
    fun `a mapped http error is still an HttpException with its status`() {
        val mapped = userFacing(http(401, """{"message":"Invalid credentials"}"""))

        assertTrue(mapped is HttpException)
        assertEquals(401, (mapped as HttpException).code())
        assertTrue(mapped.isUnauthorized())
        assertEquals("Invalid credentials", mapped.message)
    }

    @Test
    fun `the error body is only read once but serverMessage still works afterwards`() {
        val original = http(422, """{"message":"Amount is too high"}""")

        val mapped = userFacing(original) as HttpException

        assertEquals("Amount is too high", mapped.serverMessage())
        assertEquals("Amount is too high", mapped.serverMessage())
    }

    @Test
    fun `serverMessage is null when there is nothing to show`() {
        assertNull(http(500, "").serverMessage())
        assertNull(http(400, "not json").serverMessage())
        assertNull(http(400, """{"message":{"nested":true}}""").serverMessage())
        assertNull(http(400, """{"code":400}""").serverMessage())
    }

    @Test
    fun `mapping is idempotent`() {
        val once = userFacing(UnknownHostException("api"))
        val twice = userFacing(once)
        val offline = NoInternetException("offline")

        assertSame(once, twice)
        assertSame(offline, userFacing(offline))
    }

    @Test
    fun `isUnauthorized only matches a 401`() {
        assertTrue(http(401).isUnauthorized())
        assertFalse(http(403).isUnauthorized())
        assertFalse(IOException("x").isUnauthorized())
        assertFalse((null as Throwable?).isUnauthorized())
    }
}
