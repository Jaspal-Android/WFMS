package com.atvantiq.wfms.base

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.network.ApiErrorKind
import com.atvantiq.wfms.network.ApiException
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.isUnauthorized
import com.google.gson.JsonSyntaxException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
class BaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class TestViewModel(application: Application) : BaseViewModel(application) {
        val reported = mutableListOf<Exception>()

        override fun reportUnexpectedError(error: Exception) {
            reported += error
        }

        fun <T> run(
            liveData: MutableLiveData<ApiState<T>>,
            onError: ((Exception) -> Unit)? = null,
            cancelPrevious: Boolean = false,
            onSuccess: ((T) -> Unit)? = null,
            apiCall: suspend () -> T
        ) = executeApiCall(
            apiCall = apiCall,
            liveData = liveData,
            onSuccess = onSuccess,
            onError = onError,
            cancelPrevious = cancelPrevious
        )
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: TestViewModel
    private val liveData = MutableLiveData<ApiState<String>>()

    private fun text(kind: ApiErrorKind) = "msg-${kind.messageRes}"

    private fun http(code: Int, body: String = "") =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        every { application.getString(any()) } answers { "msg-${firstArg<Int>()}" }
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = TestViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun failWith(error: Exception, onError: ((Exception) -> Unit)? = null): ApiState<String> {
        viewModel.run(liveData, onError) { throw error }
        testDispatcher.scheduler.advanceUntilIdle()
        return liveData.value!!
    }

    @Test
    fun `a successful call posts success`() {
        viewModel.run(liveData) { "ok" }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Status.SUCCESS, liveData.value?.status)
        assertEquals("ok", liveData.value?.response)
    }

    @Test
    fun `a timeout reaches the screen as a readable message`() {
        val state = failWith(SocketTimeoutException("SSL handshake timed out"))

        assertEquals(Status.ERROR, state.status)
        assertEquals(text(ApiErrorKind.TIMEOUT), state.throwable?.message)
        assertTrue(viewModel.reported.isEmpty())
    }

    @Test
    fun `no route to the server reads as a connection problem and is not reported`() {
        val state = failWith(UnknownHostException("Unable to resolve host api.example.com"))

        assertEquals(text(ApiErrorKind.NO_INTERNET), state.throwable?.message)
        assertTrue(viewModel.reported.isEmpty())
    }

    @Test
    fun `a 401 stays an HttpException so screens can still show the session dialog`() {
        val state = failWith(http(401))

        assertTrue(state.throwable.isUnauthorized())
        assertEquals(401, (state.throwable as HttpException).code())
    }

    @Test
    fun `a business-rule 4xx shows the backend message`() {
        val state = failWith(http(400, """{"message":"Claim is locked"}"""))

        assertEquals("Claim is locked", state.throwable?.message)
    }

    @Test
    fun `a 5xx shows the generic server text`() {
        val state = failWith(http(500, """{"message":"stack trace here"}"""))

        assertEquals(text(ApiErrorKind.SERVER), state.throwable?.message)
        assertTrue(viewModel.reported.isEmpty())
    }

    @Test
    fun `unreadable json is reported and shows a generic message`() {
        val error = JsonSyntaxException("Expected BEGIN_OBJECT but was STRING")

        val state = failWith(error)

        assertEquals(text(ApiErrorKind.MALFORMED), state.throwable?.message)
        assertEquals(listOf<Exception>(error), viewModel.reported)
    }

    @Test
    fun `an unexpected exception is reported with the original, not the wrapper`() {
        val bug = IllegalStateException("boom")

        val state = failWith(bug)

        assertEquals(text(ApiErrorKind.UNKNOWN), state.throwable?.message)
        assertSame(bug, viewModel.reported.single())
        assertSame(bug, (state.throwable as ApiException).cause)
    }

    @Test
    fun `onError gets the same readable exception the screen gets`() {
        var received: Exception? = null

        val state = failWith(SocketTimeoutException("x")) { received = it }

        assertSame(state.throwable, received)
        assertEquals(text(ApiErrorKind.TIMEOUT), received?.message)
    }

    @Test
    fun `a cancelled call is not reported as a failure`() {
        var onErrorCalled = false
        viewModel.run(liveData, onError = { onErrorCalled = true }) { throw CancellationException("screen closed") }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Status.LOADING, liveData.value?.status)
        assertEquals(false, onErrorCalled)
        assertTrue(viewModel.reported.isEmpty())
    }

    @Test
    fun `offline calls onError and posts a localized no internet error`() {
        every { Utils.isInternet(application) } returns false
        var received: Exception? = null

        viewModel.run(liveData, onError = { received = it }) { "never runs" }

        val error = liveData.value?.throwable
        assertEquals(Status.ERROR, liveData.value?.status)
        assertTrue(error is NoInternetException)
        assertEquals(text(ApiErrorKind.NO_INTERNET), error?.message)
        assertSame(error, received)
        assertNotNull(received)
    }

    // ---- superseded requests (search text, filter, refresh, month picker) ----

    @Test
    fun `a newer request cancels the slow one before it can overwrite the result`() {
        val slowAnswer = CompletableDeferred<String>()
        val delivered = mutableListOf<String>()
        viewModel.run(liveData, cancelPrevious = true, onSuccess = { delivered += it }) { slowAnswer.await() }
        testDispatcher.scheduler.advanceUntilIdle() // the first request is now waiting on the network

        viewModel.run(liveData, cancelPrevious = true, onSuccess = { delivered += it }) { "new query" }
        testDispatcher.scheduler.advanceUntilIdle()
        slowAnswer.complete("old query") // the old answer finally arrives
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("new query", liveData.value?.response)
        assertEquals(Status.SUCCESS, liveData.value?.status)
        assertEquals(listOf("new query"), delivered)
    }

    @Test
    fun `a superseded request reports neither a result nor an error`() {
        val slowAnswer = CompletableDeferred<String>()
        var errorReported = false
        viewModel.run(liveData, onError = { errorReported = true }, cancelPrevious = true) { slowAnswer.await() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.run(liveData, cancelPrevious = true) { "new" }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, errorReported)
        assertTrue(viewModel.reported.isEmpty())
    }

    @Test
    fun `without cancelPrevious both requests still complete`() {
        val first = CompletableDeferred<String>()
        val delivered = mutableListOf<String>()
        viewModel.run(liveData, onSuccess = { delivered += it }) { first.await() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.run(liveData, onSuccess = { delivered += it }) { "second" }
        testDispatcher.scheduler.advanceUntilIdle()
        first.complete("first")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("second", "first"), delivered)
    }

    @Test
    fun `cancelling only affects requests for the same LiveData`() {
        val other = MutableLiveData<ApiState<String>>()
        val otherAnswer = CompletableDeferred<String>()
        viewModel.run(other, cancelPrevious = true) { otherAnswer.await() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.run(liveData, cancelPrevious = true) { "unrelated" }
        testDispatcher.scheduler.advanceUntilIdle()
        otherAnswer.complete("still delivered")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("still delivered", other.value?.response)
        assertEquals("unrelated", liveData.value?.response)
    }

    @Test
    fun `a newer request that finds no connection still supersedes the slow one`() {
        val slowAnswer = CompletableDeferred<String>()
        viewModel.run(liveData, cancelPrevious = true) { slowAnswer.await() }
        testDispatcher.scheduler.advanceUntilIdle()

        every { Utils.isInternet(application) } returns false
        viewModel.run(liveData, cancelPrevious = true) { "never runs" }
        slowAnswer.complete("old query")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Status.ERROR, liveData.value?.status)
        assertTrue(liveData.value?.throwable is NoInternetException)
    }

    @Test
    fun `a request can be superseded again after the previous one finished`() {
        viewModel.run(liveData, cancelPrevious = true) { "first" }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("first", liveData.value?.response)

        viewModel.run(liveData, cancelPrevious = true) { "second" }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("second", liveData.value?.response)
    }
}
