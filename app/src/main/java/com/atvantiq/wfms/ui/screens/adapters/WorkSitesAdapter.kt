package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemWorkSiteBinding
import com.atvantiq.wfms.models.workSites.workSites.WorkSite

/** The sites an employee worked on one day; [onTapSite] opens Site Work Detail. */
class WorkSitesAdapter(private val onTapSite: (position: Int, item: WorkSite) -> Unit) :
    RecyclerView.Adapter<WorkSitesAdapter.Holder>() {

    private val workSites = ArrayList<WorkSite>()

    inner class Holder(val binding: ItemWorkSiteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemWorkSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = workSites.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val workSite = workSites[position]
        holder.binding.item = workSite
        holder.binding.reviewRow.setOnClickListener { onTapSite(position, workSite) }
        holder.binding.executePendingBindings()
    }

    fun submitData(data: List<WorkSite>) {
        workSites.clear()
        workSites.addAll(data)
        notifyDataSetChanged()
    }
}
