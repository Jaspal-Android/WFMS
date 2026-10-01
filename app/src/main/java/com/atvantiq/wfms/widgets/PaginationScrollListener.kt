package com.atvantiq.wfms.widgets

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Calls [onEndReached] when the user scrolls down to the last item of a [LinearLayoutManager]
 * list. Whether another page should actually load is the ViewModel's call (see
 * `BaseViewModel.PagedList.loadNextPage`).
 */
class PaginationScrollListener(private val onEndReached: () -> Unit) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy <= 0) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible >= 0 && firstVisible + layoutManager.childCount >= layoutManager.itemCount) {
            onEndReached()
        }
    }
}
