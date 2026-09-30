package com.atvantiq.wfms.utils

/**
 * Paging rules for a list that loads page by page. Work Management, Claims and Sites each carried
 * their own copy, with the same bugs:
 *
 *  - the page counter advanced when a page was *requested*, so a failed page was skipped for good;
 *  - a rejected or empty response never cleared the loading flag, so the list could not load more;
 *  - a response was interpreted using whatever page the screen was on when it *arrived*, so a slow
 *    answer to an old query was applied as if it belonged to the new one.
 *
 * Here the counter moves only when a page arrives, every failure path releases the lock, and a
 * response that nobody is waiting for is ignored. The screen still owns the adapter; it also
 * cancels the superseded request (see `cancelPrevious` in `BaseViewModel.executeApiCall`).
 */
class PagedListState(private val pageSize: Int) {

    /** Last page received and applied to the list; 0 before the first one. */
    var loadedPage = 0
        private set

    /** Page currently being requested, or 0 when nothing is in flight. */
    var requestedPage = 0
        private set

    var isLoading = false
        private set

    var isLastPage = false
        private set

    /** True while the first page is being fetched (after a start, refresh, search or filter change). */
    val isLoadingFirstPage: Boolean get() = isLoading && requestedPage == FIRST_PAGE

    /**
     * Starts the next page. Returns the page to request, or null when a request is already
     * running or the end of the list was reached.
     */
    fun startNextPage(): Int? {
        if (isLoading || isLastPage) return null
        requestedPage = loadedPage + 1
        isLoading = true
        return requestedPage
    }

    /** Throws away anything in flight and starts over. Returns the page to request (always 1). */
    fun restart(): Int {
        loadedPage = 0
        isLastPage = false
        requestedPage = FIRST_PAGE
        isLoading = true
        return requestedPage
    }

    /**
     * A page arrived with [itemCount] items. Returns whether it was the first page (replace the
     * list instead of appending), or null when no page was awaited, e.g. a replayed old result.
     */
    fun onPageReceived(itemCount: Int): Boolean? {
        if (!isLoading) return null
        val isFirstPage = requestedPage == FIRST_PAGE
        loadedPage = requestedPage
        isLoading = false
        if (itemCount < pageSize) isLastPage = true
        return isFirstPage
    }

    /** The request failed or was rejected. The same page is requested again next time. */
    fun onRequestFailed() {
        isLoading = false
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
