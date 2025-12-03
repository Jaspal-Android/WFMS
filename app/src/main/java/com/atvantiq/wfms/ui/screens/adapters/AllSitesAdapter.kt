package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemSitesBinding
import com.atvantiq.wfms.models.site.allSites.AllSiteData
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.widgets.FooterRecyclerView

class AllSitesAdapter : FooterRecyclerView() {
    private val VIEW_TYPE_ITEM = 1
    private var sites: MutableList<Site>? = mutableListOf()

    inner class SitesHolder(var binding:ItemSitesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun count(): Int {
        return sites?.size ?: 0
    }

    override fun viewType(): Int {
        return VIEW_TYPE_ITEM
    }

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemSitesBinding = ItemSitesBinding.inflate(infalter, parent, false)
        return SitesHolder(binding)
    }

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SitesHolder) {
            val site = sites?.get(position)
            holder.binding.itemSiteData = site
            holder.binding.addedDate = "Added "+ DateUtils.formatApiDateToMonthDayYear(site?.createdAt)
            holder.binding.executePendingBindings()
        }
    }

    fun addData(assignedTasks: List<Site>) {
        this.sites?.addAll(assignedTasks)
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<Site>) {
        this.sites?.clear()
        this.sites?.addAll(newItems)
        notifyDataSetChanged()
    }
}