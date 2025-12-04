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

class WorkSitesAdapter(
    private val context: Context,
    private val role: String,
    private val onSiteApprovedReject: (status: Int, workSite: WorkSite) -> Unit
) : RecyclerView.Adapter<WorkSitesAdapter.Holder>() {

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

        val isPmPending = role.equals(ValConstants.ROLE_PM, true) && workSite.pm.status == 0
        val isOpsPending = role.equals(ValConstants.ROLE_OPS, true) && workSite.ops.status == 0
        val isAdminPending = role.equals(ValConstants.ROLE_Admin, true) &&
            (workSite.pm.status == 0 || workSite.ops.status == 0)
        holder.binding.isPending = isPmPending || isOpsPending || isAdminPending

        if (role.equals(ValConstants.ROLE_PM, true)) {
            holder.binding.approverString = when (workSite.pm.status) {
                0 -> context.getString(R.string.pending)
                1 -> context.getString(R.string.approved)
                2 -> context.getString(R.string.rejected)
                else -> ""
            }
        }

        if (role.equals(ValConstants.ROLE_OPS, true)) {
            holder.binding.showPmStatus = true
            holder.binding.pmStatusTextView.text = context.getString(R.string.pm_status) + " " +
                when (workSite.pm.status) {
                    0 -> context.getString(R.string.pending)
                    1 -> context.getString(R.string.approved)
                    2 -> context.getString(R.string.rejected)
                    else -> ""
                }
            holder.binding.pmRemarksTextView.text = context.getString(R.string.pm_remarks) + " " + (workSite.pm.remarks ?: "")
            holder.binding.approverString = when (workSite.ops.status) {
                0 -> context.getString(R.string.pending)
                1 -> context.getString(R.string.approved)
                2 -> context.getString(R.string.rejected)
                else -> ""
            }
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
