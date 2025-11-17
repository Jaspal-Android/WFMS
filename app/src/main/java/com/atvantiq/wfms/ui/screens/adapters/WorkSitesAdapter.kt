package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ItemAttendanceListBinding
import com.atvantiq.wfms.databinding.ItemWorkSiteBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.Record
import com.atvantiq.wfms.models.workSites.WorkSite
import com.atvantiq.wfms.utils.DateUtils
import com.google.android.gms.common.util.DataUtils
import com.google.errorprone.annotations.Var

class WorkSitesAdapter(
    var role: String,
    var onSiteApprovedReject: (status: Int, workSite: WorkSite) -> Unit
) : RecyclerView.Adapter<WorkSitesAdapter.Holder>() {

    private var workSties = ArrayList<WorkSite>()

    inner class Holder(var binding: ItemWorkSiteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemWorkSiteBinding = ItemWorkSiteBinding.inflate(infalter, parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return workSties.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var workSite = workSties[position]
        var timeRange = DateUtils.formatApiDateToTime(
            workSite.startTime ?: "",
        ) + " - " + DateUtils.formatApiDateToTime(
            workSite.endTime ?: "",
        )
        holder.binding.timeRangeString = timeRange
        holder.binding.item = workSite

        if (role.equals(ValConstants.ROLE_PM, true) || role.equals(ValConstants.ROLE_Admin, true) && workSite.approvedByPm) {
            holder.binding.isApproved = true
        }else if (role.equals(ValConstants.ROLE_OPS, true) || role.equals(ValConstants.ROLE_Admin, true) && workSite.approvedByOps) {
            holder.binding.isApproved = true
        } else {
            holder.binding.isApproved = false
        }

        holder.binding.btnApprove.setOnClickListener {
            onSiteApprovedReject(1, workSite)
        }
        holder.binding.btnReject.setOnClickListener {
            onSiteApprovedReject(2, workSite)
        }
        holder.binding.executePendingBindings()
    }

    fun submitData(data: List<WorkSite>) {
        workSties.clear()
        workSties.addAll(data)
        notifyDataSetChanged()
    }
}