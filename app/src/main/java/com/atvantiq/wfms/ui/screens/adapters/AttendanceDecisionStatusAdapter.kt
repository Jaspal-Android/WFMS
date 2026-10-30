package com.atvantiq.wfms.ui.screens.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemAttendanceDecisionStatusBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus

/** The 8 radio tiles of the Attendance Decision grid. */
class AttendanceDecisionStatusAdapter(
    private val onSelect: (AttendanceApprovalStatus) -> Unit
) : RecyclerView.Adapter<AttendanceDecisionStatusAdapter.Holder>() {

    private val statuses = AttendanceApprovalStatus.decisions
    private var selected: AttendanceApprovalStatus? = null

    inner class Holder(val binding: ItemAttendanceDecisionStatusBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAttendanceDecisionStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = statuses.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val status = statuses[position]
        val isSelected = status == selected
        val context = holder.itemView.context
        val background = ContextCompat.getColor(context, if (isSelected) R.color.status_present_bg else R.color.status_unmarked_bg)
        val foreground = ContextCompat.getColor(context, if (isSelected) R.color.status_present_text else R.color.status_unmarked_text)
        with(holder.binding) {
            tile.setCardBackgroundColor(background)
            tile.strokeColor = if (isSelected) foreground else background
            tile.isSelected = isSelected
            tile.contentDescription = context.getString(status.labelRes)
            tile.setOnClickListener { onSelect(status) }
            radio.setImageResource(if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked)
            ImageViewCompat.setImageTintList(radio, ColorStateList.valueOf(foreground))
            label.setText(status.labelRes)
            label.setTextColor(foreground)
        }
    }

    fun select(status: AttendanceApprovalStatus?) {
        selected = status
        notifyDataSetChanged()
    }
}
