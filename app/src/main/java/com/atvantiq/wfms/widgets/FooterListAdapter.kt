package com.atvantiq.wfms.widgets

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R

/**
 * A list adapter for the paged lists: rows are diffed on a background thread, so a refresh or a
 * new page animates only what changed instead of redrawing everything, and an optional loading
 * footer sits after the last row.
 *
 * Subclasses draw one kind of row; the footer is handled here.
 */
abstract class FooterListAdapter<T : Any>(diff: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, RecyclerView.ViewHolder>(diff) {

    private var isFooterShown = false

    abstract fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder

    abstract fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: T)

    final override fun getItemCount(): Int = super.getItemCount() + if (isFooterShown) 1 else 0

    final override fun getItemViewType(position: Int): Int =
        if (isFooterShown && position == super.getItemCount()) VIEW_TYPE_FOOTER else VIEW_TYPE_ITEM

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_FOOTER) {
            FooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.footer_loader, parent, false))
        } else {
            onCreateItemHolder(parent)
        }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == VIEW_TYPE_ITEM) onBindItemHolder(holder, getItem(position))
    }

    /**
     * Shows [items]. [changedPosition] is a row that was changed in place (its object is the same,
     * so the diff cannot see it); it is redrawn once the list is applied.
     */
    fun submitList(items: List<T>, changedPosition: Int?) {
        submitList(items) { changedPosition?.let { notifyItemChanged(it) } }
    }

    /** Shows or hides the loading footer; safe to call with the same value repeatedly. */
    fun showLoadingFooter(visible: Boolean) {
        if (visible == isFooterShown) return
        val rows = super.getItemCount()
        isFooterShown = visible
        if (visible) notifyItemInserted(rows) else notifyItemRemoved(rows)
    }

    private class FooterHolder(view: android.view.View) : RecyclerView.ViewHolder(view)

    private companion object {
        const val VIEW_TYPE_ITEM = 1
        const val VIEW_TYPE_FOOTER = 0
    }
}

/**
 * Rows are the same row when [idOf] gives the same id (or they are the same object), and
 * unchanged when they are equal, so [T] must be a data class (or otherwise implement `equals`).
 * [idOf] returns null for a row without an id, which is then only ever the same row as itself.
 */
@SuppressLint("DiffUtilEquals") // every row type is a data class; see above
fun <T : Any> diffById(idOf: (T) -> Any?): DiffUtil.ItemCallback<T> = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
        oldItem === newItem || idOf(oldItem)?.let { it == idOf(newItem) } == true

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}
