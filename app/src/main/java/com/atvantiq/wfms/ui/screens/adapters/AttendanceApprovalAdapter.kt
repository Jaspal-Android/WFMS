package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemAttendanceApprovalBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.checkInOutLabel
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.dayLabel
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/**
 * Attendance Approval rows, with the paging footer. A row that [canMark] opens the decision
 * through [onMark]; every row opens the review screen through [onDetails].
 */
class AttendanceApprovalAdapter(
    private val canMark: (AttendanceRecord) -> Boolean,
    private val onMark: (AttendanceRecord) -> Unit,
    private val onDetails: (AttendanceRecord) -> Unit
) : FooterListAdapter<AttendanceRecord>(diffById { it.id }) {

    class Holder(val binding: ItemAttendanceApprovalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        Holder(ItemAttendanceApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: AttendanceRecord) {
        if (holder !is Holder) return
        val markable = canMark(item)
        with(holder.binding.card) {
            record = item
            dateLabel = item.dayLabel
            timeRange = item.checkInOutLabel(holder.itemView.context)
            canMark = markable
            showDetails = true
            onMark = if (markable) View.OnClickListener { this@AttendanceApprovalAdapter.onMark(item) } else null
            onDetails = View.OnClickListener { this@AttendanceApprovalAdapter.onDetails(item) }
            executePendingBindings()
        }
    }
}
