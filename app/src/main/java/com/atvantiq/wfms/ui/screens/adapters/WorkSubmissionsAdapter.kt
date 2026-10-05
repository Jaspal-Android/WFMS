package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemWorkSubmissionBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.day
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/** Work Approval rows, with the paging footer. [onReview] gets the record of the tapped row. */
class WorkSubmissionsAdapter(private val onReview: (AttendanceRecord) -> Unit) :
    FooterListAdapter<AttendanceRecord>(diffById { it.id }) {

    class Holder(val binding: ItemWorkSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        Holder(ItemWorkSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: AttendanceRecord) {
        if (holder !is Holder) return
        holder.binding.item = item
        holder.binding.dateLabel = DateUtils.formatYmdLabel(item.day)
        holder.binding.reviewRow.setOnClickListener { onReview(item) }
        holder.binding.executePendingBindings()
    }
}
