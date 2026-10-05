package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemWorkSiteBinding
import com.atvantiq.wfms.models.workSites.workSites.WorkSite
import com.atvantiq.wfms.widgets.diffById

/** The sites an employee worked on one day; [onTapSite] opens Site Work Detail. */
class WorkSitesAdapter(private val onTapSite: (position: Int, item: WorkSite) -> Unit) :
    ListAdapter<WorkSite, WorkSitesAdapter.Holder>(diffById { it.id }) {

    class Holder(val binding: ItemWorkSiteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemWorkSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val workSite = getItem(position)
        holder.binding.item = workSite
        holder.binding.reviewRow.setOnClickListener {
            val current = holder.adapterPosition
            if (current != RecyclerView.NO_POSITION) onTapSite(current, workSite)
        }
        holder.binding.executePendingBindings()
    }
}
