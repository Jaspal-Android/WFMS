package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentAttendanceStatusBinding
import com.atvantiq.wfms.models.calendar.AttendanceDay
import com.atvantiq.wfms.models.calendar.AttendanceStatus
import com.atvantiq.wfms.ui.screens.adapters.CalendarAdapter
import com.atvantiq.wfms.widgets.CalendarView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * A simple [Fragment] subclass.
 * Use the [AttendanceStatusFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class AttendanceStatusFragment : BaseFragment<FragmentAttendanceStatusBinding,AttendanceStatusVM>() {

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance_status,AttendanceStatusVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        binding.customCalender.setCustomCalendarEventHandler(object :CalendarView.CustomCalendarEventHandler{
            override fun onDayClickListener(position: Int, day: AttendanceDay) {

            }

            override fun onPrevMonthClickListener(calendar: Calendar?) {

            }

            override fun onNextMonthClickListener(calendar: Calendar?) {

            }
        })


    }

    override fun subscribeToEvents(vm: AttendanceStatusVM) {

    }

}