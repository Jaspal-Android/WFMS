package com.atvantiq.wfms.base

/**
 * Everything a paged list screen draws, owned by its ViewModel (see [BaseViewModel.PagedList]).
 * A recreated screen redraws from this, so loaded pages and the end-of-list flag survive drawer
 * re-entry, the back stack and rotation.
 */
data class PagedListUiState<T>(
    val items: List<T> = emptyList(),
    /** Page 1 is loading and nothing is on screen yet: show the full-screen progress. */
    val isLoadingFirstPage: Boolean = false,
    /** Page 1 is reloading behind items already on screen: no spinner, the list stays usable. */
    val isRefreshing: Boolean = false,
    /** A later page is loading: show the list footer. */
    val isLoadingMore: Boolean = false,
    /** A first page arrived and it was empty. */
    val isEmpty: Boolean = false,
    /**
     * Page 1 failed (offline, timeout, server error or a rejected response) and nothing is on
     * screen: show the error state with Retry instead of a blank page. Items already on screen
     * stay when a refresh fails, so this stays false then.
     */
    val isFirstPageFailed: Boolean = false,
    /**
     * The row changed in place by [BaseViewModel.PagedList.updateItem]. The object is the same, so a
     * list diff cannot see the change; the adapter redraws this row. Only set on that update.
     */
    val changedPosition: Int? = null
)
