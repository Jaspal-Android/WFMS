package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemSitesBinding
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/** The Sites list, with the paging footer. */
class AllSitesAdapter : FooterListAdapter<Site>(diffById { it.id }) {

    class SitesHolder(val binding: ItemSitesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SitesHolder(ItemSitesBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: Site) {
        if (holder !is SitesHolder) return
        holder.binding.itemSiteData = item
        holder.binding.executePendingBindings()
    }
}
