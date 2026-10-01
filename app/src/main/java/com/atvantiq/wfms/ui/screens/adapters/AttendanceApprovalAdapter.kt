package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemAttendanceApprovalBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.checkInOutLabel
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.dayLabel
import com.atvantiq.wfms.widgets.FooterRecyclerView

/**
 * Attendance Approval rows, with the paging footer. A row that [canMark] opens the decision
 * through [onMark]; every row opens the review screen through [onDetails].
 */
class AttendanceApprovalAdapter(
    private val canMark: (AttendanceRecord) -> Boolean,
    private val onMark: (AttendanceRecord) -> Unit,
    private val onDetails: (AttendanceRecord) -> Unit
) : FooterRecyclerView() {

    private val records = mutableListOf<AttendanceRecord>()

    inner class Holder(val binding: ItemAttendanceApprovalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun count(): Int = records.size

    override fun viewType(): Int = VIEW_TYPE_ITEM

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        Holder(ItemAttendanceApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is Holder) return
        val record = records[position]
        val markable = canMark(record)
        with(holder.binding.card) {
            this.record = record
            dateLabel = record.dayLabel
            timeRange = record.checkInOutLabel(holder.itemView.context)
            canMark = markable
            showDetails = true
            onMark = if (markable) View.OnClickListener { this@AttendanceApprovalAdapter.onMark(record) } else null
            onDetails = View.OnClickListener { this@AttendanceApprovalAdapter.onDetails(record) }
            executePendingBindings()
        }
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
