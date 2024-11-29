package com.atvantiq.wfms.widgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R


abstract class FooterRecyclerView : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val FOOTER_VIEW = 0
    internal var progressbar: ProgressBar? = null
    abstract fun count(): Int
    abstract fun viewType(): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == FOOTER_VIEW) {
            FooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.footer_loader, parent, false))
        } else {
            onCreateHolderMethod(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != FOOTER_VIEW) {
            onBindViewHolderMethod(holder, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == count()) {
            FOOTER_VIEW
        } else {
            viewType()
        }
    }

    override fun getItemCount(): Int {
        return count()+1
    }

    abstract fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    abstract fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int)

    fun hideProgressBar(status: Boolean) {
        if (progressbar != null && status) {
            progressbar!!.visibility = View.GONE
        } else if (progressbar != null && !status) {
            progressbar!!.visibility = View.VISIBLE
        }
    }
	
	internal inner class FooterHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            progressbar = itemView.findViewById(R.id.bottomProgressBar)
            if (count() == 0) {
                progressbar?.visibility = View.GONE
            }
        }
    }


}
