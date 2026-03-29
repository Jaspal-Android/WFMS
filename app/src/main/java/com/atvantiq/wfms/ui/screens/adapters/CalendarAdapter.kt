package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AttendanceStatus
import com.atvantiq.wfms.databinding.ItemCalendarDayBinding
import com.atvantiq.wfms.models.calendar.AttendanceDay
import com.atvantiq.wfms.network.Status

class CalendarAdapter(
    private val context: Context,
    private val onDateSelected: (position: Int, day: AttendanceDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var days: List<AttendanceDay> = emptyList()

    fun addDays(days: List<AttendanceDay>) {
        this.days = days
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = DataBindingUtil.inflate<ItemCalendarDayBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_calendar_day,
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        with(holder.binding) {
            if (day.date.isEmpty()) {
                tvDay.text = ""
                tvDay.setBackgroundColor(context.getColor(R.color.white))
                root.setOnClickListener(null)
            } else {
                tvDay.text = day.date.substringAfterLast("-")
                val (bgColor, textColor) = getStatusColor(day.status, context)
                holder.binding.root.setBackgroundColor(ContextCompat.getColor(context, bgColor))
                holder.binding.tvDay.setTextColor(ContextCompat.getColor(context, textColor))
                root.setOnClickListener { onDateSelected(position, day) }
            }
        }
    }

    fun getStatusColor(status: String, context: Context): Pair<Int, Int> {
        return when (status) {
            AttendanceStatus.PRESENT    -> Pair(R.color.status_present_bg,    R.color.status_present_text)
            AttendanceStatus.ABSENT     -> Pair(R.color.status_absent_bg,     R.color.status_absent_text)
            AttendanceStatus.ABSENT_NA  -> Pair(R.color.status_absent_na_bg,  R.color.status_absent_na_text)
            AttendanceStatus.INCOMPLETE -> Pair(R.color.status_incomplete_bg, R.color.status_incomplete_text)
            AttendanceStatus.LEAVE      -> Pair(R.color.status_leave_bg,      R.color.status_leave_text)
            AttendanceStatus.IDLE       -> Pair(R.color.status_idle_bg,       R.color.status_idle_text)
            AttendanceStatus.WORK_OFF   -> Pair(R.color.status_work_off_bg,   R.color.status_work_off_text)
            AttendanceStatus.HOLIDAY    -> Pair(R.color.status_holiday_bg,    R.color.status_holiday_text)
            AttendanceStatus.NO_ACTION  -> Pair(R.color.status_submitted_bg,  R.color.status_submitted_text)
            else                        -> Pair(R.color.status_unmarked_bg,   R.color.status_unmarked_text)
        }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
