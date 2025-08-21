package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AttendanceStatus
import com.atvantiq.wfms.databinding.ItemCalendarDayBinding
import com.atvantiq.wfms.models.calendar.AttendanceDay

class CalendarAdapter(
    private var context: Context,
    var onDateSelected: (position: Int, day: AttendanceDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var days: List<AttendanceDay> = ArrayList()

    fun addDays(days: List<AttendanceDay>) {
        this.days = days
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        var inflater = LayoutInflater.from(parent.context)
        var binding: ItemCalendarDayBinding =
            DataBindingUtil.inflate(inflater, R.layout.item_calendar_day, parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        if (day.date.isEmpty()) {
            holder.binding.tvDay.text = ""
            holder.binding.tvDay.setBackgroundColor(context.getColor(R.color.white))
        } else {
            holder.binding.tvDay.text = day.date.split("-").last() // Show only day part

            // Set background color based on status
            val backgroundColor = when (day.status) {
                AttendanceStatus.NO_ACTION -> context.getColor(R.color.lightGray)
                AttendanceStatus.PRESENT -> context.getColor(R.color.green_pastel)
                AttendanceStatus.ABSENT -> context.getColor(R.color.red_pastel)
                AttendanceStatus.LEAVE -> context.getColor(R.color.primary_pastal)
                AttendanceStatus.IDLE -> context.getColor(R.color.orange_pastal)
                AttendanceStatus.HOLIDAY -> context.getColor(R.color.yellow_pastal)
                AttendanceStatus.WORK_OFF -> context.getColor(R.color.purple_pastal)
                else -> context.getColor(R.color.lightGray)
            }
            holder.binding.tvDay.setBackgroundColor(backgroundColor)

            holder.binding.root.setOnClickListener {
                onDateSelected.invoke(position, day)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(var binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
