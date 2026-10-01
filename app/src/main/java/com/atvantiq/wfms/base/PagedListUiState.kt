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
    val isEmpty: Boolean = false
)
