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
import com.atvantiq.wfms.databinding.ItemCalendarDayBinding
import com.atvantiq.wfms.models.calendar.AttendanceDay
import com.atvantiq.wfms.models.calendar.AttendanceStatus

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

            val background = when (day.status) {
                AttendanceStatus.PRESENT -> R.drawable.calendar_present_bg
                AttendanceStatus.ABSENT -> R.drawable.calendar_absent_bg
                AttendanceStatus.IDLE -> R.drawable.calendar_idle_bg
                else -> R.drawable.calendar_default_bg
            }
            holder.binding.tvDay.setBackgroundResource(background)

            holder.binding.root.setOnClickListener {
                onDateSelected.invoke(position, day)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(var binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
