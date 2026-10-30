package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemWorkAssignmentBinding
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignment
import com.atvantiq.wfms.models.workSites.workAssignments.workDate
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/** Work Approval rows, with the paging footer. [onReview] gets the assignment of the tapped row. */
class WorkAssignmentsAdapter(private val onReview: (WorkAssignment) -> Unit) :
    FooterListAdapter<WorkAssignment>(diffById { it.id }) {

    class Holder(val binding: ItemWorkAssignmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        Holder(ItemWorkAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: WorkAssignment) {
        if (holder !is Holder) return
        holder.binding.item = item
        holder.binding.dateLabel = DateUtils.formatYmdLabel(item.workDate)
        holder.binding.progressLabel = progressLabel(holder, item)
        holder.binding.reviewRow.setOnClickListener { onReview(item) }
        holder.binding.executePendingBindings()
    }

    /** "Visit 1  •  0/1 done", or whichever part the API sent (a visit of 0 means work not started). */
    private fun progressLabel(holder: Holder, item: WorkAssignment): String? {
        val context = holder.itemView.context
        val visit = item.visitNo?.takeIf { it > 0 }?.let { context.getString(R.string.work_visit_format, it) }
        val progress = item.progress?.takeIf { it.total != null && it.completed != null }
            ?.let { context.getString(R.string.work_progress_format, it.completed, it.total) }
        return listOfNotNull(visit, progress).takeIf { it.isNotEmpty() }?.joinToString(context.getString(R.string.work_detail_separator))
    }
}
