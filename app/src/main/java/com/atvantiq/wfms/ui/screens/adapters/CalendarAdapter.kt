package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AttendanceStatus
import com.atvantiq.wfms.databinding.ItemCalendarDayBinding
import com.atvantiq.wfms.models.calendar.AttendanceDay

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
                tvDay.setBackgroundColor(
                    when (day.status) {
                        AttendanceStatus.NO_ACTION -> context.getColor(R.color.lightGray)
                        AttendanceStatus.PRESENT -> context.getColor(R.color.green_pastel)
                        AttendanceStatus.ABSENT -> context.getColor(R.color.red_pastel)
                        AttendanceStatus.LEAVE -> context.getColor(R.color.primary_pastal)
                        AttendanceStatus.IDLE -> context.getColor(R.color.orange_pastal)
                        AttendanceStatus.HOLIDAY -> context.getColor(R.color.yellow_pastal)
                        AttendanceStatus.WORK_OFF -> context.getColor(R.color.purple_pastal)
                        else -> context.getColor(R.color.lightGray)
                    }
                )
                root.setOnClickListener { onDateSelected(position, day) }
            }
        }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
