// BaseViewModel.kt
package com.atvantiq.wfms.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.network.ApiErrorKind
import com.atvantiq.wfms.network.ApiErrorMapper
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.PagedListState
import com.atvantiq.wfms.utils.Utils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    // Requests that may be superseded, by the LiveData they report to. Only touched on the main
    // thread (viewModelScope runs on Dispatchers.Main.immediate).
    private val supersedable = mutableMapOf<MutableLiveData<*>, Job>()

    /**
     * @param cancelPrevious for reads that a newer request replaces (search text, filter, page 1
     * after a refresh, month picker): cancels the request still running for [liveData], so a slow
     * answer to an older query can never overwrite the result of the newer one. A cancelled
     * request reports nothing. Never use it for writes: cancelling a POST mid-flight leaves the
     * user unsure whether it was applied.
     */
    open fun <T> executeApiCall(
        apiCall: suspend () -> T,
        liveData: MutableLiveData<ApiState<T>>,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
        cancelPrevious: Boolean = false
    ) {
        if (cancelPrevious) supersedable.remove(liveData)?.cancel()
        if (Utils.isInternet(getApplication())) {
            val job = viewModelScope.launch {
                liveData.postValue(ApiState.loading())
                try {
                    val response = apiCall()
                    onSuccess?.invoke(response)
                    liveData.postValue(ApiState.success(response))
                } catch (e: CancellationException) {
                    // The screen is gone (or the caller cancelled); this is not a failed request.
                    throw e
                } catch (e: Exception) {
                    val error = toUserFacingError(e)
                    onError?.invoke(error)
                    liveData.postValue(ApiState.error(error))
                }
            }
            if (cancelPrevious && job.isActive) {
                supersedable[liveData] = job
                job.invokeOnCompletion { if (supersedable[liveData] === job) supersedable.remove(liveData) }
            }
        } else {
            val error = noInternetError()
            onError?.invoke(error)
            liveData.postValue(ApiState.error(error))
        }
    }

    protected fun <S, T> executeStateRequest(
        state: MutableStateFlow<S>,
        apiCall: suspend () -> T,
        setLoading: (S) -> S,
        onSuccess: (S, T) -> S,
        onError: (S, Throwable) -> S
    ) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                state.update(setLoading)
                try {
                    val response = apiCall()
                    state.update { prev -> onSuccess(prev, response) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = toUserFacingError(e)
                    state.update { prev -> onError(prev, error) }
                }
            }
        } else {
            state.update { prev -> onError(prev, noInternetError()) }
        }
    }

    /** The offline error shown by screens; its message is localized. */
    protected fun noInternetError(): NoInternetException =
        NoInternetException(getApplication<Application>().getString(ApiErrorKind.NO_INTERNET.messageRes))

    /**
     * Screens show `throwable.message`, so every failure is converted once here into an exception
     * with a message a user can act on. HTTP failures stay `HttpException`s, so 401 handling in
     * the screens is unaffected.
     */
    private fun toUserFacingError(e: Exception): Exception {
        val error = ApiErrorMapper.userFacing(e) { kind ->
            getApplication<Application>().getString(kind.messageRes)
        }
        if (ApiErrorMapper.kindOf(error).isUnexpected) reportUnexpectedError(e)
        return error
    }

    /**
     * Failures that are neither connectivity nor a server answer (bad JSON, an unexpected
     * exception) are bugs, and the user only sees a generic message, so report the original.
     */
    protected open fun reportUnexpectedError(error: Exception) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
    }

    /**
     * A list loaded page by page and kept in the ViewModel. The screen observes [state], forwards
     * scroll-to-end, refresh and query changes, and shows [failure].
     *
     * @param fetch requests one page.
     * @param pageItems the items of a successful page, or null when the server rejected it
     * (e.g. a non-success `code`); a rejected page is reported through [failure] and retried.
     */
    inner class PagedList<R, T>(
        private val pageSize: Int = ValConstants.DEFAULT_PAGE_SIZE,
        private val fetch: suspend (page: Int, pageSize: Int) -> R,
        private val pageItems: (R) -> List<T>?
    ) {
        private val paging = PagedListState(pageSize)
        private val items = mutableListOf<T>()

        // The last page 1 request failed; cleared when the next request starts.
        private var firstPageFailed = false

        // Carries the page requests so a newer one cancels the one still running (cancelPrevious).
        private val pageResponse = MutableLiveData<ApiState<R>>()

        val state = MutableLiveData(PagedListUiState<T>())

        /**
         * A page that failed (ERROR) or that the server rejected (SUCCESS with the rejected body).
         * One-shot: observers gate on [ApiState.consumeOnce].
         */
        val failure = MutableLiveData<ApiState<R>>()

        /** Screen shown: keep what is already loaded on screen and refresh page 1 behind it. */
        fun open() = loadFirstPage(keepItems = true)

        /** Pull-to-refresh, or the list changed elsewhere (item created, edited, deleted). */
        fun refresh() = loadFirstPage(keepItems = true)

        /** The query changed (search, filter): the old items no longer apply. */
        fun reload() = loadFirstPage(keepItems = false)

        /** Scrolled to the end. Ignored while a page is loading or after the last page. */
        fun loadNextPage() {
            paging.startNextPage()?.let { request(it) }
        }

        /** Applies a change made elsewhere (e.g. a status updated on a detail screen). */
        fun updateItem(position: Int, change: (T) -> Unit) {
            items.getOrNull(position)?.let(change) ?: return
            publish(changedPosition = position)
        }

        private fun loadFirstPage(keepItems: Boolean) {
            if (!keepItems) items.clear()
            request(paging.restart())
        }

        private fun request(page: Int) {
            firstPageFailed = false
            publish()
            executeApiCall(
                apiCall = { fetch(page, pageSize) },
                liveData = pageResponse,
                onSuccess = { response -> onPageArrived(response) },
                onError = { error -> onRequestFailed(ApiState.error(error)) },
                cancelPrevious = true
            )
        }

        private fun onRequestFailed(reason: ApiState<R>) {
            firstPageFailed = paging.isLoadingFirstPage
            paging.onRequestFailed()
            publish()
            failure.value = reason
        }

        private fun onPageArrived(response: R) {
            val pageList = pageItems(response)
            if (pageList == null) {
                onRequestFailed(ApiState.success(response))
                return
            }
            val isFirstPage = paging.onPageReceived(pageList.size) ?: return
            if (isFirstPage) items.clear()
            items.addAll(pageList)
            publish()
        }

        private fun publish(changedPosition: Int? = null) {
            val loadingFirst = paging.isLoadingFirstPage
            state.value = PagedListUiState(
                items = items.toList(),
                isLoadingFirstPage = loadingFirst && items.isEmpty(),
                isRefreshing = loadingFirst && items.isNotEmpty(),
                isLoadingMore = paging.isLoading && !loadingFirst,
                isEmpty = !paging.isLoading && paging.loadedPage > 0 && items.isEmpty(),
                isFirstPageFailed = firstPageFailed && items.isEmpty(),
                changedPosition = changedPosition
            )
        }
    }
}
