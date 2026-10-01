package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemWorkSubmissionBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.day
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.widgets.FooterRecyclerView

/** Work Approval rows, with the paging footer. [onReview] gets the record of the tapped row. */
class WorkSubmissionsAdapter(private val onReview: (AttendanceRecord) -> Unit) : FooterRecyclerView() {

    private val records = mutableListOf<AttendanceRecord>()

    inner class Holder(val binding: ItemWorkSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun count(): Int = records.size

    override fun viewType(): Int = VIEW_TYPE_ITEM

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        Holder(ItemWorkSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is Holder) return
        val record = records[position]
        holder.binding.item = record
        holder.binding.dateLabel = DateUtils.formatYmdLabel(record.day)
        holder.binding.reviewRow.setOnClickListener { onReview(record) }
        holder.binding.executePendingBindings()
    }

    fun submitList(items: List<AttendanceRecord>) {
        records.clear()
        records.addAll(items)
        notifyDataSetChanged()
    }

    private companion object {
        const val VIEW_TYPE_ITEM = 1
    }
}
