package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ItemWorkSiteBinding
import com.atvantiq.wfms.models.workSites.workSites.WorkSite
import com.atvantiq.wfms.utils.DateUtils

class WorkSitesAdapter(var onTapSite:(position:Int,item:WorkSite)->Unit) : RecyclerView.Adapter<WorkSitesAdapter.Holder>() {

    private val workSties = ArrayList<WorkSite>()

    inner class Holder(val binding: ItemWorkSiteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWorkSiteBinding.inflate(inflater, parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = workSties.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val workSite = workSties[position]
        val timeRange = buildString {
            append(DateUtils.formatApiDateToTime(workSite.startTime ?: ""))
            append(" - ")
            append(DateUtils.formatApiDateToTime(workSite.endTime ?: ""))
        }
        holder.binding.timeRangeString = timeRange
        holder.binding.item = workSite
        holder.binding.root.setOnClickListener {
            onTapSite(position,workSite)
        }
        holder.binding.executePendingBindings()
    }

    fun submitData(data: List<WorkSite>) {
        workSties.clear()
        workSties.addAll(data)
        notifyDataSetChanged()
    }
}
